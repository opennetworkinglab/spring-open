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
import net.onrc.onos.core.metrics.OnosMetrics;
import net.onrc.onos.core.metrics.OnosMetrics.MetricsComponent;
import net.onrc.onos.core.metrics.OnosMetrics.MetricsFeature;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.EventEntry;
import net.onrc.onos.core.util.SwitchPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;

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
public class TopologyManager {

    private static final Logger log = LoggerFactory
            .getLogger(TopologyManager.class);

    private IEventChannel<byte[], TopologyEvent> eventChannel;
    public static final String EVENT_CHANNEL_NAME = "onos.topology";
    private EventHandler eventHandler = new EventHandler();

    private final TopologyImpl topology = new TopologyImpl();
    private TopologyEventPreprocessor eventPreprocessor;
    private CopyOnWriteArrayList<ITopologyListener> topologyListeners =
        new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<ITopologyListener> newTopologyListeners =
        new CopyOnWriteArrayList<>();

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
    // Local state for keeping the last ADD Mastership Event entries.
    // TODO: In the future, we might have to keep this state somewhere else.
    //
    private Map<ByteBuffer, MastershipData> lastAddMastershipDataEntries =
        new HashMap<>();

    //
    // Local state for keeping track of the application event notifications
    //
    //  - Queue of events, which will be dispatched to local listeners
    //    on next notification.

    private List<MastershipData> apiAddedMastershipDataEntries =
        new LinkedList<>();
    private List<MastershipData> apiRemovedMastershipDataEntries =
        new LinkedList<>();
    private List<SwitchData> apiAddedSwitchDataEntries = new LinkedList<>();
    private List<SwitchData> apiRemovedSwitchDataEntries = new LinkedList<>();
    private List<PortData> apiAddedPortDataEntries = new LinkedList<>();
    private List<PortData> apiRemovedPortDataEntries = new LinkedList<>();
    private List<LinkData> apiAddedLinkDataEntries = new LinkedList<>();
    private List<LinkData> apiRemovedLinkDataEntries = new LinkedList<>();
    private List<HostData> apiAddedHostDataEntries = new LinkedList<>();
    private List<HostData> apiRemovedHostDataEntries = new LinkedList<>();

    /**
     * Constructor.
     *
     * @param registryService the Registry Service to use.
     */
    public TopologyManager(IControllerRegistryService registryService) {
        this.eventPreprocessor =
            new TopologyEventPreprocessor(registryService);
    }

    /**
     * Get the MutableTopology.
     *
     * @return the MutableTopology.
     */
    MutableTopology getTopology() {
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
            // Read all topology state
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
            // Process pending (new) listeners
            //
            processPendingListeners();

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
                    // Ignore NO-OP events
                    if (event.isNoop()) {
                        continue;
                    }

                    TopologyEvent topologyEvent = event.eventData();

                    // Get the event itself
                    MastershipData mastershipData =
                        topologyEvent.getMastershipData();
                    SwitchData switchData = topologyEvent.getSwitchData();
                    PortData portData = topologyEvent.getPortData();
                    LinkData linkData = topologyEvent.getLinkData();
                    HostData hostData = topologyEvent.getHostData();
                    boolean wasAdded = false;

                    //
                    // Extract the events
                    //
                    switch (event.eventType()) {
                    case ENTRY_ADD:
                        if (mastershipData != null) {
                            wasAdded = addMastershipData(mastershipData);
                        }
                        if (switchData != null) {
                            wasAdded = addSwitch(switchData);
                        }
                        if (portData != null) {
                            wasAdded = addPort(portData);
                        }
                        if (linkData != null) {
                            wasAdded = addLink(linkData);
                        }
                        if (hostData != null) {
                            wasAdded = addHost(hostData);
                        }
                        // If the item wasn't added, probably it was reordered
                        if (!wasAdded) {
                            ByteBuffer id = topologyEvent.getIDasByteBuffer();
                            eventPreprocessor.reorderedEvents.put(id, topologyEvent);
                        }
                        break;
                    case ENTRY_REMOVE:
                        if (mastershipData != null) {
                            removeMastershipData(mastershipData);
                        }
                        if (switchData != null) {
                            removeSwitch(switchData);
                        }
                        if (portData != null) {
                            removePort(portData);
                        }
                        if (linkData != null) {
                            removeLink(linkData);
                        }
                        if (hostData != null) {
                            removeHost(hostData);
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

        /**
         * Informs the event handler that a new listener has been added,
         * and that listener expects the first event to be a snapshot of the
         * current topology.
         */
        void listenerAdded() {
            //
            // Generate a NO-OP event so the Event Handler processing can be
            // triggered to generate in-order a snapshot of the current
            // topology.
            // TODO: This is a hack.
            //
            EventEntry<TopologyEvent> eventEntry = EventEntry.makeNoop();
            topologyEvents.add(eventEntry);
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
     * Adds a listener for topology events.
     *
     * @param listener the listener to add.
     * @param startFromSnapshot if true, and if the topology is not
     * empty, the first event should be a snapshot of the current topology.
     */
    void addListener(ITopologyListener listener, boolean startFromSnapshot) {
        if (startFromSnapshot) {
            newTopologyListeners.addIfAbsent(listener);
            eventHandler.listenerAdded();
        } else {
            topologyListeners.addIfAbsent(listener);
        }
    }

    /**
     * Removes a listener for topology events. The listener will no longer
     * receive topology events after this call.
     *
     * @param listener the listener to remove.
     */
    void removeListener(ITopologyListener listener) {
        topologyListeners.remove(listener);
        newTopologyListeners.remove(listener);
    }

    /**
     * Processes pending (new) listeners.
     * <p>
     * During the processing, we dispatch Topology Snapshot Events to new
     * listeners.
     */
    private void processPendingListeners() {
        if (newTopologyListeners.isEmpty()) {
            return;
        }

        //
        // Create the Topology Snapshot Event
        //
        TopologyEvents events = null;
        Collection<MastershipData> mastershipDataEntries =
            lastAddMastershipDataEntries.values();
        Collection<SwitchData> switchDataEntries = topology.getAllSwitchDataEntries();
        Collection<PortData> portDataEntries = topology.getAllPortDataEntries();
        Collection<LinkData> linkDataEntries = topology.getAllLinkDataEntries();
        Collection<HostData> hostDataEntries = topology.getAllHostDataEntries();
        if (!(mastershipDataEntries.isEmpty() &&
              switchDataEntries.isEmpty() &&
              portDataEntries.isEmpty() &&
              linkDataEntries.isEmpty() &&
              hostDataEntries.isEmpty())) {
            events = new TopologyEvents(mastershipDataEntries,
                                        switchDataEntries,
                                        portDataEntries,
                                        linkDataEntries,
                                        hostDataEntries);
        }

        //
        // Dispatch Snapshot Event to each new listener, and keep track
        // of each processed listener.
        //
        // NOTE: We deliver the event only if it is not empty.
        // NOTE: We need to execute the loop so we can properly
        // move the new listeners together with the older listeners.
        //
        List<ITopologyListener> processedListeners = new LinkedList<>();
        for (ITopologyListener listener : newTopologyListeners) {
            processedListeners.add(listener);
            // Move the new listener together with the rest of the listeners
            topologyListeners.addIfAbsent(listener);

            // Dispatch the event
            if (events != null) {
                listener.topologyEvents(events);
            }
        }
        newTopologyListeners.removeAll(processedListeners);
    }

    /**
     * Dispatch Topology Events to the listeners.
     */
    private void dispatchTopologyEvents() {
        if (apiAddedMastershipDataEntries.isEmpty() &&
                apiRemovedMastershipDataEntries.isEmpty() &&
                apiAddedSwitchDataEntries.isEmpty() &&
                apiRemovedSwitchDataEntries.isEmpty() &&
                apiAddedPortDataEntries.isEmpty() &&
                apiRemovedPortDataEntries.isEmpty() &&
                apiAddedLinkDataEntries.isEmpty() &&
                apiRemovedLinkDataEntries.isEmpty() &&
                apiAddedHostDataEntries.isEmpty() &&
                apiRemovedHostDataEntries.isEmpty()) {
            return;        // No events to dispatch
        }

        if (log.isDebugEnabled()) {
            //
            // Debug statements
            // TODO: Those statements should be removed in the future
            //
            for (MastershipData mastershipData : apiAddedMastershipDataEntries) {
                log.debug("Dispatch Topology Event: ADDED {}",
                          mastershipData);
            }
            for (MastershipData mastershipData : apiRemovedMastershipDataEntries) {
                log.debug("Dispatch Topology Event: REMOVED {}",
                          mastershipData);
            }
            for (SwitchData switchData : apiAddedSwitchDataEntries) {
                log.debug("Dispatch Topology Event: ADDED {}", switchData);
            }
            for (SwitchData switchData : apiRemovedSwitchDataEntries) {
                log.debug("Dispatch Topology Event: REMOVED {}", switchData);
            }
            for (PortData portData : apiAddedPortDataEntries) {
                log.debug("Dispatch Topology Event: ADDED {}", portData);
            }
            for (PortData portData : apiRemovedPortDataEntries) {
                log.debug("Dispatch Topology Event: REMOVED {}", portData);
            }
            for (LinkData linkData : apiAddedLinkDataEntries) {
                log.debug("Dispatch Topology Event: ADDED {}", linkData);
            }
            for (LinkData linkData : apiRemovedLinkDataEntries) {
                log.debug("Dispatch Topology Event: REMOVED {}", linkData);
            }
            for (HostData hostData : apiAddedHostDataEntries) {
                log.debug("Dispatch Topology Event: ADDED {}", hostData);
            }
            for (HostData hostData : apiRemovedHostDataEntries) {
                log.debug("Dispatch Topology Event: REMOVED {}", hostData);
            }
        }

        //
        // Update the metrics
        //
        long totalEvents =
            apiAddedMastershipDataEntries.size() + apiRemovedMastershipDataEntries.size() +
            apiAddedSwitchDataEntries.size() + apiRemovedSwitchDataEntries.size() +
            apiAddedPortDataEntries.size() + apiRemovedPortDataEntries.size() +
            apiAddedLinkDataEntries.size() + apiRemovedLinkDataEntries.size() +
            apiAddedHostDataEntries.size() + apiRemovedHostDataEntries.size();
        this.listenerEventRate.mark(totalEvents);
        this.lastEventTimestampEpochMs = System.currentTimeMillis();

        //
        // Allocate the events to deliver.
        //
        TopologyEvents events = new TopologyEvents(
                apiAddedMastershipDataEntries,
                apiRemovedMastershipDataEntries,
                apiAddedSwitchDataEntries,
                apiRemovedSwitchDataEntries,
                apiAddedPortDataEntries,
                apiRemovedPortDataEntries,
                apiAddedLinkDataEntries,
                apiRemovedLinkDataEntries,
                apiAddedHostDataEntries,
                apiRemovedHostDataEntries);

        //
        // Deliver the events
        //
        for (ITopologyListener listener : this.topologyListeners) {
            listener.topologyEvents(events);
        }

        //
        // Cleanup
        //
        apiAddedMastershipDataEntries.clear();
        apiRemovedMastershipDataEntries.clear();
        apiAddedSwitchDataEntries.clear();
        apiRemovedSwitchDataEntries.clear();
        apiAddedPortDataEntries.clear();
        apiRemovedPortDataEntries.clear();
        apiAddedLinkDataEntries.clear();
        apiRemovedLinkDataEntries.clear();
        apiAddedHostDataEntries.clear();
        apiRemovedHostDataEntries.clear();
    }

    //
    // Methods to update topology replica
    //

    /**
     * Adds Switch Mastership event.
     *
     * @param mastershipData the MastershipData to process.
     * @return true if the item was successfully added, otherwise false.
     */
    @GuardedBy("topology.writeLock")
    private boolean addMastershipData(MastershipData mastershipData) {
        log.debug("Added Mastership event {}", mastershipData);
        lastAddMastershipDataEntries.put(mastershipData.getIDasByteBuffer(),
                                         mastershipData);
        apiAddedMastershipDataEntries.add(mastershipData);
        return true;
    }

    /**
     * Removes Switch Mastership event.
     *
     * @param mastershipData the MastershipData to process.
     */
    @GuardedBy("topology.writeLock")
    private void removeMastershipData(MastershipData mastershipData) {
        log.debug("Removed Mastership event {}", mastershipData);
        lastAddMastershipDataEntries.remove(mastershipData.getIDasByteBuffer());
        apiRemovedMastershipDataEntries.add(mastershipData);
    }

    /**
     * Adds a switch to the topology replica.
     *
     * @param switchData the SwitchData with the switch to add.
     * @return true if the item was successfully added, otherwise false.
     */
    @GuardedBy("topology.writeLock")
    private boolean addSwitch(SwitchData switchData) {
        if (log.isDebugEnabled()) {
            SwitchData sw = topology.getSwitchData(switchData.getDpid());
            if (sw != null) {
                log.debug("Update {}", switchData);
            } else {
                log.debug("Added {}", switchData);
            }
        }
        topology.putSwitch(switchData.freeze());
        apiAddedSwitchDataEntries.add(switchData);
        return true;
    }

    /**
     * Removes a switch from the topology replica.
     * <p/>
     * It will call {@link #removePort(PortData)} for each ports on this
     * switch.
     *
     * @param switchData the SwitchData with the switch to remove.
     */
    @GuardedBy("topology.writeLock")
    private void removeSwitch(SwitchData switchData) {
        final Dpid dpid = switchData.getDpid();

        SwitchData swInTopo = topology.getSwitchData(dpid);
        if (swInTopo == null) {
            log.warn("Switch {} already removed, ignoring", switchData);
            return;
        }

        //
        // Remove all Ports on the Switch
        //
        ArrayList<PortData> portsToRemove = new ArrayList<>();
        for (Port port : topology.getPorts(dpid)) {
            log.warn("Port {} on Switch {} should be removed prior to removing Switch. Removing Port now.",
                    port, switchData);
            PortData portData = new PortData(port.getSwitchPort());
            portsToRemove.add(portData);
        }
        for (PortData portData : portsToRemove) {
            removePort(portData);
        }

        log.debug("Removed {}", swInTopo);
        topology.removeSwitch(dpid);
        apiRemovedSwitchDataEntries.add(swInTopo);
    }

    /**
     * Adds a port to the topology replica.
     *
     * @param portData the PortData with the port to add.
     * @return true if the item was successfully added, otherwise false.
     */
    @GuardedBy("topology.writeLock")
    private boolean addPort(PortData portData) {
        Switch sw = topology.getSwitch(portData.getDpid());
        if (sw == null) {
            // Reordered event
            log.debug("{} reordered because switch is null", portData);
            return false;
        }

        if (log.isDebugEnabled()) {
            PortData port = topology.getPortData(portData.getSwitchPort());
            if (port != null) {
                log.debug("Update {}", portData);
            } else {
                log.debug("Added {}", portData);
            }
        }
        topology.putPort(portData.freeze());
        apiAddedPortDataEntries.add(portData);
        return true;
    }

    /**
     * Removes a port from the topology replica.
     * <p/>
     * It will remove attachment points from each hosts on this port
     * and call {@link #removeLink(LinkData)} for each links on this port.
     *
     * @param portData the PortData with the port to remove.
     */
    @GuardedBy("topology.writeLock")
    private void removePort(PortData portData) {
        SwitchData sw = topology.getSwitchData(portData.getDpid());
        if (sw == null) {
            log.warn("Parent Switch for Port {} already removed, ignoring",
                    portData);
            return;
        }

        final SwitchPort switchPort = portData.getSwitchPort();
        PortData portInTopo = topology.getPortData(switchPort);
        if (portInTopo == null) {
            log.warn("Port {} already removed, ignoring", portData);
            return;
        }

        //
        // Remove all Host attachment points bound to this Port
        //
        List<HostData> hostsToUpdate = new ArrayList<>();
        for (Host host : topology.getHosts(switchPort)) {
            log.debug("Removing Host {} on Port {}", host, portInTopo);
            HostData hostData = topology.getHostData(host.getMacAddress());
            hostsToUpdate.add(hostData);
        }
        for (HostData hostData : hostsToUpdate) {
            HostData newHostData = new HostData(hostData);
            newHostData.removeAttachmentPoint(switchPort);
            newHostData.freeze();

            // TODO should this event be fired inside #addHost?
            if (newHostData.getAttachmentPoints().isEmpty()) {
                // No more attachment point left -> remove Host
                removeHost(hostData);
            } else {
                // Update Host
                addHost(newHostData);
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
            LinkData linkData = topology.getLinkData(link.getLinkTuple());
            if (linkData != null) {
                log.debug("Removing Link {} on Port {}", link, portInTopo);
                removeLink(linkData);
            }
        }

        // Remove the Port from Topology
        log.debug("Removed {}", portInTopo);
        topology.removePort(switchPort);

        apiRemovedPortDataEntries.add(portInTopo);
    }

    /**
     * Adds a link to the topology replica.
     * <p/>
     * It will remove attachment points from each hosts using the same ports.
     *
     * @param linkData the LinkData with the link to add.
     * @return true if the item was successfully added, otherwise false.
     */
    @GuardedBy("topology.writeLock")
    private boolean addLink(LinkData linkData) {
        PortData srcPort = topology.getPortData(linkData.getSrc());
        PortData dstPort = topology.getPortData(linkData.getDst());
        if ((srcPort == null) || (dstPort == null)) {
            // Reordered event
            log.debug("{} reordered because {} port is null", linkData,
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
        if (topology.getLinkData(linkData.getLinkTuple()) == null) {
            // Only check for existing Host when adding new Link.
            // Remove all Hosts attached to the ports on both ends

            Set<HostData> hostsToUpdate =
                new TreeSet<>(new Comparator<HostData>() {
                // Comparison only using ID(=MAC)
                @Override
                public int compare(HostData o1, HostData o2) {
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
                            host, port, linkData);

                    HostData hostData =
                        topology.getHostData(host.getMacAddress());
                    hostsToUpdate.add(hostData);
                }
            }
            // Remove attachment point from them
            for (HostData hostData : hostsToUpdate) {
                // Remove port from attachment point and update
                HostData newHostData = new HostData(hostData);
                newHostData.removeAttachmentPoint(srcPort.getSwitchPort());
                newHostData.removeAttachmentPoint(dstPort.getSwitchPort());
                newHostData.freeze();

                // TODO should this event be fired inside #addHost?
                if (newHostData.getAttachmentPoints().isEmpty()) {
                    // No more attachment point left -> remove Host
                    removeHost(hostData);
                } else {
                    // Update Host
                    addHost(newHostData);
                }
            }
        }

        if (log.isDebugEnabled()) {
            LinkData link = topology.getLinkData(linkData.getLinkTuple());
            if (link != null) {
                log.debug("Update {}", linkData);
            } else {
                log.debug("Added {}", linkData);
            }
        }
        topology.putLink(linkData.freeze());
        apiAddedLinkDataEntries.add(linkData);
        return true;
    }

    /**
     * Removes a link from the topology replica.
     *
     * @param linkData the LinkData with the link to remove.
     */
    @GuardedBy("topology.writeLock")
    private void removeLink(LinkData linkData) {
        Port srcPort = topology.getPort(linkData.getSrc().getDpid(),
                linkData.getSrc().getPortNumber());
        if (srcPort == null) {
            log.warn("Src Port for Link {} already removed, ignoring",
                    linkData);
            return;
        }

        Port dstPort = topology.getPort(linkData.getDst().getDpid(),
                linkData.getDst().getPortNumber());
        if (dstPort == null) {
            log.warn("Dst Port for Link {} already removed, ignoring",
                    linkData);
            return;
        }

        LinkData linkInTopo = topology.getLinkData(linkData.getLinkTuple(),
                linkData.getType());
        if (linkInTopo == null) {
            log.warn("Link {} already removed, ignoring", linkData);
            return;
        }

        if (log.isDebugEnabled()) {
            // only do sanity check on debug level

            Link linkIn = dstPort.getIncomingLink(linkData.getType());
            if (linkIn == null) {
                log.warn("Link {} already removed on destination Port",
                         linkData);
            }
            Link linkOut = srcPort.getOutgoingLink(linkData.getType());
            if (linkOut == null) {
                log.warn("Link {} already removed on src Port", linkData);
            }
        }

        log.debug("Removed {}", linkInTopo);
        topology.removeLink(linkData.getLinkTuple(), linkData.getType());
        apiRemovedLinkDataEntries.add(linkInTopo);
    }

    /**
     * Adds a host to the topology replica.
     * <p/>
     * TODO: Host-related work is incomplete.
     * TODO: Eventually, we might need to consider reordering
     * or {@link #addLink(LinkData)} and {@link #addHost(HostData)} events
     * on the same port.
     *
     * @param hostData the HostData with the host to add.
     * @return true if the item was successfully added, otherwise false.
     */
    @GuardedBy("topology.writeLock")
    private boolean addHost(HostData hostData) {

        // TODO Decide how to handle update scenario.
        // If the new HostData has less attachment point compared to
        // existing HostData, what should the event be?
        // - Add HostData with some attachment point removed? (current behavior)

        // create unfrozen copy
        //  for removing attachment points which already has a link
        HostData modifiedHostData = new HostData(hostData);

        // Verify each attachment point
        boolean attachmentFound = false;
        for (SwitchPort swp : hostData.getAttachmentPoints()) {
            // XXX domain knowledge: Port must exist before Host
            //      but this knowledge cannot be pushed down to driver.

            // Attached Ports must exist
            Port port = topology.getPort(swp.getDpid(), swp.getPortNumber());
            if (port == null) {
                log.debug("{} reordered because port {} was not there",
                          hostData, swp);
                // Reordered event
                return false; // should not continue if re-applying later
            }
            // Attached Ports must not have Link
            if (port.getOutgoingLink() != null ||
                    port.getIncomingLink() != null) {
                log.warn("Link (Out:{},In:{}) exist on the attachment point. "
                        + "Ignoring this attachmentpoint ({}) from {}.",
                        port.getOutgoingLink(), port.getIncomingLink(),
                        swp, modifiedHostData);
                // FIXME Should either reject, reorder this HostData,
                //       or remove attachment point from given HostData
                // Removing attachment point from given HostData for now.
                modifiedHostData.removeAttachmentPoint(swp);
                continue;
            }

            attachmentFound = true;
        }

        // Update the host in the topology
        if (attachmentFound) {
            if (modifiedHostData.getAttachmentPoints().isEmpty()) {
                log.warn("No valid attachment point left. Ignoring."
                        + "original: {}, modified: {}",
                         hostData, modifiedHostData);
                // TODO Should we call #removeHost to trigger remove event?
                //      only if this call is update.
                return false;
            }

            if (log.isDebugEnabled()) {
                HostData host = topology.getHostData(hostData.getMac());
                if (host != null) {
                    log.debug("Update {}", modifiedHostData);
                } else {
                    log.debug("Added {}", modifiedHostData);
                }
            }
            topology.putHost(modifiedHostData.freeze());
            apiAddedHostDataEntries.add(modifiedHostData);
            return true;
        }
        return false;
    }

    /**
     * Removes a host from the topology replica.
     * <p/>
     * TODO: Host-related work is incomplete.
     *
     * @param hostData the Host Event with the host to remove.
     */
    @GuardedBy("topology.writeLock")
    private void removeHost(HostData hostData) {

        final MACAddress mac = hostData.getMac();
        HostData hostInTopo = topology.getHostData(mac);
        if (hostInTopo == null) {
            log.warn("Host {} already removed, ignoring", hostData);
            return;
        }

        log.debug("Removed {}", hostInTopo);
        topology.removeHost(mac);
        apiRemovedHostDataEntries.add(hostInTopo);
    }
}
