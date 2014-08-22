package net.onrc.onos.api.flowmanager;

import java.util.Objects;

/**
 * Represents ID for {@link FlowBatchOperation}.
 */
public class FlowBatchId {
    private final long id;

    /**
     * Creates a new FlowBatchId object using long value.
     */
    public FlowBatchId(long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return Long.toString(id);
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
