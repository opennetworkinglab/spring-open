package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;

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
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("src-switch",
                link.getSrcSwitch().getDpid().toString());
        // XXX port number as unsigned?
        jsonGenerator.writeNumberField("src-port",
                link.getSrcPort().getNumber().value());
        jsonGenerator.writeStringField("dst-switch",
                link.getDstSwitch().getDpid().toString());
        jsonGenerator.writeNumberField("dst-port",
                link.getDstPort().getNumber().value());
        jsonGenerator.writeEndObject();
    }

}
