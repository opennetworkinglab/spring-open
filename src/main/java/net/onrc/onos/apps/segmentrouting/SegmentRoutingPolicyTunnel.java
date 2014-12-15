package net.onrc.onos.apps.segmentrouting;

import java.util.List;

import net.onrc.onos.core.matchaction.match.PacketMatch;

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

}
