package net.onrc.onos.core.metrics.web;

import net.floodlightcontroller.restserver.RestletRoutable;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

/**
 * Restlet Router for Metrics REST APIs.
 */
public class MetricsWebRoutable implements RestletRoutable {
    /**
     * Creates the Restlet router and binds to the proper resources.
     */
    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        // GET all metrics
        router.attach("", MetricsResource.class);
        return router;
    }


    /**
     * Sets the base path for the Metrics.
     */
    @Override
    public String basePath() {
        return "/wm/onos/metrics";
    }
}
