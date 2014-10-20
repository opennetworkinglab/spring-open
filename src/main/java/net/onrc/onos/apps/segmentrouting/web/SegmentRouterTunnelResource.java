package net.onrc.onos.apps.segmentrouting.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.onrc.onos.apps.segmentrouting.ISegmentRoutingService;
import net.onrc.onos.core.util.Dpid;

import org.codehaus.jackson.map.ObjectMapper;
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
        ISegmentRoutingService segmentRoutingService =
                (ISegmentRoutingService) getContext().getAttributes().
                        get(ISegmentRoutingService.class.getCanonicalName());
        ObjectMapper mapper = new ObjectMapper();
        TunnelCreateParams createParams = null;
        try {
            if (tunnelParams != null) {
                createParams = mapper.readValue(tunnelParams, TunnelCreateParams.class);
            }
        } catch (IOException ex) {
            log.error("Exception occurred parsing inbound JSON", ex);
            return "fail";
        }
        log.debug("createTunnel with tunnelId {} tunnelPath{}",
                createParams.getTunnel_id(), createParams.getTunnel_path());
        List<Dpid> tunnelDpids = new ArrayList<Dpid>();
        for (String dpid : createParams.getTunnel_path()) {
            tunnelDpids.add(new Dpid(dpid));
        }
        boolean result = segmentRoutingService.createTunnel(createParams.getTunnel_id(),
                tunnelDpids);
        return (result == true) ? "success" : "fail";
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
