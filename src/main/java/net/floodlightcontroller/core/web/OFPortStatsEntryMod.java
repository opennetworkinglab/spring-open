package net.floodlightcontroller.core.web;

import net.floodlightcontroller.core.web.serializers.OFPortStatsEntrySerializer;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.projectfloodlight.openflow.protocol.OFPortStatsEntry;

@JsonSerialize(using = OFPortStatsEntrySerializer.class)
public class OFPortStatsEntryMod {
	private OFPortStatsEntry portStatsEntry = null;

	public OFPortStatsEntryMod() {
	}

	public OFPortStatsEntryMod(OFPortStatsEntry portStatsEntry) {
		this.portStatsEntry = portStatsEntry;
	}

	public OFPortStatsEntry getPortStatsEntry() {
		return portStatsEntry;
	}
}

