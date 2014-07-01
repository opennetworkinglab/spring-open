package net.onrc.onos.core.topology;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.concurrent.GuardedBy;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.datagrid.IDatagridService;
import net.onrc.onos.core.datagrid.IEventChannel;
import net.onrc.onos.core.datagrid.IEventChannelListener;
import net.onrc.onos.core.datastore.topology.KVDevice;
import net.onrc.onos.core.datastore.topology.KVLink;
import net.onrc.onos.core.datastore.topology.KVPort;
import net.onrc.onos.core.datastore.topology.KVSwitch;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.EventEntry;
import net.onrc.onos.core.util.SwitchPort;
import net.onrc.onos.core.util.serializers.KryoFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;

/**
 * The TopologyManager receives topology updates from the southbound discovery
 * modules and from other ONOS instances. These updates are processed and
 * applied to the in-memory topology instance.
 * <p/>
 * - Maintain Invariant/Relationships between Topology Objects.
 * <p/>
 * TODO To be synchronized based on TopologyEvent Notification.
 * <p/>
 * TODO TBD: Caller is expected to maintain parent/child calling order. Parent
 * Object must exist before adding sub component(Add Switch -> Port).
 * <p/>
 * TODO TBD: This class may delay the requested change to handle event
 * re-ordering. e.g.) Link Add came in, but Switch was not there.
 */
public class TopologyManager implements TopologyDiscoveryInterface {

    private static final Logger log = LoggerFactory
            .getLogger(TopologyManager.class);

    private IEventChannel<byte[], TopologyEvent> eventChannel;
    public static final String EVENT_CHANNEL_NAME = "onos.topology";
    private EventHandler eventHandler = new EventHandler();

    private final TopologyDatastore datastore;
    private final TopologyImpl topology = new TopologyImpl();
    private final IControllerRegistryService registryService;
    private CopyOnWriteArrayList<ITopologyListener> topologyListeners;
    private Kryo kryo = KryoFactory.newKryoObject();

    //
    // Local state for keeping track of reordered events.
    // NOTE: Switch Events are not affected by the event reordering.
    //
    private Map<ByteBuffer, PortEvent> reorderedAddedPortEvents =
            new HashMap<ByteBuffer, PortEvent>();
    private Map<ByteBuffer, LinkEvent> reorderedAddedLinkEvents =
            new HashMap<ByteBuffer, LinkEvent>();
    private Map<ByteBuffer, DeviceEvent> reorderedAddedDeviceEvents =
            new HashMap<ByteBuffer, DeviceEvent>();

    //
    // Local state for keeping track of locally discovered events so we can
    // cleanup properly when a Switch or Port is removed.
    //
    // We keep all Port, (incoming) Link and Device events per Switch DPID:
    //  - If a switch goes down, we remove all corresponding Port, Link and
    //    Device events.
    //  - If a port on a switch goes down, we remove all corresponding Link
    //    and Device events discovered by this instance.
    //
    // How to handle side-effect of remote events.
    //  - Remote Port Down event -> Link Down
    //      Not handled. (XXX Shouldn't it be removed from discovered.. Map)
    //  - Remote Device Added -> lose ownership of Device)
    //      Not handled. (XXX Shouldn't it be removed from discovered.. Map)
    //
    // XXX Domain knowledge based invariant maintenance should be moved to
    //     driver module, since the invariant may be different on optical, etc.
    //
    // What happens on leadership change?
    //  - Probably should: remove from discovered.. Maps, but not send DELETE events
    //     XXX Switch/Port can be rediscovered by new leader, but Link, Device?
    //  - Current: There is no way to recognize leadership change?
    //      ZookeeperRegistry.requestControl(long, ControlChangeCallback)
    //      is the only way to register listener, and it allows only 1 listener,
    //      which is already used by Controller class.
    //
    // FIXME Replace with concurrent variant.
    //   #removeSwitchDiscoveryEvent(SwitchEvent) runs in different thread.
    //
    private Map<Dpid, Map<ByteBuffer, PortEvent>> discoveredAddedPortEvents =
            new HashMap<>();
    private Map<Dpid, Map<ByteBuffer, LinkEvent>> discoveredAddedLinkEvents =
            new HashMap<>();
    private Map<Dpid, Map<ByteBuffer, DeviceEvent>> discoveredAddedDeviceEvents =
            new HashMap<>();

    //
    // Local state for keeping track of the application event notifications
    //
    //  - Queue of events, which will be dispatched to local listeners
    //    on next notification.

    private List<SwitchEvent> apiAddedSwitchEvents = new LinkedList<>();
    private List<SwitchEvent> apiRemovedSwitchEvents = new LinkedList<>();
    private List<PortEvent> apiAddedPortEvents = new LinkedList<>();
    private List<PortEvent> apiRemovedPortEvents = new LinkedList<>();
    private List<LinkEvent> apiAddedLinkEvents = new LinkedList<>();
    private List<LinkEvent> apiRemovedLinkEvents = new LinkedList<>();
    private List<DeviceEvent> apiAddedDeviceEvents = new LinkedList<>();
    private List<DeviceEvent> apiRemovedDeviceEvents = new LinkedList<>();

    /**
     * Constructor.
     *
     * @param registryService the Registry Service to use.
     * @param topologyListeners the collection of topology listeners to use.
     */
    public TopologyManager(IControllerRegistryService registryService,
                           CopyOnWriteArrayList<ITopologyListener> topologyListeners) {
        datastore = new TopologyDatastore();
        this.registryService = registryService;
        this.topologyListeners = topologyListeners;
    }

    /**
     * Get the Topology.
     *
     * @return the Topology.
     */
    Topology getTopology() {
        return topology;
    }

    /**
     * Event handler class.
     */
    private class EventHandler extends Thread implements
            IEventChannelListener<byte[], TopologyEvent> {
        private BlockingQueue<EventEntry<TopologyEvent>> topologyEvents =
                new LinkedBlockingQueue<EventEntry<TopologyEvent>>();

        /**
         * Startup processing.
         */
        private void startup() {
            //
            // TODO: Read all state from the database:
            //
            // Collection<EventEntry<TopologyEvent>> collection =
            //    readWholeTopologyFromDB();
            //
            // For now, as a shortcut we read it from the datagrid
            //
            Collection<TopologyEvent> allTopologyEvents =
                    eventChannel.getAllEntries();
            Collection<EventEntry<TopologyEvent>> collection =
                    new LinkedList<EventEntry<TopologyEvent>>();

            for (TopologyEvent topologyEvent : allTopologyEvents) {
                EventEntry<TopologyEvent> eventEntry =
                        new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                                topologyEvent);
                collection.add(eventEntry);
            }
            processEvents(collection);
        }

        /**
         * Run the thread.
         */
        @Override
        public void run() {
            Collection<EventEntry<TopologyEvent>> collection =
                    new LinkedList<EventEntry<TopologyEvent>>();

            this.setName("TopologyManager.EventHandler " + this.getId());
            startup();

            //
            // The main loop
            //
            while (true) {
                try {
                    EventEntry<TopologyEvent> eventEntry =
                        topologyEvents.take();
                    collection.add(eventEntry);
                    topologyEvents.drainTo(collection);

                    processEvents(collection);
                    collection.clear();
                } catch (Exception exception) {
                    log.debug("Exception processing Topology Events: ",
                              exception);
                }
            }
        }

        /**
         * Process all topology events.
         *
         * @param events the events to process.
         */
        private void processEvents(Collection<EventEntry<TopologyEvent>> events) {
            // Local state for computing the final set of events
            Map<ByteBuffer, SwitchEvent> addedSwitchEvents = new HashMap<>();
            Map<ByteBuffer, SwitchEvent> removedSwitchEvents = new HashMap<>();
            Map<ByteBuffer, PortEvent> addedPortEvents = new HashMap<>();
            Map<ByteBuffer, PortEvent> removedPortEvents = new HashMap<>();
            Map<ByteBuffer, LinkEvent> addedLinkEvents = new HashMap<>();
            Map<ByteBuffer, LinkEvent> removedLinkEvents = new HashMap<>();
            Map<ByteBuffer, DeviceEvent> addedDeviceEvents = new HashMap<>();
            Map<ByteBuffer, DeviceEvent> removedDeviceEvents = new HashMap<>();

            //
            // Classify and suppress matching events
            //
            for (EventEntry<TopologyEvent> event : events) {
                TopologyEvent topologyEvent = event.eventData();
                SwitchEvent switchEvent = topologyEvent.switchEvent;
                PortEvent portEvent = topologyEvent.portEvent;
                LinkEvent linkEvent = topologyEvent.linkEvent;
                DeviceEvent deviceEvent = topologyEvent.deviceEvent;

                //
                // Extract the events
                //
                switch (event.eventType()) {
                    case ENTRY_ADD:
                        log.debug("Topology event ENTRY_ADD: {}", topologyEvent);
                        if (switchEvent != null) {
                            ByteBuffer id = switchEvent.getIDasByteBuffer();
                            addedSwitchEvents.put(id, switchEvent);
                            removedSwitchEvents.remove(id);
                            // Switch Events are not affected by event reordering
                        }
                        if (portEvent != null) {
                            ByteBuffer id = portEvent.getIDasByteBuffer();
                            addedPortEvents.put(id, portEvent);
                            removedPortEvents.remove(id);
                            reorderedAddedPortEvents.remove(id);
                        }
                        if (linkEvent != null) {
                            ByteBuffer id = linkEvent.getIDasByteBuffer();
                            addedLinkEvents.put(id, linkEvent);
                            removedLinkEvents.remove(id);
                            reorderedAddedLinkEvents.remove(id);
                        }
                        if (deviceEvent != null) {
                            ByteBuffer id = deviceEvent.getIDasByteBuffer();
                            addedDeviceEvents.put(id, deviceEvent);
                            removedDeviceEvents.remove(id);
                            reorderedAddedDeviceEvents.remove(id);
                        }
                        break;
                    case ENTRY_REMOVE:
                        log.debug("Topology event ENTRY_REMOVE: {}", topologyEvent);
                        if (switchEvent != null) {
                            ByteBuffer id = switchEvent.getIDasByteBuffer();
                            addedSwitchEvents.remove(id);
                            removedSwitchEvents.put(id, switchEvent);
                            // Switch Events are not affected by event reordering
                        }
                        if (portEvent != null) {
                            ByteBuffer id = portEvent.getIDasByteBuffer();
                            addedPortEvents.remove(id);
                            removedPortEvents.put(id, portEvent);
                            reorderedAddedPortEvents.remove(id);
                        }
                        if (linkEvent != null) {
                            ByteBuffer id = linkEvent.getIDasByteBuffer();
                            addedLinkEvents.remove(id);
                            removedLinkEvents.put(id, linkEvent);
                            reorderedAddedLinkEvents.remove(id);
                        }
                        if (deviceEvent != null) {
                            ByteBuffer id = deviceEvent.getIDasByteBuffer();
                            addedDeviceEvents.remove(id);
                            removedDeviceEvents.put(id, deviceEvent);
                            reorderedAddedDeviceEvents.remove(id);
                        }
                        break;
                    default:
                        log.error("Unknown topology event {}",
                                  event.eventType());
                }
            }

            //
            // Lock the topology while it is modified
            //
            topology.acquireWriteLock();

            try {
                //
                // Apply the classified events.
                //
                // Apply the "add" events in the proper order:
                //   switch, port, link, device
                //
                for (SwitchEvent switchEvent : addedSwitchEvents.values()) {
                    addSwitch(switchEvent);
                }
                for (PortEvent portEvent : addedPortEvents.values()) {
                    addPort(portEvent);
                }
                for (LinkEvent linkEvent : addedLinkEvents.values()) {
                    addLink(linkEvent);
                }
                for (DeviceEvent deviceEvent : addedDeviceEvents.values()) {
                    addDevice(deviceEvent);
                }
                //
                // Apply the "remove" events in the reverse order:
                //   device, link, port, switch
                //
                for (DeviceEvent deviceEvent : removedDeviceEvents.values()) {
                    removeDevice(deviceEvent);
                }
                for (LinkEvent linkEvent : removedLinkEvents.values()) {
                    removeLink(linkEvent);
                }
                for (PortEvent portEvent : removedPortEvents.values()) {
                    removePort(portEvent);
                }
                for (SwitchEvent switchEvent : removedSwitchEvents.values()) {
                    removeSwitch(switchEvent);
                }

                //
                // Apply reordered events
                //
                applyReorderedEvents(!addedSwitchEvents.isEmpty(),
                        !addedPortEvents.isEmpty());

            } finally {
                //
                // Topology modifications completed: Release the lock
                //
                topology.releaseWriteLock();
            }

            //
            // Dispatch the Topology Notification Events to the applications
            //
            dispatchTopologyEvents();
        }

        /**
         * Receive a notification that an entry is added.
         *
         * @param value the value for the entry.
         */
        @Override
        public void entryAdded(TopologyEvent value) {
            EventEntry<TopologyEvent> eventEntry =
                    new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                            value);
            topologyEvents.add(eventEntry);
        }

        /**
         * Receive a notification that an entry is removed.
         *
         * @param value the value for the entry.
         */
        @Override
        public void entryRemoved(TopologyEvent value) {
            EventEntry<TopologyEvent> eventEntry =
                    new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_REMOVE,
                            value);
            topologyEvents.add(eventEntry);
        }

        /**
         * Receive a notification that an entry is updated.
         *
         * @param value the value for the entry.
         */
        @Override
        public void entryUpdated(TopologyEvent value) {
            // NOTE: The ADD and UPDATE events are processed in same way
            entryAdded(value);
        }
    }

    /**
     * Startup processing.
     *
     * @param datagridService the datagrid service to use.
     */
    void startup(IDatagridService datagridService) {
        eventChannel = datagridService.addListener(EVENT_CHANNEL_NAME,
                eventHandler,
                byte[].class,
                TopologyEvent.class);
        eventHandler.start();
    }

    /**
     * Dispatch Topology Events to the listeners.
     */
    private void dispatchTopologyEvents() {
        if (apiAddedSwitchEvents.isEmpty() &&
                apiRemovedSwitchEvents.isEmpty() &&
                apiAddedPortEvents.isEmpty() &&
                apiRemovedPortEvents.isEmpty() &&
                apiAddedLinkEvents.isEmpty() &&
                apiRemovedLinkEvents.isEmpty() &&
                apiAddedDeviceEvents.isEmpty() &&
                apiRemovedDeviceEvents.isEmpty()) {
            return;        // No events to dispatch
        }

        if (log.isDebugEnabled()) {
            //
            // Debug statements
            // TODO: Those statements should be removed in the future
            //
            for (SwitchEvent switchEvent : apiAddedSwitchEvents) {
                log.debug("Dispatch Topology Event: ADDED {}", switchEvent);
            }
            for (SwitchEvent switchEvent : apiRemovedSwitchEvents) {
                log.debug("Dispatch Topology Event: REMOVED {}", switchEvent);
            }
            for (PortEvent portEvent : apiAddedPortEvents) {
                log.debug("Dispatch Topology Event: ADDED {}", portEvent);
            }
            for (PortEvent portEvent : apiRemovedPortEvents) {
                log.debug("Dispatch Topology Event: REMOVED {}", portEvent);
            }
            for (LinkEvent linkEvent : apiAddedLinkEvents) {
                log.debug("Dispatch Topology Event: ADDED {}", linkEvent);
            }
            for (LinkEvent linkEvent : apiRemovedLinkEvents) {
                log.debug("Dispatch Topology Event: REMOVED {}", linkEvent);
            }
            for (DeviceEvent deviceEvent : apiAddedDeviceEvents) {
                log.debug("Dispatch Topology Event: ADDED {}", deviceEvent);
            }
            for (DeviceEvent deviceEvent : apiRemovedDeviceEvents) {
                log.debug("Dispatch Topology Event: REMOVED {}", deviceEvent);
            }
        }

        // Deliver the events
        long timestamp = System.nanoTime();
        for (ITopologyListener listener : this.topologyListeners) {
            TopologyEvents events =
                new TopologyEvents(timestamp,
                                   kryo.copy(apiAddedSwitchEvents),
                                   kryo.copy(apiRemovedSwitchEvents),
                                   kryo.copy(apiAddedPortEvents),
                                   kryo.copy(apiRemovedPortEvents),
                                   kryo.copy(apiAddedLinkEvents),
                                   kryo.copy(apiRemovedLinkEvents),
                                   kryo.copy(apiAddedDeviceEvents),
                                   kryo.copy(apiRemovedDeviceEvents));
            listener.topologyEvents(events);
        }

        //
        // Cleanup
        //
        apiAddedSwitchEvents.clear();
        apiRemovedSwitchEvents.clear();
        apiAddedPortEvents.clear();
        apiRemovedPortEvents.clear();
        apiAddedLinkEvents.clear();
        apiRemovedLinkEvents.clear();
        apiAddedDeviceEvents.clear();
        apiRemovedDeviceEvents.clear();
    }

    /**
     * Apply reordered events.
     *
     * @param hasAddedSwitchEvents true if there were Added Switch Events.
     * @param hasAddedPortEvents   true if there were Added Port Events.
     */
    @GuardedBy("topology.writeLock")
    private void applyReorderedEvents(boolean hasAddedSwitchEvents,
                                      boolean hasAddedPortEvents) {
        if (!(hasAddedSwitchEvents || hasAddedPortEvents)) {
            return;        // Nothing to do
        }

        //
        // Try to apply the reordered events.
        //
        // NOTE: For simplicity we try to apply all events of a particular
        // type if any "parent" type event was processed:
        //  - Apply reordered Port Events if Switches were added
        //  - Apply reordered Link and Device Events if Switches or Ports
        //    were added
        //

        //
        // Apply reordered Port Events if Switches were added
        //
        if (hasAddedSwitchEvents) {
            Map<ByteBuffer, PortEvent> portEvents = reorderedAddedPortEvents;
            reorderedAddedPortEvents = new HashMap<>();
            for (PortEvent portEvent : portEvents.values()) {
                addPort(portEvent);
            }
        }
        //
        // Apply reordered Link and Device Events if Switches or Ports
        // were added.
        //
        Map<ByteBuffer, LinkEvent> linkEvents = reorderedAddedLinkEvents;
        reorderedAddedLinkEvents = new HashMap<>();
        for (LinkEvent linkEvent : linkEvents.values()) {
            addLink(linkEvent);
        }
        //
        Map<ByteBuffer, DeviceEvent> deviceEvents = reorderedAddedDeviceEvents;
        reorderedAddedDeviceEvents = new HashMap<>();
        for (DeviceEvent deviceEvent : deviceEvents.values()) {
            addDevice(deviceEvent);
        }
    }

    /**
     * Switch discovered event.
     *
     * @param switchEvent the switch event.
     * @param portEvents  the corresponding port events for the switch.
     */
    @Override
    public void putSwitchDiscoveryEvent(SwitchEvent switchEvent,
                                        Collection<PortEvent> portEvents) {
        if (datastore.addSwitch(switchEvent, portEvents)) {
            log.debug("Sending add switch: {}", switchEvent);
            // Send out notification
            TopologyEvent topologyEvent = new TopologyEvent(switchEvent);
            eventChannel.addEntry(topologyEvent.getID(), topologyEvent);

            // Send out notification for each port
            for (PortEvent portEvent : portEvents) {
                log.debug("Sending add port: {}", portEvent);
                topologyEvent = new TopologyEvent(portEvent);
                eventChannel.addEntry(topologyEvent.getID(), topologyEvent);
            }

            //
            // Keep track of the added ports
            //
            // Get the old Port Events
            Map<ByteBuffer, PortEvent> oldPortEvents =
                    discoveredAddedPortEvents.get(switchEvent.getDpid());
            if (oldPortEvents == null) {
                oldPortEvents = new HashMap<>();
            }

            // Store the new Port Events in the local cache
            Map<ByteBuffer, PortEvent> newPortEvents = new HashMap<>();
            for (PortEvent portEvent : portEvents) {
                ByteBuffer id = portEvent.getIDasByteBuffer();
                newPortEvents.put(id, portEvent);
            }
            discoveredAddedPortEvents.put(switchEvent.getDpid(),
                    newPortEvents);

            //
            // Extract the removed ports
            //
            List<PortEvent> removedPortEvents = new LinkedList<>();
            for (Map.Entry<ByteBuffer, PortEvent> entry : oldPortEvents.entrySet()) {
                ByteBuffer key = entry.getKey();
                PortEvent portEvent = entry.getValue();
                if (!newPortEvents.containsKey(key)) {
                    removedPortEvents.add(portEvent);
                }
            }

            // Cleanup old removed ports
            for (PortEvent portEvent : removedPortEvents) {
                removePortDiscoveryEvent(portEvent);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Called by {@link TopologyPublisher.SwitchCleanup} thread.
     */
    @Override
    public void removeSwitchDiscoveryEvent(SwitchEvent switchEvent) {
        // Get the old Port Events
        Map<ByteBuffer, PortEvent> oldPortEvents =
                discoveredAddedPortEvents.get(switchEvent.getDpid());
        if (oldPortEvents == null) {
            oldPortEvents = new HashMap<>();
        }

        if (datastore.deactivateSwitch(switchEvent, oldPortEvents.values())) {
            log.debug("Sending remove switch: {}", switchEvent);
            // Send out notification
            eventChannel.removeEntry(switchEvent.getID());

            //
            // Send out notification for each port.
            //
            // NOTE: We don't use removePortDiscoveryEvent() for the cleanup,
            // because it will attempt to remove the port from the database,
            // and the deactiveSwitch() call above already removed all ports.
            //
            for (PortEvent portEvent : oldPortEvents.values()) {
                log.debug("Sending remove port:", portEvent);
                eventChannel.removeEntry(portEvent.getID());
            }
            discoveredAddedPortEvents.remove(switchEvent.getDpid());

            // Cleanup for each link
            Map<ByteBuffer, LinkEvent> oldLinkEvents =
                    discoveredAddedLinkEvents.get(switchEvent.getDpid());
            if (oldLinkEvents != null) {
                for (LinkEvent linkEvent : new ArrayList<>(oldLinkEvents.values())) {
                    removeLinkDiscoveryEvent(linkEvent);
                }
                discoveredAddedLinkEvents.remove(switchEvent.getDpid());
            }

            // Cleanup for each device
            Map<ByteBuffer, DeviceEvent> oldDeviceEvents =
                    discoveredAddedDeviceEvents.get(switchEvent.getDpid());
            if (oldDeviceEvents != null) {
                for (DeviceEvent deviceEvent : new ArrayList<>(oldDeviceEvents.values())) {
                    removeDeviceDiscoveryEvent(deviceEvent);
                }
                discoveredAddedDeviceEvents.remove(switchEvent.getDpid());
            }
        }
    }

    /**
     * Port discovered event.
     *
     * @param portEvent the port event.
     */
    @Override
    public void putPortDiscoveryEvent(PortEvent portEvent) {
        if (datastore.addPort(portEvent)) {
            log.debug("Sending add port: {}", portEvent);
            // Send out notification
            TopologyEvent topologyEvent = new TopologyEvent(portEvent);
            eventChannel.addEntry(topologyEvent.getID(), topologyEvent);

            // Store the new Port Event in the local cache
            Map<ByteBuffer, PortEvent> oldPortEvents =
                    discoveredAddedPortEvents.get(portEvent.getDpid());
            if (oldPortEvents == null) {
                oldPortEvents = new HashMap<>();
                discoveredAddedPortEvents.put(portEvent.getDpid(),
                        oldPortEvents);
            }
            ByteBuffer id = portEvent.getIDasByteBuffer();
            oldPortEvents.put(id, portEvent);
        }
    }

    /**
     * Port removed event.
     *
     * @param portEvent the port event.
     */
    @Override
    public void removePortDiscoveryEvent(PortEvent portEvent) {
        if (datastore.deactivatePort(portEvent)) {
            log.debug("Sending remove port: {}", portEvent);
            // Send out notification
            eventChannel.removeEntry(portEvent.getID());

            // Cleanup the Port Event from the local cache
            Map<ByteBuffer, PortEvent> oldPortEvents =
                    discoveredAddedPortEvents.get(portEvent.getDpid());
            if (oldPortEvents != null) {
                ByteBuffer id = portEvent.getIDasByteBuffer();
                oldPortEvents.remove(id);
            }

            // Cleanup for the incoming link
            Map<ByteBuffer, LinkEvent> oldLinkEvents =
                    discoveredAddedLinkEvents.get(portEvent.getDpid());
            if (oldLinkEvents != null) {
                for (LinkEvent linkEvent : new ArrayList<>(oldLinkEvents.values())) {
                    if (linkEvent.getDst().equals(portEvent.getSwitchPort())) {
                        removeLinkDiscoveryEvent(linkEvent);
                        // XXX If we change our model to allow multiple Link on
                        // a Port, this loop must be fixed to allow continuing.
                        break;
                    }
                }
            }

            // Cleanup for the connected devices
            // TODO: The implementation below is probably wrong
            List<DeviceEvent> removedDeviceEvents = new LinkedList<>();
            Map<ByteBuffer, DeviceEvent> oldDeviceEvents =
                    discoveredAddedDeviceEvents.get(portEvent.getDpid());
            if (oldDeviceEvents != null) {
                for (DeviceEvent deviceEvent : new ArrayList<>(oldDeviceEvents.values())) {
                    for (SwitchPort swp : deviceEvent.getAttachmentPoints()) {
                        if (swp.equals(portEvent.getSwitchPort())) {
                            removedDeviceEvents.add(deviceEvent);
                        }
                    }
                }
                for (DeviceEvent deviceEvent : removedDeviceEvents) {
                    removeDeviceDiscoveryEvent(deviceEvent);
                }
            }
        }
    }

    /**
     * Link discovered event.
     *
     * @param linkEvent the link event.
     */
    @Override
    public void putLinkDiscoveryEvent(LinkEvent linkEvent) {
        if (datastore.addLink(linkEvent)) {
            log.debug("Sending add link: {}", linkEvent);
            // Send out notification
            TopologyEvent topologyEvent = new TopologyEvent(linkEvent);
            eventChannel.addEntry(topologyEvent.getID(), topologyEvent);

            // Store the new Link Event in the local cache
            Map<ByteBuffer, LinkEvent> oldLinkEvents =
                    discoveredAddedLinkEvents.get(linkEvent.getDst().getDpid());
            if (oldLinkEvents == null) {
                oldLinkEvents = new HashMap<>();
                discoveredAddedLinkEvents.put(linkEvent.getDst().getDpid(),
                        oldLinkEvents);
            }
            ByteBuffer id = linkEvent.getIDasByteBuffer();
            oldLinkEvents.put(id, linkEvent);
        }
    }

    /**
     * Link removed event.
     *
     * @param linkEvent the link event.
     */
    @Override
    public void removeLinkDiscoveryEvent(LinkEvent linkEvent) {
        if (datastore.removeLink(linkEvent)) {
            log.debug("Sending remove link: {}", linkEvent);
            // Send out notification
            eventChannel.removeEntry(linkEvent.getID());

            // Cleanup the Link Event from the local cache
            Map<ByteBuffer, LinkEvent> oldLinkEvents =
                    discoveredAddedLinkEvents.get(linkEvent.getDst().getDpid());
            if (oldLinkEvents != null) {
                ByteBuffer id = linkEvent.getIDasByteBuffer();
                oldLinkEvents.remove(id);
            }
        }
    }

    /**
     * Device discovered event.
     *
     * @param deviceEvent the device event.
     */
    @Override
    public void putDeviceDiscoveryEvent(DeviceEvent deviceEvent) {
        if (datastore.addDevice(deviceEvent)) {
            // Send out notification
            TopologyEvent topologyEvent = new TopologyEvent(deviceEvent);
            eventChannel.addEntry(topologyEvent.getID(), topologyEvent);
            log.debug("Put the device info into the cache of the topology. mac {}", deviceEvent.getMac());

            // Store the new Device Event in the local cache
            // TODO: The implementation below is probably wrong
            for (SwitchPort swp : deviceEvent.getAttachmentPoints()) {
                Map<ByteBuffer, DeviceEvent> oldDeviceEvents =
                        discoveredAddedDeviceEvents.get(swp.getDpid());
                if (oldDeviceEvents == null) {
                    oldDeviceEvents = new HashMap<>();
                    discoveredAddedDeviceEvents.put(swp.getDpid(),
                            oldDeviceEvents);
                }
                ByteBuffer id = deviceEvent.getIDasByteBuffer();
                oldDeviceEvents.put(id, deviceEvent);
            }
        }
    }

    /**
     * Device removed event.
     *
     * @param deviceEvent the device event.
     */
    @Override
    public void removeDeviceDiscoveryEvent(DeviceEvent deviceEvent) {
        if (datastore.removeDevice(deviceEvent)) {
            // Send out notification
            eventChannel.removeEntry(deviceEvent.getID());
            log.debug("Remove the device info into the cache of the topology. mac {}", deviceEvent.getMac());

            // Cleanup the Device Event from the local cache
            // TODO: The implementation below is probably wrong
            ByteBuffer id = ByteBuffer.wrap(deviceEvent.getID());
            for (SwitchPort swp : deviceEvent.getAttachmentPoints()) {
                Map<ByteBuffer, DeviceEvent> oldDeviceEvents =
                        discoveredAddedDeviceEvents.get(swp.getDpid());
                if (oldDeviceEvents != null) {
                    oldDeviceEvents.remove(id);
                }
            }
        }
    }

    /**
     * Add a switch to the topology replica.
     *
     * @param switchEvent the SwitchEvent with the switch to add.
     */
    @GuardedBy("topology.writeLock")
    private void addSwitch(SwitchEvent switchEvent) {
        Switch sw = topology.getSwitch(switchEvent.getDpid());
        if (sw == null) {
            sw = new SwitchImpl(topology, switchEvent);
            topology.putSwitch(sw);
        } else {
            // TODO: Update the switch attributes
            log.debug("Update switch attributes");
            SwitchImpl impl = (SwitchImpl) sw;
            impl.replaceStringAttributes(switchEvent);
        }
        apiAddedSwitchEvents.add(switchEvent);
    }

    /**
     * Remove a switch from the topology replica.
     * <p/>
     * It will call {@link #removePort(PortEvent)} for each ports on this switch.
     *
     * @param switchEvent the SwitchEvent with the switch to remove.
     */
    @GuardedBy("topology.writeLock")
    private void removeSwitch(SwitchEvent switchEvent) {
        Switch sw = topology.getSwitch(switchEvent.getDpid());
        if (sw == null) {
            log.warn("Switch {} already removed, ignoring", switchEvent);
            return;
        }

        //
        // Remove all Ports on the Switch
        //
        ArrayList<PortEvent> portsToRemove = new ArrayList<>();
        for (Port port : sw.getPorts()) {
            log.warn("Port {} on Switch {} should be removed prior to removing Switch. Removing Port now.",
                    port, switchEvent);
            PortEvent portEvent = new PortEvent(port.getDpid(),
                    port.getNumber());
            portsToRemove.add(portEvent);
        }
        for (PortEvent portEvent : portsToRemove) {
            removePort(portEvent);
        }

        topology.removeSwitch(switchEvent.getDpid());
        apiRemovedSwitchEvents.add(switchEvent);
    }

    /**
     * Add a port to the topology replica.
     *
     * @param portEvent the PortEvent with the port to add.
     */
    @GuardedBy("topology.writeLock")
    private void addPort(PortEvent portEvent) {
        Switch sw = topology.getSwitch(portEvent.getDpid());
        if (sw == null) {
            log.debug("{} reordered because switch is null", portEvent);
            // Reordered event: delay the event in local cache
            ByteBuffer id = portEvent.getIDasByteBuffer();
            reorderedAddedPortEvents.put(id, portEvent);
            return;
        }


        Port port = sw.getPort(portEvent.getPortNumber());
        if (port == null) {
            port = new PortImpl(topology, portEvent);
            topology.putPort(port);
        } else {
            // TODO: Update the port attributes
            log.debug("Update port attributes");
            PortImpl impl = (PortImpl) port;
            impl.replaceStringAttributes(portEvent);
        }
        apiAddedPortEvents.add(portEvent);
    }

    /**
     * Remove a port from the topology replica.
     * <p/>
     * It will call {@link #removeDevice(DeviceEvent)} for each hosts on this port
     * and call {@link #removeLink(LinkEvent)} for each links on this port.
     *
     * @param portEvent the PortEvent with the port to remove.
     */
    @GuardedBy("topology.writeLock")
    private void removePort(PortEvent portEvent) {
        Switch sw = topology.getSwitch(portEvent.getDpid());
        if (sw == null) {
            log.warn("Parent Switch for Port {} already removed, ignoring",
                    portEvent);
            return;
        }

        Port port = sw.getPort(portEvent.getPortNumber());
        if (port == null) {
            log.warn("Port {} already removed, ignoring", portEvent);
            return;
        }

        //
        // Remove all Devices attached to the Port
        //
        ArrayList<DeviceEvent> devicesToRemove = new ArrayList<>();
        for (Device device : port.getDevices()) {
            log.debug("Removing Device {} on Port {}", device, portEvent);
            DeviceEvent deviceEvent = new DeviceEvent(device.getMacAddress());
            SwitchPort switchPort = new SwitchPort(port.getSwitch().getDpid(),
                    port.getNumber());
            deviceEvent.addAttachmentPoint(switchPort);
            devicesToRemove.add(deviceEvent);
        }
        for (DeviceEvent deviceEvent : devicesToRemove) {
            removeDevice(deviceEvent);
        }

        //
        // Remove all Links connected to the Port
        //
        Set<Link> links = new HashSet<>();
        links.add(port.getOutgoingLink());
        links.add(port.getIncomingLink());
        ArrayList<LinkEvent> linksToRemove = new ArrayList<>();
        for (Link link : links) {
            if (link == null) {
                continue;
            }
            log.debug("Removing Link {} on Port {}", link, portEvent);
            LinkEvent linkEvent = new LinkEvent(link.getSrcSwitch().getDpid(),
                    link.getSrcPort().getNumber(),
                    link.getDstSwitch().getDpid(),
                    link.getDstPort().getNumber());
            linksToRemove.add(linkEvent);
        }
        for (LinkEvent linkEvent : linksToRemove) {
            removeLink(linkEvent);
        }

        // Remove the Port from the Switch
        topology.removePort(port);

        apiRemovedPortEvents.add(portEvent);
    }

    /**
     * Add a link to the topology replica.
     * <p/>
     * It will call {@link #removeDevice(DeviceEvent)} for each hosts on both ports.
     *
     * @param linkEvent the LinkEvent with the link to add.
     */
    @GuardedBy("topology.writeLock")
    private void addLink(LinkEvent linkEvent) {
        Port srcPort = topology.getPort(linkEvent.getSrc().getDpid(),
                linkEvent.getSrc().getPortNumber());
        Port dstPort = topology.getPort(linkEvent.getDst().getDpid(),
                linkEvent.getDst().getPortNumber());
        if ((srcPort == null) || (dstPort == null)) {
            log.debug("{} reordered because {} port is null", linkEvent,
                    (srcPort == null) ? "src" : "dst");

            // XXX domain knowledge: port must be present before link.
            // Reordered event: delay the event in local cache
            ByteBuffer id = linkEvent.getIDasByteBuffer();
            reorderedAddedLinkEvents.put(id, linkEvent);
            return;
        }

        // Get the Link instance from the Destination Port Incoming Link
        // XXX domain knowledge: Link is discovered by LLDP,
        //      thus incoming link is likely to be more up-to-date
        // FIXME potentially local replica may not be up-to-date yet due to HZ delay.
        //        may need to manage local truth and use them instead.
        Link link = dstPort.getIncomingLink();
        assert (link == srcPort.getOutgoingLink());
        if (link == null) {
            link = new LinkImpl(topology, linkEvent);
            topology.putLink(link);

            // Remove all Devices attached to the Ports
            ArrayList<DeviceEvent> devicesToRemove = new ArrayList<>();
            ArrayList<Port> ports = new ArrayList<>();
            ports.add(srcPort);
            ports.add(dstPort);
            for (Port port : ports) {
                for (Device device : port.getDevices()) {
                    log.error("Device {} on Port {} should have been removed prior to adding Link {}",
                            device, port, linkEvent);
                    // FIXME must get Device info from topology, when we add attrs.
                    DeviceEvent deviceEvent =
                            new DeviceEvent(device.getMacAddress());
                    SwitchPort switchPort =
                            new SwitchPort(port.getSwitch().getDpid(),
                                    port.getNumber());
                    // adding attachment port which needs to be removed
                    deviceEvent.addAttachmentPoint(switchPort);
                    devicesToRemove.add(deviceEvent);
                }
            }
            for (DeviceEvent deviceEvent : devicesToRemove) {
                removeDevice(deviceEvent);
            }
        } else {
            // TODO: Update the link attributes
            log.debug("Update link attributes");
            LinkImpl impl = (LinkImpl) link;
            impl.replaceStringAttributes(linkEvent);
        }

        apiAddedLinkEvents.add(linkEvent);
    }

    /**
     * Remove a link from the topology replica.
     *
     * @param linkEvent the LinkEvent with the link to remove.
     */
    @GuardedBy("topology.writeLock")
    private void removeLink(LinkEvent linkEvent) {
        Port srcPort = topology.getPort(linkEvent.getSrc().getDpid(),
                linkEvent.getSrc().getPortNumber());
        if (srcPort == null) {
            log.warn("Src Port for Link {} already removed, ignoring",
                    linkEvent);
            return;
        }

        Port dstPort = topology.getPort(linkEvent.getDst().getDpid(),
                linkEvent.getDst().getPortNumber());
        if (dstPort == null) {
            log.warn("Dst Port for Link {} already removed, ignoring",
                    linkEvent);
            return;
        }

        //
        // Remove the Link instance from the Destination Port Incoming Link
        // and the Source Port Outgoing Link.
        //
        Link link = dstPort.getIncomingLink();
        if (link == null) {
            log.warn("Link {} already removed on destination Port", linkEvent);
        }
        link = srcPort.getOutgoingLink();
        if (link == null) {
            log.warn("Link {} already removed on src Port", linkEvent);
        }

        // TODO should we check that we get the same link from each port?
        if (link != null) {
            topology.removeLink(link);
        }

        apiRemovedLinkEvents.add(linkEvent);
    }

    /**
     * Add a device to the topology replica.
     * <p/>
     * TODO: Device-related work is incomplete.
     * TODO: Eventually, we might need to consider reordering
     * or addLink() and addDevice() events on the same port.
     *
     * @param deviceEvent the DeviceEvent with the device to add.
     */
    @GuardedBy("topology.writeLock")
    private void addDevice(DeviceEvent deviceEvent) {
        log.debug("Adding a device to the topology with mac {}", deviceEvent.getMac());
        Device device = topology.getDeviceByMac(deviceEvent.getMac());

        if (device == null) {
            log.debug("Existing device was not found in the Topology: Adding new device: mac {}", deviceEvent.getMac());
            device = new DeviceImpl(topology, deviceEvent.getMac());
        }

        DeviceImpl deviceImpl = (DeviceImpl) device;

        // Process each attachment point
        boolean attachmentFound = false;
        for (SwitchPort swp : deviceEvent.getAttachmentPoints()) {
            // XXX domain knowledge: Port must exist before Device
            //      but this knowledge cannot be pushed down to driver.

            // Attached Ports must exist
            Port port = topology.getPort(swp.getDpid(), swp.getPortNumber());
            if (port == null) {
                // Reordered event: delay the event in local cache
                ByteBuffer id = deviceEvent.getIDasByteBuffer();
                reorderedAddedDeviceEvents.put(id, deviceEvent);
                continue;
            }
            // Attached Ports must not have Link
            if (port.getOutgoingLink() != null ||
                    port.getIncomingLink() != null) {
                log.warn("Link (Out:{},In:{}) exist on the attachment point, skipping mutation.",
                        port.getOutgoingLink(),
                        port.getIncomingLink());
                continue;
            }

            // Add Device <-> Port attachment
            deviceImpl.addAttachmentPoint(port);
            attachmentFound = true;
        }

        // Update the device in the topology
        if (attachmentFound) {
            log.debug("Storing the device info into the Topology: mac {}", deviceEvent.getMac());
            topology.putDevice(device);
            apiAddedDeviceEvents.add(deviceEvent);
        }
    }

    /**
     * Remove a device from the topology replica.
     * <p/>
     * TODO: Device-related work is incomplete.
     *
     * @param deviceEvent the Device Event with the device to remove.
     */
    @GuardedBy("topology.writeLock")
    private void removeDevice(DeviceEvent deviceEvent) {
        log.debug("Removing a device from the topology: mac {}", deviceEvent.getMac());
        Device device = topology.getDeviceByMac(deviceEvent.getMac());
        if (device == null) {
            log.warn("Device {} already removed, ignoring", deviceEvent);
            return;
        }

        topology.removeDevice(device);
        apiRemovedDeviceEvents.add(deviceEvent);
    }

    /**
     * Read the whole topology from the database.
     *
     * @return a collection of EventEntry-encapsulated Topology Events for
     * the whole topology.
     */
    private Collection<EventEntry<TopologyEvent>> readWholeTopologyFromDB() {
        Collection<EventEntry<TopologyEvent>> collection =
                new LinkedList<EventEntry<TopologyEvent>>();

        // XXX May need to clear whole topology first, depending on
        // how we initially subscribe to replication events

        // Add all active switches
        for (KVSwitch sw : KVSwitch.getAllSwitches()) {
            if (sw.getStatus() != KVSwitch.STATUS.ACTIVE) {
                continue;
            }

            SwitchEvent switchEvent = new SwitchEvent(sw.getDpid());
            TopologyEvent topologyEvent = new TopologyEvent(switchEvent);
            EventEntry<TopologyEvent> eventEntry =
                    new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                            topologyEvent);
            collection.add(eventEntry);
        }

        // Add all active ports
        for (KVPort p : KVPort.getAllPorts()) {
            if (p.getStatus() != KVPort.STATUS.ACTIVE) {
                continue;
            }

            PortEvent portEvent = new PortEvent(p.getDpid(), p.getNumber());
            TopologyEvent topologyEvent = new TopologyEvent(portEvent);
            EventEntry<TopologyEvent> eventEntry =
                    new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                            topologyEvent);
            collection.add(eventEntry);
        }

         for (KVDevice d : KVDevice.getAllDevices()) {
              DeviceEvent devEvent = new DeviceEvent(MACAddress.valueOf(d.getMac()));
              for (byte[] portId : d.getAllPortIds()) {
                  devEvent.addAttachmentPoint(
                          new SwitchPort(KVPort.getDpidFromKey(portId),
                                  KVPort.getNumberFromKey(portId)));
              }
         }

        for (KVLink l : KVLink.getAllLinks()) {
            LinkEvent linkEvent = new LinkEvent(l.getSrc().dpid,
                    l.getSrc().number,
                    l.getDst().dpid,
                    l.getDst().number);
            TopologyEvent topologyEvent = new TopologyEvent(linkEvent);
            EventEntry<TopologyEvent> eventEntry =
                    new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                            topologyEvent);
            collection.add(eventEntry);
        }

        return collection;
    }
}
