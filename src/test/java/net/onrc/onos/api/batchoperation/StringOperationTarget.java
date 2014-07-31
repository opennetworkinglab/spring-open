package net.onrc.onos.api.batchoperation;

public class StringOperationTarget implements IBatchOperationTarget {
    private StringOperationTargetId id;

    public StringOperationTarget(StringOperationTargetId id) {
        this.id = id;
    }

    public StringOperationTarget(String id) {
        this.id = new StringOperationTargetId(id);
    }

    @Override
    public StringOperationTargetId getId() {
        return id;
    }
}
