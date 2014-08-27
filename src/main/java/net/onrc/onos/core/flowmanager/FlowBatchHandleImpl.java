package net.onrc.onos.core.flowmanager;


import  static com.google.common.base.Preconditions.*;

import net.onrc.onos.api.flowmanager.FlowBatchHandle;
import net.onrc.onos.api.flowmanager.FlowBatchId;
import net.onrc.onos.api.flowmanager.FlowBatchOperation;
import net.onrc.onos.api.flowmanager.FlowBatchState;

public class FlowBatchHandleImpl implements FlowBatchHandle {
    private final FlowBatchMap flowBatchMap;
    private final FlowBatchId batchId;

    /**
     * Creates a handle using batch operation ID.
     * <p>
     * The ID is automatically generated and assigned by FlowManager, and used
     * as an internal key for the flow batch operation map.
     *
     * @param map the {@link FlowBatchMap} object which maintains the flow batch
     *        operation
     * @param id the Id for this batch operation
     */
    public FlowBatchHandleImpl(FlowBatchMap map, FlowBatchId id) {
        flowBatchMap = map;
        batchId = id;
    }

    @Override
    public FlowBatchOperation getFlowBatchOperation() {
        FlowBatchOperation op = checkNotNull(flowBatchMap.get(batchId),
                "The requested flow batch operation does not exist in the map.");

        // TODO: should be an instance of immutable batch operation class.
        return new FlowBatchOperation(op.getOperations());
    }

    @Override
    public FlowBatchState getState() {
        return flowBatchMap.getState(batchId);
    }

    @Override
    public void purge() {
        FlowBatchState state = getState();
        if (state == FlowBatchState.COMPLETED || state == FlowBatchState.FAILED) {
            flowBatchMap.remove(batchId);
        }
    }
}
