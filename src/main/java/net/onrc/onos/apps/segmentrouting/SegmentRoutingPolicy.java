package net.onrc.onos.apps.segmentrouting;

import net.onrc.onos.core.matchaction.match.PacketMatch;

public class SegmentRoutingPolicy {

    /**
     * Enums for policy type
     *
     */
    public enum PolicyType{
        TUNNEL_FLOW,
        LOADBALANCE,
        AVOID,
        DENY
    }

    protected SegmentRoutingManager srManager;
    protected String policyId;
    protected PacketMatch match;
    protected int priority;
    protected PolicyType type;

    public SegmentRoutingPolicy(SegmentRoutingManager srm, String pid,
            PolicyType type, PacketMatch match, int priority) {
        this.srManager = srm;
        this.policyId = pid;
        this.match = match;
        this.priority = priority;
        this.type = type;
    }

    public SegmentRoutingPolicy(String pid, PacketMatch match, int priority) {
        this.policyId = pid;
        this.match = match;
        this.priority = priority;
        this.type = PolicyType.TUNNEL_FLOW;
    }

    public String getPolicyId(){
        return this.policyId;
    }

    public PacketMatch getMatch(){
        return this.match;
    }

    public int getPriority(){
        return this.priority;
    }

    public PolicyType getType(){
        return this.type;
    }

    public boolean createPolicy() {
        return false;
    }

    public boolean removePolicy() {
        return false;
    }

}
