package net.onrc.onos.apps.segmentrouting;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOF13Switch.NeighborSet;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.SingletonTask;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.onrc.onos.api.packet.IPacketListener;
import net.onrc.onos.api.packet.IPacketService;
import net.onrc.onos.core.flowprogrammer.IFlowPusherService;
import net.onrc.onos.core.main.config.IConfigInfoService;
import net.onrc.onos.core.matchaction.MatchAction;
import net.onrc.onos.core.matchaction.MatchActionId;
import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.CopyTtlInAction;
import net.onrc.onos.core.matchaction.action.CopyTtlOutAction;
import net.onrc.onos.core.matchaction.action.DecMplsTtlAction;
import net.onrc.onos.core.matchaction.action.DecNwTtlAction;
import net.onrc.onos.core.matchaction.action.GroupAction;
import net.onrc.onos.core.matchaction.action.PopMplsAction;
import net.onrc.onos.core.matchaction.action.PushMplsAction;
import net.onrc.onos.core.matchaction.action.SetMplsIdAction;
import net.onrc.onos.core.matchaction.match.Ipv4PacketMatch;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.matchaction.match.MplsMatch;
import net.onrc.onos.core.packet.ARP;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.packet.IPv4;
import net.onrc.onos.core.topology.ITopologyListener;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.MutableTopology;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.topology.TopologyEvents;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.IPv4Net;
import net.onrc.onos.core.util.SwitchPort;

import org.json.JSONArray;
import org.json.JSONException;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SegmentRoutingManager implements IFloodlightModule,
						ITopologyListener, IPacketListener {

    private static final Logger log = LoggerFactory
            .getLogger(SegmentRoutingManager.class);
    private ITopologyService topologyService;
    private IPacketService packetService;
    private MutableTopology mutableTopology;

    private List<ArpEntry> arpEntries;
    private ArpHandler arpHandler;
    private GenericIpHandler ipHandler;
    private IcmpHandler icmpHandler;
    private boolean networkConverged;
    private IThreadPoolService threadPool;
    private SingletonTask discoveryTask;

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

        arpHandler = new ArpHandler(context, this);
        icmpHandler = new IcmpHandler(context, this);
        ipHandler = new GenericIpHandler(context, this);
        arpEntries = new ArrayList<ArpEntry>();
        topologyService = context.getServiceImpl(ITopologyService.class);
        threadPool = context.getServiceImpl(IThreadPoolService.class);
        mutableTopology = topologyService.getTopology();
        topologyService.addListener(this, false);

        this.packetService = context.getServiceImpl(IPacketService.class);
        packetService.registerPacketListener(this);
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        networkConverged = false;

        ScheduledExecutorService ses = threadPool.getScheduledExecutor();

        discoveryTask = new SingletonTask(ses, new Runnable() {
            @Override
            public void run() {
                populateEcmpRoutingRules();
            }
        });

        discoveryTask.reschedule(10, TimeUnit.SECONDS);
    }

    @Override
    public void receive(Switch sw, Port inPort, Ethernet payload) {
    	if (payload.getEtherType() == Ethernet.TYPE_ARP)
    		arpHandler.processPacketIn(sw, inPort, payload);
        if (payload.getEtherType() == Ethernet.TYPE_IPV4) {
        	if (((IPv4)payload.getPayload()).getProtocol() != IPv4.PROTOCOL_ICMP)
        		icmpHandler.processPacketIn(sw, inPort, payload);
        	else
        		ipHandler.processPacketIn(sw, inPort, payload);
        }
    }
    /**
     * Update ARP Cache using ARP packets
     * It is used to set destination MAC address to forward packets to known hosts.
     * But, it will be replace with Host information of Topology service later.
     *
     * @param arp APR packets to use for updating ARP entries
     */
    public void updateArpCache(ARP arp) {

        ArpEntry arpEntry = new ArpEntry(arp.getSenderHardwareAddress(), arp.getSenderProtocolAddress());
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

        while (iterator.hasNext() ) {
            ArpEntry arpEntry = iterator.next();
            byte[] address = arpEntry.targetIpAddress;

            IPv4Address a = IPv4Address.of(address);
            IPv4Address b = IPv4Address.of(ipAddressInByte);

            if ( a.equals(b)) {
                log.debug("Found an arp entry");
                return arpEntry.targetMacAddress;
            }
        }

        return null;
    }

    /**
     * Send an ARP request via ArpHandler
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
    	/**
    	 * Any Link update events, compute the ECMP path graph for all switch nodes

    	if ((topologyEvents.getAddedLinkDataEntries() != null) ||
    		(topologyEvents.getRemovedLinkDataEntries() != null))
    	{
    		Iterable<Switch> switches= mutableTopology.getSwitches();
            for (Switch sw : switches) {
            	ECMPShortestPathGraph ecmpSPG = new ECMPShortestPathGraph(sw);
                log.debug("ECMPShortestPathGraph is computed for switch {}",
                		HexString.toHexString(sw.getDpid().value()));


                HashMap<Integer, HashMap<Switch,ArrayList<Path>>> pathGraph =
                                    ecmpSPG.getCompleteLearnedSwitchesAndPaths();
                for (Integer itrIdx: pathGraph.keySet()){

                    HashMap<Switch, ArrayList<Path>> swPathsMap =
                                                pathGraph.get(itrIdx);
                    for (Switch targetSw: swPathsMap.keySet()){
                        log.debug("ECMPShortestPathGraph:Paths in Pass{} from "
                                + "             switch {} to switch {}:****",
                                itrIdx,
                                HexString.toHexString(sw.getDpid().value()),
                                HexString.toHexString(targetSw.getDpid().value()));
                        int i=0;
                        for (Path path:swPathsMap.get(targetSw)){
                            log.debug("****ECMPShortestPathGraph:Path{} is {}",i++,path);
                        }
                    }
                }

                HashMap<Integer, HashMap<Switch,ArrayList<ArrayList<Dpid>>>> switchVia =
                        ecmpSPG.getAllLearnedSwitchesAndVia();
                for (Integer itrIdx: switchVia.keySet()){
                    log.debug("ECMPShortestPathGraph:Switches learned in "
                            + "Iteration{} from switch {}:",
                            itrIdx,
                            HexString.toHexString(sw.getDpid().value()));

                    HashMap<Switch, ArrayList<ArrayList<Dpid>>> swViaMap =
                                    switchVia.get(itrIdx);
                    for (Switch targetSw: swViaMap.keySet()){
                        log.debug("ECMPShortestPathGraph:****switch {} via:",
                                HexString.toHexString(targetSw.getDpid().value()));
                        int i=0;
                        for (ArrayList<Dpid> via:swViaMap.get(targetSw)){
                            log.debug("ECMPShortestPathGraph:******{}) {}",++i,via);
                        }
                    }
                }

            }
    	}
    	*/

        if ((topologyEvents.getAddedLinkDataEntries() != null) ||
                (topologyEvents.getRemovedLinkDataEntries() != null))
        {

            if (networkConverged) {
                populateEcmpRoutingRules();
            }
        }

    }

    /**
     * Populate routing rules walking through the ECMP shortest paths
     *
     */
    private void populateEcmpRoutingRules() {

        Iterable<Switch> switches= mutableTopology.getSwitches();
        for (Switch sw : switches) {
            ECMPShortestPathGraph ecmpSPG = new ECMPShortestPathGraph(sw);
            log.debug("ECMPShortestPathGraph is computed for switch {}",
                    HexString.toHexString(sw.getDpid().value()));

            HashMap<Integer, HashMap<Switch,ArrayList<ArrayList<Dpid>>>> switchVia =
                    ecmpSPG.getAllLearnedSwitchesAndVia();
            for (Integer itrIdx: switchVia.keySet()){
                log.debug("ECMPShortestPathGraph:Switches learned in "
                        + "Iteration{} from switch {}:",
                        itrIdx,
                        HexString.toHexString(sw.getDpid().value()));
                HashMap<Switch, ArrayList<ArrayList<Dpid>>> swViaMap =
                                switchVia.get(itrIdx);
                for (Switch targetSw: swViaMap.keySet()){
                    log.debug("ECMPShortestPathGraph:****switch {} via:",
                            HexString.toHexString(targetSw.getDpid().value()));
                    String destSw = sw.getDpid().toString();
                    List<String> fwdToSw = new ArrayList<String>();

                    int i=0;
                    for (ArrayList<Dpid> via:swViaMap.get(targetSw)){
                        log.debug("ECMPShortestPathGraph:******{}) {}",++i,via);
                        if (via.isEmpty()) {
                            fwdToSw.add(destSw);
                        }
                        else {
                            fwdToSw.add(via.get(0).toString());
                        }
                    }
                    setRoutingRule(targetSw, destSw, fwdToSw);
                }
            }
        }

        networkConverged = true;
    }

    /**
     *
     * Set routing rules in targetSw
     * {forward packets to fwdToSw switches in order to send packets to destSw}
     * - If the target switch is an edge router and final destnation switch is also
     *   an edge router, then set IP forwarding rules to subnets
     * - If only the target switch is an edge router, then set IP forwarding rule to
     *   the transit router loopback IP address
     * - If the target is a transit router, then just set the MPLS forwarding rule
     *
     * @param targetSw Switch to set the rules
     * @param destSw  Final destination switches
     * @param fwdToSw next hop switches
     */
    private void setRoutingRule(Switch targetSw, String destSw, List<String> fwdToSw) {


        if (fwdToSw.isEmpty()) {
            fwdToSw.add(destSw);
        }

        // if it is an edge router, then set IP table
        if (IsEdgeRouter(targetSw.getDpid().toString()) &&
                IsEdgeRouter(destSw)) {
            // We assume that there is at least one transit router b/w edge routers
            Switch destSwitch = mutableTopology.getSwitch(new Dpid(destSw));
            String subnets = destSwitch.getStringAttribute("subnets");
            try {
                JSONArray arry = new JSONArray(subnets);
                for (int i = 0; i < arry.length(); i++) {
                    String subnetIp = (String) arry.getJSONObject(i).get("subnetIp");
                    setIpTableRouter(targetSw, subnetIp, getMplsLabel(destSw)
                            ,fwdToSw);

                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            String routerIp = destSwitch.getStringAttribute("routerIp");
            setIpTableRouter(targetSw, routerIp, getMplsLabel(destSw), fwdToSw);
        }
        // if the target switch is the edge router, then set the IP rule to router IPs
        else if (IsEdgeRouter(targetSw.getDpid().toString())) {
            // We assume that there is at least one transit router b/w edge routers
            Switch destSwitch = mutableTopology.getSwitch(new Dpid(destSw));
            String routerIp = destSwitch.getStringAttribute("routerIp");
            setIpTableRouter(targetSw, routerIp, getMplsLabel(destSw), fwdToSw);
        }
        // if it is a transit router, then set rules in the MPLS table
        else {
            setMplsTable(targetSw, getMplsLabel(destSw), fwdToSw);
        }

    }

    /**
     * Check if the switch is the edge router or not
     * If any subnet information is defined in the config file, the we assume
     * it is an edge router
     *
     * @param dpid  Dpid of the switch to check
     * @return true if it is an edge router, otherwise false
     */
    private boolean IsEdgeRouter(String dpid) {

        for (Switch sw: mutableTopology.getSwitches()) {
            String dpidStr = sw.getDpid().toString();
            if (dpid.equals(dpidStr)) {
                String subnetInfo = sw.getStringAttribute("subnets");
                if (subnetInfo == null || subnetInfo.equals("[]")) {
                    return false;
                }
                else
                    return true;
            }
        }

        return false;
    }

    /**
     * Set IP forwarding rule
     *  - If the destination is the next hop, then do not push MPLS,
     *    just decrease the NW TTL
     *  - Otherwise, push MPLS label and set the MPLS ID
     *
     * @param sw  target switch to set rules
     * @param subnetIp Match IP address
     * @param mplsLabel MPLS label of final destination router
     * @param fwdToSws next hop routers
     */
    private void setIpTableRouter(Switch sw, String subnetIp, String mplsLabel,
            List<String> fwdToSws) {

        Ipv4PacketMatch ipMatch = new Ipv4PacketMatch(subnetIp);
        List<Action> actions = new ArrayList<>();

        // If destination SW is the same as the fwd SW, then do not push MPLS label
        if (fwdToSws.size() == 1) {
            String fwdToSw = fwdToSws.get(0);
            if (getMplsLabel(fwdToSw).equals(mplsLabel)) {
                DecNwTtlAction decTtlAction = new DecNwTtlAction(1);
                actions.add(decTtlAction);
            }
        }
        else {
            PushMplsAction pushMplsAction = new PushMplsAction();
            SetMplsIdAction setIdAction = new SetMplsIdAction(Integer.parseInt(mplsLabel));
            CopyTtlOutAction copyTtlOutAction = new CopyTtlOutAction();

            actions.add(pushMplsAction);
            actions.add(setIdAction);
            actions.add(copyTtlOutAction);
        }

        GroupAction groupAction = new GroupAction();

        for (String fwdSw : fwdToSws) {
            groupAction.addSwitch(new Dpid(fwdSw));
        }
        actions.add(groupAction);

        //MatchAction matchAction = new MatchAction(maIdGenerator.getNewId(),
        MatchAction matchAction = new MatchAction(new MatchActionId(0),
                new SwitchPort((long)0,(short)0), ipMatch, actions);

        MatchActionOperationEntry maEntry =
            new MatchActionOperationEntry(
                    net.onrc.onos.core.matchaction.MatchActionOperations.Operator.ADD,
                    matchAction);

        printMatchActionOperationEntry(sw, maEntry);

    }


    /**
     * Set MPLS forwarding rules to MPLS table
     *   - If the destination is the same as the next hop to forward packets
     *     then, pop the MPLS label according to PHP rule
     *   - Otherwise, just forward packets to next hops using Group action
     *
     * @param sw  Switch to set the rules
     * @param mplsLabel destination MPLS label
     * @param fwdSws  next hop switches
     */
    private void setMplsTable(Switch sw, String mplsLabel, List<String> fwdSws) {

        MplsMatch mplsMatch = new MplsMatch(Integer.parseInt(mplsLabel));

        List<Action> actions = new ArrayList<Action>();
        // Either when packet is forwarded to edge router or the dest is the router

        if (fwdSws.size() == 1) {
            String fwdSw = fwdSws.get(0);
            if (mplsLabel.equals(getMplsLabel(fwdSw))) {
                PopMplsAction popAction = new PopMplsAction();
                CopyTtlInAction copyTtlInAction = new CopyTtlInAction();

                actions.add(popAction);
                actions.add(copyTtlInAction);
            }
        }
        else {
            DecMplsTtlAction decMplsTtlAction = new DecMplsTtlAction(1);
            actions.add(decMplsTtlAction);
        }

        GroupAction groupAction = new GroupAction();
        for (String fwdSw: fwdSws)
            groupAction.addSwitch(new Dpid(fwdSw));
        actions.add(groupAction);

        MatchAction matchAction = new MatchAction(new MatchActionId(0),
                new SwitchPort((long)0,(short)0), mplsMatch, actions);

        MatchActionOperationEntry maEntry =
            new MatchActionOperationEntry(
                    net.onrc.onos.core.matchaction.MatchActionOperations.Operator.ADD,
                    matchAction);

        printMatchActionOperationEntry(sw, maEntry);

    }


    /**
     * Debugging function to print out the Match Action Entry
     *
     * @param maEntry
     */
    private void printMatchActionOperationEntry(Switch sw, MatchActionOperationEntry maEntry) {

        StringBuilder logStr = new StringBuilder("In switch " + sw.getDpid() + ", ");

        MatchAction ma = maEntry.getTarget();
        Match m = ma.getMatch();
        List<Action> actions = ma.getActions();

        if (m instanceof Ipv4PacketMatch) {
            logStr.append("If the IP matches with ");
            IPv4Net ip = ((Ipv4PacketMatch) m).getDestination();
            logStr.append(ip.toString());
            logStr.append(" then ");
        }
        else if (m instanceof MplsMatch) {
            logStr.append("If the MPLS label matches with ");
            int mplsLabel = ((MplsMatch) m).getMplsLabel();
            logStr.append(mplsLabel);
            logStr.append(" then ");
        }

        logStr.append(" do { ");
        for (Action action: actions) {
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
                NeighborSet dpids = ((GroupAction)action).getDpids();
                logStr.append(dpids.toString() + ",");

            }
            else if (action instanceof PopMplsAction) {
                logStr.append("Pop MPLS label, ");
            }
            else if (action instanceof PushMplsAction) {
                logStr.append("Push MPLS label, ");
            }
            else if (action instanceof SetMplsIdAction) {
                int id = ((SetMplsIdAction)action).getMplsId();
                logStr.append("Set MPLS ID as " + id + ", ");

            }
        }

        log.debug(logStr.toString());

    }

    /**
     * Get MPLS label reading the config file
     *
     * @param dipid  DPID of the switch
     * @return MPLS label for the switch
     */

    private String getMplsLabel(String dpid) {

        String mplsLabel = null;
        for (Switch sw: mutableTopology.getSwitches()) {
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
    public boolean netMatch(String addr, String addr1){ //addr is subnet address and addr1 is ip address. Function will return true, if addr1 is within addr(subnet)

        String[] parts = addr.split("/");
        String ip = parts[0];
        int prefix;

        if (parts.length < 2) {
            prefix = 0;
        } else {
            prefix = Integer.parseInt(parts[1]);
        }

        Inet4Address a =null;
        Inet4Address a1 =null;
        try {
            a = (Inet4Address) InetAddress.getByName(ip);
            a1 = (Inet4Address) InetAddress.getByName(addr1);
        } catch (UnknownHostException e){}

        byte[] b = a.getAddress();
        int ipInt = ((b[0] & 0xFF) << 24) |
                         ((b[1] & 0xFF) << 16) |
                         ((b[2] & 0xFF) << 8)  |
                         ((b[3] & 0xFF) << 0);

        byte[] b1 = a1.getAddress();
        int ipInt1 = ((b1[0] & 0xFF) << 24) |
                         ((b1[1] & 0xFF) << 16) |
                         ((b1[2] & 0xFF) << 8)  |
                         ((b1[3] & 0xFF) << 0);

        int mask = ~((1 << (32 - prefix)) - 1);

        if ((ipInt & mask) == (ipInt1 & mask)) {
            return true;
        }
        else {
            return false;
        }
    }

    public void addRouteToHost(Switch sw, int hostIpAddress, byte[] hostMacAddress) {
        ipHandler.addRouteToHost(sw, hostIpAddress, hostMacAddress);

    }
}
