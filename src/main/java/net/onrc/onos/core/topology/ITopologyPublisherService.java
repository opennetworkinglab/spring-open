package net.onrc.onos.core.topology;

import net.floodlightcontroller.core.module.IFloodlightService;

/**
 * Interface for providing the topology publisher service to other modules.
 */
public interface ITopologyPublisherService extends IFloodlightService {
    /**
     * Publishes Topology Batch Operations.
     *
     * @param tbo the Topology Batch Operations to publish
     * @return true on success, otherwise false
     */
    public boolean publish(TopologyBatchOperation tbo);
}
