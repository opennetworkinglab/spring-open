package net.onrc.onos.apps.segmentrouting.web;



import static net.onrc.onos.core.topology.web.TopologyResource.eval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.onrc.onos.apps.segmentrouting.ISegmentRoutingService;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.MutableTopology;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.util.Dpid;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.projectfloodlight.openflow.util.HexString;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
/**
 * Base class for return router statistics
 *
 */
public class SegmentRouterResource extends ServerResource {
    /**
     * Gets the switches/routers and ports information from the network topology.
     *
     * @return a Representation of a Collection of switches/routers from the network
     * topology. Each switch contains the switch ports.
     */
    @Get("json")
    public Object retrieve() {
        String routerId = (String) getRequestAttributes().get("routerId");
        String statsType = (String) getRequestAttributes().get("statsType");
        ITopologyService topologyService =
                (ITopologyService) getContext().getAttributes()
                .get(ITopologyService.class.getCanonicalName());

        MutableTopology mutableTopology = topologyService.getTopology();
        mutableTopology.acquireReadLock();
        try {
            if (routerId == null && statsType == null){
                return eval(toRepresentation(mutableTopology.getSwitches(), null));
        }
            else if(routerId != null && statsType.equals("port")){
                Switch sw = mutableTopology
                .getSwitch(new Dpid(HexString.toLong(routerId)));
                if(sw ==null){
                    //TODO: Add exception
                    return null;
                }
                ISegmentRoutingService segmentRoutingService =
                        (ISegmentRoutingService) getContext().getAttributes().
                                get(ISegmentRoutingService.class.getCanonicalName());
                Map <String, List<SegmentRouterPortInfo>> result = new HashMap <String, List<SegmentRouterPortInfo>>();
                List<SegmentRouterPortInfo> listPortInfo = new ArrayList<SegmentRouterPortInfo>();
                Collection<Port> portList =sw.getPorts();
                String subnets = null;
                if (sw.getAllStringAttributes().containsKey("subnets")){
                    subnets = sw.getAllStringAttributes().get("subnets");
                    JSONArray subnetArray = JSONArray.fromObject(subnets);
                    Iterator<Port> pI = portList.iterator();
                    int nodeSid = Integer.parseInt(sw.getStringAttribute("nodeSid"));
                    HashMap<Integer, List<Integer>> adjPortInfo = segmentRoutingService.getAdjacencyInfo(nodeSid);
                    while(pI.hasNext()){
                        Port p = pI.next();
                        Iterator<Integer> keyIt = adjPortInfo.keySet().iterator();
                        Iterator<?> sI = subnetArray.iterator();
                        List<Integer> adjacency = new ArrayList<Integer>();
                        while(keyIt.hasNext()){
                            Integer adj = keyIt.next();
                            List<Integer> adjPortList = adjPortInfo.get(adj);
                            if(adjPortList.contains(Integer.valueOf(p.getNumber().shortValue()))){
                                adjacency.add(adj);
                            }
                        }
                        String subnet = null;
                        while(sI.hasNext()){
                            JSONObject portSubnetIp = (JSONObject) sI.next();
                            subnet = null;
                            if(portSubnetIp.getString("portNo").equals(p.getNumber().toString())){
                                subnet = portSubnetIp.getString("subnetIp");
                                break;
                            }
                        }
                        listPortInfo.add( new SegmentRouterPortInfo(subnet,p, adjacency));
                    }
                    result.put(routerId, listPortInfo);
                    return eval(toRepresentation(result,null));
                }
                else{
                    Iterator<Port> pI = portList.iterator();
                    int nodeSid = Integer.parseInt(sw.getStringAttribute("nodeSid"));
                    HashMap<Integer, List<Integer>> adjPortInfo = segmentRoutingService.getAdjacencyInfo(nodeSid);
                    while(pI.hasNext()){
                        Port p = pI.next();
                        String subnet = null;
                        Iterator<Integer> keyIt = adjPortInfo.keySet().iterator();
                        List<Integer> adjacency = new ArrayList<Integer>();
                        while(keyIt.hasNext()){
                            Integer adj = keyIt.next();
                            List<Integer> adjPortList = adjPortInfo.get(adj);
                            if(adjPortList.contains(Integer.valueOf(p.getNumber().shortValue()))){
                                adjacency.add(adj);
                            }
                        }
                        listPortInfo.add( new SegmentRouterPortInfo(subnet,p, adjacency));
                    }
                    result.put(routerId, listPortInfo);
                    return eval(toRepresentation(result,null));
                }
            }
            else if(routerId != null && statsType.equals("adjacency")){
                ISegmentRoutingService segmentRoutingService =
                        (ISegmentRoutingService) getContext().getAttributes().
                                get(ISegmentRoutingService.class.getCanonicalName());
                Switch sw = mutableTopology
                .getSwitch(new Dpid(HexString.toLong(routerId)));
                if(sw ==null){
                    //TODO: Add exception
                    return null;
                }
                int nodeSid = Integer.parseInt(sw.getStringAttribute("nodeSid"));
                HashMap<Integer, List<Integer>> adjPortInfo = segmentRoutingService.getAdjacencyInfo(nodeSid);
                Iterator<Integer> aPIt = adjPortInfo.keySet().iterator();
                List<SegmentRouterAdjacencyInfo> result= new ArrayList<SegmentRouterAdjacencyInfo>();
                while(aPIt.hasNext()){
                    Integer adj = aPIt.next();
                    result.add( new SegmentRouterAdjacencyInfo(adj,
                            adjPortInfo.get(adj)));
                }
                return  result;
            }
        } finally {
            mutableTopology.releaseReadLock();
        }
    //Should Never get to this point.
    return null;
    }
}
