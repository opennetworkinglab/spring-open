package net.onrc.onos.api.intent;

import net.onrc.onos.api.batchoperation.BatchOperationTargetId;

/**
 * The class representing intent's ID.
 *
 * This class is immutable.
 */
public final class IntentId extends BatchOperationTargetId {
    private final long id;

    /**
     * Constructs the ID corresponding to a given long value.
     *
     * In the future, this constructor will not be exposed to avoid misuses.
     *
     * @param id the underlay value of this ID.
     */
    public IntentId(long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof IntentId)) {
            return false;
        }

        IntentId that = (IntentId) obj;
        return this.id == that.id;
    }

    @Override
    public String toString() {
        return "0x" + Long.toHexString(id);
    }
}
