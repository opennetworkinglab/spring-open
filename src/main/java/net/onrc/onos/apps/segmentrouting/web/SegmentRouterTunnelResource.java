package net.onrc.onos.apps.segmentrouting.web;

import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for return router statistics
 *
 */
public class SegmentRouterTunnelResource extends ServerResource {
    protected final static Logger log =
            LoggerFactory.getLogger(SegmentRouterTunnelResource.class);

    @Post("json")
    public String createTunnel(String tunnelParams) {
        String reply = "success";
        log.debug("createTunnel with params {}", tunnelParams);
        return reply;
    }

    @Delete("json")
    public String deleteTunnel(String tunnelId) {
        String reply = "deleted";
        log.debug("deleteTunnel with Id {}", tunnelId);
        return reply;
    }

    @Get("json")
    public String getTunnel() {
        String reply = "success";
        log.debug("getTunnel with params");
        return reply;
    }
}
