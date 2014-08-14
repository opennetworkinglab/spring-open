package net.onrc.onos.core.topology;

import net.onrc.onos.api.batchoperation.BatchOperation;
import net.onrc.onos.api.batchoperation.BatchOperationEntry;

/**
 * A list of topology operations.
 */
public class TopologyBatchOperation extends
        BatchOperation<BatchOperationEntry<TopologyBatchOperation.Operator, ?>> {
    /**
     * The topology operations' operators.
     */
    public enum Operator {
        /**
         * Adds a new topology event.
         */
        ADD,

        /**
         * Removes an existing topology event.
         */
        REMOVE,
    }

    /**
     * Adds an add-TopologyEvent operation.
     *
     * @param topologyEvent the Topology Event to be added
     * @return the TopologyBatchOperation object if succeeded, null otherwise
     */
    public TopologyBatchOperation addAddTopologyOperation(
                                        TopologyEvent topologyEvent) {
        return (null == super.addOperation(
                new BatchOperationEntry<Operator, TopologyEvent>(
                                        Operator.ADD, topologyEvent)))
                ? null : this;
    }

    /**
     * Adds a remove-TopologyEvent operation.
     *
     * @param topologyEvent the Topology Event to be removed
     * @return the TopologyBatchOperation object if succeeded, null otherwise
     */
    public TopologyBatchOperation addRemoveTopologyOperation(
                                        TopologyEvent topologyEvent) {
        return (null == super.addOperation(
                new BatchOperationEntry<Operator, TopologyEvent>(
                                        Operator.REMOVE, topologyEvent)))
                ? null : this;
    }
}
