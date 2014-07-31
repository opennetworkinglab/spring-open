package net.onrc.onos.core.flowmanager;

import java.util.Collection;
import java.util.EventListener;

import net.onrc.onos.api.batchoperation.BatchOperation;
import net.onrc.onos.api.flowmanager.ConflictDetectionPolicy;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.IFlow;
import net.onrc.onos.api.flowmanager.IFlowManagerService;

/**
 * Manages a set of IFlow objects, computes and maintains a set of Match-Action
 * entries based on the IFlow objects, and executes Match-Action plans.
 * <p>
 * TODO: Make all methods thread-safe
 */
public class FlowManagerModule implements IFlowManagerService {
    private ConflictDetectionPolicy conflictDetectionPolicy;

    /**
     * Constructor.
     */
    public FlowManagerModule() {
        this.conflictDetectionPolicy = ConflictDetectionPolicy.FREE;
    }

    @Override
    public boolean addFlow(IFlow flow) {
        BatchOperation<IFlow> ops = new BatchOperation<IFlow>();
        ops.addAddOperation(flow);
        return executeBatch(ops);
    }

    @Override
    public boolean removeFlow(FlowId id) {
        BatchOperation<IFlow> ops = new BatchOperation<IFlow>();
        ops.addRemoveOperation(id);
        return executeBatch(ops);
    }

    @Override
    public boolean updateFlow(IFlow flow) {
        BatchOperation<IFlow> ops = new BatchOperation<IFlow>();
        ops.addUpdateOperation(flow);
        return executeBatch(ops);
    }

    @Override
    public IFlow getFlow(FlowId id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<IFlow> getFlows() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean executeBatch(BatchOperation<IFlow> ops) {
        // TODO Auto-generated method stub
        return false;
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
    public void addEventListener(EventListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeEventListener(EventListener listener) {
        // TODO Auto-generated method stub

    }
}
