package net.onrc.onos.apps.segmentrouting.web;

import java.util.List;

import net.onrc.onos.core.topology.Port;

/**
 * This class represent the the port info of the segmentRouter
 * for rest output.
 */

public class SegmentRouterPortInfo {
    //TODO set attributes to private and provide setter and getter.
    private String subnetIp= null;
    private Port  port = null;
    private List<Integer> adjacency = null;
    
    public SegmentRouterPortInfo(String ssubnets, Port pport, List<Integer> adj){
        this.port = pport;
        this.subnetIp = ssubnets;
        this.adjacency = adj;
    }
    public String getSubnetIp(){
        return this.subnetIp;
    }
    public Port getPort(){
        return this.port;
    }
    public List<Integer> getAdjacency(){
        return this.adjacency;
    }

}
