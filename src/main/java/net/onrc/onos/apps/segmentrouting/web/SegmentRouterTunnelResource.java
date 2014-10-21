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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.onrc.onos.apps.segmentrouting.ISegmentRoutingService;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingManager;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingManager.TunnelInfo;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingManager.TunnelRouteInfo;
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
        SegmentRouterTunnelRESTParams createParams = null;
        try {
            if (tunnelParams != null) {
                createParams = mapper.readValue(tunnelParams,
                        SegmentRouterTunnelRESTParams.class);
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
    public String deleteTunnel(String tunnelParams) {
        ISegmentRoutingService segmentRoutingService =
                (ISegmentRoutingService) getContext().getAttributes().
                        get(ISegmentRoutingService.class.getCanonicalName());
        ObjectMapper mapper = new ObjectMapper();
        SegmentRouterTunnelRESTParams createParams = null;
        try {
            if (tunnelParams != null) {
                createParams = mapper.readValue(tunnelParams,
                        SegmentRouterTunnelRESTParams.class);
            }
        } catch (IOException ex) {
            log.error("Exception occurred parsing inbound JSON", ex);
            return "fail";
        }
        log.debug("deleteTunnel with Id {}", createParams.getTunnel_id());
        boolean result = segmentRoutingService.removeTunnel(
                createParams.getTunnel_id());
        return (result == true) ? "deleted" : "fail";
    }

    @Get("json")
    public Object getTunnel() {
        System.out.println("Got into getTunnel");
        ISegmentRoutingService segmentRoutingService =
                (ISegmentRoutingService) getContext().getAttributes().
                        get(ISegmentRoutingService.class.getCanonicalName());
        Iterator<TunnelInfo> ttI = segmentRoutingService.getTunnelTable().iterator();
        List<SegmentRouterTunnelInfo> infoList = new ArrayList<SegmentRouterTunnelInfo>();
        while(ttI.hasNext()){
           TunnelInfo tunnelInfo = ttI.next();
           Iterator<TunnelRouteInfo>trI = tunnelInfo.getRoutes().iterator();
           List<List<String>> labelStack = new ArrayList<List<String>>();
           while(trI.hasNext()){
               TunnelRouteInfo label = trI.next();
               labelStack.add(label.getRoute());
           }
           SegmentRouterTunnelInfo info = new SegmentRouterTunnelInfo(tunnelInfo.getTunnelId(),
                   tunnelInfo.getDpids(), labelStack );
           infoList.add(info);
           //TODO Add Group/DPID
           
        }
        log.debug("getTunnel with params");
        Map <String,List<SegmentRouterTunnelInfo>>result = new HashMap<String,List<SegmentRouterTunnelInfo>>();
        result.put("tunnels", infoList);
        return infoList;
    }
}
