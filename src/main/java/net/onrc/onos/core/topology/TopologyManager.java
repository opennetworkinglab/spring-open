package net.onrc.onos.core.topology;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
import net.onrc.onos.core.metrics.OnosMetrics;
import net.onrc.onos.core.metrics.OnosMetrics.MetricsComponent;
import net.onrc.onos.core.metrics.OnosMetrics.MetricsFeature;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.EventEntry;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;
import net.onrc.onos.core.util.serializers.KryoFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
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

    private TopologyDatastore datastore;
    private final TopologyImpl topology = new TopologyImpl();
    private final IControllerRegistryService registryService;
    private CopyOnWriteArrayList<ITopologyListener> topologyListeners;
    private Kryo kryo = KryoFactory.newKryoObject();
    private TopologyEventPreprocessor eventPreprocessor;

    //
    // Metrics
    //
    private static final MetricsComponent METRICS_COMPONENT =
        OnosMetrics.registerComponent("Topology");
    private static final MetricsFeature METRICS_FEATURE_EVENT_NOTIFICATION =
        METRICS_COMPONENT.registerFeature("EventNotification");
    //
    // Timestamp of the last Topology event (ms from the Epoch)
    private volatile long lastEventTimestampEpochMs = 0;
    private final Gauge<Long> gaugeLastEventTimestampEpochMs =
        OnosMetrics.registerMetric(METRICS_COMPONENT,
                                   METRICS_FEATURE_EVENT_NOTIFICATION,
                                   "LastEventTimestamp.EpochMs",
                                   new Gauge<Long>() {
                                       @Override
                                       public Long getValue() {
                                           return lastEventTimestampEpochMs;
                                       }
                                   });
    // Rate of the Topology events published to the Topology listeners
    private final Meter listenerEventRate =
        OnosMetrics.createMeter(METRICS_COMPONENT,
                                METRICS_FEATURE_EVENT_NOTIFICATION,
                                "ListenerEventRate");

    //
    // Local state for keeping track of locally discovered events so we can
    // cleanup properly when a Switch or Port is removed.
    //
    // We keep all Port, (incoming) Link and Host events per Switch DPID:
    //  - If a switch goes down, we remove all corresponding Port, Link and
    //    Host events.
    //  - If a port on a switch goes down, we remove all corresponding Link
    //    and Host events discovered by this instance.
    //
    // How to handle side-effect of remote events.
    //  - Remote Port Down event -> Link Down
    //      Not handled. (XXX Shouldn't it be removed from discovered.. Map)
    //  - Remote Host Added -> lose ownership of Host)
    //      Not handled. (XXX Shouldn't it be removed from discovered.. Map)
    //
    // XXX Domain knowledge based invariant maintenance should be moved to
    //     driver module, since the invariant may be different on optical, etc.
    //
    // What happens on leadership change?
    //  - Probably should: remove from discovered.. Maps, but not send DELETE events
    //     XXX Switch/Port can be rediscovered by new leader, but Link, Host?
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
    private Map<Dpid, Map<ByteBuffer, HostEvent>> discoveredAddedHostEvents =
            new HashMap<>();

    //
    // Local state for keeping track of the application event notifications
    //
    //  - Queue of events, which will be dispatched to local listeners
    //    on next notification.

    private List<MastershipEvent> apiAddedMastershipEvents =
        new LinkedList<>();
    private List<MastershipEvent> apiRemovedMastershipEvents =
        new LinkedList<>();
    private List<SwitchEvent> apiAddedSwitchEvents = new LinkedList<>();
    private List<SwitchEvent> apiRemovedSwitchEvents = new LinkedList<>();
    private List<PortEvent> apiAddedPortEvents = new LinkedList<>();
    private List<PortEvent> apiRemovedPortEvents = new LinkedList<>();
    private List<LinkEvent> apiAddedLinkEvents = new LinkedList<>();
    private List<LinkEvent> apiRemovedLinkEvents = new LinkedList<>();
    private List<HostEvent> apiAddedHostEvents = new LinkedList<>();
    private List<HostEvent> apiRemovedHostEvents = new LinkedList<>();

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
        this.eventPreprocessor =
            new TopologyEventPreprocessor(registryService);
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
    class EventHandler extends Thread implements
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
            List<EventEntry<TopologyEvent>> events =
                new LinkedList<EventEntry<TopologyEvent>>();

            for (TopologyEvent topologyEvent : allTopologyEvents) {
                EventEntry<TopologyEvent> eventEntry =
                        new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                                topologyEvent);
                events.add(eventEntry);
            }
            processEvents(events);
        }

        /**
         * Run the thread.
         */
        @Override
        public void run() {
            List<EventEntry<TopologyEvent>> events =
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
                    events.add(eventEntry);
                    topologyEvents.drainTo(events);

                    processEvents(events);
                    events.clear();
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
        private void processEvents(List<EventEntry<TopologyEvent>> events) {
            //
            // Pre-process the events
            //
            events = eventPreprocessor.processEvents(events);

            //
            // Lock the topology while it is modified
            //
            topology.acquireWriteLock();

            try {
                // Apply the events
                //
                // NOTE: The events are suppose to be in the proper order
                // to naturally build and update the topology.
                //
                for (EventEntry<TopologyEvent> event : events) {
                    TopologyEvent topologyEvent = event.eventData();

                    // Get the event itself
                    MastershipEvent mastershipEvent =
                        topologyEvent.getMastershipEvent();
                    SwitchEvent switchEvent = topologyEvent.getSwitchEvent();
                    PortEvent portEvent = topologyEvent.getPortEvent();
                    LinkEvent linkEvent = topologyEvent.getLinkEvent();
                    HostEvent hostEvent = topologyEvent.getHostEvent();
                    boolean wasAdded = false;

                    //
                    // Extract the events
                    //
                    switch (event.eventType()) {
                    case ENTRY_ADD:
                        if (mastershipEvent != null) {
                            wasAdded = addMastershipEvent(mastershipEvent);
                        }
                        if (switchEvent != null) {
                            wasAdded = addSwitch(switchEvent);
                        }
                        if (portEvent != null) {
                            wasAdded = addPort(portEvent);
                        }
                        if (linkEvent != null) {
                            wasAdded = addLink(linkEvent);
                        }
                        if (hostEvent != null) {
                            wasAdded = addHost(hostEvent);
                        }
                        // If the item wasn't added, probably it was reordered
                        if (!wasAdded) {
                            ByteBuffer id = topologyEvent.getIDasByteBuffer();
                            eventPreprocessor.reorderedEvents.put(id, topologyEvent);
                        }
                        break;
                    case ENTRY_REMOVE:
                        if (mastershipEvent != null) {
                            removeMastershipEvent(mastershipEvent);
                        }
                        if (switchEvent != null) {
                            removeSwitch(switchEvent);
                        }
                        if (portEvent != null) {
                            removePort(portEvent);
                        }
                        if (linkEvent != null) {
                            removeLink(linkEvent);
                        }
                        if (hostEvent != null) {
                            removeHost(hostEvent);
                        }
                        break;
                    default:
                        log.error("Unknown topology event {}",
                                  event.eventType());
                    }
                }
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
        if (apiAddedMastershipEvents.isEmpty() &&
                apiRemovedMastershipEvents.isEmpty() &&
                apiAddedSwitchEvents.isEmpty() &&
                apiRemovedSwitchEvents.isEmpty() &&
                apiAddedPortEvents.isEmpty() &&
                apiRemovedPortEvents.isEmpty() &&
                apiAddedLinkEvents.isEmpty() &&
                apiRemovedLinkEvents.isEmpty() &&
                apiAddedHostEvents.isEmpty() &&
                apiRemovedHostEvents.isEmpty()) {
            return;        // No events to dispatch
        }

        if (log.isDebugEnabled()) {
            //
            // Debug statements
            // TODO: Those statements should be removed in the future
            //
            for (MastershipEvent mastershipEvent : apiAddedMastershipEvents) {
                log.debug("Dispatch Topology Event: ADDED {}",
                          mastershipEvent);
            }
            for (MastershipEvent mastershipEvent : apiRemovedMastershipEvents) {
                log.debug("Dispatch Topology Event: REMOVED {}",
                          mastershipEvent);
            }
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
            for (HostEvent hostEvent : apiAddedHostEvents) {
                log.debug("Dispatch Topology Event: ADDED {}", hostEvent);
            }
            for (HostEvent hostEvent : apiRemovedHostEvents) {
                log.debug("Dispatch Topology Event: REMOVED {}", hostEvent);
            }
        }

        //
        // Update the metrics
        //
        long totalEvents =
            apiAddedMastershipEvents.size() + apiRemovedMastershipEvents.size() +
            apiAddedSwitchEvents.size() + apiRemovedSwitchEvents.size() +
            apiAddedPortEvents.size() + apiRemovedPortEvents.size() +
            apiAddedLinkEvents.size() + apiRemovedLinkEvents.size() +
            apiAddedHostEvents.size() + apiRemovedHostEvents.size();
        this.listenerEventRate.mark(totalEvents);
        this.lastEventTimestampEpochMs = System.currentTimeMillis();

        //
        // Deliver the events
        //
        for (ITopologyListener listener : this.topologyListeners) {
            TopologyEvents events =
                new TopologyEvents(kryo.copy(apiAddedMastershipEvents),
                                   kryo.copy(apiRemovedMastershipEvents),
                                   kryo.copy(apiAddedSwitchEvents),
                                   kryo.copy(apiRemovedSwitchEvents),
                                   kryo.copy(apiAddedPortEvents),
                                   kryo.copy(apiRemovedPortEvents),
                                   kryo.copy(apiAddedLinkEvents),
                                   kryo.copy(apiRemovedLinkEvents),
                                   kryo.copy(apiAddedHostEvents),
                                   kryo.copy(apiRemovedHostEvents));
            listener.topologyEvents(events);
        }

        //
        // Cleanup
        //
        apiAddedMastershipEvents.clear();
        apiRemovedMastershipEvents.clear();
        apiAddedSwitchEvents.clear();
        apiRemovedSwitchEvents.clear();
        apiAddedPortEvents.clear();
        apiRemovedPortEvents.clear();
        apiAddedLinkEvents.clear();
        apiRemovedLinkEvents.clear();
        apiAddedHostEvents.clear();
        apiRemovedHostEvents.clear();
    }

    /**
     * Mastership updated event.
     *
     * @param mastershipEvent the mastership event.
     */
    @Override
    public void putSwitchMastershipEvent(MastershipEvent mastershipEvent) {
        // Send out notification
        TopologyEvent topologyEvent =
            new TopologyEvent(mastershipEvent,
                              registryService.getOnosInstanceId());
        eventChannel.addEntry(topologyEvent.getID(), topologyEvent);
    }

    /**
     * Mastership removed event.
     *
     * @param mastershipEvent the mastership event.
     */
    @Override
    public void removeSwitchMastershipEvent(MastershipEvent mastershipEvent) {
        // Send out notification
        eventChannel.removeEntry(mastershipEvent.getID());
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
            TopologyEvent topologyEvent =
                new TopologyEvent(switchEvent,
                                  registryService.getOnosInstanceId());
            eventChannel.addEntry(topologyEvent.getID(), topologyEvent);

            // Send out notification for each port
            for (PortEvent portEvent : portEvents) {
                log.debug("Sending add port: {}", portEvent);
                topologyEvent =
                    new TopologyEvent(portEvent,
                                      registryService.getOnosInstanceId());
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

            // Cleanup for each host
            Map<ByteBuffer, HostEvent> oldHostEvents =
                    discoveredAddedHostEvents.get(switchEvent.getDpid());
            if (oldHostEvents != null) {
                for (HostEvent hostEvent : new ArrayList<>(oldHostEvents.values())) {
                    removeHostDiscoveryEvent(hostEvent);
                }
                discoveredAddedHostEvents.remove(switchEvent.getDpid());
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
            TopologyEvent topologyEvent =
                new TopologyEvent(portEvent,
                                  registryService.getOnosInstanceId());
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

            // Cleanup for the connected hosts
            // TODO: The implementation below is probably wrong
            List<HostEvent> removedHostEvents = new LinkedList<>();
            Map<ByteBuffer, HostEvent> oldHostEvents =
                    discoveredAddedHostEvents.get(portEvent.getDpid());
            if (oldHostEvents != null) {
                for (HostEvent hostEvent : new ArrayList<>(oldHostEvents.values())) {
                    for (SwitchPort swp : hostEvent.getAttachmentPoints()) {
                        if (swp.equals(portEvent.getSwitchPort())) {
                            removedHostEvents.add(hostEvent);
                        }
                    }
                }
                for (HostEvent hostEvent : removedHostEvents) {
                    removeHostDiscoveryEvent(hostEvent);
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
            TopologyEvent topologyEvent =
                new TopologyEvent(linkEvent,
                                  registryService.getOnosInstanceId());
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
     * Host discovered event.
     *
     * @param hostEvent the host event.
     */
    @Override
    public void putHostDiscoveryEvent(HostEvent hostEvent) {
        if (datastore.addHost(hostEvent)) {
            // Send out notification
            TopologyEvent topologyEvent =
                new TopologyEvent(hostEvent,
                                  registryService.getOnosInstanceId());
            eventChannel.addEntry(topologyEvent.getID(), topologyEvent);
            log.debug("Put the host info into the cache of the topology. mac {}", hostEvent.getMac());

            // Store the new Host Event in the local cache
            // TODO: The implementation below is probably wrong
            for (SwitchPort swp : hostEvent.getAttachmentPoints()) {
                Map<ByteBuffer, HostEvent> oldHostEvents =
                        discoveredAddedHostEvents.get(swp.getDpid());
                if (oldHostEvents == null) {
                    oldHostEvents = new HashMap<>();
                    discoveredAddedHostEvents.put(swp.getDpid(),
                            oldHostEvents);
                }
                ByteBuffer id = hostEvent.getIDasByteBuffer();
                oldHostEvents.put(id, hostEvent);
            }
        }
    }

    /**
     * Host removed event.
     *
     * @param hostEvent the host event.
     */
    @Override
    public void removeHostDiscoveryEvent(HostEvent hostEvent) {
        if (datastore.removeHost(hostEvent)) {
            // Send out notification
            eventChannel.removeEntry(hostEvent.getID());
            log.debug("Remove the host info into the cache of the topology. mac {}", hostEvent.getMac());

            // Cleanup the Host Event from the local cache
            // TODO: The implementation below is probably wrong
            ByteBuffer id = ByteBuffer.wrap(hostEvent.getID());
            for (SwitchPort swp : hostEvent.getAttachmentPoints()) {
                Map<ByteBuffer, HostEvent> oldHostEvents =
                        discoveredAddedHostEvents.get(swp.getDpid());
                if (oldHostEvents != null) {
                    oldHostEvents.remove(id);
                }
            }
        }
    }

    //
    // Methods to update topology replica
    //

    /**
     * Adds Switch Mastership event.
     *
     * @param mastershipEvent the MastershipEvent to process.
     * @return true if the item was successfully added, otherwise false.
     */
    @GuardedBy("topology.writeLock")
    private boolean addMastershipEvent(MastershipEvent mastershipEvent) {
        log.debug("Added Mastership event {}", mastershipEvent);
        apiAddedMastershipEvents.add(mastershipEvent);
        return true;
    }

    /**
     * Removes Switch Mastership event.
     *
     * @param mastershipEvent the MastershipEvent to process.
     */
    @GuardedBy("topology.writeLock")
    private void removeMastershipEvent(MastershipEvent mastershipEvent) {
        log.debug("Removed Mastership event {}", mastershipEvent);
        apiRemovedMastershipEvents.add(mastershipEvent);
    }

    /**
     * Adds a switch to the topology replica.
     *
     * @param switchEvent the SwitchEvent with the switch to add.
     * @return true if the item was successfully added, otherwise false.
     */
    @GuardedBy("topology.writeLock")
    private boolean addSwitch(SwitchEvent switchEvent) {
        if (log.isDebugEnabled()) {
            SwitchEvent sw = topology.getSwitchEvent(switchEvent.getDpid());
            if (sw != null) {
                log.debug("Update {}", switchEvent);
            } else {
                log.debug("Added {}", switchEvent);
            }
        }
        topology.putSwitch(switchEvent.freeze());
        apiAddedSwitchEvents.add(switchEvent);
        return true;
    }

    /**
     * Removes a switch from the topology replica.
     * <p/>
     * It will call {@link #removePort(PortEvent)} for each ports on this
     * switch.
     *
     * @param switchEvent the SwitchEvent with the switch to remove.
     */
    @GuardedBy("topology.writeLock")
    private void removeSwitch(SwitchEvent switchEvent) {
        final Dpid dpid = switchEvent.getDpid();

        SwitchEvent swInTopo = topology.getSwitchEvent(dpid);
        if (swInTopo == null) {
            log.warn("Switch {} already removed, ignoring", switchEvent);
            return;
        }

        //
        // Remove all Ports on the Switch
        //
        ArrayList<PortEvent> portsToRemove = new ArrayList<>();
        for (Port port : topology.getPorts(dpid)) {
            log.warn("Port {} on Switch {} should be removed prior to removing Switch. Removing Port now.",
                    port, switchEvent);
            PortEvent portEvent = new PortEvent(port.getSwitchPort());
            portsToRemove.add(portEvent);
        }
        for (PortEvent portEvent : portsToRemove) {
            removePort(portEvent);
        }

        log.debug("Removed {}", swInTopo);
        topology.removeSwitch(dpid);
        apiRemovedSwitchEvents.add(swInTopo);
    }

    /**
     * Adds a port to the topology replica.
     *
     * @param portEvent the PortEvent with the port to add.
     * @return true if the item was successfully added, otherwise false.
     */
    @GuardedBy("topology.writeLock")
    private boolean addPort(PortEvent portEvent) {
        Switch sw = topology.getSwitch(portEvent.getDpid());
        if (sw == null) {
            // Reordered event
            log.debug("{} reordered because switch is null", portEvent);
            return false;
        }

        if (log.isDebugEnabled()) {
            PortEvent port = topology.getPortEvent(portEvent.getSwitchPort());
            if (port != null) {
                log.debug("Update {}", portEvent);
            } else {
                log.debug("Added {}", portEvent);
            }
        }
        topology.putPort(portEvent.freeze());
        apiAddedPortEvents.add(portEvent);
        return true;
    }

    /**
     * Removes a port from the topology replica.
     * <p/>
     * It will remove attachment points from each hosts on this port
     * and call {@link #removeLink(LinkEvent)} for each links on this port.
     *
     * @param portEvent the PortEvent with the port to remove.
     */
    @GuardedBy("topology.writeLock")
    private void removePort(PortEvent portEvent) {
        SwitchEvent sw = topology.getSwitchEvent(portEvent.getDpid());
        if (sw == null) {
            log.warn("Parent Switch for Port {} already removed, ignoring",
                    portEvent);
            return;
        }

        final SwitchPort switchPort = portEvent.getSwitchPort();
        PortEvent portInTopo = topology.getPortEvent(switchPort);
        if (portInTopo == null) {
            log.warn("Port {} already removed, ignoring", portEvent);
            return;
        }

        //
        // Remove all Host attachment points bound to this Port
        //
        List<HostEvent> hostsToUpdate = new ArrayList<>();
        for (Host host : topology.getHosts(switchPort)) {
            log.debug("Removing Host {} on Port {}", host, portInTopo);
            HostEvent hostEvent = topology.getHostEvent(host.getMacAddress());
            hostsToUpdate.add(hostEvent);
        }
        for (HostEvent hostEvent : hostsToUpdate) {
            HostEvent newHostEvent = new HostEvent(hostEvent);
            newHostEvent.removeAttachmentPoint(switchPort);
            newHostEvent.freeze();

            // TODO should this event be fired inside #addHost?
            if (newHostEvent.getAttachmentPoints().isEmpty()) {
                // No more attachment point left -> remove Host
                removeHost(hostEvent);
            } else {
                // Update Host
                addHost(newHostEvent);
            }
        }

        //
        // Remove all Links connected to the Port
        //
        Set<Link> links = new HashSet<>();
        links.addAll(topology.getOutgoingLinks(switchPort));
        links.addAll(topology.getIncomingLinks(switchPort));
        for (Link link : links) {
            if (link == null) {
                continue;
            }
            LinkEvent linkEvent = topology.getLinkEvent(link.getLinkTuple());
            if (linkEvent != null) {
                log.debug("Removing Link {} on Port {}", link, portInTopo);
                removeLink(linkEvent);
            }
        }

        // Remove the Port from Topology
        log.debug("Removed {}", portInTopo);
        topology.removePort(switchPort);

        apiRemovedPortEvents.add(portInTopo);
    }

    /**
     * Adds a link to the topology replica.
     * <p/>
     * It will remove attachment points from each hosts using the same ports.
     *
     * @param linkEvent the LinkEvent with the link to add.
     * @return true if the item was successfully added, otherwise false.
     */
    @GuardedBy("topology.writeLock")
    private boolean addLink(LinkEvent linkEvent) {
        PortEvent srcPort = topology.getPortEvent(linkEvent.getSrc());
        PortEvent dstPort = topology.getPortEvent(linkEvent.getDst());
        if ((srcPort == null) || (dstPort == null)) {
            // Reordered event
            log.debug("{} reordered because {} port is null", linkEvent,
                    (srcPort == null) ? "src" : "dst");
            return false;
        }

        //
        // XXX domain knowledge: Sanity check: Port cannot have both Link and
        // Host.
        //
        // FIXME: Potentially local replica may not be up-to-date yet due to
        //        Hazelcast delay.
        // FIXME: May need to manage local truth and use them instead.
        //
        if (topology.getLinkEvent(linkEvent.getLinkTuple()) == null) {
            // Only check for existing Host when adding new Link.
            // Remove all Hosts attached to the ports on both ends

            Set<HostEvent> hostsToUpdate =
                new TreeSet<>(new Comparator<HostEvent>() {
                // Comparison only using ID(=MAC)
                @Override
                public int compare(HostEvent o1, HostEvent o2) {
                    return Long.compare(o1.getMac().toLong(), o2.getMac().toLong());
                }
            });

            List<SwitchPort> portsToCheck = Arrays.asList(
                    srcPort.getSwitchPort(),
                    dstPort.getSwitchPort());

            // Enumerate Host which needs to be updated by this Link add event
            for (SwitchPort port : portsToCheck) {
                for (Host host : topology.getHosts(port)) {
                    log.error("Host {} on Port {} should have been removed prior to adding Link {}",
                            host, port, linkEvent);

                    HostEvent hostEvent = topology.getHostEvent(host.getMacAddress());
                    hostsToUpdate.add(hostEvent);
                }
            }
            // Remove attachment point from them
            for (HostEvent hostEvent : hostsToUpdate) {
                // Remove port from attachment point and update
                HostEvent newHostEvent = new HostEvent(hostEvent);
                newHostEvent.removeAttachmentPoint(srcPort.getSwitchPort());
                newHostEvent.removeAttachmentPoint(dstPort.getSwitchPort());
                newHostEvent.freeze();

                // TODO should this event be fired inside #addHost?
                if (newHostEvent.getAttachmentPoints().isEmpty()) {
                    // No more attachment point left -> remove Host
                    removeHost(hostEvent);
                } else {
                    // Update Host
                    addHost(newHostEvent);
                }
            }
        }

        if (log.isDebugEnabled()) {
            LinkEvent link = topology.getLinkEvent(linkEvent.getLinkTuple());
            if (link != null) {
                log.debug("Update {}", linkEvent);
            } else {
                log.debug("Added {}", linkEvent);
            }
        }
        topology.putLink(linkEvent.freeze());
        apiAddedLinkEvents.add(linkEvent);
        return true;
    }

    /**
     * Removes a link from the topology replica.
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

        LinkEvent linkInTopo = topology.getLinkEvent(linkEvent.getLinkTuple(),
                linkEvent.getType());
        if (linkInTopo == null) {
            log.warn("Link {} already removed, ignoring", linkEvent);
            return;
        }

        if (log.isDebugEnabled()) {
            // only do sanity check on debug level

            Link linkIn = dstPort.getIncomingLink(linkEvent.getType());
            if (linkIn == null) {
                log.warn("Link {} already removed on destination Port", linkEvent);
            }
            Link linkOut = srcPort.getOutgoingLink(linkEvent.getType());
            if (linkOut == null) {
                log.warn("Link {} already removed on src Port", linkEvent);
            }
        }

        log.debug("Removed {}", linkInTopo);
        topology.removeLink(linkEvent.getLinkTuple(), linkEvent.getType());
        apiRemovedLinkEvents.add(linkInTopo);
    }

    /**
     * Adds a host to the topology replica.
     * <p/>
     * TODO: Host-related work is incomplete.
     * TODO: Eventually, we might need to consider reordering
     * or {@link #addLink(LinkEvent)} and {@link #addHost(HostEvent)} events
     * on the same port.
     *
     * @param hostEvent the HostEvent with the host to add.
     * @return true if the item was successfully added, otherwise false.
     */
    @GuardedBy("topology.writeLock")
    private boolean addHost(HostEvent hostEvent) {

        // TODO Decide how to handle update scenario.
        // If the new HostEvent has less attachment point compared to
        // existing HostEvent, what should the event be?
        // - AddHostEvent with some attachment point removed? (current behavior)

        // create unfrozen copy
        //  for removing attachment points which already has a link
        HostEvent modifiedHostEvent = new HostEvent(hostEvent);

        // Verify each attachment point
        boolean attachmentFound = false;
        for (SwitchPort swp : hostEvent.getAttachmentPoints()) {
            // XXX domain knowledge: Port must exist before Host
            //      but this knowledge cannot be pushed down to driver.

            // Attached Ports must exist
            Port port = topology.getPort(swp.getDpid(), swp.getPortNumber());
            if (port == null) {
                log.debug("{} reordered because port {} was not there", hostEvent, swp);
                // Reordered event
                return false; // should not continue if re-applying later
            }
            // Attached Ports must not have Link
            if (port.getOutgoingLink() != null ||
                    port.getIncomingLink() != null) {
                log.warn("Link (Out:{},In:{}) exist on the attachment point. "
                        + "Ignoring this attachmentpoint ({}) from {}.",
                        port.getOutgoingLink(), port.getIncomingLink(),
                        swp, modifiedHostEvent);
                // FIXME Should either reject, reorder this HostEvent,
                //       or remove attachment point from given HostEvent
                // Removing attachment point from given HostEvent for now.
                modifiedHostEvent.removeAttachmentPoint(swp);
                continue;
            }

            attachmentFound = true;
        }

        // Update the host in the topology
        if (attachmentFound) {
            if (modifiedHostEvent.getAttachmentPoints().isEmpty()) {
                log.warn("No valid attachment point left. Ignoring."
                        + "original: {}, modified: {}", hostEvent, modifiedHostEvent);
                // TODO Should we call #removeHost to trigger remove event?
                //      only if this call is update.
                return false;
            }

            if (log.isDebugEnabled()) {
                HostEvent host = topology.getHostEvent(hostEvent.getMac());
                if (host != null) {
                    log.debug("Update {}", modifiedHostEvent);
                } else {
                    log.debug("Added {}", modifiedHostEvent);
                }
            }
            topology.putHost(modifiedHostEvent.freeze());
            apiAddedHostEvents.add(modifiedHostEvent);
            return true;
        }
        return false;
    }

    /**
     * Removes a host from the topology replica.
     * <p/>
     * TODO: Host-related work is incomplete.
     *
     * @param hostEvent the Host Event with the host to remove.
     */
    @GuardedBy("topology.writeLock")
    private void removeHost(HostEvent hostEvent) {

        final MACAddress mac = hostEvent.getMac();
        HostEvent hostInTopo = topology.getHostEvent(mac);
        if (hostInTopo == null) {
            log.warn("Host {} already removed, ignoring", hostEvent);
            return;
        }

        log.debug("Removed {}", hostInTopo);
        topology.removeHost(mac);
        apiRemovedHostEvents.add(hostInTopo);
    }

    /**
     * Read the whole topology from the database.
     *
     * @return a list of EventEntry-encapsulated Topology Events for
     * the whole topology.
     */
    private List<EventEntry<TopologyEvent>> readWholeTopologyFromDB() {
        List<EventEntry<TopologyEvent>> events =
                new LinkedList<EventEntry<TopologyEvent>>();

        // XXX May need to clear whole topology first, depending on
        // how we initially subscribe to replication events

        // Add all active switches
        for (KVSwitch sw : KVSwitch.getAllSwitches()) {
            if (sw.getStatus() != KVSwitch.STATUS.ACTIVE) {
                continue;
            }

            //
            // TODO: Using the local ONOS Instance ID below is incorrect.
            // Currently, this code is not used, and it might go away in the
            // future.
            //
            SwitchEvent switchEvent = new SwitchEvent(new Dpid(sw.getDpid()));
            TopologyEvent topologyEvent =
                new TopologyEvent(switchEvent,
                                  registryService.getOnosInstanceId());
            EventEntry<TopologyEvent> eventEntry =
                    new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                            topologyEvent);
            events.add(eventEntry);
        }

        // Add all active ports
        for (KVPort p : KVPort.getAllPorts()) {
            if (p.getStatus() != KVPort.STATUS.ACTIVE) {
                continue;
            }

            //
            // TODO: Using the local ONOS Instance ID below is incorrect.
            // Currently, this code is not used, and it might go away in the
            // future.
            //
            PortEvent portEvent =
                new PortEvent(new Dpid(p.getDpid()),
                              new PortNumber(p.getNumber().shortValue()));
            TopologyEvent topologyEvent =
                new TopologyEvent(portEvent,
                                  registryService.getOnosInstanceId());
            EventEntry<TopologyEvent> eventEntry =
                    new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                            topologyEvent);
            events.add(eventEntry);
        }

        for (KVDevice d : KVDevice.getAllDevices()) {
            //
            // TODO: Using the local ONOS Instance ID below is incorrect.
            // Currently, this code is not used, and it might go away in the
            // future.
            //
            HostEvent devEvent = new HostEvent(MACAddress.valueOf(d.getMac()));
            for (byte[] portId : d.getAllPortIds()) {
                devEvent.addAttachmentPoint(
                        new SwitchPort(KVPort.getDpidFromKey(portId),
                        KVPort.getNumberFromKey(portId)));
            }
        }

        for (KVLink l : KVLink.getAllLinks()) {
            //
            // TODO: Using the local ONOS Instance ID below is incorrect.
            // Currently, this code is not used, and it might go away in the
            // future.
            //
            LinkEvent linkEvent = new LinkEvent(
                        new SwitchPort(l.getSrc().dpid, l.getSrc().number),
                        new SwitchPort(l.getDst().dpid, l.getDst().number));
            TopologyEvent topologyEvent =
                new TopologyEvent(linkEvent,
                                  registryService.getOnosInstanceId());
            EventEntry<TopologyEvent> eventEntry =
                    new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                            topologyEvent);
            events.add(eventEntry);
        }

        return events;
    }
}
