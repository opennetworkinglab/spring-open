package net.onrc.onos.core.topology.web.serializers;

import net.onrc.onos.core.topology.PortEvent.SwitchPort;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.openflow.util.HexString;

import java.io.IOException;

/**
 * JSON Serializer for SwitchPorts.
 */
public class SwitchPortSerializer extends SerializerBase<SwitchPort> {

    /**
     * Public constructor - just calls its super class constructor.
     */
    public SwitchPortSerializer() {
        super(SwitchPort.class);
    }

    /**
     * Serializes a SwitchPort object.
     *
     * @param switchPort object to serialize
     * @param jsonGenerator generator to add the serialized object to
     * @param serializerProvider not used
     * @throws IOException if the serialization fails
     */
    @Override
    public void serialize(final SwitchPort switchPort,
                          final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider)
            throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeStringField("dpid",
                                       HexString.toHexString(switchPort.getDpid()));
        jsonGenerator.writeStringField("portNumber",
                                       Long.toString(switchPort.getNumber()));

        jsonGenerator.writeEndObject();
    }

}
