package net.onrc.onos.core.matchaction;

import net.onrc.onos.api.batchoperation.BatchOperation;

public final class MatchActionOperations
        extends BatchOperation<MatchActionOperationEntry> {
    /**
     * The MatchAction operators.
     */
    public enum Operator {
        ADD,
        REMOVE,
    }

    // TODO waiting on updated BatchOperation as of 8/7
}
