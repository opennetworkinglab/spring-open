package net.onrc.onos.apps.segmentrouting.web;

import java.util.HashMap;
import java.util.List;

/**
 * Contain the adjacency port info, which is exposed to REST
 *
 */

public class SegmentRouterAdjacencyInfo {
    private Integer adjacencySid =null;
    private List<Integer>  ports = null;
    
    public SegmentRouterAdjacencyInfo(Integer adj, List<Integer> pList){
        this.adjacencySid = adj;
        this.ports = pList;
    }
    
    public Integer getAdjacencySid(){
        return this.adjacencySid;
    }
    public List<Integer> getPorts(){
        return this.ports;
    }
}
