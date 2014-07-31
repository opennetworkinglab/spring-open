package net.onrc.onos.api.flowmanager;

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
     * Adds an add-flow operation.
     *
     * @param flow the flow to be added
     * @return the FlowBatchOperation object if succeeded, null otherwise
     */
    public FlowBatchOperation addAddFlowOperation(IFlow flow) {
        return (null == super.addOperation(
                new BatchOperationEntry<Operator, IFlow>(Operator.ADD, flow)))
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
