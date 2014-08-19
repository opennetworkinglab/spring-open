package net.floodlightcontroller.core.web.serializers;

import java.io.IOException;

import net.floodlightcontroller.core.IOFSwitch;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortDesc;

public class IOFSwitchSerializer extends SerializerBase<IOFSwitch> {

    protected IOFSwitchSerializer() {
        super(IOFSwitch.class);
    }

    @Override
    public void serialize(IOFSwitch sw, JsonGenerator jGen, SerializerProvider sp)
            throws IOException, JsonGenerationException {

        jGen.writeStartObject();
        jGen.writeStringField("dpid", sw.getStringId());

        jGen.writeArrayFieldStart("ports");
        for (OFPortDesc port : sw.getPorts()) {
            jGen.writeStartObject();
            jGen.writeNumberField("number", port.getPortNo().getPortNumber());
            jGen.writeStringField("macAddress", port.getHwAddr().toString());
            jGen.writeStringField("name", port.getName());
            jGen.writeEndObject();
        }
        jGen.writeEndArray();

        jGen.writeStringField("version", sw.getOFVersion().toString());

        // Description from OFDescStatsReply
        OFDescStatsReply desc = sw.getSwitchDescription();
        jGen.writeObjectFieldStart("description");
        jGen.writeStringField("manufacturer", desc.getMfrDesc());
        jGen.writeStringField("hardware", desc.getHwDesc());
        jGen.writeStringField("switch", desc.getSwDesc());
        jGen.writeStringField("serialNum", desc.getSerialNum());
        jGen.writeStringField("datapath", desc.getDpDesc());
        jGen.writeEndObject();

        jGen.writeStringField("connectedSince", sw.getConnectedSince().toString());
        jGen.writeObjectField("role", sw.getRole());

        jGen.writeEndObject();
    }

}
