package net.floodlightcontroller.core.web;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.web.serializers.OFFlowStatsEntryModSerializer;

import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.codehaus.jackson.map.annotate.JsonSerialize;
@JsonSerialize(using = OFFlowStatsEntryModSerializer.class)
public class OFFlowStatsEntryMod {
    private OFFlowStatsEntry FlowStatsEntry = null;
    /*
     * need switch to get table name so the we could sepcify table name (ip,acl,mpls)
     * instead of table number
     */
    private IOFSwitch sw;

    public  OFFlowStatsEntryMod() {
    }

    public  OFFlowStatsEntryMod(OFFlowStatsEntry FlowStatsEntry, IOFSwitch sw1) {
        this.FlowStatsEntry = FlowStatsEntry;
        this.sw = sw1;
    }

    public OFFlowStatsEntry getFlowStatsEntry() {
        return this.FlowStatsEntry;
    }
    public IOFSwitch getSwitch(){
        return this.sw;
    }

}
