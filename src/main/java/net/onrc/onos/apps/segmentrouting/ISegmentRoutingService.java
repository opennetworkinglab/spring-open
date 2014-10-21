package net.onrc.onos.apps.segmentrouting;

import java.util.Collection;
import java.util.List;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingManager.TunnelInfo;
import net.onrc.onos.core.util.IPv4Net;

/**
 * The API exported by the main SDN-IP class. This is the interface between the
 * REST handlers and the SDN-IP module.
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
    public boolean removeTunnel(String tunnelId);

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

    public Collection<TunnelInfo> getTunnelTable();
}
