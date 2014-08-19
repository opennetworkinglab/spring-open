package net.onrc.onos.core.flowmanager.web;

import java.io.IOException;
import java.util.Collection;

import net.floodlightcontroller.restserver.CustomSerializerHelper;
import net.onrc.onos.api.flowmanager.Flow;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowManagerService;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * Handles REST requests for flow resources.
 */
public class FlowResource extends ServerResource {

    private final CustomSerializerHelper flowSerializers;

    /**
     * Constructs a FlowResource.
     * <p/>
     * A custom serializer for {@link FlowId} is automatically registered,
     * because FlowId can't be serialized by default.
     */
    public FlowResource() {
        flowSerializers = new CustomSerializerHelper();
        flowSerializers.addSerializer(FlowId.class, new SerializerBase<FlowId>(FlowId.class) {
            @Override
            public void serialize(FlowId flowId, JsonGenerator jgen,
                    SerializerProvider provider) throws IOException,
                    JsonGenerationException {
                jgen.writeString(flowId.toString());
            }
        });
    }

    /**
     * Handles REST requests for all flow resources.
     *
     * @return JSON-serializable Representation of all flow resources
     */
    @Get("json")
    public Representation retrieve() {
        FlowManagerService flowService =
                (FlowManagerService) getContext().getAttributes()
                    .get(FlowManagerService.class.getCanonicalName());

        Collection<Flow> flows = flowService.getFlows();

        return flowSerializers.applySerializers(
                (JacksonRepresentation<?>) toRepresentation(flows, null));
    }
}
