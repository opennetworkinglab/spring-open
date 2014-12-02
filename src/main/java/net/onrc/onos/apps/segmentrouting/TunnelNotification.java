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

    /**
     * default constructor
     */
    public TunnelNotification() {

    }

    /**
     * Constructor
     *
     * @param srTunnel tunnel information
     */
    public TunnelNotification(SegmentRoutingTunnel srTunnel) {
        this.tunnelId = srTunnel.getTunnelId();
        this.labelIds = srTunnel.getLabelids();
        this.routes = srTunnel.getRoutes();
    }

    /**
     * Get the tunnel ID
     *
     * @return tunnel ID
     */
    public String getTunnelId() {
        return tunnelId;
    }

    /**
     * Get the label stack for the tunnel
     *
     * @return List of label IDs
     */
    public List<Integer> getLabelIds() {
        return labelIds;
    }

    /**
     * Get the all sub tunnel information
     *
     * @return List of TunnelRouteInfo objects
     */
    public List<TunnelRouteInfo> getRouteInfo() {
        return routes;
    }


}
