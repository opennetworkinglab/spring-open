package net.onrc.onos.api.batchoperation;

/**
 * An abstract class to represent ID of the batch operation target.
 * <p>
 * The sub-classes must implement equals() and hashCode() methods so that
 * instance of this interface could be used as Map keys.
 */
public abstract class BatchOperationTargetId {
    /**
     * Returns a string representation of the target object's ID.
     *
     * @return a string representation of the target object's ID.
     */
    @Override
    public abstract String toString();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);
}
