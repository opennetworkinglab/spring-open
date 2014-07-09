package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;

import net.onrc.onos.core.topology.PortEvent;
import net.onrc.onos.core.topology.TopologyElement;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

public class PortEventSerializer extends SerializerBase<PortEvent> {

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
        //
        // FIXME: The solution below to preresent the "short" port number
        // as an unsigned value is a hack. The fix should be elsewhere
        // (e.g., in class PortNumber itself).
        //
        jsonGenerator.writeNumberField("portNumber",
                                       (0xffff & portEvent.getPortNumber().value()));
        jsonGenerator.writeStringField("desc",
                                       null /* port.getDescription() */);
        jsonGenerator.writeEndObject();
    }
}
