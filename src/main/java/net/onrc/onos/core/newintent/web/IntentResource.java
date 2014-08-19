package net.onrc.onos.core.newintent.web;

import java.io.IOException;
import java.util.Set;

import net.floodlightcontroller.restserver.CustomSerializerHelper;
import net.onrc.onos.api.newintent.Intent;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.api.newintent.IntentService;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * Handles REST requests for intent resources.
 */
public class IntentResource extends ServerResource {

    CustomSerializerHelper intentSerializers;

    /**
     * Constructs an IntentResource.
     * <p/>
     * A custom serializer for {@link IntentId} is automatically registered,
     * because IntentId can't be serialized by default.
     */
    public IntentResource() {
        intentSerializers = new CustomSerializerHelper();
        intentSerializers.addSerializer(IntentId.class,
            new SerializerBase<IntentId>(IntentId.class) {
                @Override
                public void serialize(IntentId id, JsonGenerator jGen,
                        SerializerProvider sp) throws IOException,
                        JsonProcessingException {
                    jGen.writeString(id.toString());
                }
            });
    }

    /**
     * Handles REST requests for all intent resources.
     *
     * @return JSON-serializable Representation of all intent resources
     */
    @Get("json")
    public Representation retrieve() {
        IntentService intentService =
                (IntentService) getContext().getAttributes()
                    .get(IntentService.class.getCanonicalName());

        Set<Intent> intents = intentService.getIntents();

        return intentSerializers.applySerializers(
                (JacksonRepresentation<?>) toRepresentation(intents, null));
    }
}
