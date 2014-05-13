package net.onrc.onos.core.intent.runtime.web;

import net.floodlightcontroller.restserver.RestletRoutable;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

/**
 * REST API implementation for the Intents.
 */
public class IntentWebRoutable implements RestletRoutable {
    /**
     * Creates the Restlet router and bind to the proper resources.
     */
    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        // GET all high-level intents
        // POST (create) a collection of high-level intents
        // DELETE all intents (TODO: delete a collection of high-level intents)
        router.attach("/high", IntentHighResource.class);

        // GET a high-level intent object
        // PUT (update) a high-level intent object (entire object) (LATER?)
        // DELETE a high-level intent object
        router.attach("/high/{intent-id}", IntentHighObjectResource.class);

        // GET all low-level intents
        router.attach("/low", IntentLowResource.class);

        // GET a low-level intent object
        router.attach("/low/{intent-id}", IntentLowObjectResource.class);

        // GET a Shortest Path between two Switch DPIDs
        router.attach("/path/switch/{src-dpid}/shortest-path/{dst-dpid}",
                      ShortestPathResource.class);

        return router;
    }

    /**
     * Sets the base path for the Intents.
     */
    @Override
    public String basePath() {
        return "/wm/onos/intent";
    }
}
