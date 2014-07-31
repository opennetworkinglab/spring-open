package net.onrc.onos.core.matchaction;

import net.onrc.onos.api.batchoperation.BatchOperation;
import net.onrc.onos.api.batchoperation.BatchOperationEntry;

public class MatchActionPhase extends
        BatchOperation<BatchOperationEntry<MatchActionPhase.Operator, ?>> {
    /**
     * The MatchAction operators.
     */
    public enum Operator {
        ADD,
        REMOVE,
    }

    /**
     * Adds an add-operation.
     *
     * @param matchAction match-action object to be added
     * @return the MatchActionPhase object if succeeded, null otherwise
     */
    public MatchActionPhase addAddOperation(MatchAction matchAction) {
        return (null == super.addOperation(
                new BatchOperationEntry<Operator, MatchAction>(
                        Operator.ADD, matchAction)))
                ? null : this;
    }

    /**
     * Adds a remove-operation.
     *
     * @param id the ID of the match-action object to be removed
     * @return the MatchActionPhase object if succeeded, null otherwise
     */
    public MatchActionPhase addRemoveOperation(MatchActionId id) {
        return (null == super.addOperation(
                new BatchOperationEntry<Operator, MatchActionId>(
                        Operator.REMOVE, id)))
                ? null : this;
    }

}
