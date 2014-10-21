package net.onrc.onos.apps.segmentrouting.web;

import java.util.List;

/**
 * This class contains tunnel info of ONOS Segement Routing App
 * Used for rest API
 */
public class SegmentRouterTunnelInfo {
    private String tunnelId;
    // private List<Dpid> nodes;
    private List<List<String>> labelStack;

    public SegmentRouterTunnelInfo(String tId, /*List<Dpid> dpids,*/
            List<List<String>> tunnelRoutes){
        this.tunnelId = tId;
        // this.nodes = dpids;
        this.labelStack = tunnelRoutes;
    }
    public String getTunnelId (){
        return this.tunnelId;
    }

    /*public List<Dpid> getnodes (){
        return this.nodes;
    }*/
    public List<List<String>> getLabelStack (){
        return this.labelStack;
    }
}
