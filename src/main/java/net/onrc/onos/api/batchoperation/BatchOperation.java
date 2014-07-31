package net.onrc.onos.api.batchoperation;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A list of BatchOperationEntry.
 *
 * @param <T> the enum of operators <br>
 *        This enum must be defined in each sub-classes.
 *
 */
public abstract class BatchOperation<T extends BatchOperationEntry<?, ?>> {
    private List<T> ops;

    /**
     * Constructor.
     */
    public BatchOperation() {
        ops = new LinkedList<>();
    }

    /**
     * Removes all operations maintained in this object.
     */
    public void clear() {
        ops.clear();
    }

    /**
     * Returns the number of operations in this object.
     *
     * @return the number of operations in this object
     */
    public int size() {
        return ops.size();
    }

    /**
     * Returns the operations in this object.
     *
     * @return the operations in this object
     */
    public List<T> getOperations() {
        return Collections.unmodifiableList(ops);
    }

    /**
     * Adds an operation.
     *
     * @param entry the operation to be added
     * @return this object if succeeded, null otherwise
     */
    public BatchOperation<T> addOperation(T entry) {
        return ops.add(entry) ? this : null;
    }
}
