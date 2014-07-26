package net.onrc.onos.api.batchoperation;

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
    private List<BatchOperationEntry> ops;

    /**
     * Constructor.
     */
    public BatchOperation() {
        ops = new LinkedList<BatchOperationEntry>();
    }

    /**
     * Removes all operations maintained in this object.
     */
    public void clear() {
        ops.clear();
    }

    /**
     * Returns an iterator over the operations in this object.
     *
     * @return an iterator over the operations in this object.
     */
    public Iterator<BatchOperationEntry> iterator() {
        return ops.iterator();
    }

    /**
     * Adds an add-operation.
     *
     * @param target IBatchOperationTarget to be added.
     * @return true if succeeded, false otherwise.
     */
    public boolean addAddOperation(T target) {
        return ops.add(new AddOperation(target));
    }

    /**
     * Adds a remove-operation.
     *
     * @param id ID of the target to be removed.
     * @return true if succeeded, false otherwise.
     */
    public boolean addRemoveOperation(BatchOperationTargetId id) {
        return ops.add(new RemoveOperation(id));
    }

    /**
     * Adds a update-operation.
     *
     * @param oldTargetId ID of the existing target to be overwritten.
     * @param newTarget The new target to be added.
     * @return true if succeeded, false otherwise.
     */
    public boolean addUpdateOperation(BatchOperationTargetId oldTargetId, T newTarget) {
        return ops.add(new UpdateOperation(oldTargetId, newTarget));
    }
}
