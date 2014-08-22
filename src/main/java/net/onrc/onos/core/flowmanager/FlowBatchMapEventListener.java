package net.onrc.onos.core.flowmanager;

import net.onrc.onos.api.flowmanager.FlowBatchId;
import net.onrc.onos.api.flowmanager.FlowBatchOperation;
import net.onrc.onos.api.flowmanager.FlowBatchState;

interface FlowBatchMapEventListener {
    /**
     * Invoked when a {@link FlowBatchOperation} object is added.
     *
     * @param id the ID of the {@link FlowBatchOperation}.
     * @param flowOp the {@link FlowBatchOperation} object.
     */
    void flowBatchOperationAdded(FlowBatchId id, FlowBatchOperation flowOp);

    /**
     * Invoked when a {@link FlowBatchOperation} object is removed.
     *
     * @param id the ID of the {@link FlowBatchOperation}.
     */
    void flowBatchOperationRemoved(FlowBatchId id);

    /**
     * Invoked when a {@link FlowBatchState} of a {@link FlowBatchOperation}
     * object is changed.
     *
     * @param id the ID of the {@link FlowBatchOperation}
     * @param oldState the old state of the {@link FlowBatchOperation}
     * @param currentState the current state of the {@link FlowBatchOperation}
     */
    void flowStateChanged(FlowBatchId id,
            FlowBatchState oldState, FlowBatchState currentState);

}
