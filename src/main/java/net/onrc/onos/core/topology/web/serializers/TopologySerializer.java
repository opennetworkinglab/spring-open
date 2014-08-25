package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;

import net.onrc.onos.core.topology.Host;
import net.onrc.onos.core.topology.Link;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.topology.MutableTopology;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

/**
 * JSON serializer for Topology objects.
 */
public class TopologySerializer extends SerializerBase<MutableTopology> {
    /**
     * Default constructor.
     */
    public TopologySerializer() {
        super(MutableTopology.class);
    }

    /**
     * Serializes a Topology object in JSON.  The resulting JSON contains the
     * switches, links and ports provided by the Topology object.
     *
     * @param mutableTopology the Topology that is being converted to JSON
     * @param jsonGenerator generator to place the serialized JSON into
     * @param serializerProvider unused but required for method override
     * @throws IOException if the JSON serialization process fails
     */
    @Override
    public void serialize(MutableTopology mutableTopology,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider)
        throws IOException {

        // Start the object
        jsonGenerator.writeStartObject();

        // Output the switches array
        jsonGenerator.writeArrayFieldStart("switches");
        for (final Switch swtch : mutableTopology.getSwitches()) {
            jsonGenerator.writeObject(swtch);
        }
        jsonGenerator.writeEndArray();

        // Output the links array
        jsonGenerator.writeArrayFieldStart("links");
        for (final Link link : mutableTopology.getLinks()) {
            jsonGenerator.writeObject(link);
        }
        jsonGenerator.writeEndArray();

        // Output the hosts array
        jsonGenerator.writeArrayFieldStart("hosts");
        for (final Host host : mutableTopology.getHosts()) {
            jsonGenerator.writeObject(host);
        }
        jsonGenerator.writeEndArray();

        // All done
        jsonGenerator.writeEndObject();
    }
}
