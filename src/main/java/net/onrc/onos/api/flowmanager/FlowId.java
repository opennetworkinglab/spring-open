package net.onrc.onos.api.flowmanager;

import java.util.Objects;

import net.onrc.onos.api.batchoperation.BatchOperationTarget;

/**
 * Represents ID for Flow objects.
 */
public class FlowId implements BatchOperationTarget {
    private final long value;

    /**
     * Default constructor for Kryo deserialization.
     */
    @Deprecated
    protected FlowId() {
        value = 0;
    }

    /**
     * Creates new instance with string ID.
     * <p>
     * This FlowId instance should be generated with {@link FlowIdGenerator}.
     *
     * @param id String representation of the ID.
     */
    public FlowId(long id) {
        value = id;
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
        if (obj instanceof FlowId) {
            FlowId that = (FlowId) obj;
            return Objects.equals(this.value, that.value);
        }
        return false;
    }
}
