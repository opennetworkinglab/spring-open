package net.onrc.onos.core.flowmanager.web;

import net.floodlightcontroller.restserver.RestletRoutable;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

/**
 * Routable class for flow REST API URLs.
 */
public class FlowManagerWebRoutable implements RestletRoutable {

    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router();
        router.attach("", FlowResource.class);
        return router;
    }

    @Override
    public String basePath() {
        return "/wm/onos/flow";
    }

}
