package net.onrc.onos.api.batchoperation;

public class StringOperationTargetId extends BatchOperationTargetId {
    private final String id;

    public StringOperationTargetId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StringOperationTargetId) {
            return id.equals(obj.toString());
        } else {
            return false;
        }
    }
}
