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
    private List<String> remove_tunnel_params = new ArrayList<String>();

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
        return (this.tunnel_params.size() > 0)?this.tunnel_params:null;
    }

	public List<String> getRemove_tunnel_params() {
		return (remove_tunnel_params.size()>0)?remove_tunnel_params:null;
	}

	public void setRemove_tunnel_params(List<String> remove_tunnel_params) {
		this.remove_tunnel_params = remove_tunnel_params;
	}
}
