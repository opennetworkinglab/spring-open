package net.onrc.onos.apps.segmentrouting;

import java.io.Serializable;
import java.util.List;

public class TunnelNotification implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private String tunnelId;
    private List<Integer> labelIds;
    private List<TunnelRouteInfo> routes;

    public TunnelNotification() {

    }

    public TunnelNotification(SegmentRoutingTunnel srTunnel) {
        this.tunnelId = srTunnel.getTunnelId();
        this.labelIds = srTunnel.getLabelids();
        this.routes = srTunnel.getRoutes();
    }

    public String getTunnelId() {
        return tunnelId;
    }

    public List<Integer> getLabelIds() {
        return labelIds;
    }

    public List<TunnelRouteInfo> getRouteInfo() {
        return routes;
    }


}
