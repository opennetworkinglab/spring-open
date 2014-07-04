package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;

import net.onrc.onos.core.topology.SwitchEvent;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

public class SwitchEventSerializer extends SerializerBase<SwitchEvent> {

    public SwitchEventSerializer() {
        super(SwitchEvent.class);
    }

    @Override
    public void serialize(SwitchEvent switchEvent, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {
        //
        // TODO: For now, the JSON format of the serialized output should
        // be same as the JSON format of the corresponding class Switch.
        // In the future, we will use a single serializer.
        //

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("dpid",
                                       switchEvent.getDpid().toString());
        jsonGenerator.writeStringField("state", "ACTIVE");
        //
        // TODO: For now, we write empty "ports" array for consistency
        // with the corresponding Switch JSON serializer.
        //
        jsonGenerator.writeArrayFieldStart("ports");
        /*
        for (Port port : sw.getPorts()) {
            jsonGenerator.writeObject(port);
        }
        */
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }
}
