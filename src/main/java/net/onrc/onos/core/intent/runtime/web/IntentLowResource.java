package net.onrc.onos.core.intent.runtime.web;

import java.io.IOException;
import java.util.Collection;

import net.onrc.onos.core.intent.Intent;
import net.onrc.onos.core.intent.IntentMap;
import net.onrc.onos.core.intent.runtime.IPathCalcRuntimeService;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to access the low-level intents.
 */
public class IntentLowResource extends ServerResource {
    private static final Logger log = LoggerFactory.getLogger(IntentLowResource.class);

    /**
     * Gets all low-level intents.
     *
     * @return a collection of all low-leve intents.
     */
    @Get("json")
    public Collection<Intent> retrieve() throws IOException {
        IPathCalcRuntimeService pathRuntime = (IPathCalcRuntimeService) getContext().
                getAttributes().get(IPathCalcRuntimeService.class.getCanonicalName());

        //
        // Get all low-level intents
        //
        IntentMap intentMap = pathRuntime.getPathIntents();
        Collection<Intent> intents = intentMap.getAllIntents();

        return intents;
    }
}
