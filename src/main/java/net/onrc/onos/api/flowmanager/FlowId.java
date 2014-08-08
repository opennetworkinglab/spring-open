package net.onrc.onos.api.flowmanager;

import net.onrc.onos.api.batchoperation.BatchOperationTarget;

/**
 * Represents ID for IFlow objects.
 */
public class FlowId implements BatchOperationTarget {
    private final String value;

    /**
     * Creates new instance with string ID.
     *
     * @param id String representation of the ID.
     */
    public FlowId(String id) {
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
        if (obj instanceof FlowId) {
            FlowId other = (FlowId) obj;
            return (this.value.equals(other.value));
        }
        return false;
    }
}
