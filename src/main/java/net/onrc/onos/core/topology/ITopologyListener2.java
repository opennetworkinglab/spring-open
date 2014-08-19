package net.onrc.onos.core.topology;

import java.util.List;

import net.onrc.onos.api.batchoperation.BatchOperationEntry;
import net.onrc.onos.core.topology.TopologyBatchOperation.Operator;

// FIXME replace ITopologyListener
/**
 * Topology event listener interface.
 */
public interface ITopologyListener2 {

    /**
     * Topology event handler.
     *
     * @param updated Topology after applying changes
     * @param changes topology events
     */
    public void topologyEvent(ImmutableTopology updated,
            List<BatchOperationEntry<Operator, TopologyBatchTarget>> changes);
}
