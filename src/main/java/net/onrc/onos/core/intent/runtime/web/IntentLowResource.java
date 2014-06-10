package net.onrc.onos.core.intent.runtime.web;

import java.util.Collection;

import net.onrc.onos.core.intent.Intent;
import net.onrc.onos.core.intent.IntentMap;
import net.onrc.onos.core.intent.runtime.IPathCalcRuntimeService;

import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * A class to access the low-level intents.
 */
public class IntentLowResource extends ServerResource {
    /**
     * Gets all low-level intents.
     *
     * @return a Representation of a collection of all low-level intents.
     */
    @Get("json")
    public Representation retrieve() {
        IPathCalcRuntimeService pathRuntime =
            (IPathCalcRuntimeService) getContext().getAttributes()
                .get(IPathCalcRuntimeService.class.getCanonicalName());

        //
        // Get all low-level intents
        //
        IntentMap intentMap = pathRuntime.getPathIntents();
        Collection<Intent> intents = intentMap.getAllIntents();

        return toRepresentation(intents, null);
    }
}
