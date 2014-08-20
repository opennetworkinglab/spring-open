package net.onrc.onos.core.matchaction;

import net.onrc.onos.api.batchoperation.BatchOperationTarget;

/**
 * A unique identifier for a MatchAction.  Objects of this class are immutable.
 */
public final class MatchActionId implements BatchOperationTarget {
    private final String value;

    /**
     * Creates a new Match Action Identifier based on the given id string.
     *
     * @param id unique id string
     */
    public MatchActionId(String id) {
        value = id;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MatchActionId) {
            MatchActionId other = (MatchActionId) obj;
            return (value.equals(other.value));
        }
        return false;
    }

}
