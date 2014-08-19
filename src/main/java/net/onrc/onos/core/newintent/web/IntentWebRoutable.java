package net.onrc.onos.core.newintent.web;

import net.floodlightcontroller.restserver.RestletRoutable;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

/**
 * Routable class for intent REST API URLs.
 */
public class IntentWebRoutable implements RestletRoutable {

    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        router.attach("", IntentResource.class);
        return router;
    }

    @Override
    public String basePath() {
        return "/wm/onos/newintent";
    }

}
