package net.onrc.onos.apps.segmentrouting.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.onrc.onos.apps.segmentrouting.ISegmentRoutingService;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingManager.removeTunnelMessages;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingPolicy;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingPolicy.PolicyType;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingPolicyTunnel;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingTunnel;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingTunnel.TunnelRouteInfo;

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
        log.debug("createTunnel with tunnelParams {}", tunnelParams);
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
            else
                return "fail";
        } catch (IOException ex) {
            log.error("Exception occurred parsing inbound JSON", ex);
            return "fail";
        }
        log.debug("createTunnel with tunnelId {} Label Path{}",
                createParams.getTunnel_id(), createParams.getLabel_path());
        boolean result = segmentRoutingService.createTunnel(createParams.getTunnel_id(),
                createParams.getLabel_path());
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
        removeTunnelMessages result = segmentRoutingService.removeTunnel(
                createParams.getTunnel_id());
        return result.name()+" "+result.toString();
    }

    @Get("json")
    public Object getTunnel() {
        ISegmentRoutingService segmentRoutingService =
                (ISegmentRoutingService) getContext().getAttributes().
                        get(ISegmentRoutingService.class.getCanonicalName());
        Iterator<SegmentRoutingTunnel> ttI = segmentRoutingService.getTunnelTable().iterator();
        List<SegmentRouterTunnelInfo> infoList = new ArrayList<SegmentRouterTunnelInfo>();
        while(ttI.hasNext()){
           SegmentRoutingTunnel tunnelInfo = ttI.next();
           List<Integer> tunnelPath = tunnelInfo.getLabelids();
           String tunnelId = tunnelInfo.getTunnelId();
           Collection<SegmentRoutingPolicy> policies = segmentRoutingService.getPoclicyTable();
           Iterator<SegmentRoutingPolicy> piI = policies.iterator();
           String policiesId = "";
           while(piI.hasNext()){
               SegmentRoutingPolicy policy = piI.next();
               if(policy.getType() == PolicyType.TUNNEL_FLOW &&
                 ((SegmentRoutingPolicyTunnel)policy).getTunnelId().equals(tunnelId)){
                   policiesId += (policy.getPolicyId()+",");
               }
           }
           if (policiesId.endsWith(",")){
               policiesId = (String) policiesId.subSequence(0, policiesId.length()-1);
           }
           Iterator<TunnelRouteInfo>trI = tunnelInfo.getRoutes().iterator();
           List<List<String>> labelStack = new ArrayList<List<String>>();
           List<String> dpidGroup = new ArrayList<String>();
           while(trI.hasNext()){
               TunnelRouteInfo label = trI.next();
               labelStack.add(label.getRoute());
               Integer gId = segmentRoutingService.getTunnelGroupId(tunnelId,
                       label.getSrcSwDpid());
               dpidGroup.add(label.getSrcSwDpid() + "/"+ gId);
           }
           SegmentRouterTunnelInfo info = new SegmentRouterTunnelInfo(tunnelId,
                    labelStack, dpidGroup, tunnelPath, policiesId );
           infoList.add(info);
        }
        log.debug("getTunnel with params");
        return infoList;
    }
}
