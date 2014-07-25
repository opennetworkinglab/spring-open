package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;
import java.util.Map.Entry;

import net.onrc.onos.core.topology.PortEvent;
import net.onrc.onos.core.topology.TopologyElement;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

/**
 * JSON serializer for PortEvent objects.
 */
public class PortEventSerializer extends SerializerBase<PortEvent> {
    /**
     * Default constructor.
     */
    public PortEventSerializer() {
        super(PortEvent.class);
    }

    /**
     * Serializes a PortEvent object in JSON.
     *
     * @param portEvent the PortEvent that is being converted to JSON
     * @param jsonGenerator generator to place the serialized JSON into
     * @param serializerProvider unused but required for method override
     * @throws IOException if the JSON serialization process fails
     */
    @Override
    public void serialize(PortEvent portEvent, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider)
        throws IOException {

        //
        // TODO: For now, the JSON format of the serialized output should
        // be same as the JSON format of the corresponding class Port.
        // In the future, we will use a single serializer.
        //

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(TopologyElement.TYPE, portEvent.getType());
        jsonGenerator.writeStringField("state", "ACTIVE");
        jsonGenerator.writeStringField("dpid", portEvent.getDpid().toString());
        jsonGenerator.writeNumberField("portNumber",
                                       portEvent.getPortNumber().value());
        jsonGenerator.writeStringField("desc",
                                       null /* port.getDescription() */);
        jsonGenerator.writeObjectFieldStart("stringAttributes");
        for (Entry<String, String> entry : portEvent.getAllStringAttributes().entrySet()) {
            jsonGenerator.writeStringField(entry.getKey(), entry.getValue());
        }
        jsonGenerator.writeEndObject();         // stringAttributes
        jsonGenerator.writeEndObject();
    }
}
