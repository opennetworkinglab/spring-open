package net.onrc.onos.core.packetservice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.onrc.onos.api.packet.IPacketListener;
import net.onrc.onos.api.packet.IPacketService;
import net.onrc.onos.core.datagrid.IDatagridService;
import net.onrc.onos.core.datagrid.IEventChannel;
import net.onrc.onos.core.datagrid.IEventChannelListener;
import net.onrc.onos.core.flowprogrammer.IFlowPusherService;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.topology.Topology;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class PacketModule implements IOFMessageListener, IPacketService,
        IFloodlightModule {
    private static final Logger log = LoggerFactory.getLogger(PacketModule.class);

    private final CopyOnWriteArrayList<IPacketListener> listeners;

    private IFloodlightProviderService floodlightProvider;
    private Topology topology;
    private IDatagridService datagrid;
    private IFlowPusherService flowPusher;

    private IEventChannel<Long, PacketOutNotification> packetOutEventChannel;

    private static final String PACKET_OUT_CHANNEL_NAME =
            "onos.packet_out";

    private PacketOutEventHandler packetOutEventHandler =
            new PacketOutEventHandler();

    private class PacketOutEventHandler implements
            IEventChannelListener<Long, PacketOutNotification> {

        @Override
        public void entryAdded(PacketOutNotification value) {
            Multimap<Long, Short> localPorts = HashMultimap.create();
            for (IOFSwitch sw : floodlightProvider.getSwitches().values()) {
                for (OFPortDesc port : sw.getEnabledPorts()) {
                    // XXX S fix this to int
                    localPorts.put(sw.getId(), port.getPortNo().getShortPortNumber());
                }
            }
            Multimap<Long, Short> outPorts = value.calculateOutPorts(
                    localPorts, topology);
            sendPacketToSwitches(outPorts, value.getPacketData());
        }

        @Override
        public void entryUpdated(PacketOutNotification value) {
            entryAdded(value);
        }

        @Override
        public void entryRemoved(PacketOutNotification value) {
            // Not used
        }
    }

    public PacketModule() {
        listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public void registerPacketListener(IPacketListener listener) {
        listeners.addIfAbsent(listener);
    }

    @Override
    public void sendPacket(Ethernet eth, SwitchPort switchPort) {
        SinglePacketOutNotification notification =
                new SinglePacketOutNotification(eth.serialize(), 0,
                        switchPort.getDpid().value(), switchPort.getPortNumber().shortValue());

        // TODO We shouldn't care what the destination MAC is
        long dstMac = eth.getDestinationMAC().toLong();
        packetOutEventChannel.addTransientEntry(dstMac, notification);
    }

    @Override
    public void sendPacket(Ethernet eth, List<SwitchPort> switchPorts) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void broadcastPacketOutEdge(Ethernet eth) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void broadcastPacketOutEdge(Ethernet eth, SwitchPort inSwitchPort) {
        BroadcastPacketOutNotification notification =
                new BroadcastPacketOutNotification(eth.serialize(), 0,
                        inSwitchPort.getDpid().value(), inSwitchPort.getPortNumber().shortValue());

        long dstMac = eth.getDestinationMAC().toLong();
        packetOutEventChannel.addTransientEntry(dstMac, notification);
    }

    @Override
    public String getName() {
        return "packetmodule";
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        return false;
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg,
            FloodlightContext cntx) {
        if (!(msg instanceof OFPacketIn)) {
            return Command.CONTINUE;
        }

        Ethernet eth = IFloodlightProviderService.bcStore.
                get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        // FIXME losing port number precision
        short inport = (short) cntx.getStorage()
                .get(IFloodlightProviderService.CONTEXT_PI_INPORT);

        Switch topologySwitch;
        Port inPort;
        try {
            topology.acquireReadLock();
            Dpid dpid = new Dpid(sw.getId());
            PortNumber p = PortNumber.uint16(inport);
            topologySwitch = topology.getSwitch(dpid);
            inPort = topology.getPort(dpid, p);
        } finally {
            topology.releaseReadLock();
        }

        if (topologySwitch == null || inPort == null) {
            // We can't send packets for switches or ports that aren't in the
            // topology yet
            return Command.CONTINUE;
        }

        for (IPacketListener listener : listeners) {
            listener.receive(topologySwitch, inPort, eth);
        }

        return Command.CONTINUE;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        List<Class<? extends IFloodlightService>> services = new ArrayList<>();
        services.add(IPacketService.class);
        return services;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService>
            getServiceImpls() {

        Map<Class<? extends IFloodlightService>, IFloodlightService> serviceImpls = new HashMap<>();
        serviceImpls.put(IPacketService.class, this);
        return serviceImpls;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        List<Class<? extends IFloodlightService>> dependencies = new ArrayList<>();
        dependencies.add(IFloodlightProviderService.class);
        dependencies.add(ITopologyService.class);
        dependencies.add(IDatagridService.class);
        dependencies.add(IFlowPusherService.class);
        return dependencies;
    }

    @Override
    public void init(FloodlightModuleContext context)
            throws FloodlightModuleException {
        floodlightProvider =
                context.getServiceImpl(IFloodlightProviderService.class);
        topology = context.getServiceImpl(ITopologyService.class)
                .getTopology();
        datagrid = context.getServiceImpl(IDatagridService.class);
        flowPusher = context.getServiceImpl(IFlowPusherService.class);
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
        packetOutEventChannel = datagrid.addListener(PACKET_OUT_CHANNEL_NAME,
                packetOutEventHandler,
                Long.class,
                PacketOutNotification.class);
    }

    private void sendPacketToSwitches(Multimap<Long, Short> outPorts,
            byte[] packetData) {
        for (Long dpid : outPorts.keySet()) {
            IOFSwitch sw = floodlightProvider.getSwitches().get(dpid);

            if (sw == null) {
                log.warn("Switch {} not found when sending packet", dpid);
                continue;
            }

            OFFactory factory = sw.getFactory();

            List<OFAction> actions = new ArrayList<>();
            for (Short port : outPorts.get(dpid)) {
                actions.add(factory.actions().output(OFPort.of(port), Short.MAX_VALUE));
            }

            OFPacketOut po = factory.buildPacketOut()
                    .setData(packetData)
                    .setActions(actions)
                    .build();

            flowPusher.add(sw, po);
        }
    }

}
