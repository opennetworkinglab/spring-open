package net.onrc.onos.core.flowmanager;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.onrc.onos.api.batchoperation.BatchOperationEntry;
import net.onrc.onos.api.flowmanager.ConflictDetectionPolicy;
import net.onrc.onos.api.flowmanager.Flow;
import net.onrc.onos.api.flowmanager.FlowBatchHandle;
import net.onrc.onos.api.flowmanager.FlowBatchOperation;
import net.onrc.onos.api.flowmanager.FlowBatchOperation.Operator;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowIdGenerator;
import net.onrc.onos.api.flowmanager.FlowManagerListener;
import net.onrc.onos.api.flowmanager.FlowManagerService;
import net.onrc.onos.core.matchaction.MatchActionIdGeneratorWithIdBlockAllocator;
import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.matchaction.MatchActionOperations;
import net.onrc.onos.core.matchaction.MatchActionOperationsIdGeneratorWithIdBlockAllocator;
import net.onrc.onos.core.util.IdBlockAllocator;

/**
 * Manages a set of Flow objects, computes and maintains a set of Match-Action
 * entries based on the Flow objects, and executes Match-Action plans.
 * <p>
 * TODO: Make all methods thread-safe
 */
public class FlowManagerModule implements FlowManagerService {
    private ConflictDetectionPolicy conflictDetectionPolicy;
    private FlowOperationMap flowOperationMap;
    private MatchActionIdGeneratorWithIdBlockAllocator maIdGenerator;
    private MatchActionOperationsIdGeneratorWithIdBlockAllocator maoIdGenerator;
    private FlowIdGeneratorWithIdBlockAllocator flowIdGenerator;

    /**
     * Constructs FlowManagerModule with {@link IdBlockAllocator}.
     */
    public FlowManagerModule(IdBlockAllocator idBlockAllocator) {
        this.flowIdGenerator =
                new FlowIdGeneratorWithIdBlockAllocator(idBlockAllocator);
        this.conflictDetectionPolicy = ConflictDetectionPolicy.FREE;
        this.flowOperationMap = new FlowOperationMap(idBlockAllocator);

        // TODO: MatchActionOperationsIdGenerator should be retrieved from MatchAction Module.
        this.maIdGenerator =
                new MatchActionIdGeneratorWithIdBlockAllocator(idBlockAllocator);
        this.maoIdGenerator =
                new MatchActionOperationsIdGeneratorWithIdBlockAllocator(idBlockAllocator);
    }

    @Override
    public FlowBatchHandle addFlow(Flow flow) {
        FlowBatchOperation ops = new FlowBatchOperation();
        ops.addAddFlowOperation(flow);
        return executeBatch(ops);
    }

    @Override
    public FlowBatchHandle removeFlow(FlowId id) {
        FlowBatchOperation ops = new FlowBatchOperation();
        ops.addRemoveFlowOperation(id);
        return executeBatch(ops);
    }

    @Override
    public Flow getFlow(FlowId id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Flow> getFlows() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Executes batch operation of Flow object asynchronously.
     * <p>
     * To track the execution result, use the returned FlowBatchHandle object.
     * <p>
     * This method just put the batch-operation object to the global flow
     * operation map with unique ID. The worker process for execution and
     * installation will get the appended operation when it gets events from the
     * map. This method returns a handler for obtaining the result of this
     * operation, control the executing process, etc.
     *
     * @param ops flow operations to be executed
     * @return FlowBatchHandle object if succeeded, null otherwise
     */
    @Override
    public FlowBatchHandle executeBatch(FlowBatchOperation ops) {
        return flowOperationMap.putBatchOperation(ops);
    }

    @Override
    public FlowIdGenerator getFlowIdGenerator() {
        return flowIdGenerator;
    }

    @Override
    public void setConflictDetectionPolicy(ConflictDetectionPolicy policy) {
        if (policy == ConflictDetectionPolicy.FREE) {
            conflictDetectionPolicy = policy;
        } else {
            throw new UnsupportedOperationException(
                    policy.toString() + " is not supported.");
        }
    }

    @Override
    public ConflictDetectionPolicy getConflictDetectionPolicy() {
        return conflictDetectionPolicy;
    }

    @Override
    public void addListener(FlowManagerListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeListener(FlowManagerListener listener) {
        // TODO Auto-generated method stub

    }

    private MatchActionOperations createNewMatchActionOperations() {
        return new MatchActionOperations(maoIdGenerator.getNewId());
    }

    /**
     * Generates the series of MatchActionOperations from the
     * {@link FlowBatchOperation}.
     * <p>
     * Note: Currently supporting ADD operations only.
     * <p>
     * Note: Currently supporting PacketPathFlow and SingleDstTreeFlow only.
     *
     * @param op the {@link FlowBatchOperation} object
     * @return the list of {@link MatchActionOperations} objects
     */
    private List<MatchActionOperations>
            generateMatchActionOperationsList(FlowBatchOperation op) {
        MatchActionOperations firstOps = createNewMatchActionOperations();
        MatchActionOperations secondOps = createNewMatchActionOperations();

        for (BatchOperationEntry<Operator, ?> e : op.getOperations()) {
            if (e.getOperator() != FlowBatchOperation.Operator.ADD) {
                throw new UnsupportedOperationException(
                        "FlowManager supports ADD operations only.");
            }
            if (!(e.getTarget() instanceof Flow)) {
                throw new IllegalStateException(
                        "The target is not Flow object: " + e.getTarget());
            }

            Flow flow = (Flow) e.getTarget();
            List<MatchActionOperations> maOps = flow.compile(
                    e.getOperator(), maIdGenerator, maoIdGenerator);
            checkNotNull(maOps, "Could not compile the flow: " + flow);
            checkState(maOps.size() == 2,
                    "The flow generates unspported match-action operations.");

            for (MatchActionOperationEntry mae : maOps.get(0).getOperations()) {
                checkState(mae.getOperator() == MatchActionOperations.Operator.ADD);
                firstOps.addOperation(mae);
            }

            for (MatchActionOperationEntry mae : maOps.get(1).getOperations()) {
                checkState(mae.getOperator() == MatchActionOperations.Operator.ADD);
                secondOps.addOperation(mae);
            }
        }

        return Arrays.asList(firstOps, secondOps);
    }
}
