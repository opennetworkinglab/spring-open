package net.floodlightcontroller.core.web.serializers;

import java.io.IOException;

import net.floodlightcontroller.core.web.OFPortStatsEntryMod;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.projectfloodlight.openflow.protocol.OFPortStatsEntry;

public class OFPortStatsEntrySerializer extends SerializerBase<OFPortStatsEntryMod> {

    protected OFPortStatsEntrySerializer() {
        super(OFPortStatsEntryMod.class);
    }

    @Override
    public void serialize(OFPortStatsEntryMod portStatModEntry, JsonGenerator jGen,
    		SerializerProvider sp) throws IOException, JsonGenerationException {

    	OFPortStatsEntry portStatEntry = portStatModEntry.getPortStatsEntry();
        jGen.writeStartObject();
        jGen.writeNumberField("portNumber", portStatEntry.getPortNo().getPortNumber());
        jGen.writeNumberField("receivePackets", portStatEntry.getRxPackets().getValue());
        jGen.writeNumberField("transmitPackets", portStatEntry.getTxPackets().getValue());
        jGen.writeNumberField("receiveBytes", portStatEntry.getRxBytes().getValue());
        jGen.writeNumberField("transmitBytes", portStatEntry.getTxBytes().getValue());
        jGen.writeNumberField("receiveDropped", portStatEntry.getRxDropped().getValue());
        jGen.writeNumberField("transmitDropped", portStatEntry.getTxDropped().getValue());
        jGen.writeNumberField("receiveErrors", portStatEntry.getRxErrors().getValue());
        jGen.writeNumberField("transmitErrors", portStatEntry.getTxErrors().getValue());
        jGen.writeNumberField("receiveFrameErrors", portStatEntry.getRxFrameErr().getValue());
        jGen.writeNumberField("receiveOverrunErrors", portStatEntry.getRxOverErr().getValue());
        jGen.writeNumberField("receiveCRCErrors", portStatEntry.getRxCrcErr().getValue());
        jGen.writeNumberField("collisions", portStatEntry.getCollisions().getValue());

        jGen.writeEndObject();
    }

}
