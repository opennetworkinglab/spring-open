package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;
import java.util.Map.Entry;

import net.onrc.onos.core.topology.Port;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

public class PortSerializer extends SerializerBase<Port> {

    public PortSerializer() {
        super(Port.class);
    }

    @Override
    public void serialize(Port port, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider)
            throws IOException {
        //
        // TODO: For now, the JSON format of the serialized output should
        // be same as the JSON format of the corresponding class PortEvent.
        // In the future, we will use a single serializer.
        //

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("state", "ACTIVE");
        jsonGenerator.writeStringField("dpid", port.getDpid().toString());
        //
        // FIXME: The solution below to preresent the "short" port number
        // as an unsigned value is a hack. The fix should be elsewhere
        // (e.g., in class PortNumber itself).
        //
        jsonGenerator.writeNumberField("portNumber",
                                       (0xffff & port.getNumber().value()));
        jsonGenerator.writeStringField("desc", port.getDescription());
        jsonGenerator.writeObjectFieldStart("stringAttributes");
        for (Entry<String, String> entry : port.getAllStringAttributes().entrySet()) {
            jsonGenerator.writeStringField(entry.getKey(), entry.getValue());
        }
        jsonGenerator.writeEndObject(); // stringAttributes
        jsonGenerator.writeEndObject();
    }
}
