package net.onrc.onos.api.intent;

import net.onrc.onos.api.batchoperation.BatchOperationTargetId;

public class IntentId extends BatchOperationTargetId {
    private final String value;

    public IntentId(String id) {
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
        if (obj instanceof IntentId) {
            IntentId other = (IntentId) obj;
            return (this.value.equals(other.value));
        }
        return false;
    }
}
