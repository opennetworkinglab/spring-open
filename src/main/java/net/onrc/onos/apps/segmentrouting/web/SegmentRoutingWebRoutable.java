package net.onrc.onos.apps.segmentrouting.web;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;
import net.onrc.onos.core.topology.web.SwitchesResource;
/**
 *  Handle all URI's for SegmentRouter web
 *
 */

public class SegmentRoutingWebRoutable implements RestletRoutable {

    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        //TODO: rewrite SwitchesResource for router specific info.
        router.attach("/routers",  RouterStatisticsResource.class);
        router.attach("/router/{routerId}/{statsType}",  RouterStatisticsResource.class);
        return router;
    }

    @Override
    public String basePath() {
        return "/wm/onos/segmentrouting";
    }

}
