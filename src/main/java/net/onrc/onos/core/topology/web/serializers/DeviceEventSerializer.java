package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;

import net.onrc.onos.core.topology.DeviceEvent;
import net.onrc.onos.core.util.SwitchPort;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

public class DeviceEventSerializer extends SerializerBase<DeviceEvent> {

    public DeviceEventSerializer() {
        super(DeviceEvent.class);
    }

    @Override
    public void serialize(DeviceEvent deviceEvent, JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider) throws IOException {

        //
        // TODO: For now, the JSON format of the serialized output should
        // be same as the JSON format of the corresponding class Device.
        // In the future, we will use a single serializer.
        //

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("mac", deviceEvent.getMac().toString());
        jsonGenerator.writeFieldName("attachmentPoints");
        jsonGenerator.writeStartArray();
        for (SwitchPort switchPort : deviceEvent.getAttachmentPoints()) {
            jsonGenerator.writeObject(switchPort);
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }
}
