package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;

import net.onrc.onos.core.topology.PortEvent;
import net.onrc.onos.core.topology.TopologyElement;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

/**
 * Serializes a PortEvent object as JSON.
 */
public class PortEventSerializer extends SerializerBase<PortEvent> {

    /**
     * Constructs a PortEvent serializer.
     */
    public PortEventSerializer() {
        super(PortEvent.class);
    }

    @Override
    public void serialize(PortEvent portEvent, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider)
            throws IOException {
        //
        // TODO: For now, the JSON format of the serialized output should
        // be same as the JSON format of the corresponding class Port.
        // In the future, we will use a single serializer.
        //

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(TopologyElement.TYPE, portEvent.getType());
        jsonGenerator.writeStringField("state", "ACTIVE");
        jsonGenerator.writeStringField("dpid", portEvent.getDpid().toString());
        jsonGenerator.writeNumberField("portNumber",
                                       portEvent.getPortNumber().value());
        jsonGenerator.writeStringField("desc",
                                       null /* port.getDescription() */);
        jsonGenerator.writeEndObject();
    }
}
