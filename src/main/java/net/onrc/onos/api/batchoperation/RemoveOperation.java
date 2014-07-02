package net.onrc.onos.api.batchoperation;

/**
 * A remove-operation entry of a batch operation.
 */
public class RemoveOperation implements BatchOperationEntry {
    protected String targetId;

    /**
     * Creates a remove-operation with specified target.
     *
     * @param targetId The target object ID to be assigned to this
     *        remove-operation.
     */
    public RemoveOperation(String targetId) {
        this.targetId = targetId;
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
    public String getTargetId() {
        return targetId;
    }
}
