package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;

import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.Switch;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

public class SwitchSerializer extends SerializerBase<Switch> {

    public SwitchSerializer() {
        super(Switch.class);
    }

    @Override
    public void serialize(Switch sw, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("dpid", sw.getDpid().toString());
        jsonGenerator.writeStringField("state", "ACTIVE");
        jsonGenerator.writeArrayFieldStart("ports");
        for (Port port : sw.getPorts()) {
            jsonGenerator.writeObject(port);
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }

}
