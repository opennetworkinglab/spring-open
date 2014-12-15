package net.onrc.onos.apps.segmentrouting;

import java.util.ArrayList;
import java.util.List;

import net.onrc.onos.core.matchaction.MatchAction;
import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.matchaction.MatchActionOperations.Operator;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.DecNwTtlAction;
import net.onrc.onos.core.matchaction.action.GroupAction;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.action.SetDAAction;
import net.onrc.onos.core.matchaction.action.SetSAAction;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.projectfloodlight.openflow.types.MacAddress;

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

    protected void populateAclRule(List<TunnelRouteInfo> routes) {
        for (TunnelRouteInfo route : routes) {
            List<Action> actions = new ArrayList<>();

            // Check PHP was done by stitching
            // If no MPLS label is added, then NW TTL needs to be decremented

            if (route.getRoute().isEmpty()) {

                DecNwTtlAction decNwTtlAction = new DecNwTtlAction(1);
                actions.add(decNwTtlAction);

                Switch srcSw = srManager.getSwitch(route.getSrcSwDpid());
                Switch destSwitch = srManager.getSwitch(route.getFwdSwDpid().get(0).toString());
                MacAddress srcMac =
                        MacAddress.of(srcSw.getStringAttribute("routerMac"));
                MacAddress dstMac =
                        MacAddress.of(destSwitch.getStringAttribute("routerMac"));
                SetSAAction setSAAction = new SetSAAction(srcMac);
                SetDAAction setDAAction = new SetDAAction(dstMac);
                actions.add(setSAAction);
                actions.add(setDAAction);

                List<String> fwdSwDpids = new ArrayList<String>();
                for (Dpid dpid: route.getFwdSwDpid()) {
                    fwdSwDpids.add(dpid.toString());
                }
                PortNumber port = srManager.pickOnePort(srcSw, fwdSwDpids);
                OutputAction outputAction = new OutputAction(port);
                actions.add(outputAction);
            }
            else {
                GroupAction groupAction = new GroupAction();
                groupAction.setGroupId(route.getGroupId());
                actions.add(groupAction);
            }

            MatchAction matchAction = new MatchAction(
                    srManager.getMatchActionId(),
                    new SwitchPort((new Dpid(route.getSrcSwDpid())).value(), (long)0), match, priority,
                    actions);
            MatchActionOperationEntry maEntry =
                    new MatchActionOperationEntry(Operator.ADD, matchAction);

            srManager.executeMatchActionOpEntry(maEntry);

        }
    }

    /**
     * Remove rules from the ACL table
     *
     * @param routes ACL rule information
     */
    protected void removeAclRules(List<TunnelRouteInfo> routes) {
        List<Action> actions = new ArrayList<>();
        int gropuId = 0; // dummy group ID
        GroupAction groupAction = new GroupAction();
        groupAction.setGroupId(gropuId);
        actions.add(groupAction);

        for (TunnelRouteInfo route : routes) {

            MatchAction matchAction = new MatchAction(
                    srManager.getMatchActionId(),
                    new SwitchPort((new Dpid(route.getSrcSwDpid())).value(), (long)0), match, priority,
                    actions);
            MatchActionOperationEntry maEntry =
                    new MatchActionOperationEntry(Operator.REMOVE, matchAction);

            srManager.executeMatchActionOpEntry(maEntry);
        }
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

    /**
     * Update the policy rules if necessary according to the topology changes
     */
    public void updatePolicy() {

    }

}
