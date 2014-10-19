package net.onrc.onos.apps.segmentrouting.web;

import java.util.ArrayList;
import java.util.HashMap;

import net.onrc.onos.core.topology.Port;

/**
 * This class represent the the port info of the segmentRouter
 * for rest output.
 */

public class SegmentRouterPortInfo {
    //TODO set attributes to private and provide setter and getter.
    public String subnetIp= null;
    public Port  port = null;
    
    public SegmentRouterPortInfo(String ssubnets, Port pport){
        this.port = pport;
        this.subnetIp = ssubnets;
    }

    public void setInfo(String subnetIp, Port p) {
        this.port = p;
        this.subnetIp = subnetIp;
        
    }

}
