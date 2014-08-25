package net.onrc.onos.core.topology.web;

import static net.onrc.onos.core.topology.web.TopologyResource.eval;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.MutableTopology;

import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * A class to access links information from the network topology.
 */
public class LinksResource extends ServerResource {
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

        MutableTopology mutableTopology = topologyService.getTopology();
        mutableTopology.acquireReadLock();
        try {
            return eval(toRepresentation(mutableTopology.getLinks(), null));
        } finally {
            mutableTopology.releaseReadLock();
        }
    }
}
