package net.onrc.onos.apps.segmentrouting.web;

import java.util.List;
/**
 * This class contains tunnelset info of ONOS Segement Routing App
 * Used for rest API
 */
public class SegmentRouterTunnelsetInfo {
    private String tunnelsetId;
    private List<SegmentRouterTunnelInfo> constituentTunnels;
    private String policies;
    
    public SegmentRouterTunnelsetInfo(String tunnelsetId) {
    	this.tunnelsetId = tunnelsetId;
    	this.constituentTunnels = null;
    }
    
	public String getTunnelsetId() {
		return tunnelsetId;
	}
	public void setTunnelsetId(String tunnelsetId) {
		this.tunnelsetId = tunnelsetId;
	}
	public List<SegmentRouterTunnelInfo> getConstituentTunnels() {
		return constituentTunnels;
	}
	public void setConstituentTunnels(List<SegmentRouterTunnelInfo> constituentTunnels) {
		this.constituentTunnels = constituentTunnels;
	}

	public String getPolicies() {
		return policies;
	}

	public void setPolicies(String policies) {
		this.policies = policies;
	}
}
