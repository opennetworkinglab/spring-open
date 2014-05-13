package net.onrc.onos.core.intent.runtime.web;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import net.onrc.onos.core.intent.Intent;
import net.onrc.onos.core.intent.IntentMap;
import net.onrc.onos.core.intent.IntentOperation;
import net.onrc.onos.core.intent.IntentOperationList;
import net.onrc.onos.core.intent.runtime.IPathCalcRuntimeService;

import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to access a single high-level intent.
 */
public class IntentHighObjectResource extends ServerResource {
    private static final Logger log = LoggerFactory.getLogger(IntentHighObjectResource.class);
    // TODO need to assign proper application id.
    private static final String APPLN_ID = "1";

    /**
     * Gets a single high-level intent.
     *
     * @return a Collection with the single high-level intent if found,
     * otherwise null.
     */
    @Get("json")
    public Collection<Intent> retrieve() throws IOException {
        IPathCalcRuntimeService pathRuntime = (IPathCalcRuntimeService) getContext().
                getAttributes().get(IPathCalcRuntimeService.class.getCanonicalName());
        Collection<Intent> intents = null;

        String intentId = (String) getRequestAttributes().get("intent-id");
        if (intentId == null) {
            return null;        // Missing Intent ID
        }

        //
        // Get a single high-level Intent: use the Intent ID to find it
        //
        IntentMap intentMap = pathRuntime.getHighLevelIntents();
        String applnIntentId = APPLN_ID + ":" + intentId;
        Intent intent = intentMap.getIntent(applnIntentId);
        if (intent != null) {
            intents = new LinkedList<>();
            intents.add(intent);
        }

        return intents;
    }

    /**
     * Deletes a single high-level intent.
     *
     * @return the status of the operation (TBD).
     */
    @Delete("json")
    public String store() {
        IPathCalcRuntimeService pathRuntime = (IPathCalcRuntimeService) getContext().
                getAttributes().get(IPathCalcRuntimeService.class.getCanonicalName());

        String intentId = (String) getRequestAttributes().get("intent-id");
        if (intentId == null) {
            return null;        // Missing Intent ID
        }

        //
        // Remove a single high-level Intent: use the Intent ID to find it
        //
        //
        // TODO: The implementation below is broken - waiting for the Java API
        // TODO: The deletion should use synchronous Java API?
        IntentMap intentMap = pathRuntime.getHighLevelIntents();
        String applnIntentId = APPLN_ID + ":" + intentId;
        Intent intent = intentMap.getIntent(applnIntentId);
        if (intent != null) {
            IntentOperationList operations = new IntentOperationList();
            operations.add(IntentOperation.Operator.REMOVE, intent);
            pathRuntime.executeIntentOperations(operations);
        }
        return "";      // TODO no reply yet from the purge intents call
    }
}
