package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;
import java.util.Map.Entry;

import net.onrc.onos.core.topology.HostEvent;
import net.onrc.onos.core.topology.TopologyElement;
import net.onrc.onos.core.util.SwitchPort;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

/**
 * JSON serializer for HostEvent objects.
 */
public class HostEventSerializer extends SerializerBase<HostEvent> {
    /**
     * Default constructor.
     */
    public HostEventSerializer() {
        super(HostEvent.class);
    }

    /**
     * Serializes a HostEvent object in JSON.
     *
     * @param hostEvent the HostEvent that is being converted to JSON
     * @param jsonGenerator generator to place the serialized JSON into
     * @param serializerProvider unused but required for method override
     * @throws IOException if the JSON serialization process fails
     */
    @Override
    public void serialize(HostEvent hostEvent, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider)
        throws IOException {

        //
        // TODO: For now, the JSON format of the serialized output should
        // be same as the JSON format of the corresponding class Host.
        // In the future, we will use a single serializer.
        //

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(TopologyElement.TYPE, hostEvent.getType());
        jsonGenerator.writeStringField("mac", hostEvent.getMac().toString());
        jsonGenerator.writeFieldName("attachmentPoints");
        jsonGenerator.writeStartArray();
        for (SwitchPort switchPort : hostEvent.getAttachmentPoints()) {
            jsonGenerator.writeObject(switchPort);
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeObjectFieldStart("stringAttributes");
        for (Entry<String, String> entry : hostEvent.getAllStringAttributes().entrySet()) {
            jsonGenerator.writeStringField(entry.getKey(), entry.getValue());
        }
        jsonGenerator.writeEndObject();         // stringAttributes
        jsonGenerator.writeEndObject();
    }
}
