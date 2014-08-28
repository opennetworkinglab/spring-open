package net.onrc.onos.core.flowmanager;

import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;

import java.util.Arrays;
import java.util.List;

import net.onrc.onos.api.batchoperation.BatchOperationEntry;
import net.onrc.onos.api.flowmanager.Flow;
import net.onrc.onos.api.flowmanager.FlowBatchId;
import net.onrc.onos.api.flowmanager.FlowBatchOperation;
import net.onrc.onos.api.flowmanager.FlowBatchOperation.Operator;
import net.onrc.onos.api.flowmanager.FlowBatchState;
import net.onrc.onos.core.matchaction.MatchActionId;
import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.matchaction.MatchActionOperations;
import net.onrc.onos.core.matchaction.MatchActionOperationsId;
import net.onrc.onos.core.matchaction.MatchActionService;
import net.onrc.onos.core.util.IdGenerator;

/**
 * Executes {@link FlowBatchOperation}.
 * <p>
 * This class compiles pairs of {@link Flow} and ADD/REMOVE operation to a list
 * of a set of match-action operations.
 */
class FlowBatchOperationExecutor implements FlowBatchMapEventListener {
    private final FlowBatchMap flowBatchMap;
    // TODO: ADD later: private final FlowMap flowMap;
    private final MatchActionService matchActionService;
    private IdGenerator<MatchActionId> maIdGenerator;
    private IdGenerator<MatchActionOperationsId> maoIdGenerator;

    /**
     * Creates {@link FlowBatchOperationExecutor} instance.
     */
    public FlowBatchOperationExecutor(MatchActionService matchActionService,
            FlowMap flowMap, FlowBatchMap flowBatchMap) {
        this.flowBatchMap = flowBatchMap;
        // TODO: Add later: this.flowMap = flowMap;
        this.matchActionService = matchActionService;
        this.maIdGenerator = matchActionService.getMatchActionIdGenerator();
        this.maoIdGenerator = matchActionService.getMatchActionOperationsIdGenerator();
    }

    /**
     * Generates the series of MatchActionOperations from the
     * {@link FlowBatchOperation}.
     * <p>
     * FIXME: Currently supporting ADD operations only.
     * <p>
     * FIXME: Currently supporting PacketPathFlow and SingleDstTreeFlow only.
     * <p>
     * FIXME: MatchActionOperations should have dependency field to the other
     * match action operations, and this method should use this.
     *
     * @param op the {@link FlowBatchOperation} object
     * @return the list of {@link MatchActionOperations} objects
     */
    private List<MatchActionOperations>
            generateMatchActionOperationsList(FlowBatchOperation op) {

        // MatchAction operations at head (ingress) switches.
        MatchActionOperations headOps =
                new MatchActionOperations(maoIdGenerator.getNewId());

        // MatchAction operations at rest of the switches.
        MatchActionOperations tailOps =
                new MatchActionOperations(maoIdGenerator.getNewId());

        for (BatchOperationEntry<Operator, ?> e : op.getOperations()) {

            // Check if it includes unsupported operations
            if (e.getOperator() != FlowBatchOperation.Operator.ADD) {
                throw new UnsupportedOperationException(
                        "FlowManager supports ADD operations only.");
            }
            if (!(e.getTarget() instanceof Flow)) {
                throw new IllegalStateException(
                        "The target is not Flow object: " + e.getTarget());
            }

            // Compile flows to match-actions
            Flow flow = (Flow) e.getTarget();
            List<MatchActionOperations> maOps = flow.compile(
                    e.getOperator(), maIdGenerator, maoIdGenerator);
            verifyNotNull(maOps, "Could not compile the flow: " + flow);
            verify(maOps.size() == 2,
                    "The flow generates unspported match-action operations.");

            // Merge match-action operations
            for (MatchActionOperationEntry mae : maOps.get(0).getOperations()) {
                verify(mae.getOperator() == MatchActionOperations.Operator.ADD);
                tailOps.addOperation(mae);
            }
            for (MatchActionOperationEntry mae : maOps.get(1).getOperations()) {
                verify(mae.getOperator() == MatchActionOperations.Operator.ADD);
                headOps.addOperation(mae);
            }
        }

        return Arrays.asList(tailOps, headOps);
    }

    @Override
    public void flowBatchOperationAdded(FlowBatchId id, FlowBatchOperation flowOp) {

        if (flowBatchMap.isLocal(id)) {
            // TODO: update flowMap based on flowOp.

            List<MatchActionOperations> maOps = generateMatchActionOperationsList(flowOp);
            for (MatchActionOperations maOp : maOps) {
                matchActionService.executeOperations(maOp);
            }
        }
    }

    @Override
    public void flowBatchOperationRemoved(FlowBatchId id) {
        // not used.
    }

    @Override
    public void flowBatchOperationStateChanged(FlowBatchId id, FlowBatchState oldState,
            FlowBatchState currentState) {
        // TODO: update flow states in flowMap.
        // TODO: update flow batch operation states in flowBatchMap.
    }

    /**
     * Starts executor.
     */
    public void start() {
        flowBatchMap.addListener(this);
    }

    /**
     * Stops executor.
     */
    public void stop() {
        flowBatchMap.removeListener(this);
    }
}
