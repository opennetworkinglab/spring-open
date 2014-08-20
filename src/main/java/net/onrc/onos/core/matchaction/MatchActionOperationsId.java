package net.onrc.onos.core.matchaction;

import java.util.UUID;

/**
 * Identifier for a MatchActionOperations object.  This is an immutable class
 * that encapsulates a globally unique identifier for the MatchActionOperations
 * object.
 */
public final class MatchActionOperationsId {

    private static final String OPERATIONS_ID_PREFIX = "MatchActionOperationsId-";
    private final String id;

    /**
     * Constructs an Operations identifier and allocates a unique identifier
     * for it.
     */
    private MatchActionOperationsId() {
        id = OPERATIONS_ID_PREFIX + UUID.randomUUID();
    }

    /**
     * Gets the identifier for the Operations object.
     *
     * @return Operations object identifier as a string
     */
    public String getId() {
        return id;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof MatchActionOperationsId)) {
            return false;
        }

        final MatchActionOperationsId otherMatchActionOperationsId =
                (MatchActionOperationsId) other;

        return otherMatchActionOperationsId.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Creates a new Id for a MatchActionOperation.
     *
     * @return new Id for a MatchActionOperation
     */
    public static MatchActionOperationsId createNewOperationsId() {
        return new MatchActionOperationsId();
    }

}
