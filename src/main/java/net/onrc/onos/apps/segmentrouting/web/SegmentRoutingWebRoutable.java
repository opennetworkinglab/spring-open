package net.onrc.onos.apps.segmentrouting.web;

import java.util.Iterator;

import net.floodlightcontroller.restserver.RestletRoutable;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingManager;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;
/**
 *  Handle all URI's for SegmentRouter web
 *
 */

public class SegmentRoutingWebRoutable implements RestletRoutable {

    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        //TODO: rewrite Router/SwitchesResource for router specific info.
        router.attach("/routers",  SegmentRouterResource.class);
        router.attach("/router/{routerId}/{statsType}",  SegmentRouterResource.class);
        router.attach("/tunnel", SegmentRouterTunnelResource.class);
        router.attach("/policy", SegmentRouterPolicyResource.class);
        // SegmentRouterTunnelResource.class);
        return router;
    }

    @Override
    public String basePath() {
        return "/wm/onos/segmentrouting";
    }

}
