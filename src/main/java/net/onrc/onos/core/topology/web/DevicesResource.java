package net.onrc.onos.core.topology.web;

import static net.onrc.onos.core.topology.web.TopologyResource.eval;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.Topology;

import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * A class to access hosts information from the network topology.
 */
public class DevicesResource extends ServerResource {
    /**
     * Gets the hosts information from the network topology.
     *
     * @return a Representation of a Collection of hosts from the network
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
            return eval(toRepresentation(topology.getDevices(), null));
        } finally {
            topology.releaseReadLock();
        }
    }
}
