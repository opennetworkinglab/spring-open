package net.onrc.onos.api.batchoperation;

/**
 * An add-operation entry of a batch operation.
 */
public class AddOperation implements BatchOperationEntry {
    private final IBatchOperationTarget target;

    /**
     * Creates a add-operation with specified target.
     *
     * @param target The target object to be assigned to this add-operation.
     */
    public AddOperation(IBatchOperationTarget target) {
        this.target = target;
    }

    @Override
    public BatchOperator getOperator() {
        return BatchOperator.ADD;
    }

    /**
     * Gets the target object which assigned to this add-operation.
     *
     * @return The target object which assigned to this add-operation.
     */
    public IBatchOperationTarget getTarget() {
        return target;
    }
}
