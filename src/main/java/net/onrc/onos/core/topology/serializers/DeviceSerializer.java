package net.onrc.onos.core.topology.serializers;

import java.io.IOException;

import net.onrc.onos.core.topology.Device;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.PortEvent.SwitchPort;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

public class DeviceSerializer extends SerializerBase<Device> {

    public DeviceSerializer() {
        super(Device.class);
    }

    @Override
    public void serialize(Device dev, JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider) throws IOException,
        JsonGenerationException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("mac", dev.getMacAddress().toLong());
        jsonGenerator.writeFieldName("attachmentPoints");
        jsonGenerator.writeStartArray();
        for (Port port : dev.getAttachmentPoints()) {
            SwitchPort sp = new SwitchPort(port.getDpid(), port.getNumber());
            jsonGenerator.writeObject(sp);
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }

}
