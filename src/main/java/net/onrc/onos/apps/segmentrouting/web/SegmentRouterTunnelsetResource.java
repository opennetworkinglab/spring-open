package net.onrc.onos.apps.segmentrouting.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.floodlightcontroller.core.IOF13Switch.GroupChain;
import net.onrc.onos.apps.segmentrouting.ISegmentRoutingService;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingPolicy;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingPolicyTunnel;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingTunnel;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingTunnelset;
import net.onrc.onos.apps.segmentrouting.TunnelRouteInfo;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingManager.removeTunnelMessages;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingPolicy.PolicyType;

import org.codehaus.jackson.map.ObjectMapper;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SegmentRouterTunnelsetResource extends ServerResource {
    protected final static Logger log =
            LoggerFactory.getLogger(SegmentRouterTunnelsetResource.class);

    @Post("json")
    public String createTunnelset(String tunnelsetParams) {
        log.debug("createTunnelset with tunnelsetParams {}", tunnelsetParams);
        ISegmentRoutingService segmentRoutingService =
                (ISegmentRoutingService) getContext().getAttributes().
                        get(ISegmentRoutingService.class.getCanonicalName());
        ObjectMapper mapper = new ObjectMapper();
        SegmentRouterTunnelsetRESTParams createParams = null;
        try {
            if (tunnelsetParams != null) {
                createParams = mapper.readValue(tunnelsetParams,
                        SegmentRouterTunnelsetRESTParams.class);
            }
            else
                return "fail";
        } catch (IOException ex) {
            log.error("Exception occurred parsing inbound JSON", ex);
            return "fail";
        }
        log.debug("createTunnelset with tunnelsetId {} tunnel params{}",
                createParams.getTunnelset_id(), createParams.getTunnelParams().get(0));
        boolean result = true;
        result = segmentRoutingService.createTunnelset(createParams.getTunnelset_id(),
                createParams);
        return (result == true) ? "success" : "fail";
    }
/*
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
    }*/

    @Get("json")
    public Object getTunnelset() {
        ISegmentRoutingService segmentRoutingService =
                (ISegmentRoutingService) getContext().getAttributes().
                        get(ISegmentRoutingService.class.getCanonicalName());
        Iterator<SegmentRoutingTunnelset> ttI = 
        		segmentRoutingService.getTunnelsetTable().iterator();
        List<SegmentRouterTunnelsetInfo> tunnelsetInfoList = 
        		new ArrayList<SegmentRouterTunnelsetInfo>();
        while(ttI.hasNext()){
           SegmentRoutingTunnelset tunnelset = ttI.next();
           String tunnelsetId = tunnelset.getTunnelsetId();
           SegmentRouterTunnelsetInfo tunnelsetInfo = new SegmentRouterTunnelsetInfo(
        		   tunnelsetId);
           Collection<SegmentRoutingPolicy> policies = segmentRoutingService.getPoclicyTable();
           Iterator<SegmentRoutingPolicy> piI = policies.iterator();
           String policiesId = "";
           while(piI.hasNext()){
               SegmentRoutingPolicy policy = piI.next();
               if(policy.getType() == PolicyType.TUNNEL_FLOW &&
                 (((SegmentRoutingPolicyTunnel)policy).isTunnelsetId() &&
                  ((SegmentRoutingPolicyTunnel)policy).getTunnelId().equals(tunnelsetId))){
                   policiesId += (policy.getPolicyId()+",");
               }
           }
           if (policiesId.endsWith(",")){
               policiesId = (String) policiesId.subSequence(0, policiesId.length()-1);
           }
           HashMap<String, SegmentRoutingTunnel> constituentTunnels = 
        		   				tunnelset.getTunnels();
           List<SegmentRouterTunnelInfo> tunnelInfoList = 
           		new ArrayList<SegmentRouterTunnelInfo>();
           for (SegmentRoutingTunnel tunnel:constituentTunnels.values()) {
               String tunnelId = tunnel.getTunnelId();
               List<Integer> tunnelPath = tunnel.getLabelids();
               String parentTunnelsetId = tunnel.getTunnelsetId();
               Iterator<TunnelRouteInfo>trI = tunnel.getRoutes().iterator();
               List<List<String>> labelStack = new ArrayList<List<String>>();
               List<String> dpidGroup = new ArrayList<String>();
               while(trI.hasNext()){
                   TunnelRouteInfo label = trI.next();
                   labelStack.add(label.getRoute());
                   Integer gId = tunnelset.getTunnelGroupChain(tunnelId).
                		   get(label.getSrcSwDpid()).getInnermostGroupId();
                   dpidGroup.add(label.getSrcSwDpid()+"("
                           + segmentRoutingService.getMplsLabel(label.getSrcSwDpid())+ ")"
                           + "/"+ gId
                           );
               }
               SegmentRouterTunnelInfo info = new SegmentRouterTunnelInfo(tunnelId,
                       labelStack, dpidGroup, tunnelPath, null, parentTunnelsetId );
               tunnelInfoList.add(info);
           }
           tunnelsetInfo.setPolicies(policiesId);
           tunnelsetInfo.setConstituentTunnels(tunnelInfoList);
           tunnelsetInfoList.add(tunnelsetInfo);
        }
        log.debug("getTunnelset with params");
        return tunnelsetInfoList;
    }
}
