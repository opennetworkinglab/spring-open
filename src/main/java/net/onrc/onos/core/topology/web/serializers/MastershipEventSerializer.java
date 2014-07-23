package net.onrc.onos.core.topology.web.serializers;

import net.onrc.onos.core.topology.MastershipEvent;
import net.onrc.onos.core.topology.TopologyElement;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

import java.io.IOException;

/**
 * JSON serializer for MastershipEvents.
 */
public class MastershipEventSerializer extends SerializerBase<MastershipEvent> {

    /**
     * Public constructor - just calls its super class constructor.
     */
    public MastershipEventSerializer() {
        super(MastershipEvent.class);
    }

    /**
     * Serializes a MastershipEvent object.
     *
     * @param mastershipEvent MastershipEvent to serialize
     * @param jsonGenerator generator to add the serialized object to
     * @param serializerProvider not used
     * @throws IOException if the JSON serialization fails
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
                                       mastershipEvent.getOnosInstanceId());
        jsonGenerator.writeStringField("role",
                                       mastershipEvent.getRole().name());
        jsonGenerator.writeEndObject();
    }
}
