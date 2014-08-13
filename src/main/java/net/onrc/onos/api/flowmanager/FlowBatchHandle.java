package net.onrc.onos.api.flowmanager;

import net.onrc.onos.core.flowmanager.FlowOperationMap;


/**
 * Handle class to handle flow batch operation.
 */
public class FlowBatchHandle {
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
    public FlowBatchHandle(FlowOperationMap opMap, FlowBatchId id) {
        flowOperationMap = opMap;
        batchId = id;
    }

    /**
     * Gets the flow batch operation ID.
     *
     * @return the flow batch operation ID
     */
    public FlowBatchId getBatchOperationId() {
        return batchId;
    }

    public FlowBatchState getState() {
        return flowOperationMap.getState(batchId);
    }
}
