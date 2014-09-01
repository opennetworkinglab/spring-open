package net.onrc.onos.core.topology.web.serializers;

import java.io.IOException;

import net.onrc.onos.core.topology.HostData;
import net.onrc.onos.core.topology.LinkData;
import net.onrc.onos.core.topology.MastershipEvent;
import net.onrc.onos.core.topology.PortData;
import net.onrc.onos.core.topology.SwitchData;
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

        // Output the added switch mastership array
        jsonGenerator.writeArrayFieldStart("addedSwitchMasterships");
        for (final MastershipEvent mastershipEvent : topologyEvents.getAddedMastershipEvents()) {
            jsonGenerator.writeObject(mastershipEvent);
        }
        jsonGenerator.writeEndArray();

        // Output the removed switch mastership array
        jsonGenerator.writeArrayFieldStart("removedSwitchMasterships");
        for (final MastershipEvent mastershipEvent : topologyEvents.getRemovedMastershipEvents()) {
            jsonGenerator.writeObject(mastershipEvent);
        }
        jsonGenerator.writeEndArray();

        // Output the added switches array
        jsonGenerator.writeArrayFieldStart("addedSwitches");
        for (final SwitchData switchData : topologyEvents.getAddedSwitchDataEntries()) {
            jsonGenerator.writeObject(switchData);
        }
        jsonGenerator.writeEndArray();

        // Output the removed switches array
        jsonGenerator.writeArrayFieldStart("removedSwitches");
        for (final SwitchData switchData : topologyEvents.getRemovedSwitchDataEntries()) {
            jsonGenerator.writeObject(switchData);
        }
        jsonGenerator.writeEndArray();

        // Output the added ports array
        jsonGenerator.writeArrayFieldStart("addedPorts");
        for (final PortData portData : topologyEvents.getAddedPortDataEntries()) {
            jsonGenerator.writeObject(portData);
        }
        jsonGenerator.writeEndArray();

        // Output the removed ports array
        jsonGenerator.writeArrayFieldStart("removedPorts");
        for (final PortData portData : topologyEvents.getRemovedPortDataEntries()) {
            jsonGenerator.writeObject(portData);
        }
        jsonGenerator.writeEndArray();

        // Output the added links array
        jsonGenerator.writeArrayFieldStart("addedLinks");
        for (final LinkData linkData : topologyEvents.getAddedLinkDataEntries()) {
            jsonGenerator.writeObject(linkData);
        }
        jsonGenerator.writeEndArray();

        // Output the removed links array
        jsonGenerator.writeArrayFieldStart("removedLinks");
        for (final LinkData linkData : topologyEvents.getRemovedLinkDataEntries()) {
            jsonGenerator.writeObject(linkData);
        }
        jsonGenerator.writeEndArray();

        // Output the added hosts array
        jsonGenerator.writeArrayFieldStart("addedHosts");
        for (final HostData hostData : topologyEvents.getAddedHostDataEntries()) {
            jsonGenerator.writeObject(hostData);
        }
        jsonGenerator.writeEndArray();

        // Output the removed hosts array
        jsonGenerator.writeArrayFieldStart("removedHosts");
        for (final HostData hostData : topologyEvents.getRemovedHostDataEntries()) {
            jsonGenerator.writeObject(hostData);
        }
        jsonGenerator.writeEndArray();

        // All done
        jsonGenerator.writeEndObject();
    }
}
