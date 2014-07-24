package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;

import net.onrc.onos.core.topology.Host;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.TopologyElement;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

/**
 * Serializes Host objects as JSON.
 */
public class HostSerializer extends SerializerBase<Host> {

    /**
     * Constructs a Host serializer.
     */
    public HostSerializer() {
        super(Host.class);
    }

    @Override
    public void serialize(Host host, JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider) throws IOException {

        //
        // TODO: For now, the JSON format of the serialized output should
        // be same as the JSON format of the corresponding class HostEvent.
        // In the future, we will use a single serializer.
        //

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(TopologyElement.TYPE, host.getType());
        jsonGenerator.writeStringField("mac", host.getMacAddress().toString());
        jsonGenerator.writeFieldName("attachmentPoints");
        jsonGenerator.writeStartArray();
        for (Port port : host.getAttachmentPoints()) {
            jsonGenerator.writeObject(port.getSwitchPort());
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }
}
