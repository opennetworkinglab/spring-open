package net.onrc.onos.core.intent.runtime.web;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import net.onrc.onos.core.intent.Intent;
import net.onrc.onos.core.intent.IntentMap;
import net.onrc.onos.core.intent.runtime.IPathCalcRuntimeService;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to access a single low-level intent.
 */
public class IntentLowObjectResource extends ServerResource {
    private static final Logger log = LoggerFactory.getLogger(IntentLowObjectResource.class);
    // TODO need to assign proper application id.
    private static final String APPLN_ID = "1";

    /**
     * Gets a single low-level intent.
     *
     * @return a Collection with the single low-level intent if found,
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
        // Get a single low-level Intent: use the Intent ID to find it
        //
        IntentMap intentMap = pathRuntime.getPathIntents();
        String applnIntentId = APPLN_ID + ":" + intentId;
        Intent intent = intentMap.getIntent(applnIntentId);
        if (intent != null) {
            intents = new LinkedList<>();
            intents.add(intent);
        }

        return intents;
    }
}
