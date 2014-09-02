package net.onrc.onos.api.flowmanager;

import java.util.Objects;

import javax.annotation.concurrent.Immutable;

/**
 * Represents ID for {@link FlowBatchOperation}.
 * <p>
 * This class is immutable.
 */
@Immutable
public final class FlowBatchId {
    private static final int DEC = 10;
    private static final int HEX = 16;

    private final long id;

    /**
     * Creates a flow batch identifier from the specified string
     * representation.
     *
     * @param value long value
     * @return flow batch identifier
     */
    public static FlowBatchId valueOf(String value) {
        long id = value.toLowerCase().startsWith("0x")
                ? Long.parseLong(value.substring(2), HEX)
                : Long.parseLong(value, DEC);
        return new FlowBatchId(id);
    }

    /**
     * Creates a new FlowBatchId object using long value.
     */
    public FlowBatchId(long id) {
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
        return (obj instanceof FlowBatchId) ? id == ((FlowBatchId) obj).id : false;
    }
}
