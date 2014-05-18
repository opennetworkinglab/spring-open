package net.onrc.onos.apps.forwarding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.api.packet.IPacketListener;
import net.onrc.onos.api.packet.IPacketService;
import net.onrc.onos.apps.proxyarp.IProxyArpService;
import net.onrc.onos.core.devicemanager.IOnosDeviceService;
import net.onrc.onos.core.intent.Intent;
import net.onrc.onos.core.intent.Intent.IntentState;
import net.onrc.onos.core.intent.IntentMap;
import net.onrc.onos.core.intent.IntentMap.ChangedEvent;
import net.onrc.onos.core.intent.IntentMap.ChangedListener;
import net.onrc.onos.core.intent.IntentOperation;
import net.onrc.onos.core.intent.IntentOperationList;
import net.onrc.onos.core.intent.PathIntent;
import net.onrc.onos.core.intent.ShortestPathIntent;
import net.onrc.onos.core.intent.runtime.IPathCalcRuntimeService;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.topology.Device;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.LinkEvent;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.topology.Topology;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.FlowPath;
import net.onrc.onos.core.util.SwitchPort;

import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

public class Forwarding implements /*IOFMessageListener,*/ IFloodlightModule,
        IForwardingService, IPacketListener, ChangedListener {
    private static final Logger log = LoggerFactory.getLogger(Forwarding.class);

    private static final int SLEEP_TIME_FOR_DB_DEVICE_INSTALLED = 100; // milliseconds
    private static final int NUMBER_OF_THREAD_FOR_EXECUTOR = 1;
    private static final int SRC_SWITCH_TIMEOUT_ADJUST_SECOND = 2;
    private static final int DEFAULT_IDLE_TIMEOUT = 5;
    private int idleTimeout = DEFAULT_IDLE_TIMEOUT;

    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(NUMBER_OF_THREAD_FOR_EXECUTOR);

    private final String callerId = "Forwarding";

    private IPacketService packetService;
    private IControllerRegistryService controllerRegistryService;

    private ITopologyService topologyService;
    private Topology topology;
    private IPathCalcRuntimeService pathRuntime;
    private IntentMap pathIntentMap;
    private IntentMap highLevelIntentMap;

    // TODO it seems there is a Guava collection that will time out entries.
    // We should see if this will work here.
    private Map<Path, PushedFlow> pendingFlows;
    private ListMultimap<String, PacketToPush> waitingPackets;

    private final Object lock = new Object();

    private static class PacketToPush {
        public final Ethernet eth;
        public final long dpid;

        public PacketToPush(Ethernet eth, long dpid) {
            this.eth = eth;
            this.dpid = dpid;
        }
    }

    private static class PushedFlow {
        public final String intentId;
        public boolean installed = false;
        public short firstOutPort;

        public PushedFlow(String flowId) {
            this.intentId = flowId;
        }
    }

    private static final class Path {
        public final MACAddress srcMac;
        public final MACAddress dstMac;

        public Path(MACAddress srcMac, MACAddress dstMac) {
            this.srcMac = srcMac;
            this.dstMac = dstMac;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Path)) {
                return false;
            }

            Path otherPath = (Path) other;
            return srcMac.equals(otherPath.srcMac) &&
                    dstMac.equals(otherPath.dstMac);
        }

        @Override
        public int hashCode() {
            int hash = 17;
            hash = 31 * hash + srcMac.hashCode();
            hash = 31 * hash + dstMac.hashCode();
            return hash;
        }

        @Override
        public String toString() {
            return "(" + srcMac + ") => (" + dstMac + ")";
        }
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        List<Class<? extends IFloodlightService>> services =
                new ArrayList<Class<? extends IFloodlightService>>(1);
        services.add(IForwardingService.class);
        return services;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> impls =
                new HashMap<Class<? extends IFloodlightService>, IFloodlightService>(1);
        impls.put(IForwardingService.class, this);
        return impls;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        List<Class<? extends IFloodlightService>> dependencies =
                new ArrayList<Class<? extends IFloodlightService>>();
        dependencies.add(IControllerRegistryService.class);
        dependencies.add(IOnosDeviceService.class);
        dependencies.add(ITopologyService.class);
        dependencies.add(IPathCalcRuntimeService.class);
        // We don't use the IProxyArpService directly, but reactive forwarding
        // requires it to be loaded and answering ARP requests
        dependencies.add(IProxyArpService.class);
        dependencies.add(IPacketService.class);
        return dependencies;
    }

    @Override
    public void init(FloodlightModuleContext context) {
        controllerRegistryService = context.getServiceImpl(IControllerRegistryService.class);
        topologyService = context.getServiceImpl(ITopologyService.class);
        pathRuntime = context.getServiceImpl(IPathCalcRuntimeService.class);
        packetService = context.getServiceImpl(IPacketService.class);

        pendingFlows = new HashMap<Path, PushedFlow>();
        waitingPackets = LinkedListMultimap.create();
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        Map<String, String> configOptions = context.getConfigParams(this);

        try {
            if (Integer.parseInt(configOptions.get("idletimeout")) > 0) {
                idleTimeout = Integer.parseInt(configOptions.get("idletimeout"));
                log.info("idle_timeout for Forwarding is set to {}.", idleTimeout);
            } else {
                log.info("idle_timeout for Forwarding is less than 0. Use default {}.", idleTimeout);
            }
        } catch (NumberFormatException e) {
            log.info("idle_timeout related config options were not set. Use default.");
        }

        packetService.registerPacketListener(this);

        topology = topologyService.getTopology();
        highLevelIntentMap = pathRuntime.getHighLevelIntents();
        pathIntentMap = pathRuntime.getPathIntents();
        pathIntentMap.addChangeListener(this);
    }

    @Override
    public void receive(Switch sw, Port inPort, Ethernet eth) {
        if (log.isTraceEnabled()) {
            log.trace("Receive PACKET_IN swId {}, portId {}", sw.getDpid(), inPort.getNumber());
        }

        if (eth.getEtherType() != Ethernet.TYPE_IPV4) {
            // Only handle IPv4 packets right now
            return;
        }

        if (eth.isBroadcast() || eth.isMulticast()) {
            handleBroadcast(sw, inPort, eth);
        } else {
            // Unicast
            handlePacketIn(sw, inPort, eth);
        }
    }

    private void handleBroadcast(Switch sw, Port inPort, Ethernet eth) {
        if (log.isTraceEnabled()) {
            log.trace("Sending broadcast packet to other ONOS instances");
        }

        packetService.broadcastPacketOutEdge(eth,
                new SwitchPort(sw.getDpid(), inPort.getNumber().shortValue()));
    }

    private void handlePacketIn(Switch sw, Port inPort, Ethernet eth) {
        if (log.isTraceEnabled()) {
           log.trace("Start handlePacketIn swId {}, portId {}", sw.getDpid(), inPort.getNumber());
        }

        String destinationMac =
                HexString.toHexString(eth.getDestinationMACAddress());

        //FIXME getDeviceByMac() is a blocking call, so it may be better way to handle it to avoid the condition.
        Device deviceObject = topology.getDeviceByMac(MACAddress.valueOf(destinationMac));

        if (deviceObject == null) {
            log.debug("No device entry found for {}",
                    destinationMac);

            //Device is not in the DB, so wait it until the device is added.
            EXECUTOR_SERVICE.schedule(new WaitDeviceArp(sw, inPort, eth), SLEEP_TIME_FOR_DB_DEVICE_INSTALLED, TimeUnit.MILLISECONDS);
            return;
        }

        continueHandlePacketIn(sw, inPort, eth, deviceObject);
    }

    private class WaitDeviceArp implements Runnable {
        Switch sw;
        Port inPort;
        Ethernet eth;

        public WaitDeviceArp(Switch sw, Port inPort, Ethernet eth) {
            super();
            this.sw = sw;
            this.inPort = inPort;
            this.eth = eth;
        }

        @Override
        public void run() {
            Device deviceObject = topology.getDeviceByMac(MACAddress.valueOf(eth.getDestinationMACAddress()));
            if (deviceObject == null) {
                log.debug("wait {}ms and device was not found. Send broadcast packet and the thread finish.", SLEEP_TIME_FOR_DB_DEVICE_INSTALLED);
                handleBroadcast(sw, inPort, eth);
                return;
            }
            log.debug("wait {}ms and device {} was found, continue", SLEEP_TIME_FOR_DB_DEVICE_INSTALLED, deviceObject.getMacAddress());
            continueHandlePacketIn(sw, inPort, eth, deviceObject);
        }
    }

    private void continueHandlePacketIn(Switch sw, Port inPort, Ethernet eth, Device deviceObject) {

        log.trace("Start continuehandlePacketIn");

        //Iterator<IPortObject> ports = deviceObject.getAttachedPorts().iterator();
        Iterator<net.onrc.onos.core.topology.Port> ports = deviceObject.getAttachmentPoints().iterator();
        if (!ports.hasNext()) {
            log.debug("No attachment point found for device {} - broadcasting packet",
                    deviceObject.getMacAddress());
            handleBroadcast(sw, inPort, eth);
            return;
        }

        //This code assumes the device has only one port. It should be problem.
        net.onrc.onos.core.topology.Port portObject = ports.next();
        short destinationPort = portObject.getNumber().shortValue();
        Switch switchObject = portObject.getSwitch();
        long destinationDpid = switchObject.getDpid();

        // TODO eliminate cast
        SwitchPort srcSwitchPort = new SwitchPort(
                new Dpid(sw.getDpid()),
                new net.onrc.onos.core.util.Port((short) inPort.getNumber().longValue()));
        SwitchPort dstSwitchPort = new SwitchPort(
                new Dpid(destinationDpid),
                new net.onrc.onos.core.util.Port(destinationPort));

        MACAddress srcMacAddress = MACAddress.valueOf(eth.getSourceMACAddress());
        MACAddress dstMacAddress = MACAddress.valueOf(eth.getDestinationMACAddress());
        Path pathspec = new Path(srcMacAddress, dstMacAddress);
        IntentOperationList operations = new IntentOperationList();

        synchronized (lock) {
            //TODO check concurrency

            PushedFlow existingFlow = pendingFlows.get(pathspec);

            //A path is installed side by side to reduce a path timeout and a wrong state.
            if (existingFlow != null) {
                // We've already start to install a flow for this pair of MAC addresses
                if (log.isDebugEnabled()) {
                    log.debug("Found existing the same pathspec {}, intent ID is {}",
                            pathspec,
                            existingFlow.intentId);
                }

                // Find the correct port here. We just assume the PI is from
                // the first hop switch, but this is definitely not always
                // the case. We'll have to retrieve the flow from HZ every time
                // because it could change (be rerouted) sometimes.
                if (existingFlow.installed) {
                    // Flow has been sent to the switches so it is safe to
                    // send a packet out now

                    // TODO Here highLevelIntentMap and pathIntentMap would be problem,
                    // because it doesn't have global information as of May 2014.
                    // However usually these lines here is used when we got packet-in and this class think
                    // the path for the packet is installed already, so it is pretty rare.
                    // I will leave it for now, and will work in the next step.
                    Intent highLevelIntent = highLevelIntentMap.getIntent(existingFlow.intentId);
                    if (highLevelIntent == null) {
                        log.debug("Intent ID {} is null in HighLevelIntentMap. return.", existingFlow.intentId);
                        return;
                    }

                    if (highLevelIntent.getState() != IntentState.INST_ACK) {
                        log.debug("Intent ID {}'s state is not INST_ACK. return.", existingFlow.intentId);
                        return;
                    }

                    ShortestPathIntent spfIntent = null;
                    if (highLevelIntent instanceof ShortestPathIntent) {
                        spfIntent = (ShortestPathIntent) highLevelIntent;
                    } else {
                        log.debug("Intent ID {} is not PathIntent or null. return.", existingFlow.intentId);
                        return;
                    }

                    PathIntent pathIntent = (PathIntent) pathIntentMap.getIntent(spfIntent.getPathIntentId());
                    if (pathIntent == null) {
                        log.debug("PathIntent ID {} is null in PathIntentMap. return.", existingFlow.intentId);
                        return;
                    }

                    if (pathIntent.getState() != IntentState.INST_ACK) {
                        log.debug("Intent ID {}'s state is not INST_ACK. return.", existingFlow.intentId);
                        return;
                    }

                    boolean isflowEntryForThisSwitch = false;
                    net.onrc.onos.core.intent.Path path = pathIntent.getPath();
                    long outPort = -1;

                    if (spfIntent.getDstSwitchDpid() == sw.getDpid()) {
                        log.trace("The packet-in sw dpid {} is on the path.", sw.getDpid());
                        isflowEntryForThisSwitch = true;
                        outPort = spfIntent.getDstPortNumber();
                    }

                    for (Iterator<LinkEvent> i = path.iterator(); i.hasNext();) {
                        LinkEvent le = i.next();

                        if (le.getSrc().dpid.equals(sw.getDpid())) {
                            log.trace("The packet-in sw dpid {} is on the path.", sw.getDpid());
                            isflowEntryForThisSwitch = true;
                            outPort = le.getSrc().getNumber();
                            break;
                        }
                    }

                    if (!isflowEntryForThisSwitch) {
                        // If we don't find a flow entry for that switch, then we're
                        // in the middle of a rerouting (or something's gone wrong).
                        // This packet will be dropped as a victim of the rerouting.
                        log.debug("Dropping packet on flow {} between {}-{}",
                                existingFlow.intentId,
                                srcMacAddress, dstMacAddress);
                    } else {
                        if (outPort < 0) {
                            outPort = existingFlow.firstOutPort;
                        }

                        log.debug("Sending packet out from sw {}, outport{}", sw.getDpid(), outPort);
                        packetService.sendPacket(eth, new SwitchPort(
                                sw.getDpid(), (short) outPort));
                    }
                } else {
                    // Flow path has not yet been installed to switches so save the
                    // packet out for later
                    log.trace("Put a packet into the waiting list. flowId {}", existingFlow.intentId);
                    waitingPackets.put(existingFlow.intentId, new PacketToPush(eth, sw.getDpid()));
                }
                return;
            }

            String intentId = Long.toString(controllerRegistryService.getNextUniqueId());
            ShortestPathIntent intent = new ShortestPathIntent(intentId,
                    sw.getDpid(), inPort.getNumber(), srcMacAddress.toLong(),
                    destinationDpid, destinationPort, dstMacAddress.toLong());

            intent.setIdleTimeout(idleTimeout + SRC_SWITCH_TIMEOUT_ADJUST_SECOND);
            intent.setFirstSwitchIdleTimeout(idleTimeout);
            IntentOperation.Operator operator = IntentOperation.Operator.ADD;
            operations.add(operator, intent);
            log.debug("Adding new flow between {} at {} and {} at {}",
                    new Object[]{srcMacAddress, srcSwitchPort, dstMacAddress, dstSwitchPort});

             // Add to waiting lists
            waitingPackets.put(intentId, new PacketToPush(eth, sw.getDpid()));
            log.trace("Put a Packet in the wating list. intent ID {}, related pathspec {}", intentId, pathspec);
            pendingFlows.put(pathspec, new PushedFlow(intentId));
            log.trace("Put a Path {} in the pending flow, intent ID {}", pathspec, intentId);
        }
        pathRuntime.executeIntentOperations(operations);
    }

    @Override
    public void flowsInstalled(Collection<FlowPath> installedFlowPaths) {
    }

    @Override
    public void flowRemoved(FlowPath removedFlowPath) {
    }

    public void flowRemoved(PathIntent removedIntent) {
        if (log.isTraceEnabled()) {
            log.trace("Path {} was removed", removedIntent.getParentIntent().getId());
        }

        ShortestPathIntent spfIntent = (ShortestPathIntent) removedIntent.getParentIntent();
        MACAddress srcMacAddress = MACAddress.valueOf(spfIntent.getSrcMac());
        MACAddress dstMacAddress = MACAddress.valueOf(spfIntent.getDstMac());
        Path removedPath = new Path(srcMacAddress, dstMacAddress);
        synchronized (lock) {
            // There *shouldn't* be any packets queued if the flow has
            // just been removed.
            List<PacketToPush> packets = waitingPackets.removeAll(spfIntent.getId());
            if (!packets.isEmpty()) {
                log.warn("Removed flow {} has packets queued.", spfIntent.getId());
            }

            pendingFlows.remove(removedPath);
            log.debug("Removed from the pendingFlow: Path {}, Flow ID {}", removedPath, spfIntent.getId());
        }
    }

    private void flowInstalled(PathIntent installedPath) {
        if (log.isTraceEnabled()) {
            log.trace("Installed intent ID {}, path {}", installedPath.getParentIntent().getId(), installedPath.getPath());
        }

        ShortestPathIntent spfIntent = (ShortestPathIntent) installedPath.getParentIntent();
        MACAddress srcMacAddress = MACAddress.valueOf(spfIntent.getSrcMac());
        MACAddress dstMacAddress = MACAddress.valueOf(spfIntent.getDstMac());
        Path path = new Path(srcMacAddress, dstMacAddress);
        log.debug("Path spec {}", path);

        // TODO waiting packets should time out. We could request a path that
        // can't be installed right now because of a network partition. The path
        // may eventually be installed, but we may have received thousands of
        // packets in the meantime and probably don't want to send very old packets.

        List<PacketToPush> packets = null;
        net.onrc.onos.core.intent.Path graphPath = installedPath.getPath();

        short outPort;
        if (graphPath.isEmpty()) {
            outPort = (short) spfIntent.getDstPortNumber();
            log.debug("Path is empty. Maybe devices on the same switch. outPort {}", outPort);
        } else {
            outPort = graphPath.get(0).getSrc().getNumber().shortValue();
            log.debug("path{}, outPort {}", graphPath, outPort);
        }

        PushedFlow existingFlow = null;

        synchronized (lock) {
            existingFlow = pendingFlows.get(path);

            if (existingFlow != null) {
                existingFlow.installed = true;
                existingFlow.firstOutPort = outPort;
            } else {
                log.debug("ExistingFlow {} is null", path);
                return;
            }

            //Check both existing flow are installed status.
            if (existingFlow.installed) {
                packets = waitingPackets.removeAll(existingFlow.intentId);
                if (log.isDebugEnabled()) {
                    log.debug("removed my packets {} to push from waitingPackets. outPort {} size {}",
                            existingFlow.intentId, existingFlow.firstOutPort, packets.size());
                }
            } else {
                log.debug("Forward or reverse flows hasn't been pushed yet. return");
                return;
            }
        }

        for (PacketToPush packet : packets) {
            log.debug("Start packetToPush to sw {}, outPort {}, path {}", packet.dpid, existingFlow.firstOutPort, path);
            packetService.sendPacket(packet.eth, new SwitchPort(
                            packet.dpid, existingFlow.firstOutPort));
        }
    }

    @Override
    public void intentsChange(LinkedList<ChangedEvent> events) {
        for (ChangedEvent event : events) {
            log.debug("path intent ID {}, eventType {}", event.intent.getId() , event.eventType);
            PathIntent pathIntent = (PathIntent) pathIntentMap.getIntent(event.intent.getId());
            if (pathIntent == null) {
                continue;
            }

            if (!(pathIntent.getParentIntent() instanceof ShortestPathIntent)) {
                continue;
            }

            switch(event.eventType) {
                case ADDED:
                    break;
                case REMOVED:
                    break;
                case STATE_CHANGED:
                    IntentState state = pathIntent.getState();
                    switch (state) {
                        case INST_REQ:
                            break;
                        case INST_ACK:
                            flowInstalled(pathIntent);
                            break;
                        case INST_NACK:
                            break;
                        case DEL_REQ:
                            break;
                        case DEL_ACK:
                            flowRemoved(pathIntent);
                            break;
                        case DEL_PENDING:
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
