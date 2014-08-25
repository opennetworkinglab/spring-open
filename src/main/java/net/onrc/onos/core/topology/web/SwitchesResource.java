package net.onrc.onos.core.topology.web;

import static net.onrc.onos.core.topology.web.TopologyResource.eval;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.MutableTopology;

import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * A class to access switches and ports information from the network topology.
 */
public class SwitchesResource extends ServerResource {
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
