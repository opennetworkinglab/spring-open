package net.floodlightcontroller.core.web;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import org.projectfloodlight.openflow.protocol.OFGroupStatsEntry;
import net.floodlightcontroller.core.web.serializers.OFGroupStatsEntryModSerializer;

@JsonSerialize(using = OFGroupStatsEntryModSerializer.class)
public class OFGroupStatsEntryMod {
    private OFGroupStatsEntry GroupStatsEntry = null;

    public  OFGroupStatsEntryMod() {
    }

    public  OFGroupStatsEntryMod(OFGroupStatsEntry GroupStatsEntry) {
        this.GroupStatsEntry = GroupStatsEntry;
    }

    public OFGroupStatsEntry getGroupStatsEntry() {
        return this.GroupStatsEntry;
    }
}
