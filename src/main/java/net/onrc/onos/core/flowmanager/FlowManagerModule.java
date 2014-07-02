package net.onrc.onos.core.flowmanager;

import java.util.Collection;
import java.util.EventListener;

import net.onrc.onos.api.batchoperation.BatchOperation;
import net.onrc.onos.api.flowmanager.ConflictDetectionPolicy;
import net.onrc.onos.api.flowmanager.IFlow;
import net.onrc.onos.api.flowmanager.IFlowManagerService;

/**
 * Manages a set of IFlow objects, computes and maintains a set of Match-Action
 * entries based on the IFlow objects, and executes Match-Action plans.
 * <p>
 * TODO: Make all methods thread-safe
 */
public class FlowManagerModule implements IFlowManagerService {
    @Override
    public boolean addFlow(IFlow flow) {
        BatchOperation<IFlow> ops = new BatchOperation<IFlow>();
        ops.addAddOperation(flow);
        return executeBatch(ops);
    }

    @Override
    public boolean removeFlow(String id) {
        BatchOperation<IFlow> ops = new BatchOperation<IFlow>();
        ops.addRemoveOperation(id);
        return executeBatch(ops);
    }

    @Override
    public boolean updateFlow(IFlow flow) {
        BatchOperation<IFlow> ops = new BatchOperation<IFlow>();
        ops.addUpdateOperation(flow.getId(), flow);
        return executeBatch(ops);
    }

    @Override
    public IFlow getFlow(String id) {
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
        // TODO Auto-generated method stub

    }

    @Override
    public ConflictDetectionPolicy getConflictDetectionPolicy() {
        // TODO Auto-generated method stub
        return null;
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
