package net.onrc.onos.apps.segmentrouting;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingManager.removeTunnelMessages;
import net.onrc.onos.core.topology.Link;
import net.onrc.onos.core.util.IPv4Net;

/**
 * The API exported by the main Segment Routing class. This is the interface between the
 * REST handlers and the vs module.
 */
public interface ISegmentRoutingService extends IFloodlightService {

    /**
     * Create a tunnel for policy routing.
     *
     * @param tunnelId ID for the tunnel
     * @param labelIds Node label IDs for the tunnel
     *
     * @return "true/false" depending tunnel creation status
     */
    public boolean createTunnel(String tunnelId, List<Integer> labelIds);

    /**
     * Remove a Segment Routing tunnel given a tunnel Id.
     *
     * @param tunnelId ID for the tunnel
     *
     * @return "true/false" depending tunnel deletion status
     */
    public removeTunnelMessages removeTunnel(String tunnelId);

    /**
     * Create a policy for policy based segment routing
     *
     * @param pid Unique Policy Identifier
     * @param srcIP Source IP address in CIDR format
     * @param dstIP Destination IP address in CIDR format
     * @param ipProto IP protocol type
     * @param srcPort Source L4 port
     * @param dstPort Destination L4 port
     * @param priority Priority of the policy
     * @param tid SR Tunnel Id to be associated with this policy
     *
     * @return "true/false" depending tunnel creation status
     */
    public boolean createPolicy(String pid, MACAddress srcMac, MACAddress dstMac,
            Short etherType, IPv4Net srcIp, IPv4Net dstIp, Byte ipProto,
            Short srcPort, Short dstPort, int priority, String tid);


    /**
     * Create a policy with the type of avoid
     *
     * @param pid policy ID
     * @param srcMac source MAC address of the match
     * @param dstMac destination MAC address of the match
     * @param etherType Ethernet Type of the match
     * @param srcIp source IP address of the match
     * @param dstIp destination IP address of the match
     * @param ipProto IP protocol type of the match
     * @param srcPort source port of the match
     * @param dstPort destination port of the match
     * @param priority priority of the policy
     * @param srcNode source node of the avoid policy
     * @param dstNode destination node of the avoid policy
     * @param nodesToAvoid nodes to avoid
     * @param linksToAvoid links to avoid
     * @return true if successful, false otherwise
     */
    public boolean createPolicy(String pid, MACAddress srcMac, MACAddress dstMac,
            Short etherType, IPv4Net srcIp, IPv4Net dstIp, Byte ipProto,
            Short srcPort, Short dstPort, int priority, int srcNode, int dstNode,
            List<Integer> nodesToAvoid, List<Link> linksToAvoid);

    /**
     * Remove a policy given policy Id
     *
     * @param pid Unique Policy Identifier
     *
     * @return "true/false" depending tunnel deletion status
     */
    public boolean removePolicy(String pid);
    /**
     * Return the collection of TunnelInfo which contains
     * info about tunnels
     * @return Collection<TunnelInfo>
     */

    public Collection<SegmentRoutingTunnel> getTunnelTable();
    /**
     * Get the first group ID for the tunnel for specific source router
     * If Segment Stitching was required to create the tunnel, there are
     * mutiple source routers.
     *
     * @param tunnelId ID for the tunnel
     * @param dpid source router DPID
     * @return the first group ID of the tunnel and -1 if sw with specifed
     * dpid is not found
     */
    public int getTunnelGroupId(String tunnelId, String dpid);
    /**
     * return list of all the policies currently there in Segment Router
     * @return Collection<PolicyInfo>
     */
    public Collection<SegmentRoutingPolicy> getPoclicyTable();
    /**
     * Returns the Adjacency Info for the node
     *
     * @param nodeSid Node SID
     * @return HashMap of <AdjacencyID, list of ports>
     */
    public HashMap<Integer, List<Integer>> getAdjacencyInfo(int nodeSid);
    /**
     * Returns the Adjacency IDs for the node
     *
     * @param nodeSid Node SID
     * @return Collection of Adjacency ID
     */
    public Collection<Integer> getAdjacencyIds(int nodeSid);
    /**
     * Get MPLS label reading the config file
     *
     * @param dipid DPID of the switch
     * @return MPLS label for the switch
     */
    public String getMplsLabel(String dpid);
}
