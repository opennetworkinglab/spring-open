package net.onrc.onos.api.flowmanager;

/**
 * An interface for handling flow batch operation.
 */
public interface FlowBatchHandle {
    /**
     * Gets the flow batch operation.
     *
     * @return the flow batch operation
     */
    public FlowBatchOperation getFlowBatchOperation();

    /**
     * Gets the state for the flow batch operation.
     *
     * @return the state for the flow batch operation
     */
    public FlowBatchState getState();

    /**
     * Purge the flow batch operation from the map.
     */
    public void purge();
}
