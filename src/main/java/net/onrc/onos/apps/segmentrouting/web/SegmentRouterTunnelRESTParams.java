package net.onrc.onos.apps.segmentrouting.web;

import java.util.List;

public class SegmentRouterTunnelRESTParams {
    private String tunnel_id;
    private List<String> tunnel_path;

    public SegmentRouterTunnelRESTParams() {
        this.tunnel_id = null;
        this.tunnel_path = null;
    }

    public void setTunnel_id(String tunnel_id) {
        this.tunnel_id = tunnel_id;
    }

    public String getTunnel_id() {
        return this.tunnel_id;
    }

    public void setTunnel_path(List<String> tunnel_path) {
        this.tunnel_path = tunnel_path;
    }

    public List<String> getTunnel_path() {
        return this.tunnel_path;
    }
}
