package net.onrc.onos.core.topology.web;

import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.Topology;

import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * A class to access links information from the network topology.
 */
public class TopologyLinksResource extends ServerResource {
    /**
     * Gets the links information from the network topology.
     *
     * @return a Representation of a Collection of links from the network
     * topology.
     */
    @Get("json")
    public Representation retrieve() {
        ITopologyService topologyService =
            (ITopologyService) getContext().getAttributes()
                .get(ITopologyService.class.getCanonicalName());

        Topology topology = topologyService.getTopology();
        topology.acquireReadLock();
        try {
            return toRepresentation(topology.getLinks(), null);
        } finally {
            topology.releaseReadLock();
        }
    }
}
