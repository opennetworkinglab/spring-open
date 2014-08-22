package net.onrc.onos.core.flowmanager;

import net.onrc.onos.api.flowmanager.FlowBatchHandle;
import net.onrc.onos.api.flowmanager.FlowBatchId;
import net.onrc.onos.api.flowmanager.FlowBatchOperation;
import net.onrc.onos.api.flowmanager.FlowBatchState;
import net.onrc.onos.core.util.IdBlockAllocator;

/**
 * Manages the set of flow operations throughout the ONOS instances.
 * <p>
 * Application can access to this map using {@link FlowBatchHandle}.
 */
public class FlowOperationMap {
    private final FlowBatchIdGeneratorWithIdBlockAllocator flowBatchIdGenerator;

    /**
     * Creates a driver for the shared flow batch operation map.
     *
     * @param allocator {@link IdBlockAllocator} to be used for batch IDs
     */
    FlowOperationMap(IdBlockAllocator allocator) {
        flowBatchIdGenerator = new FlowBatchIdGeneratorWithIdBlockAllocator(allocator);
    }

    /**
     * Adds a flow batch operation to the map.
     *
     * @param ops the flow batch operation to be added
     * @return {@link FlowBatchHandle} handle if succeeded, null otherwise
     */
    FlowBatchHandle putBatchOperation(FlowBatchOperation ops) {
        FlowBatchId id = flowBatchIdGenerator.getNewId();

        // TODO: put batch operation to map

        boolean succeeded = false;

        return succeeded ? new FlowBatchHandleImpl(this, id) : null;
    }

    /**
     * Gets the flow batch operation from the map specified with an ID.
     *
     * @param id the ID for the operation
     * @return the flow batch operation if exists, null otherwise
     */
    FlowBatchOperation getBatchOperation(FlowBatchId id) {
        // TODO: implement it
        return null;
    }

    /**
     * Removes the flow batch operation from the map specified with an ID.
     */
    void removeBatchOperation(FlowBatchId id) {
        // TODO: implement it
    }

    /**
     * Gets the state for the flow batch operation specified with an ID.
     *
     * @param id the ID for the batch operation
     * @return the state of the batch operation
     */
    FlowBatchState getState(FlowBatchId id) {
        // TODO implement it
        return null;
    }

    /**
     * Updates the state for the flow batch operation specified with an ID.
     *
     * @param id the ID for the batch operation
     * @param state new state for the batch operation
     * @return true if succeeded, false otherwise
     */
    boolean setState(FlowBatchId id, FlowBatchState state) {
        // TODO: implement it
        return false;
    }
}
