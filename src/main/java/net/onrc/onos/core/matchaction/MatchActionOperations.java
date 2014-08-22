package net.onrc.onos.core.matchaction;

import net.onrc.onos.api.batchoperation.BatchOperation;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The MatchActionOperations class holds a list of MatchActionOperationEntry
 * objects to be executed together as one set.
 */
public class MatchActionOperations
        extends BatchOperation<MatchActionOperationEntry> {

    private final MatchActionOperationsId id;
    private MatchActionOperationsState state;
    private final Set<MatchActionOperationsId> dependencies;

    /**
     * The MatchAction operators.
     */
    public enum Operator {
        ADD,
        REMOVE,
    }

    /**
     * Constructs a MatchActionOperations object from an id.  Internal
     * constructor called by a public factory method.
     *
     * @param newId match action operations identifier for this instance
     */
    public MatchActionOperations(final MatchActionOperationsId newId) {
        id = checkNotNull(newId);
        state = MatchActionOperationsState.INIT;
        dependencies = new HashSet<>();
    }

    /**
     * Gets the identifier for the Match Action Operations object.
     *
     * @return identifier for the Opertions object
     */
    public MatchActionOperationsId getOperationsId() {
        return id;
    }

    /**
     * Gets the state of the Match Action Operations.
     *
     * @return state of the operations
     */
    public MatchActionOperationsState getState() {
        return state;
    }

    /**
     * Sets the state of the Match Action Operations.
     *
     * @param newState new state of the operations
     */
    public void setState(final MatchActionOperationsState newState) {
        state = newState;
    }

    /**
     * Gets the set of IDs of operations that are dependent on this
     * operation.
     *
     * @return set of operations IDs of dependent operations
     */
    public Set<MatchActionOperationsId> getDependencies() {
        return dependencies;
    }

    public void addDependency(MatchActionOperationsId dependentOperationId) {
        dependencies.add(dependentOperationId);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MatchActionOperations) {
            final MatchActionOperations other = (MatchActionOperations) obj;
            return (id.equals(other.id));
        }
        return false;
    }
}
