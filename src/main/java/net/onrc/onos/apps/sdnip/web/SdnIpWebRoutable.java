package net.onrc.onos.apps.sdnip.web;

import net.floodlightcontroller.restserver.RestletRoutable;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

/**
 * REST URL router for SDN-IP REST calls.
 */
public class SdnIpWebRoutable implements RestletRoutable {
    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        router.attach("/json", IncomingRequestResource.class);
        router.attach("/rib/{dest}", IncomingRequestResource.class);
        router.attach("/{sysuptime}/{sequence}/{routerid}/{prefix}/{mask}/{nexthop}", IncomingRequestResource.class);
        router.attach("/{routerid}/{prefix}/{mask}/{nexthop}/synch", OutgoingRequestResource.class);
        router.attach("/{routerid}/{capability}", IncomingRequestResource.class);
        return router;
    }

    @Override
    public String basePath() {
        return "/wm/bgp";
    }
}
