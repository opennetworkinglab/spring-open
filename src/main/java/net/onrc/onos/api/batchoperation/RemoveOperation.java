package net.onrc.onos.api.batchoperation;

/**
 * A remove-operation entry of a batch operation.
 */
public class RemoveOperation implements BatchOperationEntry {
    private final BatchOperationTargetId targetId;

    /**
     * Creates a remove-operation with specified target.
     *
     * @param id The target object ID to be assigned to this
     *        remove-operation.
     */
    public RemoveOperation(BatchOperationTargetId id) {
        this.targetId = id;
    }

    @Override
    public BatchOperator getOperator() {
        return BatchOperator.REMOVE;
    }

    /**
     * Gets the target ID to be removed.
     *
     * @return The target ID to be removed.
     */
    public BatchOperationTargetId getTargetId() {
        return targetId;
    }
}
