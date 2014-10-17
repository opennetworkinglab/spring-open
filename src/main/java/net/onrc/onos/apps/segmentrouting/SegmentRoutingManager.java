package net.onrc.onos.apps.segmentrouting;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOF13Switch;
import net.floodlightcontroller.core.IOF13Switch.NeighborSet;
import net.floodlightcontroller.core.internal.OFBarrierReplyFuture;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.SingletonTask;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.api.packet.IPacketListener;
import net.onrc.onos.api.packet.IPacketService;
import net.onrc.onos.core.flowprogrammer.IFlowPusherService;
import net.onrc.onos.core.intent.Path;
import net.onrc.onos.core.main.config.IConfigInfoService;
import net.onrc.onos.core.matchaction.MatchAction;
import net.onrc.onos.core.matchaction.MatchActionId;
import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.matchaction.MatchActionOperations.Operator;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.CopyTtlInAction;
import net.onrc.onos.core.matchaction.action.CopyTtlOutAction;
import net.onrc.onos.core.matchaction.action.DecMplsTtlAction;
import net.onrc.onos.core.matchaction.action.DecNwTtlAction;
import net.onrc.onos.core.matchaction.action.GroupAction;
import net.onrc.onos.core.matchaction.action.PopMplsAction;
import net.onrc.onos.core.matchaction.action.PushMplsAction;
import net.onrc.onos.core.matchaction.action.SetMplsIdAction;
import net.onrc.onos.core.matchaction.match.Ipv4Match;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.matchaction.match.MplsMatch;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.packet.ARP;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.packet.IPv4;
import net.onrc.onos.core.topology.ITopologyListener;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.Link;
import net.onrc.onos.core.topology.LinkData;
import net.onrc.onos.core.topology.MastershipData;
import net.onrc.onos.core.topology.MutableTopology;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.PortData;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.topology.SwitchData;
import net.onrc.onos.core.topology.TopologyEvents;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.IPv4Net;
import net.onrc.onos.core.util.SwitchPort;

import org.json.JSONArray;
import org.json.JSONException;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SegmentRoutingManager implements IFloodlightModule,
        ITopologyListener, IPacketListener {

    private static final Logger log = LoggerFactory
            .getLogger(SegmentRoutingManager.class);

    private ITopologyService topologyService;
    private IPacketService packetService;
    private MutableTopology mutableTopology;
    private ConcurrentLinkedQueue<IPv4> ipPacketQueue;

    private List<ArpEntry> arpEntries;
    private ArpHandler arpHandler;
    private GenericIpHandler ipHandler;
    private IcmpHandler icmpHandler;
    private IThreadPoolService threadPool;
    private SingletonTask discoveryTask;
    private SingletonTask linkAddTask;
    private SingletonTask testTask;
    private IFloodlightProviderService floodlightProvider;

    private HashMap<Switch, ECMPShortestPathGraph> graphs;
    private HashMap<String, LinkData> linksDown;
    private HashMap<String, LinkData> linksToAdd;
    private ConcurrentLinkedQueue<TopologyEvents> topologyEventQueue;
    private HashMap<Integer, HashMap<String, PolicyRouteInfo>> stitchInfo;
    private HashMap<Integer, HashMap<String, Integer>> tunnelGroupMap;

    private int numOfEvents = 0;
    private int numOfEventProcess = 0;
    private int numOfPopulation = 0;
    private long matchActionId = 0L;
    private final int DELAY_TO_ADD_LINK = 10;
    private final int MAX_NUM_LABELS = 3;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();

        l.add(IFloodlightProviderService.class);
        l.add(IConfigInfoService.class);
        l.add(ITopologyService.class);
        l.add(IPacketService.class);
        l.add(IFlowPusherService.class);
        l.add(ITopologyService.class);

        return l;

    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        arpHandler = new ArpHandler(context, this);
        icmpHandler = new IcmpHandler(context, this);
        ipHandler = new GenericIpHandler(context, this);
        arpEntries = new ArrayList<ArpEntry>();
        topologyService = context.getServiceImpl(ITopologyService.class);
        threadPool = context.getServiceImpl(IThreadPoolService.class);
        mutableTopology = topologyService.getTopology();
        ipPacketQueue = new ConcurrentLinkedQueue<IPv4>();
        graphs = new HashMap<Switch, ECMPShortestPathGraph>();
        linksDown = new HashMap<String, LinkData>();
        linksToAdd = new HashMap<String, LinkData>();
        topologyEventQueue = new ConcurrentLinkedQueue<TopologyEvents>();
        stitchInfo = new HashMap<Integer, HashMap<String, PolicyRouteInfo>>();
        packetService = context.getServiceImpl(IPacketService.class);
        tunnelGroupMap = new HashMap<Integer, HashMap<String, Integer>>();

        packetService.registerPacketListener(this);
        topologyService.addListener(this, false);


    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        ScheduledExecutorService ses = threadPool.getScheduledExecutor();

        discoveryTask = new SingletonTask(ses, new Runnable() {
            @Override
            public void run() {
                handleTopologyChangeEvents();
            }
        });

        linkAddTask = new SingletonTask(ses, new Runnable() {
            @Override
            public void run() {
                delayedAddLink();
            }
        });

        testTask = new SingletonTask(ses, new Runnable() {
            @Override
            public void run() {
                runTest();
            }
        });

        // policy routing test task
        //testTask.reschedule(20, TimeUnit.SECONDS);
    }


    @Override
    public void receive(Switch sw, Port inPort, Ethernet payload) {
        if (payload.getEtherType() == Ethernet.TYPE_ARP)
            arpHandler.processPacketIn(sw, inPort, payload);
        if (payload.getEtherType() == Ethernet.TYPE_IPV4) {
            addPacket((IPv4) payload.getPayload());
            if (((IPv4) payload.getPayload()).getProtocol() == IPv4.PROTOCOL_ICMP)
                icmpHandler.processPacketIn(sw, inPort, payload);
            else
                ipHandler.processPacketIn(sw, inPort, payload);
        }
        else {
            log.debug("{}", payload.toString());
        }
    }

    /**
     * Update ARP Cache using ARP packets It is used to set destination MAC
     * address to forward packets to known hosts. But, it will be replace with
     * Host information of Topology service later.
     *
     * @param arp APR packets to use for updating ARP entries
     */
    public void updateArpCache(ARP arp) {

        ArpEntry arpEntry = new ArpEntry(arp.getSenderHardwareAddress(),
                arp.getSenderProtocolAddress());
        // TODO: Need to check the duplication
        arpEntries.add(arpEntry);
    }

    /**
     * Get MAC address to known hosts
     *
     * @param destinationAddress IP address to get MAC address
     * @return MAC Address to given IP address
     */
    public byte[] getMacAddressFromIpAddress(int destinationAddress) {

        // Can't we get the host IP address from the TopologyService ??

        Iterator<ArpEntry> iterator = arpEntries.iterator();

        IPv4Address ipAddress = IPv4Address.of(destinationAddress);
        byte[] ipAddressInByte = ipAddress.getBytes();

        while (iterator.hasNext()) {
            ArpEntry arpEntry = iterator.next();
            byte[] address = arpEntry.targetIpAddress;

            IPv4Address a = IPv4Address.of(address);
            IPv4Address b = IPv4Address.of(ipAddressInByte);

            if (a.equals(b)) {
                log.debug("Found an arp entry");
                return arpEntry.targetMacAddress;
            }
        }

        return null;
    }

    /**
     * Send an ARP request via ArpHandler
     *
     * @param destinationAddress
     * @param sw
     * @param inPort
     *
     */
    public void sendArpRequest(Switch sw, int destinationAddress, Port inPort) {
        arpHandler.sendArpRequest(sw, destinationAddress, inPort);
    }

    /**
     * Temporary class to to keep ARP entry
     *
     */
    private class ArpEntry {

        byte[] targetMacAddress;
        byte[] targetIpAddress;

        private ArpEntry(byte[] macAddress, byte[] ipAddress) {
            this.targetMacAddress = macAddress;
            this.targetIpAddress = ipAddress;
        }
    }

    /**
     * Topology events that have been generated.
     *
     * @param topologyEvents the generated Topology Events
     * @see TopologyEvents
     */
    public void topologyEvents(TopologyEvents topologyEvents)
    {
        topologyEventQueue.add(topologyEvents);
        discoveryTask.reschedule(100, TimeUnit.MILLISECONDS);
    }

    /**
     * Process the multiple topology events with some delay (100MS at most for now)
     *
     */
    private void handleTopologyChangeEvents() {
        numOfEventProcess ++;

        Collection<LinkData> linkEntriesAddedAll = new ArrayList<LinkData>();
        Collection<PortData> portEntriesAddedAll = new ArrayList<PortData>();
        Collection<PortData> portEntriesRemovedAll = new ArrayList<PortData>();
        Collection<LinkData> linkEntriesRemovedAll = new ArrayList<LinkData>();
        Collection<SwitchData> switchAddedAll = new ArrayList<SwitchData>();
        Collection<SwitchData> switchRemovedAll = new ArrayList<SwitchData>();
        Collection<MastershipData> mastershipRemovedAll = new ArrayList<MastershipData>();

        while (!topologyEventQueue.isEmpty()) {
            // We should handle the events in the order of when they happen
            // TODO: We need to simulate the final results of multiple events
            // and shoot only the final state.
            // Ex: link s1-s2 down, link s1-s2 up --> Do nothing
            // Ex: ink s1-s2 up, s1-p1,p2 down --> link s1-s2 down

            TopologyEvents topologyEvents = topologyEventQueue.poll();

            Collection<LinkData> linkEntriesAdded = topologyEvents.getAddedLinkDataEntries();
            Collection<PortData> portEntriesAdded = topologyEvents.getAddedPortDataEntries();
            Collection<PortData> portEntriesRemoved = topologyEvents.getRemovedPortDataEntries();
            Collection<LinkData> linkEntriesRemoved = topologyEvents.getRemovedLinkDataEntries();
            Collection<SwitchData> switchAdded = topologyEvents.getAddedSwitchDataEntries();
            Collection<SwitchData> switchRemoved = topologyEvents.getRemovedSwitchDataEntries();
            Collection<MastershipData> mastershipRemoved = topologyEvents.getRemovedMastershipDataEntries();

            linkEntriesAddedAll.addAll(linkEntriesAdded);
            portEntriesAddedAll.addAll(portEntriesAdded);
            portEntriesRemovedAll.addAll(portEntriesRemoved);
            linkEntriesRemovedAll.addAll(linkEntriesRemoved);
            switchAddedAll.addAll(switchAdded);
            switchRemovedAll.addAll(switchRemoved);
            mastershipRemovedAll.addAll(mastershipRemoved);
            numOfEvents++;

            if (!portEntriesRemoved.isEmpty()) {
                processPortRemoval(portEntriesRemoved);
            }

            if (!linkEntriesRemoved.isEmpty()) {
                processLinkRemoval(linkEntriesRemoved);
            }

            if (!switchRemoved.isEmpty()) {
                processSwitchRemoved(switchRemoved);
            }

            if (!mastershipRemoved.isEmpty()) {
                log.debug("Mastership is removed. Check if ports are down also.");
            }

            if (!linkEntriesAdded.isEmpty()) {
                processLinkAdd(linkEntriesAdded, false);
            }

            if (!portEntriesAdded.isEmpty()) {
                processPortAdd(portEntriesAdded);
            }

            if (!switchAdded.isEmpty()) {
                processSwitchAdd(switchAdded);
            }

        }

        // TODO: 100ms is enough to check both mastership removed events
        // and the port removed events? What if the PORT_STATUS packets comes late?
        if (!mastershipRemovedAll.isEmpty()) {
            if (portEntriesRemovedAll.isEmpty()) {
                log.debug("Just mastership is removed. Do not do anthing.");
            }
            else {
                HashMap<String, MastershipData> mastershipToRemove =
                        new HashMap<String, MastershipData>();
                for (MastershipData ms: mastershipRemovedAll) {
                    for (PortData port: portEntriesRemovedAll) {
                        // TODO: check ALL ports of the switch are dead ..
                        if (port.getDpid().equals(ms.getDpid())) {
                            mastershipToRemove.put(ms.getDpid().toString(), ms);
                        }
                    }
                    log.debug("Swtich {} is really down.", ms.getDpid());
                }
                processMastershipRemoved(mastershipToRemove.values());
            }
        }

        log.debug("num events {}, num of process {}, "
                + "num of Population {}", numOfEvents, numOfEventProcess,
                numOfPopulation);
    }

    /**
     * Add the link immediately
     * The function is scheduled when link add event happens and called
     * DELAY_TO_ADD_LINK seconds after the event to avoid link flip-flop.
     */
    private void delayedAddLink() {

        processLinkAdd(linksToAdd.values(), true);

    }

    /**
     * Process the SwitchAdded events from topologyMananger.
     * It does nothing. When a switch is added, then link will be added too.
     * LinkAdded event will handle process all re-computation.
     *
     * @param switchAdded
     */
    private void processSwitchAdd(Collection<SwitchData> switchAdded) {

    }

    /**
     * Remove all ports connected to the switch removed
     *
     * @param mastershipRemoved master switch info removed
     */
    private void processMastershipRemoved(Collection<MastershipData>
        mastershipRemoved) {
        for (MastershipData mastership: mastershipRemoved) {
            Switch sw = mutableTopology.getSwitch(mastership.getDpid());
            for (Link link: sw.getOutgoingLinks()) {
                Port dstPort = link.getDstPort();
                IOF13Switch dstSw = (IOF13Switch) floodlightProvider.getMasterSwitch(
                        getSwId(dstPort.getDpid().toString()));
                if (dstSw != null) {
                    dstSw.removePortFromGroups(dstPort.getNumber());
                    log.debug("MasterSwitch {} is gone: remove port {}", sw.getDpid(), dstPort);
                }
            }
        }

        linksToAdd.clear();
        linksDown.clear();
    }

    /**
     * Remove all ports connected to the switch removed
     *
     * @param switchRemoved Switch removed
     */
    private void processSwitchRemoved(Collection<SwitchData> switchRemoved) {
        log.debug("SwitchRemoved event occurred !!!");
    }

    /**
     * Report ports added to driver
     *
     * @param portEntries
     */
    private void processPortAdd(Collection<PortData> portEntries) {
        // TODO: do we need to add ports with delay?
        for (PortData port : portEntries) {
            Dpid dpid = port.getDpid();

            IOF13Switch sw = (IOF13Switch) floodlightProvider.getMasterSwitch(
                    getSwId(port.getDpid().toString()));
            if (sw != null) {
                sw.addPortToGroups(port.getPortNumber());
                //log.debug("Add port {} to switch {}", port, dpid);
            }
        }
    }

    /**
     * Reports ports of new links to driver and recalculate ECMP SPG
     * If the link to add was removed before, then we just schedule the add link
     * event and do not recompute the path now.
     *
     * @param linkEntries
     */
    private void processLinkAdd(Collection<LinkData> linkEntries, boolean delayed) {

        for (LinkData link : linkEntries) {

            SwitchPort srcPort = link.getSrc();
            SwitchPort dstPort = link.getDst();

            String key = srcPort.getDpid().toString() +
                    dstPort.getDpid().toString();
            if (!delayed) {
                if (linksDown.containsKey(key)) {
                    linksToAdd.put(key, link);
                    linksDown.remove(key);
                    linkAddTask.reschedule(DELAY_TO_ADD_LINK, TimeUnit.SECONDS);
                    log.debug("Add link {} with 5 sec delay", link);
                    // TODO: What if we have multiple events of add link:
                    // one is new link add, the other one is link up for
                    // broken link? ECMPSPG function cannot deal with it for now
                    return;
                }
            }
            else {
                if (linksDown.containsKey(key)) {
                    linksToAdd.remove(key);
                    log.debug("Do not add the link {}: it is down again!", link);
                    return;
                }
            }

            IOF13Switch srcSw = (IOF13Switch) floodlightProvider.getMasterSwitch(
                    getSwId(srcPort.getDpid().toString()));
            IOF13Switch dstSw = (IOF13Switch) floodlightProvider.getMasterSwitch(
                    getSwId(dstPort.getDpid().toString()));

            if ((srcSw == null) || (dstSw == null))
                continue;

            srcSw.addPortToGroups(srcPort.getPortNumber());
            dstSw.addPortToGroups(dstPort.getPortNumber());

            //log.debug("Add a link port {} to switch {} to add link {}", srcPort, srcSw,
            //        link);
            //log.debug("Add a link port {} to switch {} to add link {}", dstPort, dstSw,
            //        link);

        }
        populateEcmpRoutingRules(false);
    }

    /**
     * Check if all links are gone b/w the two switches. If all links are gone,
     * then we need to recalculate the path. Otherwise, just report link failure
     * to the driver.
     *
     * @param linkEntries
     */
    private void processLinkRemoval(Collection<LinkData> linkEntries) {
        boolean recomputationRequired = false;

        for (LinkData link : linkEntries) {
            SwitchPort srcPort = link.getSrc();
            SwitchPort dstPort = link.getDst();

            IOF13Switch srcSw = (IOF13Switch) floodlightProvider.getMasterSwitch(
                    getSwId(srcPort.getDpid().toString()));
            IOF13Switch dstSw = (IOF13Switch) floodlightProvider.getMasterSwitch(
                    getSwId(dstPort.getDpid().toString()));
            if ((srcSw == null) || (dstSw == null))
                /* If this link is not between two switches, ignore it */
                continue;

            srcSw.removePortFromGroups(srcPort.getPortNumber());
            dstSw.removePortFromGroups(dstPort.getPortNumber());
            log.debug("Remove port {} from switch {}", srcPort, srcSw);
            log.debug("Remove port {} from switch {}", dstPort, dstSw);

            Switch srcSwitch = mutableTopology.getSwitch(srcPort.getDpid());
            if (srcSwitch.getLinkToNeighbor(dstPort.getDpid()) == null) {
                // TODO: it is only for debugging purpose.
                // We just need to call populateEcmpRoutingRules() and return;
                recomputationRequired = true;
                log.debug("All links are gone b/w {} and {}", srcPort.getDpid(),
                        dstPort.getDpid());
            }

            String key = link.getSrc().getDpid().toString()+
                    link.getDst().getDpid().toString();
            if (!linksDown.containsKey(key)) {
                linksDown.put(key, link);
            }
        }

        if (recomputationRequired)
            populateEcmpRoutingRules(false);
    }

    /**
     * report ports removed to the driver immediately
     *
     * @param portEntries
     */
    private void processPortRemoval(Collection<PortData> portEntries) {
        for (PortData port : portEntries) {
            Dpid dpid = port.getDpid();

            IOF13Switch sw = (IOF13Switch) floodlightProvider.getMasterSwitch(
                    getSwId(port.getDpid().toString()));
            if (sw != null) {
                sw.removePortFromGroups(port.getPortNumber());
                log.debug("Remove port {} from switch {}", port, dpid);
            }
        }
    }

    /**
     * Populate routing rules walking through the ECMP shortest paths
     *
     * @param modified if true, it "modifies" the rules
     */
    private void populateEcmpRoutingRules(boolean modified) {
        graphs.clear();
        Iterable<Switch> switches = mutableTopology.getSwitches();
        for (Switch sw : switches) {
            ECMPShortestPathGraph ecmpSPG = new ECMPShortestPathGraph(sw);
            graphs.put(sw, ecmpSPG);
            //log.debug("ECMPShortestPathGraph is computed for switch {}",
            //        HexString.toHexString(sw.getDpid().value()));
            populateEcmpRoutingRulesForPath(sw, ecmpSPG, modified);
        }
        numOfPopulation++;
    }

    /**
     * populate routing rules to forward packets from the switch given to
     * all other switches.
     *
     * @param sw source switch
     * @param ecmpSPG shortest path from the the source switch to all others
     * @param modified modification flag
     */
    private void populateEcmpRoutingRulesForPath(Switch sw,
            ECMPShortestPathGraph ecmpSPG, boolean modified) {

        HashMap<Integer, HashMap<Switch, ArrayList<ArrayList<Dpid>>>> switchVia =
                ecmpSPG.getAllLearnedSwitchesAndVia();
        for (Integer itrIdx : switchVia.keySet()) {
            //log.debug("ECMPShortestPathGraph:Switches learned in "
            //        + "Iteration{} from switch {}:",
            //        itrIdx,
            //        HexString.toHexString(sw.getDpid().value()));
            HashMap<Switch, ArrayList<ArrayList<Dpid>>> swViaMap =
                    switchVia.get(itrIdx);
            for (Switch targetSw : swViaMap.keySet()) {
                //log.debug("ECMPShortestPathGraph:****switch {} via:",
                //        HexString.toHexString(targetSw.getDpid().value()));
                String destSw = sw.getDpid().toString();
                List<String> fwdToSw = new ArrayList<String>();

                for (ArrayList<Dpid> via : swViaMap.get(targetSw)) {
                    //log.debug("ECMPShortestPathGraph:******{}) {}", ++i, via);
                    if (via.isEmpty()) {
                        fwdToSw.add(destSw);
                    }
                    else {
                        fwdToSw.add(via.get(0).toString());
                    }
                }
                setRoutingRule(targetSw, destSw, fwdToSw, modified);
            }

            // Send Barrier Message and make sure all rules are set
            // before we set the rules to next routers
            IOF13Switch sw13 = (IOF13Switch) floodlightProvider.getMasterSwitch(
                    getSwId(sw.getDpid().toString()));
            if (sw13 != null) {
                try {
                    OFBarrierReplyFuture replyFuture = sw13.sendBarrier();
                    replyFuture.get(10, TimeUnit.SECONDS);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    log.error("Barrier message not received for sw: {}", sw.getDpid());
                    e.printStackTrace();
                }
            }
        }

    }


    /**
     * This class is used only for link recovery optimization in
     * modifyEcmpRoutingRules() function.
     * TODO: please remove if the optimization is not used at all
     */
    private class SwitchPair {
        private Switch src;
        private Switch dst;

        public SwitchPair(Switch src, Switch dst) {
            this.src = src;
            this.dst = dst;
        }

        public Switch getSource() {
            return src;
        }

        public Switch getDestination() {
            return dst;
        }
    }

    /**
     * Modify the routing rules for the lost links
     * - Recompute the path if the link failed is included in the path
     * (including src and dest).
     *
     * @param newLink
     */
    private void modifyEcmpRoutingRules(LinkData linkRemoved) {

        //HashMap<Switch, SwitchPair> linksToRecompute = new HashMap<Switch, SwitchPair>();
        Set<SwitchPair> linksToRecompute = new HashSet<SwitchPair>();

        for (ECMPShortestPathGraph ecmpSPG : graphs.values()) {
            Switch rootSw = ecmpSPG.getRootSwitch();
            HashMap<Integer, HashMap<Switch, ArrayList<Path>>> paths =
                    ecmpSPG.getCompleteLearnedSwitchesAndPaths();
            for (HashMap<Switch, ArrayList<Path>> p: paths.values()) {
                for (Switch destSw: p.keySet()) {
                    ArrayList<Path> path = p.get(destSw);
                    if  (checkPath(path, linkRemoved)) {
                        boolean found = false;
                        for (SwitchPair pair: linksToRecompute) {
                            if (pair.getSource().getDpid() == rootSw.getDpid() &&
                                    pair.getSource().getDpid() == destSw.getDpid()) {
                                found = true;
                            }
                        }
                        if (!found) {
                            linksToRecompute.add(new SwitchPair(rootSw, destSw));
                        }
                    }
                }
            }
        }

        // Recompute the path for the specific route
        for (SwitchPair pair: linksToRecompute) {

            log.debug("Recompute path from {} to {}", pair.getSource(), pair.getDestination());
            // We need the following function for optimization
            //ECMPShortestPathGraph ecmpSPG =
            //     new ECMPShortestPathGraph(pair.getSource(), pair.getDestination());
            ECMPShortestPathGraph ecmpSPG =
                    new ECMPShortestPathGraph(pair.getSource());
            populateEcmpRoutingRulesForPath(pair.getSource(), ecmpSPG, true);
        }
    }

    /**
     * Check if the path is affected from the link removed
     *
     * @param path Path to check
     * @param linkRemoved link removed
     * @return true if the path contains the link removed
     */
    private boolean checkPath(ArrayList<Path> path, LinkData linkRemoved) {

        for (Path ppp: path) {
            // TODO: need to check if this is a bidirectional or
            // unidirectional
            for (LinkData link: ppp) {
                if (link.getDst().getDpid().equals(linkRemoved.getDst().getDpid()) &&
                        link.getSrc().getDpid().equals(linkRemoved.getSrc().getDpid()))
                    return true;
            }
        }

        return false;
    }

    /**
     *
     * Set routing rules in targetSw {forward packets to fwdToSw switches in
     * order to send packets to destSw} - If the target switch is an edge router
     * and final destnation switch is also an edge router, then set IP
     * forwarding rules to subnets - If only the target switch is an edge
     * router, then set IP forwarding rule to the transit router loopback IP
     * address - If the target is a transit router, then just set the MPLS
     * forwarding rule
     *
     * @param targetSw Switch to set the rules
     * @param destSw Final destination switches
     * @param fwdToSw next hop switches
     */
    private void setRoutingRule(Switch targetSw, String destSw,
            List<String> fwdToSw, boolean modified) {

        if (fwdToSw.isEmpty()) {
            fwdToSw.add(destSw);
        }

        // if both target SW and dest SW are an edge router, then set IP table
        if (IsEdgeRouter(targetSw.getDpid().toString()) &&
                IsEdgeRouter(destSw)) {
            // We assume that there is at least one transit router b/w edge
            // routers
            Switch destSwitch = mutableTopology.getSwitch(new Dpid(destSw));
            String subnets = destSwitch.getStringAttribute("subnets");
            setIpTableRouterSubnet(targetSw, subnets, getMplsLabel(destSw)
                    , fwdToSw, modified);

            String routerIp = destSwitch.getStringAttribute("routerIp");
            setIpTableRouter(targetSw, routerIp, getMplsLabel(destSw), fwdToSw,
                    null, modified);
            // Edge router can be a transit router
            setMplsTable(targetSw, getMplsLabel(destSw), fwdToSw, modified);
        }
        // Only if the target switch is the edge router, then set the IP rules
        else if (IsEdgeRouter(targetSw.getDpid().toString())) {
            // We assume that there is at least one transit router b/w edge
            // routers
            Switch destSwitch = mutableTopology.getSwitch(new Dpid(destSw));
            String routerIp = destSwitch.getStringAttribute("routerIp");
            setIpTableRouter(targetSw, routerIp, getMplsLabel(destSw), fwdToSw,
                    null, modified);
            // Edge router can be a transit router
            setMplsTable(targetSw, getMplsLabel(destSw), fwdToSw, modified);
        }
        // if it is a transit router, then set rules in the MPLS table
        else {
            setMplsTable(targetSw, getMplsLabel(destSw), fwdToSw, modified);
        }

    }

    /**
     * Set IP forwarding rule to the gateway of each subnet of switches
     *
     * @param targetSw Switch to set rules
     * @param subnets  subnet information
     * @param mplsLabel destination MPLS label
     * @param fwdToSw  router to forward packets to
     */
    private void setIpTableRouterSubnet(Switch targetSw, String subnets,
            String mplsLabel, List<String> fwdToSw, boolean modified) {

        Collection<MatchActionOperationEntry> entries =
                new ArrayList<MatchActionOperationEntry>();

        try {
            JSONArray arry = new JSONArray(subnets);
            for (int i = 0; i < arry.length(); i++) {
                String subnetIp = (String) arry.getJSONObject(i).get("subnetIp");
                setIpTableRouter(targetSw, subnetIp, mplsLabel, fwdToSw, entries,
                        modified);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (!entries.isEmpty()) {
            IOF13Switch sw13 = (IOF13Switch) floodlightProvider.getMasterSwitch(
                    getSwId(targetSw.getDpid().toString()));

            if (sw13 != null) {
                try {
                    sw13.pushFlows(entries);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Check if the switch is the edge router or not.
     *
     * @param dpid Dpid of the switch to check
     * @return true if it is an edge router, otherwise false
     */
    private boolean IsEdgeRouter(String dpid) {

        for (Switch sw : mutableTopology.getSwitches()) {
            String dpidStr = sw.getDpid().toString();
            if (dpid.equals(dpidStr)) {
                /*
                String subnetInfo = sw.getStringAttribute("subnets");
                if (subnetInfo == null || subnetInfo.equals("[]")) {
                    return false;
                }
                else
                    return true;
                */
                String isEdge = sw.getStringAttribute("isEdgeRouter");
                if (isEdge != null) {
                    if (isEdge.equals("true"))
                        return true;
                    else
                        return false;
                }
            }
        }

        return false;
    }

    /**
     * Set IP forwarding rule - If the destination is the next hop, then do not
     * push MPLS, just decrease the NW TTL - Otherwise, push MPLS label and set
     * the MPLS ID
     *
     * @param sw target switch to set rules
     * @param subnetIp Match IP address
     * @param mplsLabel MPLS label of final destination router
     * @param fwdToSws next hop routers
     * @param entries
     */
    private void setIpTableRouter(Switch sw, String subnetIp, String mplsLabel,
            List<String> fwdToSws, Collection<MatchActionOperationEntry> entries,
            boolean modified) {

        Ipv4Match ipMatch = new Ipv4Match(subnetIp);
        List<Action> actions = new ArrayList<>();
        GroupAction groupAction = new GroupAction();

        // If destination SW is the same as the fwd SW, then do not push MPLS
        // label
        if (fwdToSws.size() > 1) {
            PushMplsAction pushMplsAction = new PushMplsAction();
            SetMplsIdAction setIdAction = new SetMplsIdAction(Integer.parseInt(mplsLabel));
            CopyTtlOutAction copyTtlOutAction = new CopyTtlOutAction();
            DecMplsTtlAction decMplsTtlAction = new DecMplsTtlAction(1);

            //actions.add(pushMplsAction);
            //actions.add(copyTtlOutAction);
            //actions.add(decMplsTtlAction);
            // actions.add(setIdAction);
            groupAction.setEdgeLabel(Integer.parseInt(mplsLabel));
        }
        else {
            String fwdToSw = fwdToSws.get(0);
            if (getMplsLabel(fwdToSw).equals(mplsLabel)) {
                DecNwTtlAction decTtlAction = new DecNwTtlAction(1);
                actions.add(decTtlAction);
            }
            else {
                SetMplsIdAction setIdAction = new SetMplsIdAction(
                        Integer.parseInt(mplsLabel));
                CopyTtlOutAction copyTtlOutAction = new CopyTtlOutAction();
                DecMplsTtlAction decMplsTtlAction = new DecMplsTtlAction(1);

                //actions.add(pushMplsAction);
                //actions.add(copyTtlOutAction);
                //actions.add(decMplsTtlAction);
                // actions.add(setIdAction);
                groupAction.setEdgeLabel(Integer.parseInt(mplsLabel));
            }
        }

        for (String fwdSw : fwdToSws) {
            groupAction.addSwitch(new Dpid(fwdSw));
        }
        actions.add(groupAction);

        MatchAction matchAction = new MatchAction(new MatchActionId(matchActionId++),
                new SwitchPort((long) 0, (short) 0), ipMatch, actions);

        Operator operator = null;
        if (modified)
            operator =  Operator.MODIFY;
        else
            operator = Operator.ADD;

        MatchActionOperationEntry maEntry =
                new MatchActionOperationEntry(operator, matchAction);

        IOF13Switch sw13 = (IOF13Switch) floodlightProvider.getMasterSwitch(
                getSwId(sw.getDpid().toString()));

        if (sw13 != null) {
            try {
                //printMatchActionOperationEntry(sw, maEntry);
                if (entries != null)
                    entries.add(maEntry);
                else
                    sw13.pushFlow(maEntry);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Set MPLS forwarding rules to MPLS table
     * </p>
     * If the destination is the same as the next hop to forward packets then,
     * pop the MPLS label according to PHP rule. Here, if BoS is set, then
     * copy TTL In and decrement NW TTL. Otherwise, it just decrement the MPLS
     * TTL of the another MPLS header.
     * If the next hop is not the destination, just forward packets to next
     * hops using Group action.
     *
     * @param sw Switch to set the rules
     * @param mplsLabel destination MPLS label
     * @param fwdSws next hop switches
     * */
    private void setMplsTable(Switch sw, String mplsLabel, List<String> fwdSws,
            boolean modified) {

        if (fwdSws.isEmpty())
            return;

        Collection<MatchActionOperationEntry> maEntries =
                new ArrayList<MatchActionOperationEntry>();
        String fwdSw1 = fwdSws.get(0);

        if (fwdSws.size() == 1 && mplsLabel.equals(getMplsLabel(fwdSw1))) {
            // One rule for Bos = 1
            MplsMatch mplsMatch = new MplsMatch(Integer.parseInt(mplsLabel), true);
            List<Action> actions = new ArrayList<Action>();

            PopMplsAction popAction = new PopMplsAction(EthType.IPv4);
            CopyTtlInAction copyTtlInAction = new CopyTtlInAction();
            DecNwTtlAction decNwTtlAction = new DecNwTtlAction(1);

            actions.add(copyTtlInAction);
            actions.add(popAction);
            actions.add(decNwTtlAction);

            GroupAction groupAction = new GroupAction();
            groupAction.addSwitch(new Dpid(fwdSw1));
            actions.add(groupAction);

            MatchAction matchAction = new MatchAction(new MatchActionId(matchActionId++),
                    new SwitchPort((long) 0, (short) 0), mplsMatch, actions);
            Operator operator = Operator.ADD;
            MatchActionOperationEntry maEntry =
                    new MatchActionOperationEntry(operator, matchAction);
            maEntries.add(maEntry);

            // One rule for Bos = 0
            MplsMatch mplsMatchBos = new MplsMatch(Integer.parseInt(mplsLabel), false);
            List<Action> actionsBos = new ArrayList<Action>();
            PopMplsAction popActionBos = new PopMplsAction(EthType.MPLS_UNICAST);
            DecMplsTtlAction decMplsTtlAction = new DecMplsTtlAction(1);

            actionsBos.add(copyTtlInAction);
            actionsBos.add(popActionBos);
            actionsBos.add(decMplsTtlAction);
            actionsBos.add(groupAction);

            MatchAction matchActionBos = new MatchAction(new MatchActionId(matchActionId++),
                    new SwitchPort((long) 0, (short) 0), mplsMatchBos, actionsBos);
            MatchActionOperationEntry maEntryBos =
                    new MatchActionOperationEntry(operator, matchActionBos);
            maEntries.add(maEntryBos);
        }
        else {
            MplsMatch mplsMatch = new MplsMatch(Integer.parseInt(mplsLabel), false);
            List<Action> actions = new ArrayList<Action>();

            DecMplsTtlAction decMplsTtlAction = new DecMplsTtlAction(1);
            actions.add(decMplsTtlAction);

            GroupAction groupAction = new GroupAction();
            for (String fwdSw : fwdSws)
                groupAction.addSwitch(new Dpid(fwdSw));
            actions.add(groupAction);

            MatchAction matchAction = new MatchAction(new MatchActionId(
                    matchActionId++),
                    new SwitchPort((long) 0, (short) 0), mplsMatch, actions);
            Operator operator = Operator.ADD;
            MatchActionOperationEntry maEntry =
                    new MatchActionOperationEntry(operator, matchAction);
            maEntries.add(maEntry);

            // BoS = 1
            MplsMatch mplsMatchBoS = new MplsMatch(Integer.parseInt(mplsLabel), true);
            List<Action> actionsBoS = new ArrayList<Action>();

            DecMplsTtlAction decMplsTtlActionBoS = new DecMplsTtlAction(1);
            actionsBoS.add(decMplsTtlActionBoS);

            GroupAction groupActionBoS = new GroupAction();
            for (String fwdSw : fwdSws)
                groupActionBoS.addSwitch(new Dpid(fwdSw));
            actionsBoS.add(groupActionBoS);

            MatchAction matchActionBos = new MatchAction(new MatchActionId(
                    matchActionId++),
                    new SwitchPort((long) 0, (short) 0), mplsMatchBoS, actionsBoS);
            MatchActionOperationEntry maEntryBoS =
                    new MatchActionOperationEntry(operator, matchActionBos);
            maEntries.add(maEntryBoS);
        }
        IOF13Switch sw13 = (IOF13Switch) floodlightProvider.getMasterSwitch(
                getSwId(sw.getDpid().toString()));

        if (sw13 != null) {
            try {
                //printMatchActionOperationEntry(sw, maEntry);
                sw13.pushFlows(maEntries);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Create a tunnel for policy routing
     * It delivers the node IDs of tunnels to driver.
     * Split the node IDs if number of IDs exceeds the limit for stitching.
     *
     * @param tunnelId  Node IDs for the tunnel
     * @param Ids tunnel ID
     */
    public boolean createTunnel(int tunnelId, List<String> Ids) {

        if (tunnelId < 0) {
            log.debug("Tunnel ID should be posivtive integer.");
            return false;
        }

        if (Ids.isEmpty() || Ids.size() < 2) {
            log.debug("Wrong tunnel information");
            return false;
        }

        HashMap<String, PolicyRouteInfo> stitchingRule = getStitchingRule(Ids);
        stitchInfo.put(Integer.valueOf(tunnelId), stitchingRule);
        if (stitchingRule == null) {
            log.debug("Failed to get the policy rule.");
            return false;
        }
        HashMap<String, Integer> switchGroupPair = new HashMap<String, Integer>();
        for (String targetDpid: stitchingRule.keySet()) {
            PolicyRouteInfo route = stitchingRule.get(targetDpid);

            IOF13Switch targetSw = (IOF13Switch) floodlightProvider.getMasterSwitch(
                    getSwId(targetDpid.toString()));

            if (targetSw == null) {
                log.debug("Switch {} is gone.", targetDpid);
                return false;
            }

            NeighborSet ns = new NeighborSet();
            for (Dpid dpid: route.getFwdSwDpid())
                ns.addDpid(dpid);

            printTunnelInfo(targetSw, tunnelId, route.getRoute(), ns);
            int groupId = targetSw.createTunnel(tunnelId, route.getRoute(), ns);
            switchGroupPair.put(targetDpid.toString(), groupId);

        }

        tunnelGroupMap.put(Integer.valueOf(tunnelId), switchGroupPair);

        return true;
    }

    /**
     * Set policy table for policy routing
     *
     * @param sw
     * @param mplsLabel
     */
    public void setPolicyTable(MACAddress srcMac, MACAddress dstMac,
            Short etherType, IPv4Net srcIp, IPv4Net dstIp, Byte ipProto,
            Short srcTcpPort, Short dstTcpPort, int tid) {

        HashMap<String, PolicyRouteInfo> routeInfo = stitchInfo.get(Integer.valueOf(tid));
        HashMap<String, Integer> switchGroupPair = tunnelGroupMap.get(Integer.valueOf(tid));
        for (String srcDpid: routeInfo.keySet()) {

            PacketMatchBuilder packetBuilder = new PacketMatchBuilder();

            if (srcMac != null)
                packetBuilder.setSrcMac(srcMac);
            if (dstMac != null)
                packetBuilder.setDstMac(dstMac);
            if (etherType != null) {
                packetBuilder.setEtherType(etherType);
            }
            if (srcIp != null) {
                packetBuilder.setSrcIp(srcIp.address(), srcIp.prefixLen());
            }
            if (dstIp != null) {
                packetBuilder.setDstIp(dstIp.address(), dstIp.prefixLen());
            }
            if (ipProto != null) {
                packetBuilder.setIpProto(ipProto);
            }
            if (srcTcpPort > 0) {
                packetBuilder.setSrcTcpPort(srcTcpPort);
            }
            if (dstTcpPort > 0) {
                packetBuilder.setDstTcpPort(dstTcpPort);
            }
            PacketMatch policyMatch = packetBuilder.build();

            List<Action> actions = new ArrayList<>();
            GroupAction groupAction = new GroupAction();
            int gropuId = switchGroupPair.get(srcDpid);
            groupAction.setGroupId(gropuId);
            actions.add(groupAction);

            MatchAction matchAction = new MatchAction(new MatchActionId(
                    matchActionId++),
                    new SwitchPort((long) 0, (short) 0), policyMatch, actions);
            MatchActionOperationEntry maEntry =
                    new MatchActionOperationEntry(Operator.ADD, matchAction);

            IOF13Switch sw13 = (IOF13Switch) floodlightProvider.getMasterSwitch(
                    getSwId(srcDpid));

            if (sw13 != null) {
                printMatchActionOperationEntry(sw13, maEntry);
                try {
                    sw13.pushFlow(maEntry);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Get the forwarding Switch DPIDs to send packets to a node
     *
     * @param srcSw source switch
     * @param nodeId destination node Id
     * @return list of switch DPID to forward packets to
     */

    private List<Dpid> getForwardingSwitchForNodeId(Switch srcSw, String nodeId) {

        List<Dpid> fwdSws = new ArrayList<Dpid>();
        Switch destSw = null;

        destSw = getSwitchFromNodeId(nodeId);

        if (destSw == null) {
            log.debug("Cannot find the switch with ID {}", nodeId);
            return null;
        }

        ECMPShortestPathGraph ecmpSPG = new ECMPShortestPathGraph(srcSw);

        HashMap<Integer, HashMap<Switch, ArrayList<ArrayList<Dpid>>>> switchVia =
                ecmpSPG.getAllLearnedSwitchesAndVia();
        for (Integer itrIdx : switchVia.keySet()) {
            HashMap<Switch, ArrayList<ArrayList<Dpid>>> swViaMap =
                    switchVia.get(itrIdx);
            for (Switch targetSw : swViaMap.keySet()) {
                String destSwDpid = destSw.getDpid().toString();
                if (targetSw.getDpid().toString().equals(destSwDpid)) {
                    for (ArrayList<Dpid> via : swViaMap.get(targetSw)) {
                        if (via.isEmpty()) {
                            fwdSws.add(destSw.getDpid());
                        }
                        else {
                            fwdSws.add(via.get(0));
                        }
                    }
                }
            }
        }

        return fwdSws;
    }

    /**
     * Get switch for the node Id specified
     *
     * @param nodeId node ID for switch
     * @return Switch
     */
    private Switch getSwitchFromNodeId(String nodeId) {

        for (Switch sw : mutableTopology.getSwitches()) {
            String id = sw.getStringAttribute("nodeSid");
            if (id.equals(nodeId)) {
                return sw;
            }
        }

        return null;
    }

    /**
     * Convert a string DPID to its Switch Id (integer)
     *
     * @param dpid
     * @return
     */
    private long getSwId(String dpid) {

        long swId = 0;

        String swIdHexStr = "0x"+dpid.substring(dpid.lastIndexOf(":") + 1);
        if (swIdHexStr != null)
            swId = Integer.decode(swIdHexStr);

        return swId;
    }

    private void runTest() {

        String[] routeArray = {"101", "102", "103", "104", "105", "108", "110"};
        List<String> routeList = new ArrayList<String>();
        for (int i = 0; i < routeArray.length; i++)
            routeList.add(routeArray[i]);

        if (createTunnel(1, routeList)) {
            IPv4Net srcIp = new IPv4Net("10.0.1.1/24");
            IPv4Net dstIp = new IPv4Net("10.1.2.1/24");

            this.setPolicyTable(null, null, Ethernet.TYPE_IPV4, srcIp, dstIp, IPv4.PROTOCOL_ICMP, (short)-1, (short)-1, 1);
        }
        else {
            testTask.reschedule(5, TimeUnit.SECONDS);
        }
    }

    private void runTest1() {

        String dpid1 = "00:00:00:00:00:00:00:01";
        String dpid2 = "00:00:00:00:00:00:00:0a";
        Switch srcSw = mutableTopology.getSwitch(new Dpid(dpid1));
        Switch dstSw = mutableTopology.getSwitch(new Dpid(dpid2));

        if (srcSw == null || dstSw == null) {
            testTask.reschedule(1, TimeUnit.SECONDS);
            log.debug("Switch is gone. Reschedule the test");
            return;
        }

        String[] routeArray = {"101", "102", "105", "108", "110"};
        List<String> routeList = new ArrayList<String>();
        for (int i = 0; i < routeArray.length; i++)
            routeList.add(routeArray[i]);

        List<String> optimizedRoute = this.getOptimizedPath(srcSw, dstSw, routeList);

        log.debug("Test set is {}", routeList.toString());
        log.debug("Result set is {}", optimizedRoute.toString());


    }

    /**
     * Optimize the mpls label
     * The feature will be used only for policy of "avoid a specific switch".
     * Check route to each router in route backward.
     * If there is only one route to the router and the routers are included in
     * the route, remove the id from the path.
     * A-B-C-D-E  => A-B-C-D-E -> A-E
     *   |   |    => A-B-H-I   -> A-I
     *   F-G-H-I  => A-D-I     -> A-D-I
     */
    private List<String> getOptimizedPath(Switch srcSw, Switch dstSw, List<String> route) {

        List<String> optimizedPath = new ArrayList<String>();
        optimizedPath.addAll(route);
        ECMPShortestPathGraph ecmpSPG = new ECMPShortestPathGraph(srcSw);

        HashMap<Integer, HashMap<Switch, ArrayList<Path>>> paths =
                ecmpSPG.getCompleteLearnedSwitchesAndPaths();
        for (HashMap<Switch, ArrayList<Path>> p: paths.values()) {
            for (Switch s: p.keySet()) {
                if (s.getDpid().toString().equals(dstSw.getDpid().toString())) {
                    ArrayList<Path> ecmpPaths = p.get(s);
                    if (ecmpPaths!= null && ecmpPaths.size() == 1) {
                        for (Path path: ecmpPaths) {
                            for (LinkData link: path) {
                                String srcId = getMplsLabel(link.getSrc().getDpid().toString());
                                String dstId = getMplsLabel(link.getSrc().getDpid().toString());
                                if (optimizedPath.contains(srcId)) {
                                    optimizedPath.remove(srcId);
                                }
                                if (optimizedPath.contains(dstId)) {
                                    optimizedPath.remove(dstId);
                                }
                            }
                        }
                    }
                }
            }
        }

        return optimizedPath;

    }


    class PolicyRouteInfo {

        String srcSwDpid;
        List<Dpid> fwdSwDpids;
        List<String> route;

        PolicyRouteInfo() {
            fwdSwDpids = new ArrayList<Dpid>();
            route = new ArrayList<String>();
        }

        void setSrcDpid(String dpid) {
            this.srcSwDpid = dpid;
        }

        void setFwdSwDpid(List<Dpid> dpid) {
            this.fwdSwDpids = dpid;
        }

        void addRoute(String id) {
            route.add(id);
        }

        void setRoute(List<String> r) {
            this.route = r;
        }

        String getSrcSwDpid() {
            return this.srcSwDpid;
        }

        List<Dpid> getFwdSwDpid() {
            return this.fwdSwDpids;
        }

        List<String> getRoute() {
            return this.route;
        }
    }


    /**
     *
     *
     * @param srcSw
     * @param dstSw
     * @param route
     * @return
     */
    private HashMap<String, PolicyRouteInfo> getStitchingRule(List<String> route) {

        if (route.isEmpty() || route.size() < 2)
            return null;

        HashMap<String, PolicyRouteInfo> rules = new HashMap<String, PolicyRouteInfo>();

        Switch srcSw = this.getSwitchFromNodeId(route.get(0));
        String srcDpid = srcSw.getDpid().toString();

        if (route.size() <= MAX_NUM_LABELS+1) {
            PolicyRouteInfo info = new PolicyRouteInfo();
            info.setSrcDpid(srcSw.getDpid().toString());
            List<Dpid> fwdSwDpids = getForwardingSwitchForNodeId(srcSw, route.get(1));
            info.setFwdSwDpid(fwdSwDpids);
            route.remove(0);
            info.setRoute(route);
            rules.put(srcDpid, info);
            return rules;
        }

        int i = 0;
        PolicyRouteInfo routeInfo = new PolicyRouteInfo();
        String prevNodeId = null;
        boolean checkNeighbor = true;

        for (String nodeId: route) {
            if (i == 0) {
                routeInfo.setSrcDpid(srcDpid);
                srcSw = getSwitchFromNodeId(nodeId);
                i++;
            }
            else if (i == 1) {
                if (checkNeighbor) {
                    // Check if next node is the neighbor SW of the source SW
                    List<Dpid> fwdSwDpids = getForwardingSwitchForNodeId(srcSw, nodeId);
                    if (fwdSwDpids == null || fwdSwDpids.isEmpty()) {
                        log.debug("There is no route from node {} to node {}", srcSw.getDpid(), nodeId);
                        return null;
                    }
                    // If first Id is one of the neighbors, do not include it to route, but set it as a fwd SW.
                    boolean match = false;
                    for (Dpid dpid: fwdSwDpids) {
                        if (getMplsLabel(dpid.toString()).toString().equals(nodeId)) {
                            List<Dpid> fwdSws = new ArrayList<Dpid>();
                            fwdSws.add(dpid);
                            routeInfo.setFwdSwDpid(fwdSws);
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        routeInfo.addRoute(nodeId);
                        routeInfo.setFwdSwDpid(fwdSwDpids);
                        i++;
                    }

                    checkNeighbor = false;
                }
                else {
                    routeInfo.addRoute(nodeId);
                    i++;
                }
            }
            else {
                routeInfo.addRoute(nodeId);
                i++;
            }

            if (i == MAX_NUM_LABELS+1) {
                rules.put(srcDpid, routeInfo);
                routeInfo = new PolicyRouteInfo();
                srcSw = getSwitchFromNodeId(nodeId);
                srcDpid = getSwitchFromNodeId(nodeId).getDpid().toString();
                routeInfo.setSrcDpid(srcDpid);
                i = 1;
                checkNeighbor = true;
            }
        }

        if (i < MAX_NUM_LABELS+1) {
            rules.put(srcDpid, routeInfo);
        }

        return rules;
    }

    /**
     * print tunnel info - used only for debugging.
     * @param targetSw
     *
     * @param fwdSwDpids
     * @param ids
     * @param tunnelId
     */
    private void printTunnelInfo(IOF13Switch targetSw, int tunnelId,
            List<String> ids, NeighborSet ns) {
        StringBuilder logStr = new StringBuilder("In switch " +
            targetSw.getId() + ", create a tunnel " + tunnelId + " " + " of push ");
        for (String id: ids)
            logStr.append(id + "-");
        logStr.append(" output to ");
        for (Dpid dpid: ns.getDpids())
            logStr.append(dpid + " - ");

        log.debug(logStr.toString());

    }



    /**
     * Debugging function to print out the Match Action Entry
     * @param sw13
     *
     * @param maEntry
     */
    private void printMatchActionOperationEntry(
            IOF13Switch sw13, MatchActionOperationEntry maEntry) {

        StringBuilder logStr = new StringBuilder("In switch " + sw13.getId() + ", ");

        MatchAction ma = maEntry.getTarget();
        Match m = ma.getMatch();
        List<Action> actions = ma.getActions();

        if (m instanceof Ipv4Match) {
            logStr.append("If the IP matches with ");
            IPv4Net ip = ((Ipv4Match) m).getDestination();
            logStr.append(ip.toString());
            logStr.append(" then ");
        }
        else if (m instanceof MplsMatch) {
            logStr.append("If the MPLS label matches with ");
            int mplsLabel = ((MplsMatch) m).getMplsLabel();
            logStr.append(mplsLabel);
            logStr.append(" then ");
        }
        else if (m instanceof PacketMatch) {
            GroupAction ga = (GroupAction)actions.get(0);
            logStr.append("if the policy match is XXX then go to group " +
                    ga.getGroupId());
            log.debug(logStr.toString());
            return;
        }

        logStr.append(" do { ");
        for (Action action : actions) {
            if (action instanceof CopyTtlInAction) {
                logStr.append("copy ttl In, ");
            }
            else if (action instanceof CopyTtlOutAction) {
                logStr.append("copy ttl Out, ");
            }
            else if (action instanceof DecMplsTtlAction) {
                logStr.append("Dec MPLS TTL , ");
            }
            else if (action instanceof GroupAction) {
                logStr.append("Forward packet to < ");
                NeighborSet dpids = ((GroupAction) action).getDpids();
                logStr.append(dpids.toString() + ",");

            }
            else if (action instanceof PopMplsAction) {
                logStr.append("Pop MPLS label, ");
            }
            else if (action instanceof PushMplsAction) {
                logStr.append("Push MPLS label, ");
            }
            else if (action instanceof SetMplsIdAction) {
                int id = ((SetMplsIdAction) action).getMplsId();
                logStr.append("Set MPLS ID as " + id + ", ");
            }
        }

        log.debug(logStr.toString());

    }

    /**
     * Get MPLS label reading the config file
     *
     * @param dipid DPID of the switch
     * @return MPLS label for the switch
     */

    private String getMplsLabel(String dpid) {

        String mplsLabel = null;
        for (Switch sw : mutableTopology.getSwitches()) {
            String dpidStr = sw.getDpid().toString();
            if (dpid.equals(dpidStr)) {
                mplsLabel = sw.getStringAttribute("nodeSid");
                break;
            }
        }

        return mplsLabel;
    }

    /**
     * The function checks if given IP matches to the given subnet mask
     *
     * @param addr - subnet address to match
     * @param addr1 - IP address to check
     * @return true if the IP address matches to the subnet, otherwise false
     */
    public boolean netMatch(String addr, String addr1) { // addr is subnet
                                                         // address and addr1 is
                                                         // ip address. Function
                                                         // will return true, if
                                                         // addr1 is within
                                                         // addr(subnet)

        String[] parts = addr.split("/");
        String ip = parts[0];
        int prefix;

        if (parts.length < 2) {
            prefix = 0;
        } else {
            prefix = Integer.parseInt(parts[1]);
        }

        Inet4Address a = null;
        Inet4Address a1 = null;
        try {
            a = (Inet4Address) InetAddress.getByName(ip);
            a1 = (Inet4Address) InetAddress.getByName(addr1);
        } catch (UnknownHostException e) {
        }

        byte[] b = a.getAddress();
        int ipInt = ((b[0] & 0xFF) << 24) |
                ((b[1] & 0xFF) << 16) |
                ((b[2] & 0xFF) << 8) |
                ((b[3] & 0xFF) << 0);

        byte[] b1 = a1.getAddress();
        int ipInt1 = ((b1[0] & 0xFF) << 24) |
                ((b1[1] & 0xFF) << 16) |
                ((b1[2] & 0xFF) << 8) |
                ((b1[3] & 0xFF) << 0);

        int mask = ~((1 << (32 - prefix)) - 1);

        if ((ipInt & mask) == (ipInt1 & mask)) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Add a routing rule for the host
     *
     * @param sw - Switch to add the rule
     * @param hostIpAddress Destination host IP address
     * @param hostMacAddress Destination host MAC address
     */
    public void addRouteToHost(Switch sw, int hostIpAddress, byte[] hostMacAddress) {
        ipHandler.addRouteToHost(sw, hostIpAddress, hostMacAddress);

    }

    /**
     * Add IP packet to a buffer queue
     *
     * @param ipv4
     */
    public void addPacket(IPv4 ipv4) {
        ipPacketQueue.add(ipv4);
    }

    /**
     * Retrieve all packets whose destination is the given address.
     *
     * @param destIp Destination address of packets to retrieve
     */
    public List<IPv4> getIpPacketFromQueue(byte[] destIp) {

        List<IPv4> bufferedPackets = new ArrayList<IPv4>();

        if (!ipPacketQueue.isEmpty()) {
            for (IPv4 ip : ipPacketQueue) {
                int dest = ip.getDestinationAddress();
                IPv4Address ip1 = IPv4Address.of(dest);
                IPv4Address ip2 = IPv4Address.of(destIp);
                if (ip1.equals(ip2)) {
                    bufferedPackets.add((IPv4) (ipPacketQueue.poll()).clone());
                }
            }
        }

        return bufferedPackets;
    }



}
