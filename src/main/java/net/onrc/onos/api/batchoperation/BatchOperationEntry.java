package net.onrc.onos.api.batchoperation;

/**
 * An interface for batch operation entry classes.
 * <p>
 * This is the interface to AddOperation, UpdateOperation and RemoveOperation
 * classes which are the entry maintained by BatchOperation.
 */
public interface BatchOperationEntry {
    /**
     * Gets the BatchOperator of this operation.
     *
     * @return The BatchOperator of this operation.
     */
    public BatchOperator getOperator();
}
