package net.onrc.onos.core.topology.web;

import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.Topology;

import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * A class to access the network topology information.
 */
public class TopologyAllResource extends ServerResource {
    /**
     * Gets the network topology information.
     *
     * @return a Representation of the network topology.
     */
    @Get("json")
    public Representation retrieve() {
        ITopologyService topologyService =
            (ITopologyService) getContext().getAttributes()
                .get(ITopologyService.class.getCanonicalName());

        Topology topology = topologyService.getTopology();
        topology.acquireReadLock();
        try {
            return toRepresentation(topology, null);
        } finally {
            topology.releaseReadLock();
        }
    }
}
