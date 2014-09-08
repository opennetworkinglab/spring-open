package net.onrc.onos.core.topology;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitch.PortChangeType;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.SingletonTask;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.onrc.onos.api.batchoperation.BatchOperationEntry;
import net.onrc.onos.core.configmanager.INetworkConfigService;
import net.onrc.onos.core.configmanager.INetworkConfigService.LinkConfigStatus;
import net.onrc.onos.core.configmanager.INetworkConfigService.NetworkConfigState;
import net.onrc.onos.core.configmanager.INetworkConfigService.SwitchConfigStatus;
import net.onrc.onos.core.datagrid.IDatagridService;
import net.onrc.onos.core.datagrid.IEventChannel;
import net.onrc.onos.core.hostmanager.Host;
import net.onrc.onos.core.hostmanager.IHostListener;
import net.onrc.onos.core.hostmanager.IHostService;
import net.onrc.onos.core.linkdiscovery.ILinkDiscoveryListener;
import net.onrc.onos.core.linkdiscovery.ILinkDiscoveryService;
import net.onrc.onos.core.linkdiscovery.Link;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.registry.IControllerRegistryService.ControlChangeCallback;
import net.onrc.onos.core.registry.RegistryException;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.LinkTuple;
import net.onrc.onos.core.util.OnosInstanceId;
import net.onrc.onos.core.util.PortNumberUtils;
import net.onrc.onos.core.util.SwitchPort;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for publishing topology-related events.
 *
 * The events are received from the discovery modules, reformatted and
 * published to the other ONOS instances.
 *
 * TODO: Add a synchronization mechanism when publishing the events to
 * preserve the ordering and to avoid mismatch in the local "published" state,
 * because each of the caller (the discovery modules) might be running
 * on a different thread.
 */
public class TopologyPublisher implements IOFSwitchListener,
        ILinkDiscoveryListener,
        IFloodlightModule,
        IHostListener,
        ITopologyPublisherService {
    private static final Logger log =
            LoggerFactory.getLogger(TopologyPublisher.class);

    private IFloodlightProviderService floodlightProvider;
    private ILinkDiscoveryService linkDiscovery;
    private IControllerRegistryService registryService;
    private ITopologyService topologyService;
    private IDatagridService datagridService;

    private IHostService hostService;
    private MutableTopology mutableTopology;
    private INetworkConfigService networkConfigService;

    private static final String ENABLE_CLEANUP_PROPERTY = "EnableCleanup";
    private boolean cleanupEnabled = true;
    private static final int CLEANUP_TASK_INTERVAL = 60; // in seconds
    private SingletonTask cleanupTask;
    private DelayedOperationsHandler delayedOperationsHandler =
        new DelayedOperationsHandler();

    private IEventChannel<byte[], TopologyEvent> eventChannel;

    //
    // Local state for keeping track of locally published events so we can
    // cleanup properly when an entry is removed.
    //
    // We keep all Port, (incoming) Link and Host events per Switch DPID:
    //  - If a switch goes down, we remove all corresponding Port, Link and
    //    Host events.
    //  - If a port on a switch goes down, we remove all corresponding Link
    //    and Host events attached to this port.
    //
    // TODO: What to do if the Mastership changes?
    //  - Cleanup state from publishedFoo maps, but do not send REMOVE events?
    //
    private ConcurrentMap<Dpid, MastershipData> publishedMastershipDataEntries =
        new ConcurrentHashMap<>();
    private ConcurrentMap<Dpid, SwitchData> publishedSwitchDataEntries =
        new ConcurrentHashMap<>();
    private ConcurrentMap<Dpid, ConcurrentMap<ByteBuffer, PortData>>
        publishedPortDataEntries = new ConcurrentHashMap<>();
    private ConcurrentMap<Dpid, ConcurrentMap<ByteBuffer, LinkData>>
        publishedLinkDataEntries = new ConcurrentHashMap<>();
    private ConcurrentMap<Dpid, ConcurrentMap<ByteBuffer, HostData>>
        publishedHostDataEntries = new ConcurrentHashMap<>();

    private BlockingQueue<TopologyBatchOperation> delayedOperations =
        new LinkedBlockingQueue<>();


    /**
     * Gets the ONOS Instance ID.
     *
     * @return the ONOS Instance ID.
     */
    private OnosInstanceId getOnosInstanceId() {
        return registryService.getOnosInstanceId();
    }

    /**
     * Cleanup old switches from the topology. Old switches are those which
     * have no controller in the registry.
     *
     * TODO: The overall switch cleanup mechanism needs refactoring/redesign.
     */
    private class SwitchCleanup implements ControlChangeCallback, Runnable {
        @Override
        public void run() {
            String old = Thread.currentThread().getName();
            Thread.currentThread().setName("SwitchCleanup@" + old);

            try {
                if (log.isTraceEnabled()) {
                    log.trace("Running cleanup thread");
                }
                switchCleanup();
            } finally {
                cleanupTask.reschedule(CLEANUP_TASK_INTERVAL,
                        TimeUnit.SECONDS);
                Thread.currentThread().setName(old);
            }
        }

        /**
         * First half of the switch cleanup operation. This method will attempt
         * to get control of any switch it sees without a controller via the
         * registry.
         */
        private void switchCleanup() {
            Iterable<Switch> switches = mutableTopology.getSwitches();

            if (log.isTraceEnabled()) {
                log.trace("Checking for inactive switches");
            }
            // For each switch check if a controller exists in controller
            // registry
            for (Switch sw : switches) {
                // FIXME How to handle case where Switch has never been
                // registered to ZK
                if (sw.getConfigState() == ConfigState.CONFIGURED) {
                    continue;
                }
                try {
                    String controller =
                            registryService.getControllerForSwitch(sw.getDpid().value());
                    if (controller == null) {
                        log.debug("Requesting control to set switch {} INACTIVE",
                                sw.getDpid());
                        registryService.requestControl(sw.getDpid().value(),
                                                       this);
                    }
                } catch (RegistryException e) {
                    log.error("Caught RegistryException in cleanup thread", e);
                }
            }
        }

        /**
         * Second half of the switch cleanup operation. If the registry grants
         * control of a switch, we can be sure no other instance is writing
         * this switch to the topology, so we can remove it now.
         *
         * @param dpid the dpid of the switch we requested control for
         * @param hasControl whether we got control or not
         */
        @Override
        public void controlChanged(long dpid, boolean hasControl) {
            if (hasControl) {
                log.debug("Got control to set switch {} INACTIVE",
                        HexString.toHexString(dpid));

                SwitchData switchData = new SwitchData(new Dpid(dpid));
                publishRemoveSwitchEvent(switchData);
                registryService.releaseControl(dpid);
            }
        }
    }

    /**
     * A class to deal with Topology Operations that couldn't be pushed
     * to the Global Log writer, because they need to be delayed.
     * For example, a link cannot be pushed before the switches on both
     * ends are in the Global Log.
     *
     * TODO: This is an ugly hack that should go away: right now we have to
     * keep trying periodically.
     * TODO: Currently, we retry only ADD Link Events, everything else
     * is thrown away.
     */
    private class DelayedOperationsHandler extends Thread {
        private static final long RETRY_INTERVAL_MS = 10;       // 10ms

        @Override
        public void run() {
            List<TopologyBatchOperation> operations = new LinkedList<>();

            this.setName("TopologyPublisher.DelayedOperationsHandler " +
                         this.getId());
            //
            // The main loop
            //
            while (true) {
                try {
                    //
                    // Block-waiting for an operation to be added, sleep
                    // and try to publish it again.
                    //
                    TopologyBatchOperation firstTbo = delayedOperations.take();
                    Thread.sleep(RETRY_INTERVAL_MS);
                    operations.add(firstTbo);
                    delayedOperations.drainTo(operations);

                    // Retry only the appropriate operations
                    for (TopologyBatchOperation tbo : operations) {
                        for (BatchOperationEntry<
                                TopologyBatchOperation.Operator,
                                TopologyEvent> boe : tbo.getOperations()) {
                            TopologyBatchOperation.Operator oper =
                                boe.getOperator();
                            switch (oper) {
                            case ADD:
                                TopologyEvent topologyEvent = boe.getTarget();
                                LinkData linkData =
                                    topologyEvent.getLinkData();
                                //
                                // Test whether the Link Event still can be
                                // published.
                                // TODO: The implementation below has a bug:
                                // If it happens that the same Link Event was
                                // removed in the middle of checking, we might
                                // incorrectly publish it again from here.
                                //
                                if (linkData == null) {
                                    break;
                                }
                                ConcurrentMap<ByteBuffer, LinkData>
                                    linkDataEntries = publishedLinkDataEntries.get(
                                                linkData.getDst().getDpid());
                                if (linkDataEntries == null) {
                                    break;
                                }
                                if (linkDataEntries.get(linkData.getIDasByteBuffer()) == null) {
                                    break;
                                }
                                publishAddLinkEvent(linkData);
                                break;
                            case REMOVE:
                                break;
                            default:
                                log.error("Unknown Topology Batch Operation {}", oper);
                                break;
                            }
                        }
                    }
                } catch (InterruptedException exception) {
                    log.debug("Exception processing delayed operations: ",
                              exception);
                }
            }
        }
    }

    @Override
    public void linkAdded(Link link) {
        LinkTuple linkTuple = new LinkTuple(
                new SwitchPort(link.getSrc(), link.getSrcPort()),
                new SwitchPort(link.getDst(), link.getDstPort()));

        LinkConfigStatus ret = networkConfigService.checkLinkConfig(linkTuple);
        if (ret.getConfigState() == NetworkConfigState.DENY) {
            log.warn("Discovered {} denied by configuration. {} "
                    + "Not allowing it to proceed.", link, ret.getMsg());
            return;
        }

        LinkData linkData = new LinkData(linkTuple);

        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        linkData.createStringAttribute(TopologyElement.TYPE,
                TopologyElement.TYPE_PACKET_LAYER);
        if (ret.getConfigState() == NetworkConfigState.ACCEPT_ADD) {
            Map<String, String> attr = ret.getLinkConfig().getPublishAttributes();
            for (Entry<String, String> e : attr.entrySet()) {
                linkData.createStringAttribute(e.getKey(), e.getValue());
            }
            linkData.createStringAttribute(TopologyElement.ELEMENT_CONFIG_STATE,
                    ConfigState.CONFIGURED.toString());
        } else {
            linkData.createStringAttribute(TopologyElement.ELEMENT_CONFIG_STATE,
                    ConfigState.NOT_CONFIGURED.toString());
        }
        linkData.createStringAttribute(TopologyElement.ELEMENT_ADMIN_STATUS,
                AdminStatus.ACTIVE.toString());
        linkData.freeze();

        publishAddLinkEvent(linkData);
    }

    @Override
    public void linkRemoved(Link link) {
        LinkData linkData = new LinkData(
                new SwitchPort(link.getSrc(), link.getSrcPort()),
                new SwitchPort(link.getDst(), link.getDstPort()));

        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        linkData.createStringAttribute(TopologyElement.TYPE,
                TopologyElement.TYPE_PACKET_LAYER);
        linkData.freeze();

        publishRemoveLinkEvent(linkData);
    }

    /* *****************
     * IOFSwitchListener
     * *****************/

    @Override
    public void switchActivatedMaster(long swId) {
        IOFSwitch sw = floodlightProvider.getSwitch(swId);
        final Dpid dpid = new Dpid(swId);
        if (sw == null) {
            log.warn("Added switch not available {} ", dpid);
            return;
        }

        SwitchConfigStatus ret = networkConfigService.checkSwitchConfig(dpid);
        if (ret.getConfigState() == NetworkConfigState.DENY) {
            log.warn("Activated switch {} denied by network configuration. {} "
                    + "Not allowing it to proceed.", dpid, ret.getMsg());
            return;
        }

        controllerRoleChanged(dpid, Role.MASTER);

        SwitchData switchData = new SwitchData(dpid);
        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        switchData.createStringAttribute(TopologyElement.TYPE,
                TopologyElement.TYPE_PACKET_LAYER);
        switchData.createStringAttribute("ConnectedSince",
                sw.getConnectedSince().toString());
        switchData.createStringAttribute(TopologyElement.ELEMENT_ADMIN_STATUS,
                AdminStatus.ACTIVE.toString());
        if (ret.getConfigState() == NetworkConfigState.ACCEPT_ADD) {
            Map<String, String> attr = ret.getSwitchConfig().getPublishAttributes();
            for (Entry<String, String> e : attr.entrySet()) {
                switchData.createStringAttribute(e.getKey(), e.getValue());
            }
            switchData.createStringAttribute(TopologyElement.ELEMENT_CONFIG_STATE,
                    ConfigState.CONFIGURED.toString());
        } else {
            switchData.createStringAttribute(TopologyElement.ELEMENT_CONFIG_STATE,
                    ConfigState.NOT_CONFIGURED.toString());
        }
        switchData.freeze();
        // The Port events
        List<PortData> portDataEntries = new ArrayList<PortData>();
        for (OFPortDesc port : sw.getPorts()) {
            PortData portData = new PortData(dpid,
                                                PortNumberUtils.openFlow(port));
            // FIXME should be merging, with existing attrs, etc..
            // TODO define attr name as constant somewhere.
            // TODO populate appropriate attributes.
            portData.createStringAttribute("name", port.getName());
            portData.createStringAttribute(TopologyElement.TYPE,
                    TopologyElement.TYPE_PACKET_LAYER);
            portData.createStringAttribute(TopologyElement.ELEMENT_CONFIG_STATE,
                    ConfigState.NOT_CONFIGURED.toString());
            portData.createStringAttribute(TopologyElement.ELEMENT_ADMIN_STATUS,
                    AdminStatus.ACTIVE.toString());

            portData.freeze();
            portDataEntries.add(portData);
        }
        publishAddSwitchEvent(switchData, portDataEntries);
    }

    @Override
    public void switchActivatedEqual(long swId) {
        final Dpid dpid = new Dpid(swId);
        controllerRoleChanged(dpid, Role.EQUAL);
    }

    @Override
    public void switchMasterToEqual(long swId) {
        final Dpid dpid = new Dpid(swId);
        controllerRoleChanged(dpid, Role.EQUAL);
    }

    @Override
    public void switchEqualToMaster(long swId) {
        // for now treat as switchActivatedMaster
        switchActivatedMaster(swId);
    }

    @Override
    public void switchDisconnected(long swId) {
        final Dpid dpid = new Dpid(swId);

        log.debug("Local switch disconnected: dpid = {} role = {}", dpid);

        Role role = Role.SLAVE; // TODO: Should be Role.UNKNOWN

        MastershipData mastershipData =
                new MastershipData(dpid, getOnosInstanceId(), role);
        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        mastershipData.createStringAttribute(TopologyElement.TYPE,
                TopologyElement.TYPE_ALL_LAYERS);
        mastershipData.freeze();
        publishRemoveSwitchMastershipEvent(mastershipData);
    }

    @Override
    public void switchPortChanged(long swId, OFPortDesc port,
            PortChangeType changeType) {
        switch (changeType) {
        case ADD:
            switchPortAdded(swId, port);
            break;
        case DELETE:
            switchPortRemoved(swId, port);
            break;
        case UP:
            // NOTE: Currently, we treat Port UP/DOWN same as Port ADD/DELETE
            switchPortAdded(swId, port);
            break;
        case DOWN:
            // NOTE: Currently, we treat Port UP/DOWN same as Port ADD/DELETE
            switchPortRemoved(swId, port);
            break;
        case OTHER_UPDATE:
        default:
            // XXX S what is the right set of port change handlers?
            log.debug("Topology publisher does not handle these port updates: {}",
                        changeType);
        }
    }

    /**
     * Prepares an event for adding a port on a switch.
     *
     * @param switchId the switch ID (DPID)
     * @param port the port to add
     */
    private void switchPortAdded(long switchId, OFPortDesc port) {
        final Dpid dpid = new Dpid(switchId);
        final PortData portData = new PortData(dpid,
                                        PortNumberUtils.openFlow(port));
        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        portData.createStringAttribute(TopologyElement.TYPE,
                TopologyElement.TYPE_PACKET_LAYER);
        portData.createStringAttribute("name", port.getName());
        portData.freeze();

        publishAddPortEvent(portData);
    }

    /**
     * Prepares an event for removing a port on a switch.
     *
     * @param switchId the switch ID (DPID)
     * @param port the port to remove
     */
    private void switchPortRemoved(long switchId, OFPortDesc port) {
        final Dpid dpid = new Dpid(switchId);

        final PortData portData = new PortData(dpid,
                                        PortNumberUtils.openFlow(port));
        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        portData.createStringAttribute(TopologyElement.TYPE,
                TopologyElement.TYPE_PACKET_LAYER);
        portData.createStringAttribute("name", port.getName());
        portData.freeze();

        publishRemovePortEvent(portData);
    }

    @Override
    public String getName() {
        return "topologyPublisher";
    }

    /* *****************
     * IFloodlightModule
     * *****************/

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        List<Class<? extends IFloodlightService>> services =
                new ArrayList<Class<? extends IFloodlightService>>();
        services.add(ITopologyPublisherService.class);
        return services;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService>
            getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> impls =
                new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
        impls.put(ITopologyPublisherService.class, this);
        return impls;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>>
            getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        l.add(ILinkDiscoveryService.class);
        l.add(IThreadPoolService.class);
        l.add(IControllerRegistryService.class);
        l.add(IDatagridService.class);
        l.add(ITopologyService.class);
        l.add(IHostService.class);
        l.add(INetworkConfigService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context)
            throws FloodlightModuleException {
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        linkDiscovery = context.getServiceImpl(ILinkDiscoveryService.class);
        registryService = context.getServiceImpl(IControllerRegistryService.class);
        datagridService = context.getServiceImpl(IDatagridService.class);
        hostService = context.getServiceImpl(IHostService.class);
        networkConfigService = context.getServiceImpl(INetworkConfigService.class);
        topologyService = context.getServiceImpl(ITopologyService.class);
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        floodlightProvider.addOFSwitchListener(this);
        linkDiscovery.addListener(this);
        hostService.addHostListener(this);

        eventChannel = datagridService.createChannel(
                                TopologyManager.EVENT_CHANNEL_NAME,
                                byte[].class,
                                TopologyEvent.class);

        mutableTopology = topologyService.getTopology();

        // Run the cleanup thread
        String enableCleanup =
                context.getConfigParams(this).get(ENABLE_CLEANUP_PROPERTY);
        if (enableCleanup != null
                && enableCleanup.equalsIgnoreCase("false")) {
            cleanupEnabled = false;
        }

        log.debug("Cleanup thread is {}enabled", (cleanupEnabled) ? "" : "not ");

        if (cleanupEnabled) {
            IThreadPoolService threadPool =
                    context.getServiceImpl(IThreadPoolService.class);
            cleanupTask = new SingletonTask(threadPool.getScheduledExecutor(),
                    new SwitchCleanup());
            // Run the cleanup task immediately on startup
            cleanupTask.reschedule(0, TimeUnit.SECONDS);
        }

        // Run the Delayed Operations Handler thread
        delayedOperationsHandler.start();
    }

    @Override
    public void hostAdded(Host host) {
        log.debug("Host added with MAC {}", host.getMacAddress());

        SwitchPort sp = new SwitchPort(host.getSwitchDPID(), host.getSwitchPort());
        List<SwitchPort> spLists = new ArrayList<SwitchPort>();
        spLists.add(sp);
        HostData hostData = new HostData(host.getMacAddress());
        hostData.setAttachmentPoints(spLists);
        hostData.setLastSeenTime(host.getLastSeenTimestamp().getTime());
        // Does not use vlan info now.
        hostData.freeze();

        publishAddHostEvent(hostData);
    }

    @Override
    public void hostRemoved(Host host) {
        log.debug("Host removed with MAC {}", host.getMacAddress());

        //
        // Remove all previously added HostData for this MAC address
        //
        // TODO: Currently, the caller of hostRemoved() might not include
        // the correct set of Attachment Points in the HostData entry itself.
        // Also, we might have multiple HostData entries for the same
        // host (MAC address), each containing a single (different) Attachment
        // Point.
        // Hence, here we have to cleanup all HostData entries for this
        // particular host, based on its MAC address.
        //
        List<HostData> removeHostDataEntries = new LinkedList<>();
        for (ConcurrentMap<ByteBuffer, HostData> cm : publishedHostDataEntries.values()) {
            for (HostData hostData : cm.values()) {
                if (hostData.getMac().equals(host.getMacAddress())) {
                    removeHostDataEntries.add(hostData);
                }
            }
        }
        for (HostData hostData : removeHostDataEntries) {
            publishRemoveHostEvent(hostData);
        }
    }

    @Override
    public boolean publish(TopologyBatchOperation tbo) {
        publishTopologyOperations(tbo);
        return true;
    }

    /**
     * Prepares the Controller role changed event for a switch.
     *
     * @param dpid the switch DPID
     * @param role the new role of the controller
     */
    private void controllerRoleChanged(Dpid dpid, Role role) {
        log.debug("Local switch controller mastership role changed: dpid = {} role = {}",
                dpid, role);
        MastershipData mastershipData =
                new MastershipData(dpid, getOnosInstanceId(), role);
        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        mastershipData.createStringAttribute(TopologyElement.TYPE,
                TopologyElement.TYPE_ALL_LAYERS);
        mastershipData.freeze();
        publishAddSwitchMastershipEvent(mastershipData);
    }

    /**
     * Publishes ADD Mastership Event.
     *
     * @param mastershipData the mastership event to publish
     */
    private void publishAddSwitchMastershipEvent(
                        MastershipData mastershipData) {
        // Publish the information
        TopologyBatchOperation tbo = new TopologyBatchOperation();
        TopologyEvent topologyEvent =
            new TopologyEvent(mastershipData, getOnosInstanceId());
        tbo.appendAddOperation(topologyEvent);
        publishTopologyOperations(tbo);
        publishedMastershipDataEntries.put(mastershipData.getDpid(),
                                           mastershipData);
    }

    /**
     * Publishes REMOVE Mastership Event.
     *
     * @param mastershipData the mastership event to publish
     */
    private void publishRemoveSwitchMastershipEvent(
                        MastershipData mastershipData) {
        if (publishedMastershipDataEntries.get(mastershipData.getDpid()) == null) {
            return;     // Nothing to do
        }

        // Publish the information
        TopologyBatchOperation tbo = new TopologyBatchOperation();
        TopologyEvent topologyEvent =
            new TopologyEvent(mastershipData, getOnosInstanceId());
        tbo.appendRemoveOperation(topologyEvent);
        publishTopologyOperations(tbo);
        publishedMastershipDataEntries.remove(mastershipData.getDpid());
    }

    /**
     * Publishes ADD Switch Event.
     *
     * @param switchData the switch event to publish
     * @param portDataEntries the corresponding port events for the switch to
     * publish
     */
    private void publishAddSwitchEvent(SwitchData switchData,
                                       Collection<PortData> portDataEntries) {
        if (!registryService.hasControl(switchData.getOriginDpid().value())) {
            log.debug("Not the master for switch {}. Suppressed switch add event {}.",
                      switchData.getOriginDpid(), switchData);
            return;
        }

        // Keep track of the old Port Events that should be removed
        ConcurrentMap<ByteBuffer, PortData> oldPortDataEntries =
            publishedPortDataEntries.get(switchData.getDpid());
        if (oldPortDataEntries == null) {
            oldPortDataEntries = new ConcurrentHashMap<>();
        }

        // Publish the information for the switch
        TopologyBatchOperation tbo = new TopologyBatchOperation();
        TopologyEvent topologyEvent =
            new TopologyEvent(switchData, getOnosInstanceId());
        tbo.appendAddOperation(topologyEvent);

        // Publish the information for each port
        ConcurrentMap<ByteBuffer, PortData> newPortDataEntries =
            new ConcurrentHashMap<>();
        for (PortData portData : portDataEntries) {
            topologyEvent =
                new TopologyEvent(portData, getOnosInstanceId());
            tbo.appendAddOperation(topologyEvent);

            ByteBuffer id = portData.getIDasByteBuffer();
            newPortDataEntries.put(id, portData);
            oldPortDataEntries.remove(id);
        }
        publishTopologyOperations(tbo);
        publishedSwitchDataEntries.put(switchData.getDpid(), switchData);
        publishedPortDataEntries.put(switchData.getDpid(), newPortDataEntries);

        // Cleanup for each of the old removed port
        for (PortData portData : oldPortDataEntries.values()) {
            publishRemovePortEvent(portData);
        }
    }

    /**
     * Publishes REMOVE Switch Event.
     *
     * @param switchData the switch event to publish
     */
    private void publishRemoveSwitchEvent(SwitchData switchData) {
        //
        // TODO: Removed the check for now, because currently this method is
        // also called by the SwitchCleanup thread, and in that case
        // the Switch Event was published by some other ONOS instance.
        //
        /*
        if (publishedSwitchDataEntries.get(switchData.getDpid()) == null) {
            return;     // Nothing to do
        }
        */

        // Publish the information
        TopologyBatchOperation tbo = new TopologyBatchOperation();
        TopologyEvent topologyEvent =
            new TopologyEvent(switchData, getOnosInstanceId());
        tbo.appendRemoveOperation(topologyEvent);
        publishTopologyOperations(tbo);
        publishedSwitchDataEntries.remove(switchData.getDpid());

        // Cleanup for each port
        ConcurrentMap<ByteBuffer, PortData> portDataEntries =
            publishedPortDataEntries.get(switchData.getDpid());
        if (portDataEntries != null) {
            for (PortData portData : portDataEntries.values()) {
                publishRemovePortEvent(portData);
            }
        }

        publishedPortDataEntries.remove(switchData.getDpid());
        publishedLinkDataEntries.remove(switchData.getDpid());
        publishedHostDataEntries.remove(switchData.getDpid());
    }

    /**
     * Publishes ADD Port Event.
     *
     * @param portData the port event to publish
     */
    private void publishAddPortEvent(PortData portData) {
        if (!registryService.hasControl(portData.getOriginDpid().value())) {
            log.debug("Not the master for switch {}. Suppressed port add event {}.",
                      portData.getOriginDpid(), portData);
            return;
        }

        // Publish the information
        TopologyBatchOperation tbo = new TopologyBatchOperation();
        TopologyEvent topologyEvent =
            new TopologyEvent(portData, getOnosInstanceId());
        tbo.appendAddOperation(topologyEvent);
        publishTopologyOperations(tbo);

        // Store the new Port Event in the local cache
        ConcurrentMap<ByteBuffer, PortData> portDataEntries =
            ConcurrentUtils.putIfAbsent(publishedPortDataEntries,
                        portData.getDpid(),
                        new ConcurrentHashMap<ByteBuffer, PortData>());
        portDataEntries.put(portData.getIDasByteBuffer(), portData);
    }

    /**
     * Publishes REMOVE Port Event.
     *
     * @param portData the port event to publish
     */
    private void publishRemovePortEvent(PortData portData) {
        ConcurrentMap<ByteBuffer, PortData> portDataEntries =
            publishedPortDataEntries.get(portData.getDpid());
        if (portDataEntries == null) {
            return;     // Nothing to do
        }
        if (portDataEntries.get(portData.getIDasByteBuffer()) == null) {
            return;     // Nothing to do
        }

        // Publish the information
        TopologyBatchOperation tbo = new TopologyBatchOperation();
        TopologyEvent topologyEvent =
            new TopologyEvent(portData, getOnosInstanceId());
        tbo.appendRemoveOperation(topologyEvent);
        publishTopologyOperations(tbo);

        // Cleanup for the incoming link(s)
        ConcurrentMap<ByteBuffer, LinkData> linkDataEntries =
            publishedLinkDataEntries.get(portData.getDpid());
        if (linkDataEntries != null) {
            for (LinkData linkData : linkDataEntries.values()) {
                if (linkData.getDst().equals(portData.getSwitchPort())) {
                    publishRemoveLinkEvent(linkData);
                }
            }
        }

        // Cleanup for the connected hosts
        ConcurrentMap<ByteBuffer, HostData> hostDataEntries =
            publishedHostDataEntries.get(portData.getDpid());
        if (hostDataEntries != null) {
            for (HostData hostData : hostDataEntries.values()) {
                for (SwitchPort swp : hostData.getAttachmentPoints()) {
                    if (swp.equals(portData.getSwitchPort())) {
                        publishRemoveHostEvent(hostData);
                    }
                }
            }
        }

        portDataEntries.remove(portData.getIDasByteBuffer());
    }

    /**
     * Publishes ADD Link Event.
     *
     * @param linkData the link event to publish
     */
    private void publishAddLinkEvent(LinkData linkData) {
        if (!registryService.hasControl(linkData.getOriginDpid().value())) {
            log.debug("Not the master for dst switch {}. Suppressed link add event {}.",
                      linkData.getOriginDpid(), linkData);
            return;
        }

        // Publish the information
        TopologyBatchOperation tbo = new TopologyBatchOperation();
        TopologyEvent topologyEvent =
            new TopologyEvent(linkData, getOnosInstanceId());
        tbo.appendAddOperation(topologyEvent);
        publishTopologyOperations(tbo);

        // Store the new Link Event in the local cache
        ConcurrentMap<ByteBuffer, LinkData> linkDataEntries =
            ConcurrentUtils.putIfAbsent(publishedLinkDataEntries,
                        linkData.getDst().getDpid(),
                        new ConcurrentHashMap<ByteBuffer, LinkData>());
        linkDataEntries.put(linkData.getIDasByteBuffer(), linkData);
    }

    /**
     * Publishes REMOVE Link Event.
     *
     * @param linkData the link event to publish
     */
    private void publishRemoveLinkEvent(LinkData linkData) {
        ConcurrentMap<ByteBuffer, LinkData> linkDataEntries =
            publishedLinkDataEntries.get(linkData.getDst().getDpid());
        if (linkDataEntries == null) {
            return;     // Nothing to do
        }
        if (linkDataEntries.get(linkData.getIDasByteBuffer()) == null) {
            return;     // Nothing to do
        }

        // Publish the information
        TopologyBatchOperation tbo = new TopologyBatchOperation();
        TopologyEvent topologyEvent =
            new TopologyEvent(linkData, getOnosInstanceId());
        tbo.appendRemoveOperation(topologyEvent);
        publishTopologyOperations(tbo);

        linkDataEntries.remove(linkData.getIDasByteBuffer());
    }

    /**
     * Publishes ADD Host Event.
     *
     * @param hostData the host event to publish
     */
    private void publishAddHostEvent(HostData hostData) {
        //
        // NOTE: The implementation below assumes that there is just one
        // attachment point stored in hostData. Currently, this assumption
        // is true based on the existing implementation of the caller
        // hostAdded().
        //

        if (!registryService.hasControl(hostData.getOriginDpid().value())) {
            log.debug("Not the master for attachment switch {}. Suppressed host add event {}.",
                      hostData.getOriginDpid(), hostData);
            return;
        }

        // Publish the information
        TopologyBatchOperation tbo = new TopologyBatchOperation();
        TopologyEvent topologyEvent =
            new TopologyEvent(hostData, getOnosInstanceId());
        tbo.appendAddOperation(topologyEvent);
        publishTopologyOperations(tbo);

        // Store the new Host Event in the local cache
        ConcurrentMap<ByteBuffer, HostData> hostDataEntries =
            ConcurrentUtils.putIfAbsent(publishedHostDataEntries,
                hostData.getOriginDpid(),
                new ConcurrentHashMap<ByteBuffer, HostData>());
        hostDataEntries.put(hostData.getIDasByteBuffer(), hostData);
    }

    /**
     * Publishes REMOVE Host Event.
     *
     * @param hostData the host event to publish
     */
    private void publishRemoveHostEvent(HostData hostData) {
        ConcurrentMap<ByteBuffer, HostData> hostDataEntries =
            publishedHostDataEntries.get(hostData.getOriginDpid());
        if (hostDataEntries == null) {
            return;     // Nothing to do
        }
        if (hostDataEntries.get(hostData.getIDasByteBuffer()) == null) {
            return;     // Nothing to do
        }

        // Publish the information
        TopologyBatchOperation tbo = new TopologyBatchOperation();
        TopologyEvent topologyEvent =
            new TopologyEvent(hostData, getOnosInstanceId());
        tbo.appendRemoveOperation(topologyEvent);
        publishTopologyOperations(tbo);

        hostDataEntries.remove(hostData.getIDasByteBuffer());
    }

    /**
     * Publishes Topology Operations.
     *
     * @param tbo the Topology Operations to publish
     */
    private void publishTopologyOperations(TopologyBatchOperation tbo) {
        // TODO: This flag should be configurable
        boolean isGlobalLogWriter = false;

        log.debug("Publishing: {}", tbo);

        if (isGlobalLogWriter) {
            if (!topologyService.publish(tbo)) {
                log.debug("Cannot publish: {}", tbo);
                delayedOperations.add(tbo);
            }
        } else {
            // TODO: For now we publish each TopologyEvent independently
            for (BatchOperationEntry<TopologyBatchOperation.Operator,
                     TopologyEvent> boe : tbo.getOperations()) {
                TopologyBatchOperation.Operator oper = boe.getOperator();
                TopologyEvent topologyEvent = boe.getTarget();
                switch (oper) {
                case ADD:
                    eventChannel.addEntry(topologyEvent.getID(),
                                          topologyEvent);
                    break;
                case REMOVE:
                    eventChannel.removeEntry(topologyEvent.getID());
                    break;
                default:
                    log.error("Unknown Topology Batch Operation {}", oper);
                    break;
                }
            }
        }
    }
}
