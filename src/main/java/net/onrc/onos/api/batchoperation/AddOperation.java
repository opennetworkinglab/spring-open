package net.onrc.onos.api.batchoperation;

/**
 * An add-operation entry of a batch operation.
 */
public class AddOperation<T extends IBatchOperationTarget>
        implements BatchOperationEntry<T> {
    private final T target;

    /**
     * Creates a add-operation with specified target.
     *
     * @param target the target object to be assigned to this add-operation
     */
    public AddOperation(T target) {
        this.target = target;
    }

    @Override
    public BatchOperator getOperator() {
        return BatchOperator.ADD;
    }

    /**
     * Gets the target object which assigned to this add-operation.
     *
     * @return the target object which assigned to this add-operation
     */
    public T getTarget() {
        return target;
    }
}
