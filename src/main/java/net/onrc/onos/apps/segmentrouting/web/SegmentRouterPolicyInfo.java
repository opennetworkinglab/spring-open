package net.onrc.onos.apps.segmentrouting.web;

import net.onrc.onos.core.matchaction.match.PacketMatch;

/**
 * Contains the policies info of the segment router that
 * are exposed, through REST API
 *
 */

public class SegmentRouterPolicyInfo {
    private String policyId;
    private int policyType;
    private int priority;
    private PacketMatch match;
    
    public SegmentRouterPolicyInfo(String Id,int type, int ppriority,PacketMatch flowEntries){
        this.policyId = Id;
        this.policyType = type;
        this.priority = ppriority;
        this.match = flowEntries;
    }
    public String getPolicyId(){
        return this.policyId;
    }
    public int getPolicyType(){
        return this.policyType;
    }
    public int getPriority(){
        return this.priority;
    }
    public PacketMatch getMatch(){
        return this.match;
    }
}
