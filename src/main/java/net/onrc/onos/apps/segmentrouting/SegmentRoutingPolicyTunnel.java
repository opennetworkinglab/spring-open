package net.onrc.onos.apps.segmentrouting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.floodlightcontroller.core.IOF13Switch;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingTunnel.TunnelRouteInfo;
import net.onrc.onos.core.matchaction.MatchAction;
import net.onrc.onos.core.matchaction.MatchActionId;
import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.matchaction.MatchActionOperations.Operator;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.DecNwTtlAction;
import net.onrc.onos.core.matchaction.action.GroupAction;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.util.SwitchPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.minlog.Log;

public class SegmentRoutingPolicyTunnel extends SegmentRoutingPolicy {

    private static final Logger log = LoggerFactory
            .getLogger(SegmentRoutingPolicyTunnel.class);

    private String tunnelId;

    public SegmentRoutingPolicyTunnel(SegmentRoutingManager srm, String pid,
            PolicyType type, PacketMatch match, int priority, String tid) {
        super(srm, pid, type, match, priority);
        this.tunnelId = tid;
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean createPolicy() {

        SegmentRoutingTunnel tunnelInfo = srManager.getTunnelInfo(tunnelId);

        List<TunnelRouteInfo> routes = tunnelInfo.getRoutes();

        for (TunnelRouteInfo route : routes) {
            List<Action> actions = new ArrayList<>();

            // Check PHP was done by stitching
            // If no MPLS label is added, then NW TTL needs to be decremented
            if (route.getRoute().isEmpty()) {
                DecNwTtlAction decNwTtlAction = new DecNwTtlAction(1);
                actions.add(decNwTtlAction);
            }

            GroupAction groupAction = new GroupAction();
            groupAction.setGroupId(route.getGroupId());
            actions.add(groupAction);

            MatchAction matchAction = new MatchAction(new MatchActionId(
                    srManager.getNextMatchActionID()),
                    new SwitchPort((long) 0, (short) 0), match, priority,
                    actions);
            MatchActionOperationEntry maEntry =
                    new MatchActionOperationEntry(Operator.ADD, matchAction);

            IOF13Switch sw13 = srManager.getIOF13Switch(route.getSrcSwDpid());

            if (sw13 != null) {
                srManager.printMatchActionOperationEntry(sw13, maEntry);
                try {
                    sw13.pushFlow(maEntry);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            else {
                Log.warn("Cannot find the target switch {}", route.getSrcSwDpid());
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean removePolicy() {

        List<Action> actions = new ArrayList<>();
        int gropuId = 0; // dummy group ID
        GroupAction groupAction = new GroupAction();
        groupAction.setGroupId(gropuId);
        actions.add(groupAction);

        MatchAction matchAction = new MatchAction(new MatchActionId(
                srManager.getNextMatchActionID()),
                new SwitchPort((long) 0, (short) 0), match, priority,
                actions);
        MatchActionOperationEntry maEntry =
                new MatchActionOperationEntry(Operator.REMOVE, matchAction);

        SegmentRoutingTunnel tunnel = srManager.getTunnelInfo(tunnelId);
        if (tunnel == null) {
            log.warn("Cannot find the tunnel {} for the policy {}", tunnelId,
                    policyId);
            return false;
        }
        List<TunnelRouteInfo> routes = tunnel.getRoutes();

        for (TunnelRouteInfo route : routes) {
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
        }

        return true;
    }


    public String getTunnelId(){
        return this.tunnelId;
    }

}
