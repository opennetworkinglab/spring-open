package net.onrc.onos.api.batchoperation;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A list of BatchOperationEntry.
 *
 * @param <T> IBatchOperationTarget. This should be Intent, IFlow, or
 *        MatchAction.
 */
public class BatchOperation<T extends IBatchOperationTarget> {
    private List<BatchOperationEntry<T>> ops;

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
     * Returns an iterator over the operations in this object.
     *
     * @return an iterator over the operations in this object
     */
    public Iterator<BatchOperationEntry<T>> iterator() {
        return ops.iterator();
    }

    /**
     * Returns the operations in this object.
     *
     * @return the operations in this object
     */
    public List<BatchOperationEntry<T>> getOperations() {
        return Collections.unmodifiableList(ops);
    }

    /**
     * Adds an add-operation.
     *
     * @param target IBatchOperationTarget object to be added
     * @return true if succeeded, false otherwise
     */
    public boolean addAddOperation(T target) {
        return ops.add(new AddOperation<T>(target));
    }

    /**
     * Adds a remove-operation.
     *
     * @param id ID of the target to be removed
     * @return true if succeeded, false otherwise
     */
    public boolean addRemoveOperation(BatchOperationTargetId id) {
        return ops.add(new RemoveOperation<T>(id));
    }
}
