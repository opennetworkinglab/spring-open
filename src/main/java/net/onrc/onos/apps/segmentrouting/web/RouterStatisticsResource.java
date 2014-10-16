package net.onrc.onos.apps.segmentrouting.web;


import java.util.List;
import java.util.concurrent.Future;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.MutableTopology;
import net.onrc.onos.core.util.Dpid;

import org.apache.commons.codec.binary.Hex;
import org.projectfloodlight.openflow.util.HexString;
import org.restlet.resource.ServerResource;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

import com.esotericsoftware.minlog.Log;

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
        //if (routerId == null && statsType == null){
        ITopologyService topologyService =
                (ITopologyService) getContext().getAttributes()
                .get(ITopologyService.class.getCanonicalName());

        MutableTopology mutableTopology = topologyService.getTopology();
        mutableTopology.acquireReadLock();
        try {
            if (routerId == null && statsType == null){
                return eval(toRepresentation(mutableTopology.getSwitches(), null));
        }
            else if(routerId != null && statsType == "port"){
                Log.debug("\n\n\nGot router port stats request\n\n\n");
                System.out.println("\n\n\nGot router port stats request\n\n\n");
                Long rId = HexString.toLong(routerId);
                return eval(toRepresentation(mutableTopology.getSwitch(new Dpid(rId)), null));
            }
        } finally {
            mutableTopology.releaseReadLock();
        }
      //  }
        /*else if(routerId != null && statsType == "port"){
            Long rId = HexString.toLong(routerId);
            IFloodlightProviderService floodlightProvider =
                    (IFloodlightProviderService) getContext().getAttributes().
                    get(IFloodlightProviderService.class.getCanonicalName());
            IOFSwitch sw = floodlightProvider.getSwitches().get(rId);
            Future<List<OFStatsReply>> future;
            List<OFStatsReply> values = null;
            
        }*/
    return null;
    }
    
        
 
    

}
