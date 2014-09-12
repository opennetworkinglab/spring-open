
package net.onrc.onos.apps.sdnip.web;

import net.floodlightcontroller.restserver.RestletRoutable;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

/**
 * REST URL router for SDN-IP REST calls.
 */
public class SdnIpWebRoutableNew implements RestletRoutable {

    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        //router.attach("/beginRouting/json", SdnIpSetup.class);
        router.attach("/beginRouting/json/{version}", SdnIpSetup.class);
        return router;
    }

    @Override
    public String basePath() {
        return "/wm/sdnip";
    }
}
