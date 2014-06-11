package net.onrc.onos.core.util.serializers;

import net.onrc.onos.core.util.SwitchPort;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;
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
                                       switchPort.getDpid().toString());
        //
        // FIXME: The solution below to preresent the "short" port number
        // as an unsigned value is a hack. The fix should be elsewhere
        // (e.g., in class PortNumber itself).
        //
        jsonGenerator.writeNumberField("portNumber",
                                       switchPort.getPortNumber().value());

        jsonGenerator.writeEndObject();
    }
}
