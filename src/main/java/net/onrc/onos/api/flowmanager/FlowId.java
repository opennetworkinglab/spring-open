package net.onrc.onos.api.flowmanager;

import java.util.Objects;

import javax.annotation.concurrent.Immutable;

import net.onrc.onos.api.batchoperation.BatchOperationTarget;

/**
 * Represents ID for Flow objects.
 * <p>
 * This class is immutable.
 */
@Immutable
public final class FlowId implements BatchOperationTarget {
    private final long id;

    /**
     * Default constructor for Kryo deserialization.
     */
    @Deprecated
    protected FlowId() {
        id = 0;
    }

    /**
     * Creates new instance with string ID.
     * <p>
     * This FlowId instance should be generated with {@link FlowIdGenerator}.
     *
     * @param id String representation of the ID.
     */
    public FlowId(long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "0x" + Long.toHexString(id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FlowId) {
            FlowId that = (FlowId) obj;
            return Objects.equals(this.id, that.id);
        }
        return false;
    }
}
