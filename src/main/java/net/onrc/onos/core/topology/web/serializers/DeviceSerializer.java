package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;

import net.onrc.onos.core.topology.Device;
import net.onrc.onos.core.topology.Port;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.openflow.util.HexString;

public class DeviceSerializer extends SerializerBase<Device> {

    public DeviceSerializer() {
        super(Device.class);
    }

    @Override
    public void serialize(Device dev, JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider) throws IOException,
        JsonGenerationException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("mac", dev.getMacAddress().toString());
        jsonGenerator.writeFieldName("attachmentPoints");
        jsonGenerator.writeStartArray();
        for (Port port : dev.getAttachmentPoints()) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("dpid", HexString.toHexString(port.getDpid()));
            jsonGenerator.writeNumberField("port", port.getNumber());
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }
}
