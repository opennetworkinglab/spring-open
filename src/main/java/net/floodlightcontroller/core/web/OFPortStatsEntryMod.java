package net.floodlightcontroller.core.web;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.web.serializers.OFPortStatsEntrySerializer;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.projectfloodlight.openflow.protocol.OFPortStatsEntry;

@JsonSerialize(using = OFPortStatsEntrySerializer.class)
public class OFPortStatsEntryMod {
    private OFPortStatsEntry portStatsEntry = null;
    private IOFSwitch sw= null;

    public OFPortStatsEntryMod() {
    }

    public OFPortStatsEntryMod(OFPortStatsEntry portStatsEntry, IOFSwitch switch1) {
        this.portStatsEntry = portStatsEntry;
        this.sw = switch1;
    }

    public OFPortStatsEntry getPortStatsEntry() {
        return portStatsEntry;
    }

    public IOFSwitch getSwitch(){
        return this.sw;
    }
}
