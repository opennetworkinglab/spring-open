package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;
import java.util.Map.Entry;

import net.onrc.onos.core.topology.LinkEvent;
import net.onrc.onos.core.topology.TopologyElement;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

/**
 * JSON serializer for LinkEvents objects.
 */
public class LinkEventSerializer extends SerializerBase<LinkEvent> {
    /**
     * Default constructor.
     */
    public LinkEventSerializer() {
        super(LinkEvent.class);
    }

    /**
     * Serializes a LinkEvent object in JSON.
     *
     * @param linkEvent the LinkEvent that is being converted to JSON
     * @param jsonGenerator generator to place the serialized JSON into
     * @param serializerProvider unused but required for method override
     * @throws IOException if the JSON serialization process fails
     */
    @Override
    public void serialize(final LinkEvent linkEvent,
                          final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider)
        throws IOException {

        //
        // TODO: For now, the JSON format of the serialized output should
        // be same as the JSON format of the corresponding class Link.
        // In the future, we will use a single serializer.
        //

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(TopologyElement.TYPE, linkEvent.getType());
        jsonGenerator.writeObjectField("src", linkEvent.getSrc());
        jsonGenerator.writeObjectField("dst", linkEvent.getDst());
        jsonGenerator.writeObjectFieldStart("stringAttributes");
        for (Entry<String, String> entry : linkEvent.getAllStringAttributes().entrySet()) {
            jsonGenerator.writeStringField(entry.getKey(), entry.getValue());
        }
        jsonGenerator.writeEndObject();         // stringAttributes
        jsonGenerator.writeEndObject();
    }
}
