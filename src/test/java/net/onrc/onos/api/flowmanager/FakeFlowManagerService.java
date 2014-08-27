package net.onrc.onos.api.flowmanager;

import com.google.common.collect.ImmutableList;
import net.onrc.onos.core.flowmanager.FlowBatchHandleImpl;
import net.onrc.onos.core.util.IdGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fake implementation of {@link FlowManagerService} for testing.
 */
public class FakeFlowManagerService implements FlowManagerService {
    private final List<FlowManagerListener> listeners = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final FlowId target;
    private final List<FlowState> transition;
    private final boolean returnNull;

    public FakeFlowManagerService(FlowId target, boolean returnNull, FlowState... transition) {
        this.target = target;
        this.transition = ImmutableList.copyOf(transition);
        this.returnNull = returnNull;
    }

    public FlowBatchHandle addFlow(Flow flow) {
        return processFlow();
    }

    @Override
    public FlowBatchHandle removeFlow(FlowId id) {
        return processFlow();
    }

    private FlowBatchHandle processFlow() {
        if (returnNull) {
            return null;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                changeStates();
            }
        });

        // This is a test-only workaround. Passing null to the constructor is harmful,
        // but we could not create a FlowOperationMap instance due to visibility of
        // the constructor.
        // TODO: consider correct visibility of the constructor and package structure
        return new FlowBatchHandleImpl(null, new FlowBatchId(1));
    }

    private void changeStates() {
        for (int i = 0; i < transition.size(); i++) {
            FlowStateChange change;
            if (i == 0) {
                change = new FlowStateChange(target,
                        transition.get(i), null);
            } else {
                change = new FlowStateChange(target,
                        transition.get(i), transition.get(i - 1));
            }
            HashSet<FlowStateChange> changes = new HashSet<>(Arrays.asList(change));
            invokeListeners(new FlowStatesChangedEvent(System.currentTimeMillis(), changes));
        }
    }

    @Override
    public Flow getFlow(FlowId id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Flow> getFlows() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FlowBatchHandle executeBatch(FlowBatchOperation ops) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdGenerator<FlowId> getFlowIdGenerator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setConflictDetectionPolicy(ConflictDetectionPolicy policy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConflictDetectionPolicy getConflictDetectionPolicy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addListener(FlowManagerListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(FlowManagerListener listener) {
        listeners.remove(listener);
    }

    private void invokeListeners(FlowStatesChangedEvent event) {
        for (FlowManagerListener listener: listeners) {
            listener.flowStatesChanged(event);
        }
    }
}
