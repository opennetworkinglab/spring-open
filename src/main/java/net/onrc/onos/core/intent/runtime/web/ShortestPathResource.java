package net.onrc.onos.core.intent.runtime.web;

import static net.onrc.onos.core.topology.web.TopologyResource.eval;
import java.util.LinkedList;
import java.util.List;

import net.onrc.onos.core.intent.ConstrainedBFSTree;
import net.onrc.onos.core.intent.Path;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.Link;
import net.onrc.onos.core.topology.LinkEvent;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.topology.Topology;
import net.onrc.onos.core.util.Dpid;

import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to access Shortest-Path information between switches.
 */
public class ShortestPathResource extends ServerResource {
    private static final Logger log = LoggerFactory.getLogger(ShortestPathResource.class);

    /**
     * Gets the Shortest-Path infomration between switches.
     *
     * @return a Representation with the Shortest-Path information between
     * switches if found, otherwise null. The Shortest-Path information is an
     * ordered collection of Links.
     */
    @Get("json")
    public Representation retrieve() {
        ITopologyService topologyService =
            (ITopologyService) getContext().getAttributes()
                .get(ITopologyService.class.getCanonicalName());

        //
        // Fetch the attributes
        //
        String srcDpidStr = (String) getRequestAttributes().get("src-dpid");
        String dstDpidStr = (String) getRequestAttributes().get("dst-dpid");
        Dpid srcDpid = new Dpid(srcDpidStr);
        Dpid dstDpid = new Dpid(dstDpidStr);
        log.debug("Getting Shortest Path {}--{}", srcDpidStr, dstDpidStr);

        //
        // Do the Shortest Path computation and return the result: a list of
        // links.
        //
        Topology topology = topologyService.getTopology();
        topology.acquireReadLock();
        try {
            Switch srcSwitch = topology.getSwitch(srcDpid);
            Switch dstSwitch = topology.getSwitch(dstDpid);
            if ((srcSwitch == null) || (dstSwitch == null)) {
                return null;
            }
            ConstrainedBFSTree bfsTree = new ConstrainedBFSTree(srcSwitch);
            Path path = bfsTree.getPath(dstSwitch);
            if (path == null) {
                return null;
            }
            List<Link> links = new LinkedList<>();
            for (LinkEvent linkEvent : path) {
                Link link = topology.getLink(
                        linkEvent.getSrc().getDpid(),
                        linkEvent.getSrc().getPortNumber(),
                        linkEvent.getDst().getDpid(),
                        linkEvent.getDst().getPortNumber());
                if (link == null) {
                    return null;
                }
                links.add(link);
            }
            return eval(toRepresentation(links, null));
        } finally {
            topology.releaseReadLock();
        }
    }
}
