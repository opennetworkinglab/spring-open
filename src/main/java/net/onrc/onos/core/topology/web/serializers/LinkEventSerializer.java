package net.onrc.onos.core.topology.web.serializers;

import net.onrc.onos.core.topology.LinkEvent;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

import java.io.IOException;

/**
 * JSON serializer for LinkEvents.
 */
public class LinkEventSerializer extends SerializerBase<LinkEvent> {

    /**
     * Public constructor - just calls its super class constructor.
     */
    public LinkEventSerializer() {
        super(LinkEvent.class);
    }

    /**
     * Serializes a LinkEvent object.
     *
     * @param linkEvent LinkEvent to serialize
     * @param jsonGenerator generator to add the serialized object to
     * @param serializerProvider not used
     * @throws IOException if the JSON serialization fails
     */
    @Override
    public void serialize(final LinkEvent linkEvent,
                          final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider)
            throws IOException {

        jsonGenerator.writeStartObject();

        jsonGenerator.writeObjectField("src", linkEvent.getSrc());
        jsonGenerator.writeObjectField("dst", linkEvent.getDst());

        jsonGenerator.writeEndObject();
    }
}
