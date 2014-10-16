package net.onrc.onos.apps.segmentrouting.web;

import java.util.HashMap;

import net.floodlightcontroller.core.web.ControllerSwitchesResource;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.MutableTopology;

import org.restlet.resource.ResourceException;
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
     * Gets the switches and ports information from the network topology.
     *
     * @return a Representation of a Collection of switches from the network
     * topology. Each switch contains the switch ports.
     */
    @Get("json")
    public Representation retrieve() {
        ITopologyService topologyService =
            (ITopologyService) getContext().getAttributes()
                .get(ITopologyService.class.getCanonicalName());

        MutableTopology mutableTopology = topologyService.getTopology();
        mutableTopology.acquireReadLock();
        try {
            return eval(toRepresentation(mutableTopology.getSwitches(), null));
        } finally {
            mutableTopology.releaseReadLock();
        }
    }
 
    

}
