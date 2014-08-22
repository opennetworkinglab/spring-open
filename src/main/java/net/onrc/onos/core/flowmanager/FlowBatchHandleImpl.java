package net.onrc.onos.core.flowmanager;


import  static com.google.common.base.Preconditions.*;

import net.onrc.onos.api.flowmanager.FlowBatchHandle;
import net.onrc.onos.api.flowmanager.FlowBatchId;
import net.onrc.onos.api.flowmanager.FlowBatchOperation;
import net.onrc.onos.api.flowmanager.FlowBatchState;

public class FlowBatchHandleImpl implements FlowBatchHandle {
    private final FlowOperationMap flowOperationMap;
    private final FlowBatchId batchId;

    /**
     * Creates a handle using batch operation ID.
     * <p>
     * The ID is automatically generated and assigned by FlowManager, and used
     * as an internal key for the flow batch operation map.
     *
     * @param opMap the FlowOperationMap object which maintains the flow batch
     *        operation
     * @param id the batch operation ID
     */
    public FlowBatchHandleImpl(FlowOperationMap opMap, FlowBatchId id) {
        flowOperationMap = opMap;
        batchId = id;
    }

    @Override
    public FlowBatchOperation getFlowBatchOperation() {
        FlowBatchOperation op = checkNotNull(flowOperationMap.getBatchOperation(batchId),
                "The requested flow batch operation does not exist in the map.");

        // TODO: should be an instance of immutable batch operation class.
        return new FlowBatchOperation(op.getOperations());
    }

    @Override
    public FlowBatchState getState() {
        return flowOperationMap.getState(batchId);
    }

    @Override
    public void purge() {
        FlowBatchState state = getState();
        if (state == FlowBatchState.COMPLETED || state == FlowBatchState.FAILED) {
            flowOperationMap.removeBatchOperation(batchId);
        }
    }
}
