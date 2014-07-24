package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;
import java.util.Map.Entry;

import net.onrc.onos.core.topology.Link;
import net.onrc.onos.core.topology.TopologyElement;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

/**
 * Serializes a Link object as JSON.
 */
public class LinkSerializer extends SerializerBase<Link> {

    /**
     * Constructs a Link serializer.
     */
    public LinkSerializer() {
        super(Link.class);
    }

    @Override
    public void serialize(Link link, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider)
            throws IOException {

        //
        // TODO: For now, the JSON format of the serialized output should
        // be same as the JSON format of the corresponding class LinkEvent.
        // In the future, we will use a single serializer.
        //
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(TopologyElement.TYPE, link.getType());
        jsonGenerator.writeObjectField("src", link.getSrcPort().getSwitchPort());
        jsonGenerator.writeObjectField("dst", link.getDstPort().getSwitchPort());
        jsonGenerator.writeObjectFieldStart("stringAttributes");
        for (Entry<String, String> entry : link.getAllStringAttributes().entrySet()) {
            jsonGenerator.writeStringField(entry.getKey(), entry.getValue());
        }
        jsonGenerator.writeEndObject(); // stringAttributes
        jsonGenerator.writeEndObject();
    }
}
