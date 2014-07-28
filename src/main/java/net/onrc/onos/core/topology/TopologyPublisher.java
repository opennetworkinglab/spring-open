package net.onrc.onos.core.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.SingletonTask;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.onrc.onos.api.registry.ILocalSwitchMastershipListener;
import net.onrc.onos.core.hostmanager.Host;
import net.onrc.onos.core.hostmanager.IHostListener;
import net.onrc.onos.core.hostmanager.IHostService;
import net.onrc.onos.core.linkdiscovery.ILinkDiscoveryListener;
import net.onrc.onos.core.linkdiscovery.ILinkDiscoveryService;
import net.onrc.onos.core.linkdiscovery.Link;
import net.onrc.onos.core.main.IOFSwitchPortListener;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.registry.IControllerRegistryService.ControlChangeCallback;
import net.onrc.onos.core.registry.RegistryException;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.openflow.protocol.OFPhysicalPort;
import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The TopologyPublisher subscribes to topology network events from the
 * discovery modules. These events are reformatted and relayed to the in-memory
 * topology instance.
 */
public class TopologyPublisher implements /*IOFSwitchListener,*/
        IOFSwitchPortListener,
        ILinkDiscoveryListener,
        IFloodlightModule,
        IHostListener,
        ILocalSwitchMastershipListener {
    private static final Logger log =
            LoggerFactory.getLogger(TopologyPublisher.class);

    private IFloodlightProviderService floodlightProvider;
    private ILinkDiscoveryService linkDiscovery;
    private IControllerRegistryService registryService;
    private ITopologyService topologyService;

    private IHostService hostService;

    private Topology topology;
    private TopologyDiscoveryInterface topologyDiscoveryInterface;

    private static final String ENABLE_CLEANUP_PROPERTY = "EnableCleanup";
    private boolean cleanupEnabled = true;
    private static final int CLEANUP_TASK_INTERVAL = 60; // in seconds
    private SingletonTask cleanupTask;

    /**
     * Cleanup old switches from the topology. Old switches are those
     * which have no controller in the registry.
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
            // For each switch check if a controller exists in controller registry
            for (Switch sw : switches) {
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
         * control of a switch, we can be sure no other instance is writing
         * this switch to the topology, so we can remove it now.
         *
         * @param dpid       the dpid of the switch we requested control for
         * @param hasControl whether we got control or not
         */
        @Override
        public void controlChanged(long dpid, boolean hasControl) {
            if (hasControl) {
                log.debug("Got control to set switch {} INACTIVE",
                        HexString.toHexString(dpid));

                SwitchEvent switchEvent = new SwitchEvent(new Dpid(dpid));
                topologyDiscoveryInterface.
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
        topologyDiscoveryInterface.putLinkDiscoveryEvent(linkEvent);
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
            log.debug("Not the master for dst switch {}. Suppressed link remove event {}.",
                    link.getDst(), linkEvent);
            return;
        }
        topologyDiscoveryInterface.removeLinkDiscoveryEvent(linkEvent);
    }

    @Override
    public void switchPortAdded(Long switchId, OFPhysicalPort port) {
        final Dpid dpid = new Dpid(switchId);
        PortEvent portEvent = new PortEvent(dpid, new PortNumber(port.getPortNumber()));
        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        portEvent.createStringAttribute(TopologyElement.TYPE,
                                        TopologyElement.TYPE_PACKET_LAYER);
        portEvent.createStringAttribute("name", port.getName());

        portEvent.freeze();

        if (registryService.hasControl(switchId)) {
            topologyDiscoveryInterface.putPortDiscoveryEvent(portEvent);
            linkDiscovery.enableDiscoveryOnPort(switchId, port.getPortNumber());
        } else {
            log.debug("Not the master for switch {}. Suppressed port add event {}.",
                    new Dpid(switchId), portEvent);
        }
    }

    @Override
    public void switchPortRemoved(Long switchId, OFPhysicalPort port) {
        final Dpid dpid = new Dpid(switchId);

        PortEvent portEvent = new PortEvent(dpid, new PortNumber(port.getPortNumber()));
        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        portEvent.createStringAttribute(TopologyElement.TYPE,
                                        TopologyElement.TYPE_PACKET_LAYER);
        portEvent.createStringAttribute("name", port.getName());

        portEvent.freeze();

        if (registryService.hasControl(switchId)) {
            topologyDiscoveryInterface.removePortDiscoveryEvent(portEvent);
        } else {
            log.debug("Not the master for switch {}. Suppressed port del event {}.",
                    dpid, portEvent);
        }
    }

    @Override
    public void addedSwitch(IOFSwitch sw) {
        final Dpid dpid = new Dpid(sw.getId());
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
        if (!registryService.hasControl(sw.getId())) {
            log.debug("Not the master for switch {}. Suppressed switch add event {}.",
                    dpid, switchEvent);
            return;
        }

        List<PortEvent> portEvents = new ArrayList<PortEvent>();
        for (OFPhysicalPort port : sw.getPorts()) {
            PortEvent portEvent = new PortEvent(dpid, new PortNumber(port.getPortNumber()));
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
        topologyDiscoveryInterface
                .putSwitchDiscoveryEvent(switchEvent, portEvents);

        for (OFPhysicalPort port : sw.getPorts()) {
            // Allow links to be discovered on this port now that it's
            // in the database
            linkDiscovery.enableDiscoveryOnPort(sw.getId(), port.getPortNumber());
        }
    }

    @Override
    public void removedSwitch(IOFSwitch sw) {
        // We don't use this event - switch remove is done by cleanup thread
    }

    @Override
    public void switchPortChanged(Long switchId) {
        // We don't use this event
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
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
        hostService = context.getServiceImpl(IHostService.class);

        topologyService = context.getServiceImpl(ITopologyService.class);

        floodlightProvider.addLocalSwitchMastershipListener(this);
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        floodlightProvider.addOFSwitchListener(this);
        linkDiscovery.addListener(this);
        hostService.addHostListener(this);

        topology = topologyService.getTopology();
        topologyDiscoveryInterface =
                topologyService.getTopologyDiscoveryInterface();

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

        topologyDiscoveryInterface.putHostDiscoveryEvent(event);
    }

    @Override
    public void hostRemoved(Host host) {
        log.debug("Called onosDeviceRemoved");
        HostEvent event = new HostEvent(host.getMacAddress());
        //XXX shouldn't we be setting attachment points?
        event.freeze();
        topologyDiscoveryInterface.removeHostDiscoveryEvent(event);
    }

    @Override
    public void controllerRoleChanged(Dpid dpid, Role role) {
        log.debug("Local switch controller mastership role changed: dpid = {} role = {}", dpid, role);
        MastershipEvent mastershipEvent =
            new MastershipEvent(dpid, registryService.getOnosInstanceId(),
                                role);
        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        mastershipEvent.createStringAttribute(TopologyElement.TYPE,
                                              TopologyElement.TYPE_ALL_LAYERS);
        mastershipEvent.freeze();
        topologyDiscoveryInterface.putSwitchMastershipEvent(mastershipEvent);
    }

    @Override
    public void switchDisconnected(Dpid dpid) {
        log.debug("Local switch disconnected: dpid = {} role = {}", dpid);

        Role role = Role.SLAVE;         // TODO: Should be Role.UNKNOWN

        MastershipEvent mastershipEvent =
            new MastershipEvent(dpid, registryService.getOnosInstanceId(),
                                role);
        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        mastershipEvent.createStringAttribute(TopologyElement.TYPE,
                                              TopologyElement.TYPE_ALL_LAYERS);
        mastershipEvent.freeze();
        topologyDiscoveryInterface.removeSwitchMastershipEvent(mastershipEvent);
    }
}
