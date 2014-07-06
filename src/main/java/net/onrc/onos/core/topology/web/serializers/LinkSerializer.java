package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;
import java.util.Map.Entry;

import net.onrc.onos.core.topology.Link;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

public class LinkSerializer extends SerializerBase<Link> {

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
        jsonGenerator.writeObjectField("src", link.getSrcPort().asSwitchPort());
        jsonGenerator.writeObjectField("dst", link.getDstPort().asSwitchPort());
        jsonGenerator.writeObjectFieldStart("stringAttributes");
        for (Entry<String, String> entry : link.getAllStringAttributes().entrySet()) {
            jsonGenerator.writeStringField(entry.getKey(), entry.getValue());
        }
        jsonGenerator.writeEndObject(); // stringAttributes
        jsonGenerator.writeEndObject();
    }
}
