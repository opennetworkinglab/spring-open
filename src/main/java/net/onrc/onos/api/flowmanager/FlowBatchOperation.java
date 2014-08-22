package net.onrc.onos.api.flowmanager;

import java.util.List;

import net.onrc.onos.api.batchoperation.BatchOperation;
import net.onrc.onos.api.batchoperation.BatchOperationEntry;

/**
 * A list of flow operations.
 */
public class FlowBatchOperation extends
        BatchOperation<BatchOperationEntry<FlowBatchOperation.Operator, ?>> {
    /**
     * The flow operations' operators.
     */
    public enum Operator {
        /**
         * Adds new flow.
         */
        ADD,

        /**
         * Removes the existing flow.
         */
        REMOVE,
    }

    /**
     * Creates new {@link FlowBatchOperation} object.
     */
    public FlowBatchOperation() {
        super();
    }

    /**
     * Creates new {@link FlowBatchOperation} object from a list of flow batch
     * operation entries.
     *
     * @param batchOperations the list of flow batch operation entries
     */
    public FlowBatchOperation(
            List<BatchOperationEntry<FlowBatchOperation.Operator, ?>> batchOperations) {
        super(batchOperations);
    }

    /**
     * Adds an add-flow operation.
     *
     * @param flow the flow to be added
     * @return the FlowBatchOperation object if succeeded, null otherwise
     */
    public FlowBatchOperation addAddFlowOperation(Flow flow) {
        return (null == super.addOperation(
                new BatchOperationEntry<Operator, Flow>(Operator.ADD, flow)))
                ? null : this;
    }

    /**
     * Adds a remove-flow operation.
     *
     * @param flowId the ID of flow to be removed
     * @return the FlowBatchOperation object if succeeded, null otherwise
     */
    public FlowBatchOperation addRemoveFlowOperation(FlowId flowId) {
        return (null == super.addOperation(
                new BatchOperationEntry<Operator, FlowId>(Operator.REMOVE, flowId)))
                ? null : this;
    }
}
