package net.onrc.onos.apps.segmentrouting.web;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.MutableTopology;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.util.Dpid;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.projectfloodlight.openflow.util.HexString;
import org.restlet.resource.ServerResource;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

import static net.onrc.onos.core.topology.web.TopologyResource.eval;
/**
 * Base class for return router statistics
 *
 */
public class RouterStatisticsResource extends ServerResource {
    /**
     * Gets the switches/routers and ports information from the network topology.
     *
     * @return a Representation of a Collection of switches/routers from the network
     * topology. Each switch contains the switch ports.
     */
    @Get("json")
    public Representation retrieve() {
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
                Map <String, List<SegmentRouterPortInfo>> result = new HashMap <String, List<SegmentRouterPortInfo>>();
                List<SegmentRouterPortInfo> listPortInfo = new ArrayList<SegmentRouterPortInfo>();
                Collection<Port> portList =sw.getPorts();
                String subnets = null;
                if (sw.getAllStringAttributes().containsKey("subnets")){
                    subnets = sw.getAllStringAttributes().get("subnets");
                    JSONArray subnetArray = JSONArray.fromObject(subnets);
                    Iterator pI = portList.iterator();
                    while(pI.hasNext()){
                        Port p = (Port) pI.next();
                        Iterator sI = subnetArray.iterator();
                        String subnet = null;
                        while(sI.hasNext()){
                            JSONObject portSubnetIp = (JSONObject) sI.next();
                            subnet = null;
                            if(portSubnetIp.getString("portNo").equals(p.getNumber().toString())){
                                subnet = portSubnetIp.getString("subnetIp");
                                break;
                            }
                        }
                        listPortInfo.add( new SegmentRouterPortInfo(subnet,p));
                    }
                    result.put(routerId, listPortInfo);
                    return eval(toRepresentation(result,null));
                }
                else{
                    Iterator pI = portList.iterator();
                    while(pI.hasNext()){
                        Port p = (Port) pI.next();
                        String subnet = null;
                        listPortInfo.add( new SegmentRouterPortInfo(subnet,p));
                    }
                    result.put(routerId, listPortInfo);
                    return eval(toRepresentation(result,null));
                }
            }
        } finally {
            mutableTopology.releaseReadLock();
        }
    //Should Never get to this point.
    return null;
    }
}
