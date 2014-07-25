package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;

import net.onrc.onos.core.topology.Host;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.TopologyElement;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

/**
 * JSON serializer for Host objects.
 */
public class HostSerializer extends SerializerBase<Host> {
    /**
     * Default constructor.
     */
    public HostSerializer() {
        super(Host.class);
    }

    /**
     * Serializes a Host object in JSON.
     *
     * @param host the Host that is being converted to JSON
     * @param jsonGenerator generator to place the serialized JSON into
     * @param serializerProvider unused but required for method override
     * @throws IOException if the JSON serialization process fails
     */
    @Override
    public void serialize(Host host, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider)
        throws IOException {

        //
        // TODO: For now, the JSON format of the serialized output should
        // be same as the JSON format of the corresponding class HostEvent.
        // In the future, we will use a single serializer.
        //

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(TopologyElement.TYPE, host.getType());
        jsonGenerator.writeStringField("mac", host.getMacAddress().toString());
        jsonGenerator.writeFieldName("attachmentPoints");
        jsonGenerator.writeStartArray();
        for (Port port : host.getAttachmentPoints()) {
            jsonGenerator.writeObject(port.getSwitchPort());
        }
        jsonGenerator.writeEndArray();
        //
        // NOTE: Class Host itself doesn't have stringAttributes.
        // Adding empty object for now for consistency with HostEventSerializer
        //
        jsonGenerator.writeObjectFieldStart("stringAttributes");
        /*
        for (Entry<String, String> entry : host.getAllStringAttributes().entrySet()) {
            jsonGenerator.writeStringField(entry.getKey(), entry.getValue());
        }
        */
        jsonGenerator.writeEndObject();         // stringAttributes
        jsonGenerator.writeEndObject();
    }
}
