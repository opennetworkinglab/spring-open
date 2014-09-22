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

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.onrc.onos.api.packet.IPacketService;
import net.onrc.onos.core.flowprogrammer.IFlowPusherService;
import net.onrc.onos.core.intent.Path;
import net.onrc.onos.core.main.config.IConfigInfoService;
import net.onrc.onos.core.packet.ARP;
import net.onrc.onos.core.topology.ITopologyListener;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.LinkData;
import net.onrc.onos.core.topology.MutableTopology;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.topology.TopologyEvents;
import net.onrc.onos.core.util.Dpid;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SegmentRoutingManager implements IFloodlightModule, ITopologyListener {

    private static final Logger log = LoggerFactory
            .getLogger(SegmentRoutingManager.class);
    private ITopologyService topologyService;
    private MutableTopology mutableTopology;

    private List<ArpEntry> arpEntries;
    private ArpHandler arpHandler;
    private GenericIpHandler ipHandler;

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
        IcmpHandler icmpHandler = new IcmpHandler(context, this);
        ipHandler = new GenericIpHandler(context, this);
        arpEntries = new ArrayList<ArpEntry>();
        topologyService = context.getServiceImpl(ITopologyService.class);
        mutableTopology = topologyService.getTopology();
        topologyService.addListener(this, false);

    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        // TODO Auto-generated method stub

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
    	 */
    	if ((topologyEvents.getAddedLinkDataEntries() != null) ||
    		(topologyEvents.getRemovedLinkDataEntries() != null))
    	{
    		Iterable<Switch> switches= mutableTopology.getSwitches();
            for (Switch sw : switches) {
            	ECMPShortestPathGraph ecmpSPG = new ECMPShortestPathGraph(sw);
                log.debug("ECMPShortestPathGraph is computed for switch {}",
                		HexString.toHexString(sw.getDpid().value()));
                /*
                for (Switch dstSw: mutableTopology.getSwitches()){
                	if (sw.getDpid().equals(dstSw.getDpid())){
                		continue;
                	}
                	ArrayList<Path> paths = ecmpSPG.getECMPPaths(dstSw);
                    log.debug("ECMPShortestPathGraph:Paths from switch {} to switch {} is {}",
                            HexString.toHexString(sw.getDpid().value()),
                            HexString.toHexString(dstSw.getDpid().value()), paths);
                    //setSegmentRoutingRule(sw, paths);
                }
                */
                /*
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
                */
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
    }

    /**
     * Set segment routing rule to switches in the ECMP shortest path to the switch
     *
     * @param sw source switch
     * @param paths  ECMP path
     */
    private void setSegmentRoutingRule(Switch sw, ArrayList<Path> paths) {

        log.debug("Set routing info for {} to .. ", sw.getDpid());
        for (Path path: paths) {

            for (Object obj : path.toArray()) {
                LinkData link = (LinkData)obj;
                String destMplsLabel = getMplslabel(link.getDst().getDpid());
                String targetMplsLabel = getMplslabel(sw.getDpid());
                if (destMplsLabel != null && targetMplsLabel != null)
                    setTransitRouterRule(targetMplsLabel, destMplsLabel);
            }
        }
    }

    /**
     * Get MPLS label reading the config file
     *
     * @param dipid  DPID of the switch
     * @return MPLS label for the switch
     */

    private String getMplslabel(Dpid dpid) {

        String mplsLabel = null;
        for (Switch sw: mutableTopology.getSwitches()) {
            String dpidStr = sw.getStringAttribute("nodeDpid");
            if (dpid.toString().endsWith(dpidStr)) {
                mplsLabel = sw.getStringAttribute("nodeSid");
                break;
            }
        }

        return mplsLabel;
    }

    /**
     * Test function
     *
     *
     */
    private void setTransitRouterRule(String targetMplsLabel, String destMplsLabel) {

        log.debug("Match: MPLS label {}, action: forward to {}", targetMplsLabel, destMplsLabel);

    }

    /**
     * Test function
     *
     */
    private void setBorderRouterRule() {



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
