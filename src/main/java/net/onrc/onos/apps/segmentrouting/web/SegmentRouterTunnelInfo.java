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
    private List<Integer> tunnelPath;
    private String policies;
    
    public SegmentRouterTunnelInfo (String tId,List<List<String>> tunnelRoutes,
            List<String> dpidsWithGroup,List<Integer> path, String policiesId){
        this.tunnelId = tId;
        this.labelStack = tunnelRoutes;
        this.dpidGroup = dpidsWithGroup;
        this.tunnelPath = path;
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
    public List<Integer> getTunnelPath (){
        return this.tunnelPath;
    }
    public String getPolicies (){
        return this.policies;
    }
}
