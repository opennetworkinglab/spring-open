package net.onrc.onos.api.flowmanager;


/**
 * Represents the state of {@link FlowBatchOperation}.
 */
public enum FlowBatchState {
    /**
     * The operation has been submitted, but the FlowManager is not executing
     * yet.
     */
    SUBMITTED,

    /**
     * The FlowManager is executing the operation, but not completed yet.
     */
    EXECUTING,

    /**
     * The operation has been executed successfully.
     */
    COMPLETED,

    /**
     * The operation has been failed to be submitted or executed.
     */
    FAILED,
}
