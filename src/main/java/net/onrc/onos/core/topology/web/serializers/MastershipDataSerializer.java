package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;
import java.util.Map.Entry;

import net.onrc.onos.core.topology.MastershipData;
import net.onrc.onos.core.topology.TopologyElement;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

/**
 * JSON serializer for MastershipData objects.
 */
public class MastershipDataSerializer extends SerializerBase<MastershipData> {
    /**
     * Default constructor.
     */
    public MastershipDataSerializer() {
        super(MastershipData.class);
    }

    /**
     * Serializes a MastershipData object in JSON.
     *
     * @param mastershipData the MastershipData that is being converted to
     * JSON
     * @param jsonGenerator generator to place the serialized JSON into
     * @param serializerProvider unused but required for method override
     * @throws IOException if the JSON serialization process fails
     */
    @Override
    public void serialize(final MastershipData mastershipData,
                          final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider)
        throws IOException {

        //
        // TODO: For now, the JSON format of the serialized output should
        // be same as the JSON format of the corresponding class Mastership
        // (if such class exists).
        // In the future, we will use a single serializer.
        //

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(TopologyElement.TYPE, mastershipData.getType());
        jsonGenerator.writeStringField("dpid",
                                       mastershipData.getDpid().toString());
        jsonGenerator.writeStringField("onosInstanceId",
                                       mastershipData.getOnosInstanceId().toString());
        jsonGenerator.writeStringField("role",
                                       mastershipData.getRole().name());
        jsonGenerator.writeObjectFieldStart("stringAttributes");
        for (Entry<String, String> entry : mastershipData.getAllStringAttributes().entrySet()) {
            jsonGenerator.writeStringField(entry.getKey(), entry.getValue());
        }
        jsonGenerator.writeEndObject();         // stringAttributes
        jsonGenerator.writeEndObject();
    }
}
