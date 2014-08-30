package net.onrc.onos.core.topology;

import net.onrc.onos.api.batchoperation.BatchOperationTarget;

// Note: We may not need this if we decide to use TopologyEvent as base class
// for all the TopologyBatchOperation targets.
/**
 * Tag interface for TopologyBatchOperation targets.
 */
public interface TopologyBatchTarget extends BatchOperationTarget {

}
