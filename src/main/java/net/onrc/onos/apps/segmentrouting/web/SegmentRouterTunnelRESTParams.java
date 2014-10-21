package net.onrc.onos.apps.segmentrouting.web;

import java.util.List;

public class SegmentRouterTunnelRESTParams {
    private String tunnel_id;
    private List<Integer> label_path;

    public SegmentRouterTunnelRESTParams() {
        this.tunnel_id = null;
        this.label_path = null;
    }

    public void setTunnel_id(String tunnel_id) {
        this.tunnel_id = tunnel_id;
    }

    public String getTunnel_id() {
        return this.tunnel_id;
    }

    public void setLabel_path(List<Integer> label_path) {
        this.label_path = label_path;
    }

    public List<Integer> getLabel_path() {
        return this.label_path;
    }
}
