package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;
import java.util.Map.Entry;

import net.onrc.onos.core.topology.MastershipEvent;
import net.onrc.onos.core.topology.TopologyElement;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

/**
 * JSON serializer for MastershipEvent objects.
 */
public class MastershipEventSerializer extends SerializerBase<MastershipEvent> {
    /**
     * Default constructor.
     */
    public MastershipEventSerializer() {
        super(MastershipEvent.class);
    }

    /**
     * Serializes a MastershipEvent object in JSON.
     *
     * @param mastershipEvent the MastershipEvent that is being converted to
     * JSON
     * @param jsonGenerator generator to place the serialized JSON into
     * @param serializerProvider unused but required for method override
     * @throws IOException if the JSON serialization process fails
     */
    @Override
    public void serialize(final MastershipEvent mastershipEvent,
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
        jsonGenerator.writeStringField(TopologyElement.TYPE, mastershipEvent.getType());
        jsonGenerator.writeStringField("dpid",
                                       mastershipEvent.getDpid().toString());
        jsonGenerator.writeStringField("onosInstanceId",
                                       mastershipEvent.getOnosInstanceId().toString());
        jsonGenerator.writeStringField("role",
                                       mastershipEvent.getRole().name());
        jsonGenerator.writeObjectFieldStart("stringAttributes");
        for (Entry<String, String> entry : mastershipEvent.getAllStringAttributes().entrySet()) {
            jsonGenerator.writeStringField(entry.getKey(), entry.getValue());
        }
        jsonGenerator.writeEndObject();         // stringAttributes
        jsonGenerator.writeEndObject();
    }
}
