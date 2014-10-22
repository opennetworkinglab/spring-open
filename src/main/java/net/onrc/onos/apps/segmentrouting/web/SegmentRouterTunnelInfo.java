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
    private String policies;
    
    public SegmentRouterTunnelInfo (String tId,List<List<String>> tunnelRoutes,
            List<String> dpidsWithGroup,String policiesId){
        this.tunnelId = tId;
        this.labelStack = tunnelRoutes;
        this.dpidGroup = dpidsWithGroup;
        this.policies = policiesId;
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
    public String getPolicies (){
        return this.policies;
    }
}
