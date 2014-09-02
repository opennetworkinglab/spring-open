package net.onrc.onos.core.matchaction.web;

import java.io.IOException;
import java.util.Set;

import net.floodlightcontroller.restserver.CustomSerializerHelper;
import net.onrc.onos.core.matchaction.MatchAction;
import net.onrc.onos.core.matchaction.MatchActionFloodlightService;
import net.onrc.onos.core.matchaction.MatchActionId;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * Handles REST requests for match-action resources.
 */
public class MatchActionResource extends ServerResource {

    CustomSerializerHelper matchActionSerializers;

    /**
     * Constructs a MatchActionResource.
     * <p/>
     * A custom serializer for {@link MatchActionId} is automatically
     * registered, because MatchActionId can't be serialized by default.
     */
    public MatchActionResource() {
        matchActionSerializers = new CustomSerializerHelper();
        matchActionSerializers.addSerializer(MatchActionId.class,
                new SerializerBase<MatchActionId>(MatchActionId.class) {
            @Override
            public void serialize(MatchActionId id, JsonGenerator jGen, SerializerProvider sp)
                    throws IOException, JsonProcessingException {
                jGen.writeString(id.toString());
            }
        });
    }

    /**
     * Handles REST requests for all match-action resources.
     *
     * @return JSON-serializable Representation of all match-action resources
     */
    @Get("json")
    public Representation retrieve() {
        MatchActionFloodlightService matchActionService =
                (MatchActionFloodlightService) getContext().getAttributes()
                    .get(MatchActionFloodlightService.class.getCanonicalName());

        Set<MatchAction> matchActions = matchActionService.getMatchActions();

        return matchActionSerializers.applySerializers(
                (JacksonRepresentation<?>) toRepresentation(matchActions, null));
    }
}
