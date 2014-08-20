package net.onrc.onos.core.matchaction;

import net.onrc.onos.api.batchoperation.BatchOperation;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The MatchActionOperations class holds a list of MatchActionOperationEntry
 * objects to be executed together as one set.
 * <p/>
 * Objects of this class are immutable.
 */
public final class MatchActionOperations
        extends BatchOperation<MatchActionOperationEntry> {

    private final MatchActionOperationsId id;
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
    private MatchActionOperations(final MatchActionOperationsId newId) {
        id = checkNotNull(newId);
    }

    /**
     * Creates a MatchActionOperations object from an id.
     *
     * @param newId match action operations identifier to use for the new object
     * @return Match Action Operations object
     */
    public static MatchActionOperations createMatchActionsOperations(
            final MatchActionOperationsId newId) {
        return new MatchActionOperations(newId);
    }

    /**
     * Gets the identifier for the Match Action Operations object.
     *
     * @return identifier for the Opertions object
     */
    public MatchActionOperationsId getOperationsId() {
        return id;
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
