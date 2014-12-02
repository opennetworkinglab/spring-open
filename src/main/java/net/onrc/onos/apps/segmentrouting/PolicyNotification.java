package net.onrc.onos.apps.segmentrouting;

import java.io.Serializable;

import net.onrc.onos.apps.segmentrouting.SegmentRoutingPolicy.PolicyType;
import net.onrc.onos.core.matchaction.match.PacketMatch;

public class PolicyNotification implements Serializable {

    private String policyId;
    private String policyType;
    private int priority;
    private PacketMatch match;
    private String tunnelId;  // XXX need to define PolicyTunnelNotification
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

        // XXX need to be processed in PolicyTunnelNotification
        if (PolicyType.valueOf(policyType) == PolicyType.TUNNEL_FLOW) {
            this.tunnelId = ((SegmentRoutingPolicyTunnel)srPolicy).getTunnelId();
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
    }

    public String getTunnelId() {
        return tunnelId;
    }

}
