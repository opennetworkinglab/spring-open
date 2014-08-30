package net.onrc.onos.core.topology;

import net.onrc.onos.api.batchoperation.BatchOperation;
import net.onrc.onos.api.batchoperation.BatchOperationEntry;

/**
 * A list of topology operations.
 */
public class TopologyBatchOperation extends
        BatchOperation<BatchOperationEntry<TopologyBatchOperation.Operator, TopologyEvent>> {

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
     * Appends an ADD-TopologyEvent operation.
     *
     * @param topologyEvent the Topology Event to be appended
     * @return the TopologyBatchOperation object
     */
    public TopologyBatchOperation appendAddOperation(
                                        TopologyEvent topologyEvent) {
        return (TopologyBatchOperation) addOperation(
                new BatchOperationEntry<Operator, TopologyEvent>(
                                        Operator.ADD, topologyEvent));
    }

    /**
     * Appends a REMOVE-TopologyEvent operation.
     *
     * @param topologyEvent the Topology Event to be appended
     * @return the TopologyBatchOperation object
     */
    public TopologyBatchOperation appendRemoveOperation(
                                        TopologyEvent topologyEvent) {
        return (TopologyBatchOperation) addOperation(
                new BatchOperationEntry<Operator, TopologyEvent>(
                                        Operator.REMOVE, topologyEvent));
    }

    /**
     * Concatenates specified TopologyBatchOperation.
     *
     * @param ops TopologyBatchOperation to concatenate.
     * @return this
     */
    public TopologyBatchOperation concat(final TopologyBatchOperation ops) {
        for (BatchOperationEntry<Operator, TopologyEvent> e : ops.getOperations()) {
            this.addOperation(e);
        }
        return this;
    }
}
