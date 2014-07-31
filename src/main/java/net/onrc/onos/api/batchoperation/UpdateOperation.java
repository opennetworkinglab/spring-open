package net.onrc.onos.api.batchoperation;

/**
 * An update-operation entry of a batch operation.
 */
public class UpdateOperation implements BatchOperationEntry {
    private final IBatchOperationTarget target;

    /**
     * Creates an update-operation with specified targets.
     *
     * @param target The new target to be used for the update.
     */
    public UpdateOperation(IBatchOperationTarget target) {
        this.target = target;
    }

    @Override
    public BatchOperator getOperator() {
        return BatchOperator.UPDATE;
    }

    /**
     * Gets the new target object to be used for the update.
     *
     * @return The new target object to be used for the update.
     */
    public IBatchOperationTarget getTarget() {
        return target;
    }

}
