package net.onrc.onos.core.newintent;

import java.util.Collection;
import java.util.EventListener;

import net.onrc.onos.api.batchoperation.BatchOperation;
import net.onrc.onos.api.flowmanager.ConflictDetectionPolicy;
import net.onrc.onos.api.flowmanager.IFlow;
import net.onrc.onos.api.intent.IIntentRuntimeService;
import net.onrc.onos.api.intent.Intent;
import net.onrc.onos.api.intent.IntentId;

/**
 * Implementation of Intent-Runtime Service.
 * <p>
 * TODO: Make all methods thread-safe. <br>
 * TODO: Design methods to support the ReactiveForwarding and the SDN-IP.
 */
public class IntentRuntimeModule implements IIntentRuntimeService {

    @Override
    public boolean addIntent(Intent intent) {
        BatchOperation<Intent> ops = new BatchOperation<Intent>();
        ops.addAddOperation(intent);
        return executeBatch(ops);
    }

    @Override
    public boolean removeIntent(IntentId id) {
        BatchOperation<Intent> ops = new BatchOperation<Intent>();
        ops.addRemoveOperation(id);
        return executeBatch(ops);
    }

    @Override
    public boolean updateIntent(IntentId id, Intent intent) {
        BatchOperation<Intent> ops = new BatchOperation<Intent>();
        ops.addUpdateOperation(id, intent);
        return executeBatch(ops);
    }

    @Override
    public Intent getIntent(IntentId id) {
        // TODO Auto-generated method stub
        // - retrieves intents from global distributed maps
        return null;
    }

    @Override
    public Collection<Intent> getIntents() {
        // TODO Auto-generated method stub
        // - retrieves intents from global distributed maps
        return null;
    }

    @Override
    public boolean executeBatch(BatchOperation<Intent> ops) {
        // TODO Auto-generated method stub
        // - gets flow operations using compile() method for each Intent object.
        // - allocates resources
        // - combines and executes flow operations using FlowManager Service.
        // - updates global distributed maps
        return false;
    }

    @Override
    public Collection<IFlow> getFlows(String intentId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Intent getIntentByFlow(String flowId) {
        // TODO Auto-generated method stub
        return null;
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
        // - listener for addition/removal of intents,
        // and states changes of intents
    }

    @Override
    public void removeEventListener(EventListener listener) {
        // TODO Auto-generated method stub

    }
}
