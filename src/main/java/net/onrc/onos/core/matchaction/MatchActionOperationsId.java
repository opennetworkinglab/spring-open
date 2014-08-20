package net.onrc.onos.core.matchaction;

import java.util.Objects;

/**
 * Identifier for a MatchActionOperations object.  This is an immutable class
 * that encapsulates a globally unique identifier for the MatchActionOperations
 * object.
 */
public final class MatchActionOperationsId {

    private final long id;

    /**
     * Constructs an Operations identifier and from a unique identifier.
     */
    public MatchActionOperationsId(final long newId) {
        id = newId;
    }

    /**
     * Gets the identifier for the Operations object.
     *
     * @return Operations object identifier as a string
     */
    public long getId() {
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

        final MatchActionOperationsId that = (MatchActionOperationsId) other;

        return this.getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
