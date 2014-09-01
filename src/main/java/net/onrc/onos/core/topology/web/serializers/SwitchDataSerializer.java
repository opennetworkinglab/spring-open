package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;
import java.util.Map.Entry;

import net.onrc.onos.core.topology.SwitchData;
import net.onrc.onos.core.topology.TopologyElement;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

/**
 * JSON serializer for SwitchData objects.
 */
public class SwitchDataSerializer extends SerializerBase<SwitchData> {
    /**
     * Default constructor.
     */
    public SwitchDataSerializer() {
        super(SwitchData.class);
    }

    /**
     * Serializes a SwitchData object in JSON.
     *
     * @param switchData the SwitchData that is being converted to JSON
     * @param jsonGenerator generator to place the serialized JSON into
     * @param serializerProvider unused but required for method override
     * @throws IOException if the JSON serialization process fails
     */
    @Override
    public void serialize(SwitchData switchData, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider)
        throws IOException {

        //
        // TODO: For now, the JSON format of the serialized output should
        // be same as the JSON format of the corresponding class Switch.
        // In the future, we will use a single serializer.
        //

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(TopologyElement.TYPE, switchData.getType());
        jsonGenerator.writeStringField("dpid",
                                       switchData.getDpid().toString());
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
        jsonGenerator.writeObjectFieldStart("stringAttributes");
        for (Entry<String, String> entry : switchData.getAllStringAttributes().entrySet()) {
            jsonGenerator.writeStringField(entry.getKey(), entry.getValue());
        }
        jsonGenerator.writeEndObject();         // stringAttributes
        jsonGenerator.writeEndObject();
    }
}
