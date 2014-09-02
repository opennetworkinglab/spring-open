package net.onrc.onos.core.matchaction.web;

import net.floodlightcontroller.restserver.RestletRoutable;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

/**
 * Routable class for match-action REST API URLs.
 */
public class MatchActionWebRoutable implements RestletRoutable {

    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        router.attach("", MatchActionResource.class);
        return router;
    }

    @Override
    public String basePath() {
        return "/wm/onos/matchaction";
    }

}
