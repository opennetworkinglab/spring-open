package net.onrc.onos.core.topology.web;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import net.onrc.onos.core.intent.ConstrainedBFSTree;
import net.onrc.onos.core.intent.Path;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.Link;
import net.onrc.onos.core.topology.LinkEvent;
import net.onrc.onos.core.topology.Topology;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.topology.serializers.LinkSerializer;
import net.onrc.onos.core.util.Dpid;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyShortestPathResource extends ServerResource {

    private static final Logger log = LoggerFactory.getLogger(TopologyShortestPathResource.class);

    @Get("json")
    public String retrieve() {
        ITopologyService topologyService =
                (ITopologyService) getContext().getAttributes().
                        get(ITopologyService.class.getCanonicalName());

        Topology topology = topologyService.getTopology();

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("module", new Version(1, 0, 0, null));
        module.addSerializer(new LinkSerializer());
        mapper.registerModule(module);

        //
        // Fetch the attributes
        //
        String srcDpidStr = (String) getRequestAttributes().get("src-dpid");
        String dstDpidStr = (String) getRequestAttributes().get("dst-dpid");
        Dpid srcDpid = new Dpid(srcDpidStr);
        Dpid dstDpid = new Dpid(dstDpidStr);
        log.debug("Getting Shortest Path {}--{}", srcDpidStr, dstDpidStr);

        //
        // Do the Shortest Path computation and return the result: list of
        // links.
        //
        try {
            topology.acquireReadLock();
            Switch srcSwitch = topology.getSwitch(srcDpid.value());
            Switch dstSwitch = topology.getSwitch(dstDpid.value());
            if ((srcSwitch == null) || (dstSwitch == null)) {
                return "";
            }
            ConstrainedBFSTree bfsTree = new ConstrainedBFSTree(srcSwitch);
            Path path = bfsTree.getPath(dstSwitch);
            List<Link> links = new LinkedList<>();
            for (LinkEvent linkEvent : path) {
                Link link = topology.getLink(linkEvent.getSrc().getDpid(),
                        linkEvent.getSrc().getNumber(),
                        linkEvent.getDst().getDpid(),
                        linkEvent.getDst().getNumber());
                if (link == null) {
                    return "";
                }
                links.add(link);
            }
            return mapper.writeValueAsString(links);
        } catch (IOException e) {
            log.error("Error writing Shortest Path to JSON", e);
            return "";
        } finally {
            topology.releaseReadLock();
        }
    }
}
