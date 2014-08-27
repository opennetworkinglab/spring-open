package net.onrc.onos.core.flowmanager;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.onrc.onos.api.batchoperation.BatchOperationEntry;
import net.onrc.onos.api.flowmanager.ConflictDetectionPolicy;
import net.onrc.onos.api.flowmanager.Flow;
import net.onrc.onos.api.flowmanager.FlowBatchHandle;
import net.onrc.onos.api.flowmanager.FlowBatchId;
import net.onrc.onos.api.flowmanager.FlowBatchOperation;
import net.onrc.onos.api.flowmanager.FlowBatchOperation.Operator;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowManagerFloodlightService;
import net.onrc.onos.api.flowmanager.FlowManagerListener;
import net.onrc.onos.core.datagrid.ISharedCollectionsService;
import net.onrc.onos.core.matchaction.MatchActionFloodlightService;
import net.onrc.onos.core.matchaction.MatchActionId;
import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.matchaction.MatchActionOperations;
import net.onrc.onos.core.matchaction.MatchActionOperationsId;
import net.onrc.onos.core.matchaction.MatchActionService;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.util.IdBlockAllocator;
import net.onrc.onos.core.util.IdGenerator;

/**
 * Manages a set of Flow objects, computes and maintains a set of Match-Action
 * entries based on the Flow objects, and executes Match-Action plans.
 * <p>
 * TODO: Make all methods thread-safe
 */
public class FlowManagerModule implements FlowManagerFloodlightService, IFloodlightModule {
    private ConflictDetectionPolicy conflictDetectionPolicy;
    private FlowIdGeneratorWithIdBlockAllocator flowIdGenerator;
    private FlowBatchIdGeneratorWithIdBlockAllocator flowBatchIdGenerator;
    private IdGenerator<MatchActionId> maIdGenerator;
    private IdGenerator<MatchActionOperationsId> maoIdGenerator;
    private MatchActionService matchActionService;
    private IControllerRegistryService registryService;
    private ISharedCollectionsService sharedCollectionService;
    private FlowMap flowMap;
    private FlowBatchMap flowBatchMap;
    private FlowEventDispatcher flowEventDispatcher;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        List<Class<? extends IFloodlightService>> services =
                new ArrayList<Class<? extends IFloodlightService>>();
        services.add(FlowManagerFloodlightService.class);
        return services;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> impls =
                new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
        impls.put(FlowManagerFloodlightService.class, this);
        return impls;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        return Arrays.asList(
                MatchActionFloodlightService.class,
                ISharedCollectionsService.class,
                IControllerRegistryService.class);
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        matchActionService = context.getServiceImpl(MatchActionFloodlightService.class);
        registryService = context.getServiceImpl(IControllerRegistryService.class);
        sharedCollectionService = context.getServiceImpl(ISharedCollectionsService.class);
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        IdBlockAllocator idBlockAllocator = registryService;
        flowIdGenerator =
                new FlowIdGeneratorWithIdBlockAllocator(idBlockAllocator);
        flowBatchIdGenerator =
                new FlowBatchIdGeneratorWithIdBlockAllocator(idBlockAllocator);
        maIdGenerator = matchActionService.getMatchActionIdGenerator();
        maoIdGenerator = matchActionService.getMatchActionOperationsIdGenerator();

        flowMap = new SharedFlowMap(sharedCollectionService);
        flowBatchMap = new SharedFlowBatchMap(sharedCollectionService);
        flowEventDispatcher =
                new FlowEventDispatcher(flowMap, flowBatchMap, matchActionService);
        flowEventDispatcher.start();
    }

    /**
     * Constructs FlowManagerModule.
     */
    public FlowManagerModule() {
        this.conflictDetectionPolicy = ConflictDetectionPolicy.FREE;
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
        return flowMap.get(id);
    }

    @Override
    public Collection<Flow> getFlows() {
        return flowMap.getAll();
    }

    /**
     * Executes batch operation of Flow object asynchronously.
     * <p>
     * To track the execution result, use the returned FlowBatchHandle object.
     * <p>
     * This method just put the batch-operation object to the global flow batch
     * operation map with unique ID. The worker process for execution and
     * installation will get the appended operation when it gets events from the
     * map. This method returns a handler for obtaining the result of this
     * operation, control the executing process, etc.
     *
     * @param ops flow operations to be executed
     * @return {@link FlowBatchHandle} object if succeeded, null otherwise
     */
    @Override
    public FlowBatchHandle executeBatch(FlowBatchOperation ops) {
        FlowBatchId id = flowBatchIdGenerator.getNewId();
        flowBatchMap.put(id, ops);
        return new FlowBatchHandleImpl(flowBatchMap, id);
    }

    @Override
    public IdGenerator<FlowId> getFlowIdGenerator() {
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
        flowEventDispatcher.addListener(listener);
    }

    @Override
    public void removeListener(FlowManagerListener listener) {
        flowEventDispatcher.removeListener(listener);
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
