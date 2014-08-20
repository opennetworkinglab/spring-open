package net.onrc.onos.core.topology;

/**
 * Interface for publishing Topology Operations to the Topology Replication
 * Writer.
 */
public interface ITopologyReplicationWriter {
    /**
     * Publishes Topology Batch Operations.
     *
     * @param tbo the Topology Batch Operations to publish
     * @return true on success, otherwise false
     */
    public boolean publish(TopologyBatchOperation tbo);
}
