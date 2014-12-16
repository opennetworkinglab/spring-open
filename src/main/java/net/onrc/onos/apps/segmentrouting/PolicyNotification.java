package net.onrc.onos.apps.segmentrouting;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.onrc.onos.apps.segmentrouting.SegmentRoutingPolicy.PolicyType;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.topology.Link;
import net.onrc.onos.core.topology.LinkData;

public class PolicyNotification implements Serializable {

    private String policyId;
    private String policyType;
    private int priority;
    private PacketMatch match;
    private boolean isSetId;

    // Tunnel policy
    private String tunnelId;

    // Avoid policy
    private String srcDpid;
    private String dstDpid;
    private List<String> dpidListToAvoid;
    private List<LinkData> linkListToAvoid;
    private List<TunnelNotification> tunnels;

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected PolicyNotification() {

    }

    public PolicyNotification(String pid) {
        this.policyId = pid;
    }

    public PolicyNotification(SegmentRoutingPolicy srPolicy) {
        this.policyId = srPolicy.getPolicyId();
        this.policyType = srPolicy.getType().toString();
        this.priority = srPolicy.getPriority();
        this.match = srPolicy.getMatch();

        if (PolicyType.valueOf(policyType) == PolicyType.TUNNEL_FLOW) {
            this.tunnelId = ((SegmentRoutingPolicyTunnel)srPolicy).getTunnelId();
            this.isSetId = ((SegmentRoutingPolicyTunnel)srPolicy).isTunnelsetId();
        }
        else if (PolicyType.valueOf(policyType) == PolicyType.AVOID) {
            SegmentRoutingPolicyAvoid avoidPolicy =
                    (SegmentRoutingPolicyAvoid)srPolicy;
            this.srcDpid = avoidPolicy.getSource();
            this.dstDpid = avoidPolicy.getDestination();
            this.dpidListToAvoid = avoidPolicy.getDpidListToAvoid();
            this.linkListToAvoid = new ArrayList<LinkData>();
            for (Link link: avoidPolicy.getLinkListToAvoid()) {
                LinkData linkData = new LinkData(link);
                linkListToAvoid.add(linkData);
            }
            this.tunnels = new ArrayList<TunnelNotification>();
            for (SegmentRoutingTunnel tunnel: avoidPolicy.getTunnels()) {
                TunnelNotification tn = new TunnelNotification(tunnel);
                tunnels.add(tn);
            }
        }
    }



    public String getPolicyId() {
        return policyId;
    }

    public String getPolicyType() {
        return policyType;
    }

    public int getPriority() {
        return priority;
    }

    public PacketMatch getPacketMatch() {
        return match;
    }

    public String toString() {
        return "Policy-ID:" + policyId;
    }

    public void setTunnelId(String tid) {
        this.tunnelId = tid;
        this.isSetId = false;
    }

    public void setTunnelId(String tid, boolean isSetId) {
        this.tunnelId = tid;
        this.isSetId = isSetId;
    }

    public String getTunnelId() {
        return tunnelId;
    }

    public boolean isTunnelsetId() {
    	return this.isSetId;
    }

    public String getSource() {
        return this.srcDpid;
    }

    public String getDestination() {
        return this.dstDpid;
    }

    public List<String> getDpidListToAvoid() {
        return this.dpidListToAvoid;
    }

    public List<LinkData> getLinkListToAvoid() {
        return this.linkListToAvoid;
    }

    public List<TunnelNotification> getTunnels() {
        return this.tunnels;
    }

}
