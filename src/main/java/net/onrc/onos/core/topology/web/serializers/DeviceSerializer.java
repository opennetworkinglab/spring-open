package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;

import net.onrc.onos.core.topology.Device;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.TopologyElement;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

public class DeviceSerializer extends SerializerBase<Device> {

    public DeviceSerializer() {
        super(Device.class);
    }

    @Override
    public void serialize(Device dev, JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider) throws IOException {

        //
        // TODO: For now, the JSON format of the serialized output should
        // be same as the JSON format of the corresponding class DeviceEvent.
        // In the future, we will use a single serializer.
        //

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(TopologyElement.TYPE, dev.getType());
        jsonGenerator.writeStringField("mac", dev.getMacAddress().toString());
        jsonGenerator.writeFieldName("attachmentPoints");
        jsonGenerator.writeStartArray();
        for (Port port : dev.getAttachmentPoints()) {
            jsonGenerator.writeObject(port.asSwitchPort());
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }
}
