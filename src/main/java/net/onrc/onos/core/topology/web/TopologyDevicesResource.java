package net.onrc.onos.core.topology.web;

import java.io.IOException;

import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.Topology;
import net.onrc.onos.core.topology.serializers.DeviceSerializer;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyDevicesResource extends ServerResource {
    private static final Logger log = LoggerFactory.getLogger(TopologyDevicesResource.class);

    @Get("json")
    public String retrieve() {
        ITopologyService topologyService = (ITopologyService) getContext().getAttributes().
                get(ITopologyService.class.getCanonicalName());

        Topology topology = topologyService.getTopology();

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("module", new Version(1, 0, 0, null));
        module.addSerializer(new DeviceSerializer());
        mapper.registerModule(module);

        topology.acquireReadLock();
        try {
            return mapper.writeValueAsString(topology.getDevices());
        } catch (IOException e) {
            log.error("Error writing device list to JSON", e);
            return "";
        } finally {
            topology.releaseReadLock();
        }
    }

}
