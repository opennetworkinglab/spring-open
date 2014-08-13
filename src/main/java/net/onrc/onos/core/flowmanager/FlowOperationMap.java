package net.onrc.onos.core.flowmanager;

import net.onrc.onos.api.flowmanager.FlowBatchHandle;
import net.onrc.onos.api.flowmanager.FlowBatchId;
import net.onrc.onos.api.flowmanager.FlowBatchOperation;
import net.onrc.onos.api.flowmanager.FlowBatchState;

/**
 * Manages the set of flow operations throughout the ONOS instances.
 */
public class FlowOperationMap {
    public FlowBatchHandle putOperation(FlowBatchOperation ops) {
        FlowBatchId id = getUniqueBatchOperationId();
        if (id == null) {
            return null;
        }
        if (putBatchOperation(id, ops)) {
            return null;
        }

        return new FlowBatchHandle(this, id);
    }

    public void setState(long id, FlowBatchState state) {
        // TODO implement it
    }

    public FlowBatchOperation getOperation(long id) {
        // TODO implement it
        return null;
    }

    public FlowBatchState getState(FlowBatchId id) {
        // TODO implement it
        return null;
    }

    // ====== private methods

    private FlowBatchId getUniqueBatchOperationId() {
        // TODO implement it
        return null;
    }

    private boolean putBatchOperation(FlowBatchId id, FlowBatchOperation ops) {
        // TODO implement it
        return false;
    }
}
