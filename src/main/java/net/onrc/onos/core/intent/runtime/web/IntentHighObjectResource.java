package net.onrc.onos.core.intent.runtime.web;

import java.util.Arrays;
import java.util.List;

import net.onrc.onos.api.rest.RestError;
import net.onrc.onos.api.rest.RestErrorCode;
import net.onrc.onos.core.intent.Intent;
import net.onrc.onos.core.intent.IntentMap;
import net.onrc.onos.core.intent.runtime.IPathCalcRuntimeService;

import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * A class to access a single high-level intent.
 */
public class IntentHighObjectResource extends ServerResource {
    // TODO need to assign proper application id.
    private static final String APPLICATION_ID = "1";

    /**
     * Gets a single high-level intent.
     *
     * @return a Representation of a single high-level intent. If the
     * intent is not found, return a Representation of a RestError indicating
     * the problem.
     */
    @Get("json")
    public Representation retrieve() {
        IPathCalcRuntimeService pathRuntime =
            (IPathCalcRuntimeService) getContext().getAttributes()
                .get(IPathCalcRuntimeService.class.getCanonicalName());

        String intentId = (String) getRequestAttributes().get("intent-id");
        Representation result;

        //
        // Get a single high-level Intent: use the Intent ID to find it
        //
        IntentMap intentMap = pathRuntime.getHighLevelIntents();
        String applnIntentId = APPLICATION_ID + ":" + intentId;
        Intent intent = intentMap.getIntent(applnIntentId);
        if (intent != null) {
            result = toRepresentation(intent, null);
        } else {
            setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            final RestError notFound =
                RestError.createRestError(RestErrorCode.INTENT_NOT_FOUND,
                                          applnIntentId);
            result = toRepresentation(notFound, null);
        }

        return result;
    }

    /**
     * Deletes a single high-level intent.
     *
     * @return a null Representation.
     */
    @Delete("json")
    public Representation remove() {
        IPathCalcRuntimeService pathRuntime =
            (IPathCalcRuntimeService) getContext().getAttributes()
                .get(IPathCalcRuntimeService.class.getCanonicalName());

        String intentId = (String) getRequestAttributes().get("intent-id");
        List<String> intentIds = Arrays.asList(intentId);

        if (pathRuntime.removeApplicationIntents(APPLICATION_ID, intentIds)) {
            setStatus(Status.SUCCESS_NO_CONTENT);
        } else {
            setStatus(Status.SERVER_ERROR_INTERNAL);
        }

        return null;
    }
}
