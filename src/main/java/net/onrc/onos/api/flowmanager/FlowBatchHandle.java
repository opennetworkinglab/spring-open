package net.onrc.onos.api.flowmanager;

/**
 * Handle class to handle flow batch operation.
 */
public class FlowBatchHandle {
    private final FlowBatchId batchId;

    /**
     * Creates a handle using batch operation ID.
     * <p>
     * The ID is automatically generated and assigned by FlowManager, and used
     * as an internal key for the flow batch operation map.
     *
     * @param id the batch operation ID
     */
    public FlowBatchHandle(FlowBatchId id) {
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
}
