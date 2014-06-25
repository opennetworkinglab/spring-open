package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;

import net.onrc.onos.core.topology.Device;
import net.onrc.onos.core.topology.Port;

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
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("mac", dev.getMacAddress().toString());
        jsonGenerator.writeFieldName("attachmentPoints");
        jsonGenerator.writeStartArray();
        for (Port port : dev.getAttachmentPoints()) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("dpid", port.getDpid().toString());
            // XXX Should port number be treated as unsigned?
            jsonGenerator.writeNumberField("port", port.getNumber().value());
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }
}
