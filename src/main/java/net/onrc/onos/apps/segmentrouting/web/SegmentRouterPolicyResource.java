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
public class SegmentRouterPolicyResource extends ServerResource {
    protected final static Logger log =
            LoggerFactory.getLogger(SegmentRouterPolicyResource.class);

    @Post("json")
    public String createPolicy(String policyParams) {
        String reply = "success";
        log.debug("createPolicy with params {}", policyParams);
        return reply;
    }

    @Delete("json")
    public String deletePolicy(String policyId) {
        String reply = "deleted";
        log.debug("deletePolicy with Id {}", policyId);
        return reply;
    }

    @Get("json")
    public String getPolicy() {
        String reply = "success";
        log.debug("getPolicy with params");
        return reply;
    }
}
