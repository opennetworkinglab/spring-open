package net.onrc.onos.api.batchoperation;

/**
 * A super class for batch operation entry classes.
 * <p>
 * This is the interface to classes which are maintained by BatchOperation as
 * its entries.
 */
public class BatchOperationEntry<T extends Enum<?>, U extends IBatchOperationTarget> {
    private final T operator;
    private final U target;

    /**
     * Constructs new instance for the entry of the BatchOperation.
     *
     * @param operator the operator of this operation
     * @param target the target object of this operation
     */
    public BatchOperationEntry(T operator, U target) {
        this.operator = operator;
        this.target = target;
    }

    /**
     * Gets the target object of this operation.
     *
     * @return the target object of this operation
     */
    public U getTarget() {
        return target;
    }

    /**
     * Gets the operator of this operation.
     *
     * @return the operator of this operation
     */
    public T getOperator() {
        return operator;
    }
}
