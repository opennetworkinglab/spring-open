package net.onrc.onos.core.hostmanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IUpdate;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.Topology;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostManager implements IFloodlightModule,
        IOFMessageListener,
        IHostService {

    private static final Logger log = LoggerFactory.getLogger(HostManager.class);
    private static final long HOST_CLEANING_INITIAL_DELAY = 30;
    private int cleanupSecondConfig = 60 * 60;
    private int agingMillisecConfig = 60 * 60 * 1000;

    private CopyOnWriteArrayList<IHostListener> hostListeners;
    private IFloodlightProviderService floodlightProvider;
    private static final ScheduledExecutorService EXECUTOR_SERVICE =
            Executors.newSingleThreadScheduledExecutor();

    private ITopologyService topologyService;
    private Topology topology;

    public enum HostUpdateType {
        ADD, DELETE, UPDATE;
    }

    private class HostUpdate implements IUpdate {
        private final Host host;
        private final HostUpdateType type;

        public HostUpdate(Host host, HostUpdateType type) {
            this.host = host;
            this.type = type;
        }

        @Override
        public void dispatch() {
            if (type == HostUpdateType.ADD) {
                for (IHostListener listener : hostListeners) {
                    listener.hostAdded(host);
                }
            } else if (type == HostUpdateType.DELETE) {
                for (IHostListener listener : hostListeners) {
                    listener.hostRemoved(host);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "hostmanager";
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        // We want link discovery to consume LLDP first otherwise we'll
        // end up reading bad host info from LLDP packets
        return type == OFType.PACKET_IN && "linkdiscovery".equals(name);
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        return type == OFType.PACKET_IN &&
                ("proxyarpmanager".equals(name) || "onosforwarding".equals(name));
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        if (msg.getType().equals(OFType.PACKET_IN) &&
                (msg instanceof OFPacketIn)) {
            OFPacketIn pi = (OFPacketIn) msg;

            Ethernet eth = IFloodlightProviderService.bcStore.
                    get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

            return processPacketIn(sw, pi, eth);
        }

        return Command.CONTINUE;
    }

    // This "protected" modifier is for unit test.
    // The above "receive" method couldn't be tested
    // because of IFloodlightProviderService static final field.
    protected Command processPacketIn(IOFSwitch sw, OFPacketIn pi, Ethernet eth) {
        if (log.isTraceEnabled()) {
            log.trace("Receive PACKET_IN swId {}, portId {}", sw.getId(), pi.getInPort());
        }

        final Dpid dpid = new Dpid(sw.getId());
        final PortNumber portNum = new PortNumber(pi.getInPort());
        Long mac = eth.getSourceMAC().toLong();

        Host srcHost =
                getSourceHostFromPacket(eth, dpid.value(), portNum.value());

        if (srcHost == null) {
            return Command.STOP;
        }

        // If the switch port we try to attach a new host already has a link,
        // then don't add the host
        // TODO We probably don't need to check this here, it should be done in
        // the Topology module.
        topology.acquireReadLock();
        try {
            if (topology.getOutgoingLink(dpid, portNum) != null ||
                    topology.getIncomingLink(dpid, portNum) != null) {
                log.debug("Not adding host {} as " +
                    "there is a link on the port: dpid {} port {}",
                    srcHost.getMacAddress(), dpid, portNum);
                return Command.CONTINUE;
            }
        } finally {
            topology.releaseReadLock();
        }

        addHost(mac, srcHost);

        if (log.isTraceEnabled()) {
            log.trace("Add host info: {}", srcHost);
        }
        return Command.CONTINUE;
    }

    // Thread to delete hosts periodically.
    // Remove all hosts from the map first and then finally delete hosts
    // from the DB.

    // TODO This should be sharded based on host 'owner' (i.e. the instance
    // that owns the switch it is attached to). Currently any instance can
    // issue deletes for any host, which permits race conditions and could
    // cause the Topology replicas to diverge.
    private class HostCleaner implements Runnable {
        @Override
        public void run() {
            log.debug("called HostCleaner");
            topology.acquireReadLock();
            try {
                Set<net.onrc.onos.core.topology.Host> deleteSet = new HashSet<>();
                for (net.onrc.onos.core.topology.Host host : topology.getHosts()) {
                    long now = System.currentTimeMillis();
                    if ((now - host.getLastSeenTime() > agingMillisecConfig)) {
                        if (log.isTraceEnabled()) {
                            log.trace("Removing host info: mac {}, now {}, lastSeenTime {}, diff {}",
                                    host.getMacAddress(), now, host.getLastSeenTime(), now - host.getLastSeenTime());
                        }
                        deleteSet.add(host);
                    }
                }

                for (net.onrc.onos.core.topology.Host host : deleteSet) {
                    deleteHostByMac(host.getMacAddress());
                }
            } catch (Exception e) {
                // Any exception thrown by the task will prevent the Executor
                // from running the next iteration, so we need to catch and log
                // all exceptions here.
                log.error("Exception in host cleanup thread:", e);
            } finally {
                topology.releaseReadLock();
            }
        }
    }

    /**
     * Parse a host from an {@link Ethernet} packet.
     *
     * @param eth the packet to parse
     * @param swdpid the switch on which the packet arrived
     * @param port the port on which the packet arrived
     * @return the host from the packet
     */
    protected Host getSourceHostFromPacket(Ethernet eth,
            long swdpid, long port) {
        MACAddress sourceMac = eth.getSourceMAC();

        // Ignore broadcast/multicast source
        if (sourceMac.isBroadcast() || sourceMac.isBroadcast()) {
            return null;
        }

        short vlan = eth.getVlanID();
        return new Host(sourceMac,
                ((vlan >= 0) ? vlan : null),
                swdpid,
                port,
                new Date());
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        List<Class<? extends IFloodlightService>> services =
                new ArrayList<Class<? extends IFloodlightService>>();
        services.add(IHostService.class);
        return services;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> impls =
                new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
        impls.put(IHostService.class, this);
        return impls;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        List<Class<? extends IFloodlightService>> dependencies =
                new ArrayList<Class<? extends IFloodlightService>>();
        dependencies.add(IFloodlightProviderService.class);
        dependencies.add(ITopologyService.class);
        return dependencies;
    }

    @Override
    public void init(FloodlightModuleContext context)
            throws FloodlightModuleException {
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        hostListeners = new CopyOnWriteArrayList<IHostListener>();
        topologyService = context.getServiceImpl(ITopologyService.class);
        topology = topologyService.getTopology();

        setHostManagerProperties(context);
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
        EXECUTOR_SERVICE.scheduleAtFixedRate(new HostCleaner(),
                HOST_CLEANING_INITIAL_DELAY, cleanupSecondConfig, TimeUnit.SECONDS);
    }

    @Override
    public void deleteHost(Host host) {
        floodlightProvider.publishUpdate(
                new HostUpdate(host, HostUpdateType.DELETE));
    }

    @Override
    public void deleteHostByMac(MACAddress mac) {
        Host deleteHost = null;
        topology.acquireReadLock();
        try {
            net.onrc.onos.core.topology.Host host = topology.getHostByMac(mac);

            for (Port switchPort : host.getAttachmentPoints()) {
                // We don't handle vlan now and multiple attachment points.
                deleteHost = new Host(host.getMacAddress(),
                        null,
                        switchPort.getDpid().value(),
                        switchPort.getNumber().value(),
                        new Date(host.getLastSeenTime()));
                break;
            }
        } finally {
            topology.releaseReadLock();
        }

        if (deleteHost != null) {
            deleteHost(deleteHost);
        }
    }

    @Override
    public void addHost(Long mac, Host host) {
        floodlightProvider.publishUpdate(
                new HostUpdate(host, HostUpdateType.ADD));
    }

    @Override
    public void addHostListener(IHostListener listener) {
        hostListeners.add(listener);
    }

    @Override
    public void removeHostListener(IHostListener listener) {
        hostListeners.remove(listener);
    }

    private void setHostManagerProperties(FloodlightModuleContext context) {
        Map<String, String> configOptions = context.getConfigParams(this);
        String cleanupsec = configOptions.get("cleanupsec");
        String agingmsec = configOptions.get("agingmsec");
        if (cleanupsec != null) {
            cleanupSecondConfig = Integer.parseInt(cleanupsec);
            log.debug("CLEANUP_SECOND is set to {}", cleanupSecondConfig);
        }

        if (agingmsec != null) {
            agingMillisecConfig = Integer.parseInt(agingmsec);
            log.debug("AGEING_MILLSEC is set to {}", agingMillisecConfig);
        }
    }
}
