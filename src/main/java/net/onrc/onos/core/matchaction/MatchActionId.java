package net.onrc.onos.core.matchaction;

import java.util.Objects;

import net.onrc.onos.api.batchoperation.BatchOperationTarget;

/**
 * A unique identifier for a MatchAction.  Objects of this class are immutable.
 */
public final class MatchActionId implements BatchOperationTarget {
    private final long value;

    /**
     * Creates a new Match Action Identifier based on the given id string.
     *
     * @param id unique id string
     */
    public MatchActionId(long id) {
        value = id;
    }

    /**
     * no-arg constructor for Kryo.
     */
    protected MatchActionId() {
        value = -1;
    }

    /**
     * Returns the MatchActionId as a long.
     *
     * @return MatchAction ID
     */
    public long value() {
        return value;
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MatchActionId) {
            final MatchActionId that = (MatchActionId) obj;
            return this.value == that.value;
        }
        return false;
    }

}
