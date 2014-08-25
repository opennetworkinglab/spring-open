package net.onrc.onos.core.topology.web;

import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.MutableTopology;

import org.restlet.engine.io.BufferingRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * A class to access the network topology information.
 */
public class TopologyResource extends ServerResource {
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

        MutableTopology mutableTopology = topologyService.getTopology();
        mutableTopology.acquireReadLock();
        try {
            return eval(toRepresentation(mutableTopology, null));
        } finally {
            mutableTopology.releaseReadLock();
        }
    }

    /**
     * Workaround code to trigger evaluation of Representation immediately.
     *
     * @param repr Representation to evaluate immediately
     * @return Evaluated Representation
     */
    public static Representation eval(final Representation repr) {

        BufferingRepresentation eval = new BufferingRepresentation(repr);
        // trigger evaluation
        eval.getSize();
        return eval;
    }
}
