package net.onrc.onos.apps.segmentrouting.web;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

public class SegmentRouterTunnelsetRESTParams {
    private String tunnelset_id;
    @JsonProperty("tunnel_params")
    //@JsonDeserialize(contentUsing = SegmentRouterTunnelRESTParams.class)
    private List<SegmentRouterTunnelRESTParams> tunnel_params = 
    				new ArrayList<SegmentRouterTunnelRESTParams>();

    public SegmentRouterTunnelsetRESTParams() {
        //this.tunnelset_id = null;
        //this.tunnel_params = null;
    }

    public void setTunnelset_id(String tunnelset_id) {
        this.tunnelset_id = tunnelset_id;
    }

    public String getTunnelset_id() {
        return this.tunnelset_id;
    }

    public void setTunnelParams(List<SegmentRouterTunnelRESTParams> tunnel_params) {
        this.tunnel_params = tunnel_params;
    }

    public List<SegmentRouterTunnelRESTParams> getTunnelParams() {
        return this.tunnel_params;
    }
}
