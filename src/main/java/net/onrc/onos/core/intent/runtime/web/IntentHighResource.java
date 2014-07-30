package net.onrc.onos.core.intent.runtime.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.onrc.onos.api.intent.ApplicationIntent;
import net.onrc.onos.api.rest.RestError;
import net.onrc.onos.api.rest.RestErrorCode;
import net.onrc.onos.core.intent.Intent;
import net.onrc.onos.core.intent.IntentMap;
import net.onrc.onos.core.intent.runtime.IPathCalcRuntimeService;

import org.codehaus.jackson.map.ObjectMapper;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to access the high-level intents.
 */
public class IntentHighResource extends ServerResource {
    private static final Logger log = LoggerFactory.getLogger(IntentHighResource.class);
    // TODO need to assign proper application id.
    private static final String APPLICATION_ID = "1";

    /**
     * Gets all high-level intents.
     *
     * @return a Representation for a collection with all high-level intents.
     */
    @Get("json")
    public Representation retrieve() throws IOException {
        IPathCalcRuntimeService pathRuntime =
            (IPathCalcRuntimeService) getContext().getAttributes()
                .get(IPathCalcRuntimeService.class.getCanonicalName());

        IntentMap intentMap = pathRuntime.getHighLevelIntents();
        Collection<Intent> intents = intentMap.getAllIntents();

        return toRepresentation(intents, null);
    }

    /**
     * Adds a collection of high-level intents.
     *
     * @param jsonIntent JSON representation of the intents to add.
     * @return a Representation of a collection containing the intents that
     * were added.
     */
    @Post("json")
    public Representation store(String jsonIntent) {
        IPathCalcRuntimeService pathRuntime =
            (IPathCalcRuntimeService) getContext().getAttributes()
                .get(IPathCalcRuntimeService.class.getCanonicalName());

        //
        // Extract the Application Intents
        //
        ObjectMapper mapper = new ObjectMapper();
        ApplicationIntent[] addOperations = null;
        try {
            if (jsonIntent != null) {
                addOperations = mapper.readValue(jsonIntent, ApplicationIntent[].class);
            }
        } catch (IOException ex) {
            log.error("Exception occurred parsing inbound JSON", ex);
        }
        if (addOperations == null) {
            setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            final RestError error =
                    RestError.createRestError(RestErrorCode.INTENT_INVALID);
            return toRepresentation(error, null);
        }

        //
        // Add the intents
        //
        if (pathRuntime.addApplicationIntents(APPLICATION_ID,
                                              Arrays.asList(addOperations))) {
            setStatus(Status.SUCCESS_CREATED);
        } else {
            setStatus(Status.SERVER_ERROR_INTERNAL);
        }

        return toRepresentation(addOperations, null);
    }

    /**
     * Deletes all high-level intents.
     *
     * @return a null Representation.
     */
    @Delete("json")
    public Representation remove() {
        IPathCalcRuntimeService pathRuntime =
            (IPathCalcRuntimeService) getContext().getAttributes()
                .get(IPathCalcRuntimeService.class.getCanonicalName());

        //
        // Get the optional query values: comma-separated list of Intent IDs
        //
        String intentIdValue = getQueryValue("intent_id");
        boolean success;

        //
        // Delete the intents
        //
        if (intentIdValue != null) {
            // Delete a collection of intents, specified by Intent IDs
            List<String> intentIds = Arrays.asList(intentIdValue.split(","));
            success = pathRuntime.removeApplicationIntents(APPLICATION_ID,
                                                           intentIds);
        } else {
            // Delete all intents
            success = pathRuntime.removeAllApplicationIntents(APPLICATION_ID);
        }

        if (success) {
            setStatus(Status.SUCCESS_NO_CONTENT);
        } else {
            setStatus(Status.SERVER_ERROR_INTERNAL);
        }

        return null;
    }
}
