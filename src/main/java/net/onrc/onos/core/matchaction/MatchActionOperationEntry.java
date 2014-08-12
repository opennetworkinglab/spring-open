package net.onrc.onos.core.matchaction;

import net.onrc.onos.api.batchoperation.BatchOperationEntry;

public final class MatchActionOperationEntry
       extends BatchOperationEntry<MatchActionOperations.Operator, MatchAction> {

    /**
     * Default constructor for serializer.
     */
    @SuppressWarnings("unused")
    @Deprecated
    protected MatchActionOperationEntry() {
        super(null, null);
    }

    /**
     * Constructs new instance for the entry of the BatchOperation.
     *
     * @param operator the operator of this operation
     * @param target the target object of this operation
     */
    public MatchActionOperationEntry(final MatchActionOperations.Operator operator,
                                     final MatchAction target) {
        super(operator, target);
    }
}
