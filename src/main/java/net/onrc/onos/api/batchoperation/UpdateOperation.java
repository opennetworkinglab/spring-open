package net.onrc.onos.api.batchoperation;

/**
 * An update-operation entry of a batch operation.
 */
public class UpdateOperation implements BatchOperationEntry {
    private final BatchOperationTargetId oldTargetId;
    private final IBatchOperationTarget newTarget;

    /**
     * Creates a update-operation with specified targets.
     *
     * @param oldTargetId The ID to be overwritten.
     * @param newTarget The new target to be added.
     */
    public UpdateOperation(BatchOperationTargetId oldTargetId, IBatchOperationTarget newTarget) {
        this.oldTargetId = oldTargetId;
        this.newTarget = newTarget;
    }

    @Override
    public BatchOperator getOperator() {
        return BatchOperator.UPDATE;
    }

    /**
     * Gets the old target ID to be overwritten.
     *
     * @return The old target ID to be overwritten
     */
    public BatchOperationTargetId getOldTargetId() {
        return oldTargetId;
    }

    /**
     * Gets the new target object to be added.
     *
     * @return The new target object to be added.
     */
    public IBatchOperationTarget getNewTarget() {
        return newTarget;
    }

}
