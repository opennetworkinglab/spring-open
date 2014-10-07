package net.floodlightcontroller.core.web;

import net.floodlightcontroller.core.web.serializers.OFFlowStatsEntryModSerializer;

import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;

import org.codehaus.jackson.map.annotate.JsonSerialize;
@JsonSerialize(using = OFFlowStatsEntryModSerializer.class)
public class OFFlowStatsEntryMod {
    private OFFlowStatsEntry FlowStatsEntry = null;

    public  OFFlowStatsEntryMod() {
    }

    public  OFFlowStatsEntryMod(OFFlowStatsEntry FlowStatsEntry) {
        this.FlowStatsEntry = FlowStatsEntry;
    }

    public OFFlowStatsEntry getFlowStatsEntry() {
        return this.FlowStatsEntry;
    }

}
