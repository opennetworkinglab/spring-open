package net.onrc.onos.core.topology;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The TopologyPublisher subscribes to topology network events from the
 * discovery modules. These events are reformatted and relayed to the in-memory
 * topology instance.
 */
public class TopologyPublisher implements IOFSwitchListener,
        ILinkDiscoveryListener,
        IFloodlightModule,
        IHostListener {
    private static final Logger log =
            LoggerFactory.getLogger(TopologyPublisher.class);

    private IFloodlightProviderService floodlightProvider;
    private ILinkDiscoveryService linkDiscovery;
    private IControllerRegistryService registryService;
    private ITopologyService topologyService;
    private IDatagridService datagridService;

    private IHostService hostService;

    private Topology topology;

    private static final String ENABLE_CLEANUP_PROPERTY = "EnableCleanup";
    private boolean cleanupEnabled = true;
    private static final int CLEANUP_TASK_INTERVAL = 60; // in seconds
    private SingletonTask cleanupTask;

    private IEventChannel<byte[], TopologyEvent> eventChannel;

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
    //  - Probably should: remove from discovered.. Maps, but not send DELETE
    //    events
    //    XXX Switch/Port can be rediscovered by new leader, but Link, Host?
    //  - Current: There is no way to recognize leadership change?
    //      ZookeeperRegistry.requestControl(long, ControlChangeCallback)
    //      is the only way to register listener, and it allows only one
    //      listener, which is already used by Controller class.
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


    /**
     * Cleanup old switches from the topology. Old switches are those which have
     * no controller in the registry.
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
            Iterable<Switch> switches = topology.getSwitches();

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
                        registryService.requestControl(sw.getDpid().value(), this);
                    }
                } catch (RegistryException e) {
                    log.error("Caught RegistryException in cleanup thread", e);
                }
            }
        }

        /**
         * Second half of the switch cleanup operation. If the registry grants
         * control of a switch, we can be sure no other instance is writing this
         * switch to the topology, so we can remove it now.
         * <p>
         * @param dpid the dpid of the switch we requested control for
         * @param hasControl whether we got control or not
         */
        @Override
        public void controlChanged(long dpid, boolean hasControl) {
            if (hasControl) {
                log.debug("Got control to set switch {} INACTIVE",
                        HexString.toHexString(dpid));

                SwitchEvent switchEvent = new SwitchEvent(new Dpid(dpid));
                removeSwitchDiscoveryEvent(switchEvent);
                registryService.releaseControl(dpid);
            }
        }
    }

    @Override
    public void linkAdded(Link link) {
        LinkEvent linkEvent = new LinkEvent(
                new SwitchPort(link.getSrc(), link.getSrcPort()),
                new SwitchPort(link.getDst(), link.getDstPort()));

        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        linkEvent.createStringAttribute(TopologyElement.TYPE,
                TopologyElement.TYPE_PACKET_LAYER);
        linkEvent.createStringAttribute(TopologyElement.ELEMENT_CONFIG_STATE,
                ConfigState.NOT_CONFIGURED.toString());
        linkEvent.createStringAttribute(TopologyElement.ELEMENT_ADMIN_STATUS,
                AdminStatus.ACTIVE.toString());
        linkEvent.freeze();

        if (!registryService.hasControl(link.getDst())) {
            // Don't process or send a link event if we're not master for the
            // destination switch
            log.debug("Not the master for dst switch {}. Suppressed link add event {}.",
                    link.getDst(), linkEvent);
            return;
        }
        putLinkDiscoveryEvent(linkEvent);
    }

    @Override
    public void linkRemoved(Link link) {
        LinkEvent linkEvent = new LinkEvent(
                new SwitchPort(link.getSrc(), link.getSrcPort()),
                new SwitchPort(link.getDst(), link.getDstPort()));

        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        linkEvent.createStringAttribute(TopologyElement.TYPE,
                TopologyElement.TYPE_PACKET_LAYER);
        linkEvent.freeze();

        if (!registryService.hasControl(link.getDst())) {
            // Don't process or send a link event if we're not master for the
            // destination switch
            log.debug(
                    "Not the master for dst switch {}. Suppressed link remove event {}.",
                    link.getDst(), linkEvent);
            return;
        }
        removeLinkDiscoveryEvent(linkEvent);
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

        controllerRoleChanged(dpid, Role.MASTER);

        SwitchEvent switchEvent = new SwitchEvent(dpid);
        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        switchEvent.createStringAttribute(TopologyElement.TYPE,
                TopologyElement.TYPE_PACKET_LAYER);
        switchEvent.createStringAttribute("ConnectedSince",
                sw.getConnectedSince().toString());
        switchEvent.createStringAttribute(TopologyElement.ELEMENT_CONFIG_STATE,
                ConfigState.NOT_CONFIGURED.toString());
        switchEvent.createStringAttribute(TopologyElement.ELEMENT_ADMIN_STATUS,
                AdminStatus.ACTIVE.toString());
        switchEvent.freeze();
        // TODO Not very robust
        if (!registryService.hasControl(swId)) {
            log.debug("Not the master for switch {}. Suppressed switch add event {}.",
                    dpid, switchEvent);
            return;
        }
        List<PortEvent> portEvents = new ArrayList<PortEvent>();
        for (OFPortDesc port : sw.getPorts()) {
            PortEvent portEvent = new PortEvent(dpid,
                    new PortNumber(port.getPortNo().getShortPortNumber()));
            // FIXME should be merging, with existing attrs, etc..
            // TODO define attr name as constant somewhere.
            // TODO populate appropriate attributes.
            portEvent.createStringAttribute("name", port.getName());
            portEvent.createStringAttribute(TopologyElement.TYPE,
                    TopologyElement.TYPE_PACKET_LAYER);
            portEvent.createStringAttribute(TopologyElement.ELEMENT_CONFIG_STATE,
                    ConfigState.NOT_CONFIGURED.toString());
            portEvent.createStringAttribute(TopologyElement.ELEMENT_ADMIN_STATUS,
                    AdminStatus.ACTIVE.toString());

            portEvent.freeze();
            portEvents.add(portEvent);
        }
        putSwitchDiscoveryEvent(switchEvent, portEvents);

        for (OFPortDesc port : sw.getPorts()) {
            // Allow links to be discovered on this port now that it's
            // in the database
            linkDiscovery.enableDiscoveryOnPort(sw.getId(),
                    port.getPortNo().getShortPortNumber());
        }
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

        MastershipEvent mastershipEvent =
                new MastershipEvent(dpid, registryService.getOnosInstanceId(),
                        role);
        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        mastershipEvent.createStringAttribute(TopologyElement.TYPE,
                TopologyElement.TYPE_ALL_LAYERS);
        mastershipEvent.freeze();
        removeSwitchMastershipEvent(mastershipEvent);
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

    private void switchPortAdded(long switchId, OFPortDesc port) {
        final Dpid dpid = new Dpid(switchId);
        PortEvent portEvent = new PortEvent(dpid,
                new PortNumber(port.getPortNo().getShortPortNumber()));
        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        portEvent.createStringAttribute(TopologyElement.TYPE,
                TopologyElement.TYPE_PACKET_LAYER);
        portEvent.createStringAttribute("name", port.getName());

        portEvent.freeze();

        if (registryService.hasControl(switchId)) {
            putPortDiscoveryEvent(portEvent);
            linkDiscovery.enableDiscoveryOnPort(switchId,
                    port.getPortNo().getShortPortNumber());
        } else {
            log.debug("Not the master for switch {}. Suppressed port add event {}.",
                    new Dpid(switchId), portEvent);
        }
    }

    private void switchPortRemoved(long switchId, OFPortDesc port) {
        final Dpid dpid = new Dpid(switchId);

        PortEvent portEvent = new PortEvent(dpid, new PortNumber(
                port.getPortNo().getShortPortNumber()));
        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        portEvent.createStringAttribute(TopologyElement.TYPE,
                TopologyElement.TYPE_PACKET_LAYER);
        portEvent.createStringAttribute("name", port.getName());

        portEvent.freeze();

        if (registryService.hasControl(switchId)) {
            removePortDiscoveryEvent(portEvent);
        } else {
            log.debug("Not the master for switch {}. Suppressed port del event {}.",
                    dpid, portEvent);
        }
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
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService>
            getServiceImpls() {
        return null;
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

        topology = topologyService.getTopology();

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
    }

    @Override
    public void hostAdded(Host host) {
        log.debug("Called onosDeviceAdded mac {}", host.getMacAddress());

        SwitchPort sp = new SwitchPort(host.getSwitchDPID(), host.getSwitchPort());
        List<SwitchPort> spLists = new ArrayList<SwitchPort>();
        spLists.add(sp);
        HostEvent event = new HostEvent(host.getMacAddress());
        event.setAttachmentPoints(spLists);
        event.setLastSeenTime(host.getLastSeenTimestamp().getTime());
        // Does not use vlan info now.
        event.freeze();

        putHostDiscoveryEvent(event);
    }

    @Override
    public void hostRemoved(Host host) {
        log.debug("Called onosDeviceRemoved");
        HostEvent event = new HostEvent(host.getMacAddress());
        // XXX shouldn't we be setting attachment points?
        event.freeze();
        removeHostDiscoveryEvent(event);
    }

    private void controllerRoleChanged(Dpid dpid, Role role) {
        log.debug("Local switch controller mastership role changed: dpid = {} role = {}",
                dpid, role);
        MastershipEvent mastershipEvent =
                new MastershipEvent(dpid, registryService.getOnosInstanceId(),
                        role);
        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        mastershipEvent.createStringAttribute(TopologyElement.TYPE,
                TopologyElement.TYPE_ALL_LAYERS);
        mastershipEvent.freeze();
        putSwitchMastershipEvent(mastershipEvent);
    }

    /**
     * Mastership updated event.
     *
     * @param mastershipEvent the mastership event.
     */
    private void putSwitchMastershipEvent(MastershipEvent mastershipEvent) {
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
    private void removeSwitchMastershipEvent(MastershipEvent mastershipEvent) {
        // Send out notification
        TopologyEvent topologyEvent =
            new TopologyEvent(mastershipEvent,
                              registryService.getOnosInstanceId());
        eventChannel.removeEntry(topologyEvent.getID());
    }

    /**
     * Switch discovered event.
     *
     * @param switchEvent the switch event.
     * @param portEvents  the corresponding port events for the switch.
     */
    private void putSwitchDiscoveryEvent(SwitchEvent switchEvent,
                                         Collection<PortEvent> portEvents) {
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
        discoveredAddedPortEvents.put(switchEvent.getDpid(), newPortEvents);

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

    /**
     * Switch removed event.
     *
     * @param switchEvent the switch event.
     */
    private void removeSwitchDiscoveryEvent(SwitchEvent switchEvent) {
        TopologyEvent topologyEvent;

        // Get the old Port Events
        Map<ByteBuffer, PortEvent> oldPortEvents =
                discoveredAddedPortEvents.get(switchEvent.getDpid());
        if (oldPortEvents == null) {
            oldPortEvents = new HashMap<>();
        }

        log.debug("Sending remove switch: {}", switchEvent);
        // Send out notification
        topologyEvent =
            new TopologyEvent(switchEvent,
                              registryService.getOnosInstanceId());
        eventChannel.removeEntry(topologyEvent.getID());

        //
        // Send out notification for each port.
        //
        // NOTE: We don't use removePortDiscoveryEvent() for the cleanup,
        // because it will attempt to remove the port from the database,
        // and the deactiveSwitch() call above already removed all ports.
        //
        for (PortEvent portEvent : oldPortEvents.values()) {
            log.debug("Sending remove port:", portEvent);
            topologyEvent =
                new TopologyEvent(portEvent,
                                  registryService.getOnosInstanceId());
            eventChannel.removeEntry(topologyEvent.getID());
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

    /**
     * Port discovered event.
     *
     * @param portEvent the port event.
     */
    private void putPortDiscoveryEvent(PortEvent portEvent) {
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
            discoveredAddedPortEvents.put(portEvent.getDpid(), oldPortEvents);
        }
        ByteBuffer id = portEvent.getIDasByteBuffer();
        oldPortEvents.put(id, portEvent);
    }

    /**
     * Port removed event.
     *
     * @param portEvent the port event.
     */
    private void removePortDiscoveryEvent(PortEvent portEvent) {
        log.debug("Sending remove port: {}", portEvent);
        // Send out notification
        TopologyEvent topologyEvent =
            new TopologyEvent(portEvent,
                              registryService.getOnosInstanceId());
        eventChannel.removeEntry(topologyEvent.getID());

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

    /**
     * Link discovered event.
     *
     * @param linkEvent the link event.
     */
    private void putLinkDiscoveryEvent(LinkEvent linkEvent) {
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

    /**
     * Link removed event.
     *
     * @param linkEvent the link event.
     */
    private void removeLinkDiscoveryEvent(LinkEvent linkEvent) {
        log.debug("Sending remove link: {}", linkEvent);
        // Send out notification
        TopologyEvent topologyEvent =
            new TopologyEvent(linkEvent,
                              registryService.getOnosInstanceId());
        eventChannel.removeEntry(topologyEvent.getID());

        // Cleanup the Link Event from the local cache
        Map<ByteBuffer, LinkEvent> oldLinkEvents =
            discoveredAddedLinkEvents.get(linkEvent.getDst().getDpid());
        if (oldLinkEvents != null) {
            ByteBuffer id = linkEvent.getIDasByteBuffer();
            oldLinkEvents.remove(id);
        }
    }

    /**
     * Host discovered event.
     *
     * @param hostEvent the host event.
     */
    private void putHostDiscoveryEvent(HostEvent hostEvent) {
        // Send out notification
        TopologyEvent topologyEvent =
            new TopologyEvent(hostEvent,
                              registryService.getOnosInstanceId());
        eventChannel.addEntry(topologyEvent.getID(), topologyEvent);
        log.debug("Put the host info into the cache of the topology. mac {}",
                  hostEvent.getMac());

        // Store the new Host Event in the local cache
        // TODO: The implementation below is probably wrong
        for (SwitchPort swp : hostEvent.getAttachmentPoints()) {
            Map<ByteBuffer, HostEvent> oldHostEvents =
                discoveredAddedHostEvents.get(swp.getDpid());
            if (oldHostEvents == null) {
                oldHostEvents = new HashMap<>();
                discoveredAddedHostEvents.put(swp.getDpid(), oldHostEvents);
            }
            ByteBuffer id = hostEvent.getIDasByteBuffer();
            oldHostEvents.put(id, hostEvent);
        }
    }

    /**
     * Host removed event.
     *
     * @param hostEvent the host event.
     */
    private void removeHostDiscoveryEvent(HostEvent hostEvent) {
        // Send out notification
        TopologyEvent topologyEvent =
            new TopologyEvent(hostEvent,
                              registryService.getOnosInstanceId());
        eventChannel.removeEntry(topologyEvent.getID());
        log.debug("Remove the host info into the cache of the topology. mac {}",
                  hostEvent.getMac());

        // Cleanup the Host Event from the local cache
        // TODO: The implementation below is probably wrong
        ByteBuffer id = hostEvent.getIDasByteBuffer();
        for (SwitchPort swp : hostEvent.getAttachmentPoints()) {
            Map<ByteBuffer, HostEvent> oldHostEvents =
                discoveredAddedHostEvents.get(swp.getDpid());
            if (oldHostEvents != null) {
                oldHostEvents.remove(id);
            }
        }
    }
}
