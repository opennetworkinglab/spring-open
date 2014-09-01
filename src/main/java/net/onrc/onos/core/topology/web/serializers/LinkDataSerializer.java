package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;
import java.util.Map.Entry;

import net.onrc.onos.core.topology.LinkData;
import net.onrc.onos.core.topology.TopologyElement;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

/**
 * JSON serializer for LinkData objects.
 */
public class LinkDataSerializer extends SerializerBase<LinkData> {
    /**
     * Default constructor.
     */
    public LinkDataSerializer() {
        super(LinkData.class);
    }

    /**
     * Serializes a LinkData object in JSON.
     *
     * @param linkData the LinkData that is being converted to JSON
     * @param jsonGenerator generator to place the serialized JSON into
     * @param serializerProvider unused but required for method override
     * @throws IOException if the JSON serialization process fails
     */
    @Override
    public void serialize(final LinkData linkData,
                          final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider)
        throws IOException {

        //
        // TODO: For now, the JSON format of the serialized output should
        // be same as the JSON format of the corresponding class Link.
        // In the future, we will use a single serializer.
        //

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(TopologyElement.TYPE, linkData.getType());
        jsonGenerator.writeObjectField("src", linkData.getSrc());
        jsonGenerator.writeObjectField("dst", linkData.getDst());
        jsonGenerator.writeObjectFieldStart("stringAttributes");
        for (Entry<String, String> entry : linkData.getAllStringAttributes().entrySet()) {
            jsonGenerator.writeStringField(entry.getKey(), entry.getValue());
        }
        jsonGenerator.writeEndObject();         // stringAttributes
        jsonGenerator.writeEndObject();
    }
}
