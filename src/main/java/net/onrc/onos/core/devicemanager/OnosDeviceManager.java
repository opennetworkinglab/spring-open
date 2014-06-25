package net.onrc.onos.core.devicemanager;

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
import net.onrc.onos.core.topology.Device;
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

public class OnosDeviceManager implements IFloodlightModule,
        IOFMessageListener,
        IOnosDeviceService {

    private static final Logger log = LoggerFactory.getLogger(OnosDeviceManager.class);
    private static final long DEVICE_CLEANING_INITIAL_DELAY = 30;
    private int cleanupSecondConfig = 60 * 60;
    private int agingMillisecConfig = 60 * 60 * 1000;

    private CopyOnWriteArrayList<IOnosDeviceListener> deviceListeners;
    private IFloodlightProviderService floodlightProvider;
    private static final ScheduledExecutorService EXECUTOR_SERVICE =
            Executors.newSingleThreadScheduledExecutor();

    private ITopologyService topologyService;
    private Topology topology;

    public enum OnosDeviceUpdateType {
        ADD, DELETE, UPDATE;
    }

    private class OnosDeviceUpdate implements IUpdate {
        private final OnosDevice device;
        private final OnosDeviceUpdateType type;

        public OnosDeviceUpdate(OnosDevice device, OnosDeviceUpdateType type) {
            this.device = device;
            this.type = type;
        }

        @Override
        public void dispatch() {
            if (type == OnosDeviceUpdateType.ADD) {
                for (IOnosDeviceListener listener : deviceListeners) {
                    listener.onosDeviceAdded(device);
                }
            } else if (type == OnosDeviceUpdateType.DELETE) {
                for (IOnosDeviceListener listener : deviceListeners) {
                    listener.onosDeviceRemoved(device);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "onosdevicemanager";
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        // We want link discovery to consume LLDP first otherwise we'll
        // end up reading bad device info from LLDP packets
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

        OnosDevice srcDevice =
                getSourceDeviceFromPacket(eth, dpid.value(), portNum.value());

        if (srcDevice == null) {
            return Command.STOP;
        }

        // If the switch port we try to attach a new device already has a link,
        // then don't add the device
        // TODO We probably don't need to check this here, it should be done in
        // the Topology module.
        topology.acquireReadLock();
        try {
            if (topology.getOutgoingLink(dpid, portNum) != null ||
                    topology.getIncomingLink(dpid, portNum) != null) {
                log.debug("Stop adding OnosDevice {} as " +
                    "there is a link on the port: dpid {} port {}",
                    srcDevice.getMacAddress(), dpid, portNum);
                return Command.CONTINUE;
            }
        } finally {
            topology.releaseReadLock();
        }

        addOnosDevice(mac, srcDevice);

        if (log.isTraceEnabled()) {
            log.trace("Add device info: {}", srcDevice);
        }
        return Command.CONTINUE;
    }

    // Thread to delete devices periodically.
    // Remove all devices from the map first and then finally delete devices
    // from the DB.

    // TODO This should be sharded based on device 'owner' (i.e. the instance
    // that owns the switch it is attached to). Currently any instance can
    // issue deletes for any device, which permits race conditions and could
    // cause the Topology replicas to diverge.
    private class CleanDevice implements Runnable {
        @Override
        public void run() {
            log.debug("called CleanDevice");
            topology.acquireReadLock();
            try {
                Set<Device> deleteSet = new HashSet<Device>();
                for (Device dev : topology.getDevices()) {
                    long now = System.currentTimeMillis();
                    if ((now - dev.getLastSeenTime() > agingMillisecConfig)) {
                        if (log.isTraceEnabled()) {
                            log.trace("Removing device info: mac {}, now {}, lastSeenTime {}, diff {}",
                                    dev.getMacAddress(), now, dev.getLastSeenTime(), now - dev.getLastSeenTime());
                        }
                        deleteSet.add(dev);
                    }
                }

                for (Device dev : deleteSet) {
                    deleteOnosDeviceByMac(dev.getMacAddress());
                }
            } catch (Exception e) {
                // Any exception thrown by the task will prevent the Executor
                // from running the next iteration, so we need to catch and log
                // all exceptions here.
                log.error("Exception in device cleanup thread:", e);
            } finally {
                topology.releaseReadLock();
            }
        }
    }

    /**
     * Parse a device from an {@link Ethernet} packet.
     *
     * @param eth the packet to parse
     * @param swdpid the switch on which the packet arrived
     * @param port the port on which the packet arrived
     * @return the device from the packet
     */
    protected OnosDevice getSourceDeviceFromPacket(Ethernet eth,
            long swdpid,
            long port) {
        MACAddress sourceMac = eth.getSourceMAC();

        // Ignore broadcast/multicast source
        if (sourceMac.isBroadcast() || sourceMac.isBroadcast()) {
            return null;
        }

        short vlan = eth.getVlanID();
        return new OnosDevice(sourceMac,
                ((vlan >= 0) ? vlan : null),
                swdpid,
                port,
                new Date());
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        List<Class<? extends IFloodlightService>> services =
                new ArrayList<Class<? extends IFloodlightService>>();
        services.add(IOnosDeviceService.class);
        return services;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> impls =
                new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
        impls.put(IOnosDeviceService.class, this);
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
        deviceListeners = new CopyOnWriteArrayList<IOnosDeviceListener>();
        topologyService = context.getServiceImpl(ITopologyService.class);
        topology = topologyService.getTopology();

        setOnosDeviceManagerProperty(context);
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
        EXECUTOR_SERVICE.scheduleAtFixedRate(new CleanDevice(),
                DEVICE_CLEANING_INITIAL_DELAY, cleanupSecondConfig, TimeUnit.SECONDS);
    }

    @Override
    public void deleteOnosDevice(OnosDevice dev) {
        floodlightProvider.publishUpdate(
                new OnosDeviceUpdate(dev, OnosDeviceUpdateType.DELETE));
    }

    @Override
    public void deleteOnosDeviceByMac(MACAddress mac) {
        OnosDevice deleteDevice = null;
        topology.acquireReadLock();
        try {
            Device dev = topology.getDeviceByMac(mac);

            for (Port switchPort : dev.getAttachmentPoints()) {
                // We don't handle vlan now and multiple attachment points.
                deleteDevice = new OnosDevice(dev.getMacAddress(),
                        null,
                        switchPort.getDpid().value(),
                        (long) switchPort.getNumber().value(),
                        new Date(dev.getLastSeenTime()));
                break;
            }
        } finally {
            topology.releaseReadLock();
        }

        if (deleteDevice != null) {
            deleteOnosDevice(deleteDevice);
        }
    }

    @Override
    public void addOnosDevice(Long mac, OnosDevice dev) {
        floodlightProvider.publishUpdate(
                new OnosDeviceUpdate(dev, OnosDeviceUpdateType.ADD));
    }

    @Override
    public void addOnosDeviceListener(IOnosDeviceListener listener) {
        deviceListeners.add(listener);
    }

    @Override
    public void deleteOnosDeviceListener(IOnosDeviceListener listener) {
        deviceListeners.remove(listener);
    }

    private void setOnosDeviceManagerProperty(FloodlightModuleContext context) {
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
