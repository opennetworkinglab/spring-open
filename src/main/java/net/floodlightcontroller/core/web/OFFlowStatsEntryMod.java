package net.floodlightcontroller.core.web;

import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

//@JsonSerialize(using = OFFlowStatsEntryModSerializer.class)
public class OFFlowStatsEntryMod {
    private OFFlowStatsEntry FlowStatsEntry = null;

    public  OFFlowStatsEntryMod() {
    }

    public  OFFlowStatsEntryMod(OFFlowStatsEntry FlowStatsEntry) {
        this.FlowStatsEntry = FlowStatsEntry;
    }

    public OFFlowStatsEntry getFlowStatsEntr() {
        return this.FlowStatsEntry;
    }

}
