package net.onrc.onos.apps.segmentrouting;

import net.onrc.onos.core.matchaction.match.PacketMatch;

public class SegmentRoutingPolicy {

    /**
     * Enums for policy type
     *
     */
    public enum PolicyType {
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

    /**
     * Constructor
     *
     * @param srm Segment Routing Manager object
     * @param pid Policy ID
     * @param type Policy type
     * @param match PacketMatch for the policy
     * @param priority Priority
     */
    public SegmentRoutingPolicy(SegmentRoutingManager srm, String pid,
            PolicyType type, PacketMatch match, int priority) {
        this.srManager = srm;
        this.policyId = pid;
        this.match = match;
        this.priority = priority;
        this.type = type;
    }

    public SegmentRoutingPolicy(
            PolicyNotification policyNotication) {
        this.policyId = policyNotication.getPolicyId();
        this.match = policyNotication.getPacketMatch();
        this.priority = policyNotication.getPriority();
        this.type = PolicyType.valueOf(policyNotication.getPolicyType());
    }

    /**
     * Get the policy ID
     *
     * @return policy ID
     */
    public String getPolicyId() {
        return this.policyId;
    }

    /**
     * Get Match
     *
     * @return PacketMatch object
     */
    public PacketMatch getMatch() {
        return this.match;
    }

    /**
     * Get the priority of the policy
     *
     * @return priority
     */
    public int getPriority() {
        return this.priority;
    }

    /**
     * Get the policy type
     *
     * @return policy type
     */
    public PolicyType getType() {
        return this.type;
    }

    /**
     * Create a policy
     *
     * @return true if succeeds, false otherwise
     */
    public boolean createPolicy() {
        return false;
    }

    /**
     * Remove the policy
     *
     * @return true if succeeds, false otherwise
     */
    public boolean removePolicy() {
        return false;
    }

}
