package net.onrc.onos.core.flowmanager;

import java.util.Collection;

import net.onrc.onos.api.flowmanager.ConflictDetectionPolicy;
import net.onrc.onos.api.flowmanager.Flow;
import net.onrc.onos.api.flowmanager.FlowBatchHandle;
import net.onrc.onos.api.flowmanager.FlowBatchOperation;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowManagerListener;
import net.onrc.onos.api.flowmanager.FlowManagerService;

/**
 * Manages a set of Flow objects, computes and maintains a set of Match-Action
 * entries based on the Flow objects, and executes Match-Action plans.
 * <p>
 * TODO: Make all methods thread-safe
 */
public class FlowManagerModule implements FlowManagerService {
    private ConflictDetectionPolicy conflictDetectionPolicy;
    private FlowOperationMap flowOperationMap;

    /**
     * Constructor.
     */
    public FlowManagerModule() {
        this.conflictDetectionPolicy = ConflictDetectionPolicy.FREE;
        this.flowOperationMap = new FlowOperationMap();
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

    @Override
    public FlowBatchHandle executeBatch(FlowBatchOperation ops) {
        // This method just put the batch-operation object to the global
        // flow operation map with unique ID. The leader process will get
        // the appended operation with events notified from the map.
        FlowBatchHandle handler = flowOperationMap.putOperation(ops);

        // Then it will return a handler to obtain the result of this operation,
        // control the executing process, etc.
        return handler;
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
}
