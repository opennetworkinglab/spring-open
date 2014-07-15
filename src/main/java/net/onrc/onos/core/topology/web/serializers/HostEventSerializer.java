package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;

import net.onrc.onos.core.topology.HostEvent;
import net.onrc.onos.core.topology.TopologyElement;
import net.onrc.onos.core.util.SwitchPort;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

public class HostEventSerializer extends SerializerBase<HostEvent> {

    public HostEventSerializer() {
        super(HostEvent.class);
    }

    @Override
    public void serialize(HostEvent hostEvent, JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider) throws IOException {

        //
        // TODO: For now, the JSON format of the serialized output should
        // be same as the JSON format of the corresponding class Host.
        // In the future, we will use a single serializer.
        //

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(TopologyElement.TYPE, hostEvent.getType());
        jsonGenerator.writeStringField("mac", hostEvent.getMac().toString());
        jsonGenerator.writeFieldName("attachmentPoints");
        jsonGenerator.writeStartArray();
        for (SwitchPort switchPort : hostEvent.getAttachmentPoints()) {
            jsonGenerator.writeObject(switchPort);
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }
}
