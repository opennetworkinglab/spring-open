package net.onrc.onos.core.flowprogrammer.web;

import net.onrc.onos.core.util.Dpid;

import org.projectfloodlight.openflow.util.HexString;
import org.restlet.resource.Get;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FlowProgrammer REST API implementation: Suspend sending message to switch.
 * <p/>
 * GET /wm/fprog/pusher/suspend/{dpid}/json"
 */
public class SuspendPusherResource extends PusherResource {

    private static final Logger log = LoggerFactory.getLogger(SetPushRateResource.class);

    /**
     * Implement the API.
     *
     * @return true if succeeded, false if failed.
     */
    @Get("json")
    public boolean retrieve() {
        if (!init()) {
            return false;
        }

        long dpid;
        try {
            dpid = HexString.toLong((String) getRequestAttributes().get("dpid"));
        } catch (NumberFormatException e) {
            log.error("Invalid number format");
            return false;
        }

        return pusher.suspend(new Dpid(dpid));
    }
}
