package net.floodlightcontroller.core.web;

import org.projectfloodlight.openflow.protocol.OFGroupDescStatsEntry;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import net.floodlightcontroller.core.web.serializers.OFGroupDescStatsEntryModSerializer;

@JsonSerialize(using = OFGroupDescStatsEntryModSerializer.class)

public class OFGroupDescStatsEntryMod {
    private OFGroupDescStatsEntry GroupDescStatsEntry = null;

    public OFGroupDescStatsEntryMod() {
    }

    public OFGroupDescStatsEntryMod(OFGroupDescStatsEntry GroupStatsEntry) {
        this.GroupDescStatsEntry = GroupStatsEntry;
    }

    public OFGroupDescStatsEntry getGroupDescStatsEntry() {
        return this.GroupDescStatsEntry;
    }

}
