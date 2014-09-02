package net.onrc.onos.core.flowmanager;

import java.util.concurrent.CopyOnWriteArraySet;

import net.onrc.onos.api.flowmanager.FlowBatchId;
import net.onrc.onos.api.flowmanager.FlowBatchOperation;
import net.onrc.onos.api.flowmanager.FlowBatchState;
import net.onrc.onos.core.util.serializers.KryoFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IMap;

/**
 * This class is used for managing listeners of the {@link SharedFlowBatchMap}.
 */
public class SharedFlowBatchMapEventDispatcher implements EntryListener<String, byte[]> {
    private CopyOnWriteArraySet<FlowBatchMapEventListener> listeners;
    private static final Logger log = LoggerFactory
            .getLogger(SharedFlowBatchMapEventDispatcher.class);

    /**
     * Creates dispatcher using flow batch map objects.
     *
     * @param flowBatchMap the flow batch map object
     * @param flowBatchStateMap the flow batch state map object
     */
    public SharedFlowBatchMapEventDispatcher(IMap<String, byte[]> flowBatchMap,
            IMap<String, byte[]> flowBatchStateMap) {
        listeners = new CopyOnWriteArraySet<>();
        flowBatchMap.addEntryListener(this, true);
        flowBatchStateMap.addEntryListener(this, true);
    }

    /**
     * Adds a listener for listening events related to the map.
     *
     * @param listener the {@link FlowBatchMapEventListener} to be added
     */
    public void addListener(FlowBatchMapEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener for listening events related to the map.
     *
     * @param listener the {@link FlowBatchMapEventListener} to be removed
     */
    public void removeListener(FlowBatchMapEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void entryAdded(EntryEvent<String, byte[]> event) {
        final Object value = KryoFactory.deserialize(event.getValue());
        if (value instanceof FlowBatchOperation) {
            // Handles events from flowBatchMap.
            final FlowBatchOperation flowOp = (FlowBatchOperation) value;
            final FlowBatchId id = FlowBatchId.valueOf(event.getKey());
            log.trace("Flow batch operation ID:{}, {} was added", id, flowOp);
            for (FlowBatchMapEventListener e : listeners) {
                FlowBatchOperation copiedFlowOp =
                        new FlowBatchOperation(flowOp.getOperations());
                e.flowBatchOperationAdded(id, copiedFlowOp);
                e.flowBatchOperationStateChanged(id, null, FlowBatchState.SUBMITTED);
            }
        } else if (value instanceof FlowBatchState) {
            // Handles events from flowBatchStateMap.
            final FlowBatchState state = (FlowBatchState) value;
            final FlowBatchId id = FlowBatchId.valueOf(event.getKey());
            log.trace("FlowState of FlowId {} was set to {}", id, state);
            for (FlowBatchMapEventListener e : listeners) {
                e.flowBatchOperationStateChanged(id, FlowBatchState.SUBMITTED, state);
            }
        } else {
            throw new IllegalStateException("Added illegal value: " + value);
        }
    }

    @Override
    public void entryRemoved(EntryEvent<String, byte[]> event) {
        final Object value = KryoFactory.deserialize(event.getValue());
        if (value instanceof FlowBatchOperation) {
            // Handles events from flowBatchMap.
            final FlowBatchOperation flowOp = (FlowBatchOperation) value;
            final FlowBatchId id = FlowBatchId.valueOf(event.getKey());
            log.trace("Flow batch operation ID:{}, {} was removed", id, flowOp);
            for (FlowBatchMapEventListener e : listeners) {
                e.flowBatchOperationRemoved(id);
            }
        } else if (value instanceof FlowBatchState) {
            // Handles events from flowBatchStateMap.
            log.trace("Flow batch state {} of ID:{} was removed", value, event.getKey());
        } else {
            throw new IllegalStateException("Removed illegal value: " + value);
        }
    }

    @Override
    public void entryUpdated(EntryEvent<String, byte[]> event) {
        final Object value = KryoFactory.deserialize(event.getValue());
        if (value instanceof FlowBatchOperation) {
            // Handles events from flowBatchMap.
            log.trace("Flow batch operation ID:{} updated by {}", event.getKey(), value);
        } else if (value instanceof FlowBatchState) {
            // Handles events from flowBatchStateMap.
            Object oldValue = KryoFactory.deserialize(event.getOldValue());
            final FlowBatchState currentState = (FlowBatchState) value;
            final FlowBatchState oldState = (FlowBatchState) oldValue;
            final FlowBatchId id = FlowBatchId.valueOf(event.getKey());
            log.trace("Flow batch state of ID:{} was updated from {} to {}",
                    id, oldState, currentState);
            for (FlowBatchMapEventListener e : listeners) {
                e.flowBatchOperationStateChanged(id, oldState, currentState);
            }
        } else {
            throw new IllegalStateException("Updated illegal value: " + value);
        }
    }

    @Override
    public void entryEvicted(EntryEvent<String, byte[]> event) {
        // do nothing.
    }

}
