package net.onrc.onos.core.topology.web;

import java.io.IOException;

import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.Topology;
import net.onrc.onos.core.topology.serializers.LinkSerializer;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyLinksResource extends ServerResource {

    private static final Logger log = LoggerFactory.getLogger(TopologyLinksResource.class);

    @Get("json")
    public String retrieve() {
        ITopologyService topologyService = (ITopologyService) getContext().getAttributes().
                get(ITopologyService.class.getCanonicalName());

        Topology topology = topologyService.getTopology();

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("module", new Version(1, 0, 0, null));
        module.addSerializer(new LinkSerializer());
        mapper.registerModule(module);

        try {
            topology.acquireReadLock();
            return mapper.writeValueAsString(topology.getLinks());
        } catch (IOException e) {
            log.error("Error writing link list to JSON", e);
            return "";
        } finally {
            topology.releaseReadLock();
        }
    }
}
