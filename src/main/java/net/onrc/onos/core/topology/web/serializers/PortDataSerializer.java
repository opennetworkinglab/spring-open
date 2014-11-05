package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;
import java.util.Map.Entry;

import net.onrc.onos.core.topology.PortData;
import net.onrc.onos.core.topology.TopologyElement;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

/**
 * JSON serializer for PortData objects.
 */
public class PortDataSerializer extends SerializerBase<PortData> {
    /**
     * Default constructor.
     */
    public PortDataSerializer() {
        super(PortData.class);
    }

    /**
     * Serializes a PortData object in JSON.
     *
     * @param portData the PortData that is being converted to JSON
     * @param jsonGenerator generator to place the serialized JSON into
     * @param serializerProvider unused but required for method override
     * @throws IOException if the JSON serialization process fails
     */
    @Override
    public void serialize(PortData portData, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider)
        throws IOException {

        //
        // TODO: For now, the JSON format of the serialized output should
        // be same as the JSON format of the corresponding class Port.
        // In the future, we will use a single serializer.
        //

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(TopologyElement.TYPE, portData.getType());

        if (portData.getStringAttribute("state") != null &&
                portData.getStringAttribute("state").equals("ACTIVE")) {
            jsonGenerator.writeStringField("state", "ACTIVE");
        }
        else {
            jsonGenerator.writeStringField("state", "INACTIVE");
        }
        jsonGenerator.writeStringField("dpid", portData.getDpid().toString());
        jsonGenerator.writeNumberField("portNumber",
                                       portData.getPortNumber().value());
        jsonGenerator.writeStringField("desc",
                                       null /* port.getDescription() */);
        jsonGenerator.writeObjectFieldStart("stringAttributes");
        for (Entry<String, String> entry : portData.getAllStringAttributes().entrySet()) {
            jsonGenerator.writeStringField(entry.getKey(), entry.getValue());
        }
        jsonGenerator.writeEndObject();         // stringAttributes
        jsonGenerator.writeEndObject();
    }

    private boolean isEnabled(PortData p) {
        return true;
    }

}
