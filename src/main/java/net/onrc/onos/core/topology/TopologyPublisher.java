package net.onrc.onos.core.topology;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
import net.onrc.onos.core.util.OnosInstanceId;
import net.onrc.onos.core.util.PortNumber;
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
    private ConcurrentMap<Dpid, MastershipEvent> publishedMastershipEvents =
        new ConcurrentHashMap<>();
    private ConcurrentMap<Dpid, SwitchEvent> publishedSwitchEvents =
        new ConcurrentHashMap<>();
    private ConcurrentMap<Dpid, ConcurrentMap<ByteBuffer, PortEvent>>
        publishedPortEvents = new ConcurrentHashMap<>();
    private ConcurrentMap<Dpid, ConcurrentMap<ByteBuffer, LinkEvent>>
        publishedLinkEvents = new ConcurrentHashMap<>();
    private ConcurrentMap<Dpid, ConcurrentMap<ByteBuffer, HostEvent>>
        publishedHostEvents = new ConcurrentHashMap<>();


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

                SwitchEvent switchEvent = new SwitchEvent(new Dpid(dpid));
                publishRemoveSwitchEvent(switchEvent);
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

        publishAddLinkEvent(linkEvent);
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

        publishRemoveLinkEvent(linkEvent);
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
        // The Port events
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
        publishAddSwitchEvent(switchEvent, portEvents);
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
                new MastershipEvent(dpid, getOnosInstanceId(), role);
        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        mastershipEvent.createStringAttribute(TopologyElement.TYPE,
                TopologyElement.TYPE_ALL_LAYERS);
        mastershipEvent.freeze();
        publishRemoveSwitchMastershipEvent(mastershipEvent);
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
        PortEvent portEvent = new PortEvent(dpid,
                new PortNumber(port.getPortNo().getShortPortNumber()));
        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        portEvent.createStringAttribute(TopologyElement.TYPE,
                TopologyElement.TYPE_PACKET_LAYER);
        portEvent.createStringAttribute("name", port.getName());
        portEvent.freeze();

        publishAddPortEvent(portEvent);
    }

    /**
     * Prepares an event for removing a port on a switch.
     *
     * @param switchId the switch ID (DPID)
     * @param port the port to remove
     */
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

        publishRemovePortEvent(portEvent);
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
        log.debug("Host added with MAC {}", host.getMacAddress());

        SwitchPort sp = new SwitchPort(host.getSwitchDPID(), host.getSwitchPort());
        List<SwitchPort> spLists = new ArrayList<SwitchPort>();
        spLists.add(sp);
        HostEvent event = new HostEvent(host.getMacAddress());
        event.setAttachmentPoints(spLists);
        event.setLastSeenTime(host.getLastSeenTimestamp().getTime());
        // Does not use vlan info now.
        event.freeze();

        publishAddHostEvent(event);
    }

    @Override
    public void hostRemoved(Host host) {
        log.debug("Host removed with MAC {}", host.getMacAddress());

        //
        // Remove all previously added HostEvent for this MAC address
        //
        // TODO: Currently, the caller of hostRemoved() might not include
        // the correct set of Attachment Points in the HostEvent entry itself.
        // Also, we might have multiple HostEvent entries for the same
        // host (MAC address), each containing a single (different) Attachment
        // Point.
        // Hence, here we have to cleanup all HostEvent entries for this
        // particular host, based on its MAC address.
        //
        List<HostEvent> removeHostEvents = new LinkedList<>();
        for (ConcurrentMap<ByteBuffer, HostEvent> cm : publishedHostEvents.values()) {
            for (HostEvent hostEvent : cm.values()) {
                if (hostEvent.getMac().equals(host.getMacAddress())) {
                    removeHostEvents.add(hostEvent);
                }
            }
        }
        for (HostEvent event : removeHostEvents) {
            publishRemoveHostEvent(event);
        }
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
        MastershipEvent mastershipEvent =
                new MastershipEvent(dpid, getOnosInstanceId(), role);
        // FIXME should be merging, with existing attrs, etc..
        // TODO define attr name as constant somewhere.
        // TODO populate appropriate attributes.
        mastershipEvent.createStringAttribute(TopologyElement.TYPE,
                TopologyElement.TYPE_ALL_LAYERS);
        mastershipEvent.freeze();
        publishAddSwitchMastershipEvent(mastershipEvent);
    }

    /**
     * Publishes ADD Mastership Event.
     *
     * @param mastershipEvent the mastership event to publish
     */
    private void publishAddSwitchMastershipEvent(
                        MastershipEvent mastershipEvent) {
        // Publish the information
        TopologyEvent topologyEvent =
            new TopologyEvent(mastershipEvent, getOnosInstanceId());
        log.debug("Publishing add mastership: {}", topologyEvent);
        eventChannel.addEntry(topologyEvent.getID(), topologyEvent);
        publishedMastershipEvents.put(mastershipEvent.getDpid(),
                                      mastershipEvent);
    }

    /**
     * Publishes REMOVE Mastership Event.
     *
     * @param mastershipEvent the mastership event to publish
     */
    private void publishRemoveSwitchMastershipEvent(
                        MastershipEvent mastershipEvent) {
        if (publishedMastershipEvents.get(mastershipEvent.getDpid()) == null) {
            return;     // Nothing to do
        }

        // Publish the information
        TopologyEvent topologyEvent =
            new TopologyEvent(mastershipEvent, getOnosInstanceId());
        log.debug("Publishing remove mastership: {}", topologyEvent);
        eventChannel.removeEntry(topologyEvent.getID());
        publishedMastershipEvents.remove(mastershipEvent.getDpid());
    }

    /**
     * Publishes ADD Switch Event.
     *
     * @param switchEvent the switch event to publish
     * @param portEvents the corresponding port events for the switch to
     * publish
     */
    private void publishAddSwitchEvent(SwitchEvent switchEvent,
                                       Collection<PortEvent> portEvents) {
        if (!registryService.hasControl(switchEvent.getOriginDpid().value())) {
            log.debug("Not the master for switch {}. Suppressed switch add event {}.",
                      switchEvent.getOriginDpid(), switchEvent);
            return;
        }

        // Keep track of the old Port Events that should be removed
        ConcurrentMap<ByteBuffer, PortEvent> oldPortEvents =
            publishedPortEvents.get(switchEvent.getDpid());
        if (oldPortEvents == null) {
            oldPortEvents = new ConcurrentHashMap<>();
        }

        // Publish the information for the switch
        TopologyEvent topologyEvent =
            new TopologyEvent(switchEvent, getOnosInstanceId());
        log.debug("Publishing add switch: {}", topologyEvent);
        eventChannel.addEntry(topologyEvent.getID(), topologyEvent);
        publishedSwitchEvents.put(switchEvent.getDpid(), switchEvent);

        // Publish the information for each port
        ConcurrentMap<ByteBuffer, PortEvent> newPortEvents =
            new ConcurrentHashMap<>();
        for (PortEvent portEvent : portEvents) {
            topologyEvent =
                new TopologyEvent(portEvent, getOnosInstanceId());
            log.debug("Publishing add port: {}", topologyEvent);
            eventChannel.addEntry(topologyEvent.getID(), topologyEvent);

            ByteBuffer id = portEvent.getIDasByteBuffer();
            newPortEvents.put(id, portEvent);
            oldPortEvents.remove(id);
        }
        publishedPortEvents.put(switchEvent.getDpid(), newPortEvents);

        // Cleanup for each of the old removed port
        for (PortEvent portEvent : oldPortEvents.values()) {
            publishRemovePortEvent(portEvent);
        }
    }

    /**
     * Publishes REMOVE Switch Event.
     *
     * @param switchEvent the switch event to publish
     */
    private void publishRemoveSwitchEvent(SwitchEvent switchEvent) {
        //
        // TODO: Removed the check for now, because currently this method is
        // also called by the SwitchCleanup thread, and in that case
        // the Switch Event was published by some other ONOS instance.
        //
        /*
        if (publishedSwitchEvents.get(switchEvent.getDpid()) == null) {
            return;     // Nothing to do
        }
        */

        // Publish the information
        TopologyEvent topologyEvent =
            new TopologyEvent(switchEvent, getOnosInstanceId());
        log.debug("Publishing remove switch: {}", topologyEvent);
        eventChannel.removeEntry(topologyEvent.getID());
        publishedSwitchEvents.remove(switchEvent.getDpid());

        // Cleanup for each port
        ConcurrentMap<ByteBuffer, PortEvent> portEvents =
            publishedPortEvents.get(switchEvent.getDpid());
        if (portEvents != null) {
            for (PortEvent portEvent : portEvents.values()) {
                publishRemovePortEvent(portEvent);
            }
        }

        publishedPortEvents.remove(switchEvent.getDpid());
        publishedLinkEvents.remove(switchEvent.getDpid());
        publishedHostEvents.remove(switchEvent.getDpid());
    }

    /**
     * Publishes ADD Port Event.
     *
     * @param portEvent the port event to publish
     */
    private void publishAddPortEvent(PortEvent portEvent) {
        if (!registryService.hasControl(portEvent.getOriginDpid().value())) {
            log.debug("Not the master for switch {}. Suppressed port add event {}.",
                      portEvent.getOriginDpid(), portEvent);
            return;
        }

        // Publish the information
        TopologyEvent topologyEvent =
            new TopologyEvent(portEvent, getOnosInstanceId());
        log.debug("Publishing add port: {}", topologyEvent);
        eventChannel.addEntry(topologyEvent.getID(), topologyEvent);

        // Store the new Port Event in the local cache
        ConcurrentMap<ByteBuffer, PortEvent> portEvents =
            ConcurrentUtils.putIfAbsent(publishedPortEvents,
                        portEvent.getDpid(),
                        new ConcurrentHashMap<ByteBuffer, PortEvent>());
        portEvents.put(portEvent.getIDasByteBuffer(), portEvent);
    }

    /**
     * Publishes REMOVE Port Event.
     *
     * @param portEvent the port event to publish
     */
    private void publishRemovePortEvent(PortEvent portEvent) {
        ConcurrentMap<ByteBuffer, PortEvent> portEvents =
            publishedPortEvents.get(portEvent.getDpid());
        if (portEvents == null) {
            return;     // Nothing to do
        }
        if (portEvents.get(portEvent.getIDasByteBuffer()) == null) {
            return;     // Nothing to do
        }

        // Publish the information
        TopologyEvent topologyEvent =
            new TopologyEvent(portEvent, getOnosInstanceId());
        log.debug("Publishing remove port: {}", topologyEvent);
        eventChannel.removeEntry(topologyEvent.getID());

        // Cleanup for the incoming link(s)
        ConcurrentMap<ByteBuffer, LinkEvent> linkEvents =
            publishedLinkEvents.get(portEvent.getDpid());
        if (linkEvents != null) {
            for (LinkEvent linkEvent : linkEvents.values()) {
                if (linkEvent.getDst().equals(portEvent.getSwitchPort())) {
                    publishRemoveLinkEvent(linkEvent);
                }
            }
        }

        // Cleanup for the connected hosts
        ConcurrentMap<ByteBuffer, HostEvent> hostEvents =
            publishedHostEvents.get(portEvent.getDpid());
        if (hostEvents != null) {
            for (HostEvent hostEvent : hostEvents.values()) {
                for (SwitchPort swp : hostEvent.getAttachmentPoints()) {
                    if (swp.equals(portEvent.getSwitchPort())) {
                        publishRemoveHostEvent(hostEvent);
                    }
                }
            }
        }

        portEvents.remove(portEvent.getIDasByteBuffer());
    }

    /**
     * Publishes ADD Link Event.
     *
     * @param linkEvent the link event to publish
     */
    private void publishAddLinkEvent(LinkEvent linkEvent) {
        if (!registryService.hasControl(linkEvent.getOriginDpid().value())) {
            log.debug("Not the master for dst switch {}. Suppressed link add event {}.",
                      linkEvent.getOriginDpid(), linkEvent);
            return;
        }

        // Publish the information
        TopologyEvent topologyEvent =
            new TopologyEvent(linkEvent, getOnosInstanceId());
        log.debug("Publishing add link: {}", topologyEvent);
        eventChannel.addEntry(topologyEvent.getID(), topologyEvent);

        // Store the new Link Event in the local cache
        ConcurrentMap<ByteBuffer, LinkEvent> linkEvents =
            ConcurrentUtils.putIfAbsent(publishedLinkEvents,
                        linkEvent.getDst().getDpid(),
                        new ConcurrentHashMap<ByteBuffer, LinkEvent>());
        linkEvents.put(linkEvent.getIDasByteBuffer(), linkEvent);
    }

    /**
     * Publishes REMOVE Link Event.
     *
     * @param linkEvent the link event to publish
     */
    private void publishRemoveLinkEvent(LinkEvent linkEvent) {
        ConcurrentMap<ByteBuffer, LinkEvent> linkEvents =
            publishedLinkEvents.get(linkEvent.getDst().getDpid());
        if (linkEvents == null) {
            return;     // Nothing to do
        }
        if (linkEvents.get(linkEvent.getIDasByteBuffer()) == null) {
            return;     // Nothing to do
        }

        // Publish the information
        TopologyEvent topologyEvent =
            new TopologyEvent(linkEvent, getOnosInstanceId());
        log.debug("Publishing remove link: {}", topologyEvent);
        eventChannel.removeEntry(topologyEvent.getID());

        linkEvents.remove(linkEvent.getIDasByteBuffer());
    }

    /**
     * Publishes ADD Host Event.
     *
     * @param hostEvent the host event to publish
     */
    private void publishAddHostEvent(HostEvent hostEvent) {
        //
        // NOTE: The implementation below assumes that there is just one
        // attachment point stored in hostEvent. Currently, this assumption
        // is true based on the existing implementation of the caller
        // hostAdded().
        //

        if (!registryService.hasControl(hostEvent.getOriginDpid().value())) {
            log.debug("Not the master for attachment switch {}. Suppressed host add event {}.",
                      hostEvent.getOriginDpid(), hostEvent);
            return;
        }

        // Publish the information
        TopologyEvent topologyEvent =
            new TopologyEvent(hostEvent, getOnosInstanceId());
        log.debug("Publishing add host: {}", topologyEvent);
        eventChannel.addEntry(topologyEvent.getID(), topologyEvent);

        // Store the new Host Event in the local cache
        ConcurrentMap<ByteBuffer, HostEvent> hostEvents =
            ConcurrentUtils.putIfAbsent(publishedHostEvents,
                hostEvent.getOriginDpid(),
                new ConcurrentHashMap<ByteBuffer, HostEvent>());
        hostEvents.put(hostEvent.getIDasByteBuffer(), hostEvent);
    }

    /**
     * Publishes REMOVE Host Event.
     *
     * @param hostEvent the host event to publish
     */
    private void publishRemoveHostEvent(HostEvent hostEvent) {
        ConcurrentMap<ByteBuffer, HostEvent> hostEvents =
            publishedHostEvents.get(hostEvent.getOriginDpid());
        if (hostEvents == null) {
            return;     // Nothing to do
        }
        if (hostEvents.get(hostEvent.getIDasByteBuffer()) == null) {
            return;     // Nothing to do
        }

        // Publish the information
        TopologyEvent topologyEvent =
            new TopologyEvent(hostEvent, getOnosInstanceId());
        log.debug("Publishing remove host: {}", topologyEvent);
        eventChannel.removeEntry(topologyEvent.getID());

        hostEvents.remove(hostEvent.getIDasByteBuffer());
    }
}
