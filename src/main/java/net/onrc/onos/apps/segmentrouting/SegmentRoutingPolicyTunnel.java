package net.onrc.onos.apps.segmentrouting;

import java.util.ArrayList;
import java.util.List;

import net.onrc.onos.core.matchaction.MatchAction;
import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.matchaction.MatchActionOperations.Operator;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.GroupAction;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.SwitchPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SegmentRoutingPolicyTunnel extends SegmentRoutingPolicy {

    private static final Logger log = LoggerFactory
            .getLogger(SegmentRoutingPolicyTunnel.class);

    private String tunnelId;

    public SegmentRoutingPolicyTunnel(SegmentRoutingManager srm, String pid,
            PolicyType type, PacketMatch match, int priority, String tid) {
        super(srm, pid, type, match, priority);
        this.tunnelId = tid;
    }

    public SegmentRoutingPolicyTunnel(SegmentRoutingManager srm, PolicyNotification policyNotication) {
        super(policyNotication);
        this.srManager = srm;
        this.tunnelId = policyNotication.getTunnelId();
    }

    @Override
    public boolean createPolicy() {

        SegmentRoutingTunnel tunnelInfo = srManager.getTunnelInfo(tunnelId);

        List<TunnelRouteInfo> routes = tunnelInfo.getRoutes();

        populateAclRule(routes);

        return true;
    }

    @Override
    public boolean removePolicy() {

        List<Action> actions = new ArrayList<>();
        int gropuId = 0; // dummy group ID
        GroupAction groupAction = new GroupAction();
        groupAction.setGroupId(gropuId);
        actions.add(groupAction);

        /*
        MatchAction matchAction = new MatchAction(
                srManager.getMatchActionId(),
                new SwitchPort((long) 0, (short) 0), match, priority,
                actions);
        MatchActionOperationEntry maEntry =
                new MatchActionOperationEntry(Operator.REMOVE, matchAction);
        */

        SegmentRoutingTunnel tunnel = srManager.getTunnelInfo(tunnelId);
        if (tunnel == null) {
            log.warn("Cannot find the tunnel {} for the policy {}", tunnelId,
                    policyId);
            return false;
        }
        List<TunnelRouteInfo> routes = tunnel.getRoutes();

        for (TunnelRouteInfo route : routes) {

            MatchAction matchAction = new MatchAction(
                    srManager.getMatchActionId(),
                    new SwitchPort((new Dpid(route.getSrcSwDpid())).value(), (long)0), match, priority,
                    actions);
            MatchActionOperationEntry maEntry =
                    new MatchActionOperationEntry(Operator.REMOVE, matchAction);

            srManager.executeMatchActionOpEntry(maEntry);

            /*
            IOF13Switch sw13 = srManager.getIOF13Switch(route.getSrcSwDpid());
            if (sw13 == null) {
                log.warn("Cannt find the switch {}", route.getSrcSwDpid());
                return false;
            }
            else {
                srManager.printMatchActionOperationEntry(sw13, maEntry);
                try {
                    sw13.pushFlow(maEntry);
                } catch (IOException e) {
                    e.printStackTrace();
                    log.debug("policy remove failed due to pushFlow() exception");
                    return false;
                }
            }
            */
        }

        return true;
    }

    /**
     * Get the Tunnel ID
     * @return tunnel ID
     */
    public String getTunnelId(){
        return this.tunnelId;
    }

}
