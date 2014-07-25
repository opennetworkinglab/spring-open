package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;

import net.onrc.onos.core.topology.HostEvent;
import net.onrc.onos.core.topology.LinkEvent;
import net.onrc.onos.core.topology.PortEvent;
import net.onrc.onos.core.topology.SwitchEvent;
import net.onrc.onos.core.topology.TopologyEvents;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

/**
 * JSON serializer for TopologyEvents objects.
 */
public class TopologyEventsSerializer extends SerializerBase<TopologyEvents> {
    /**
     * Default constructor.
     */
    public TopologyEventsSerializer() {
        super(TopologyEvents.class);
    }

    /**
     * Serializes a TopologyEvents object in JSON.  The resulting JSON contains
     * the added and removed topology objects: switches, links and ports.
     *
     * @param topologyEvents the TopologyEvents that is being converted to JSON
     * @param jsonGenerator generator to place the serialized JSON into
     * @param serializerProvider unused but required for method override
     * @throws IOException if the JSON serialization process fails
     */
    @Override
    public void serialize(TopologyEvents topologyEvents,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider)
        throws IOException {

        // Start the object
        jsonGenerator.writeStartObject();

        // Output the added switches array
        jsonGenerator.writeArrayFieldStart("addedSwitches");
        for (final SwitchEvent switchEvent : topologyEvents.getAddedSwitchEvents()) {
            jsonGenerator.writeObject(switchEvent);
        }
        jsonGenerator.writeEndArray();

        // Output the removed switches array
        jsonGenerator.writeArrayFieldStart("removedSwitches");
        for (final SwitchEvent switchEvent : topologyEvents.getRemovedSwitchEvents()) {
            jsonGenerator.writeObject(switchEvent);
        }
        jsonGenerator.writeEndArray();

        // Output the added ports array
        jsonGenerator.writeArrayFieldStart("addedPorts");
        for (final PortEvent portEvent : topologyEvents.getAddedPortEvents()) {
            jsonGenerator.writeObject(portEvent);
        }
        jsonGenerator.writeEndArray();

        // Output the removed ports array
        jsonGenerator.writeArrayFieldStart("removedPorts");
        for (final PortEvent portEvent : topologyEvents.getRemovedPortEvents()) {
            jsonGenerator.writeObject(portEvent);
        }
        jsonGenerator.writeEndArray();

        // Output the added links array
        jsonGenerator.writeArrayFieldStart("addedLinks");
        for (final LinkEvent linkEvent : topologyEvents.getAddedLinkEvents()) {
            jsonGenerator.writeObject(linkEvent);
        }
        jsonGenerator.writeEndArray();

        // Output the removed links array
        jsonGenerator.writeArrayFieldStart("removedLinks");
        for (final LinkEvent linkEvent : topologyEvents.getRemovedLinkEvents()) {
            jsonGenerator.writeObject(linkEvent);
        }
        jsonGenerator.writeEndArray();

        // Output the added hosts array
        jsonGenerator.writeArrayFieldStart("addedHosts");
        for (final HostEvent hostEvent : topologyEvents.getAddedHostEvents()) {
            jsonGenerator.writeObject(hostEvent);
        }
        jsonGenerator.writeEndArray();

        // Output the removed hosts array
        jsonGenerator.writeArrayFieldStart("removedHosts");
        for (final HostEvent hostEvent : topologyEvents.getRemovedHostEvents()) {
            jsonGenerator.writeObject(hostEvent);
        }
        jsonGenerator.writeEndArray();

        // All done
        jsonGenerator.writeEndObject();
    }
}
