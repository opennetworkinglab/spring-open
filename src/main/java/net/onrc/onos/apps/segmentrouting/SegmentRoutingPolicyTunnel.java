package net.onrc.onos.apps.segmentrouting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.floodlightcontroller.core.IOF13Switch.GroupChain;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SegmentRoutingPolicyTunnel extends SegmentRoutingPolicy {

    private static final Logger log = LoggerFactory
            .getLogger(SegmentRoutingPolicyTunnel.class);

    private String tunnelId;
    private boolean isSetId;

    public SegmentRoutingPolicyTunnel(SegmentRoutingManager srm, String pid,
            PolicyType type, PacketMatch match, int priority, String tid) {
        super(srm, pid, type, match, priority);
        this.tunnelId = tid;
        this.setIsTunnelsetId(false);
    }

    public SegmentRoutingPolicyTunnel(SegmentRoutingManager srm, PolicyNotification policyNotication) {
        super(policyNotication);
        this.srManager = srm;
        this.tunnelId = policyNotication.getTunnelId();
        this.isSetId = policyNotication.isTunnelsetId();
    }

    @Override
    public boolean createPolicy() {

    	if (isSetId) {
    		SegmentRoutingTunnelset tunnelset = 
    				srManager.getTunnelsetInfo(tunnelId);
    		populateAclRuleToTunnelset(tunnelset);
    	}
    	else {
	        SegmentRoutingTunnel tunnelInfo = srManager.getTunnelInfo(tunnelId);
	
	        List<TunnelRouteInfo> routes = tunnelInfo.getRoutes();
	
	        populateAclRule(routes);
    	}

        return true;
    }

    @Override
    public boolean removePolicy() {

        SegmentRoutingTunnel tunnel = srManager.getTunnelInfo(tunnelId);
        if (tunnel == null) {
            log.warn("Cannot find the tunnel {} for the policy {}", tunnelId,
                    policyId);
            return false;
        }
        List<TunnelRouteInfo> routes = tunnel.getRoutes();
        removeAclRules(routes);

        return true;
    }

    /**
     * Get the Tunnel ID
     * @return tunnel ID
     */
    public String getTunnelId(){
        return this.tunnelId;
    }

	public boolean isTunnelsetId() {
		return isSetId;
	}

	public void setIsTunnelsetId(boolean isSetId) {
		this.isSetId = isSetId;
	}

    protected void populateAclRuleToTunnelset(SegmentRoutingTunnelset tunnelset) {
    	HashMap<String,List<GroupChain>> tunnelsetGroupChain = 
    			tunnelset.getTunnelsetGroupChain();
    	for (String targetSwDpid: tunnelsetGroupChain.keySet()) {
            List<Action> actions = new ArrayList<>();
            GroupAction groupAction = new GroupAction();
            groupAction.setGroupId(tunnelsetGroupChain.
            		get(targetSwDpid).get(0).getInnermostGroupId());
            actions.add(groupAction);
    		
            MatchAction matchAction = new MatchAction(
                    srManager.getMatchActionId(),
                    new SwitchPort((new Dpid(targetSwDpid)).value(), (long)0), match, priority,
                    actions);
            MatchActionOperationEntry maEntry =
                    new MatchActionOperationEntry(Operator.ADD, matchAction);

            srManager.executeMatchActionOpEntry(maEntry);
    	}
    }
}
