package net.onrc.onos.apps.segmentrouting.web;

import java.util.List;
/**
 * This class contains tunnel info of ONOS Segement Routing App
 * Used for rest API
 */
public class SegmentRouterTunnelInfo {
    private String tunnelId;
    private List<List<String>> labelStack;
    private List<String> dpidGroup;
    
    public SegmentRouterTunnelInfo (String tId,
            List<List<String>> tunnelRoutes, List<String> dpidsWithGroup){
        this.tunnelId = tId;
        this.labelStack = tunnelRoutes;
        this.dpidGroup = dpidsWithGroup;
    }
    public String getTunnelId (){
        return this.tunnelId;
    }
    public List<List<String>> getLabelStack (){
        return this.labelStack;
    }
    public List<String> getDpidGroup (){
        return this.dpidGroup;
    }
}
