package net.onrc.onos.core.flowmanager;

import java.util.concurrent.CopyOnWriteArraySet;

import net.onrc.onos.api.flowmanager.Flow;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowState;
import net.onrc.onos.core.util.serializers.KryoFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IMap;

/**
 * This class is used for managing listeners of the {@link SharedFlowMap}.
 */
class SharedFlowMapEventDispatcher implements EntryListener<String, byte[]> {
    private CopyOnWriteArraySet<FlowMapEventListener> listeners;
    private static final Logger log = LoggerFactory
            .getLogger(SharedFlowMapEventDispatcher.class);

    /**
     * Creates dispatcher using flow map objects.
     *
     * @param flowMap the flow map object
     * @param flowStateMap the flow state map object
     */
    SharedFlowMapEventDispatcher(IMap<String, byte[]> flowMap,
            IMap<String, byte[]> flowStateMap) {
        listeners = new CopyOnWriteArraySet<>();
        flowMap.addEntryListener(this, true);
        flowStateMap.addEntryListener(this, true);
    }

    /**
     * Adds a listener for listening events related to the map.
     *
     * @param listener the {@link FlowMapEventListener} to be added
     */
    void addListener(FlowMapEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener for listening events related to the map.
     *
     * @param listener the {@link FlowMapEventListener} to be removed
     */
    void removeListener(FlowMapEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void entryAdded(EntryEvent<String, byte[]> event) {
        final Object value = KryoFactory.deserialize(event.getValue());
        if (value instanceof Flow) {
            // Handles events from flowMap.
            final Flow flow = (Flow) value;
            log.trace("Flow {} was added", flow);
            for (FlowMapEventListener e : listeners) {
                e.flowAdded(flow.getId(), flow);
                e.flowStateChanged(flow.getId(), null, FlowState.SUBMITTED);
            }
        } else if (value instanceof FlowState) {
            // Handles events from flowStateMap.
            final FlowState state = (FlowState) value;
            final FlowId id = FlowId.valueOf(event.getKey());
            log.trace("FlowState of FlowId {} was set to {}", id, state);
            for (FlowMapEventListener e : listeners) {
                e.flowStateChanged(id, FlowState.SUBMITTED, state);
            }
        } else {
            throw new IllegalStateException("Added illegal value: " + value.toString());
        }
    }

    @Override
    public void entryRemoved(EntryEvent<String, byte[]> event) {
        final Object value = KryoFactory.deserialize(event.getValue());
        if (value instanceof Flow) {
            // Handles events from flowMap.
            final Flow flow = (Flow) value;
            log.trace("Flow {} was removed", flow);
            for (FlowMapEventListener e : listeners) {
                e.flowRemoved(flow.getId());
            }
        } else if (value instanceof FlowState) {
            // Handles events from flowStateMap.
            log.trace("FlowState {} of FlowId {} was removed", value, event.getKey());
        } else {
            throw new IllegalStateException("Removed illegal value: " + value.toString());
        }
    }

    @Override
    public void entryUpdated(EntryEvent<String, byte[]> event) {
        final Object value = KryoFactory.deserialize(event.getValue());
        if (value instanceof Flow) {
            // Handles events from flowMap.
            log.trace("Flow Updated by {}", value);
        } else if (value instanceof FlowState) {
            // Handles events from flowStateMap.
            Object oldValue = KryoFactory.deserialize(event.getOldValue());
            final FlowState state = (FlowState) value;
            final FlowState oldState = (FlowState) oldValue;
            final FlowId id = FlowId.valueOf(event.getKey());
            log.trace("FlowState of FlowId {} was updated from {} to {}",
                    id, oldState, state);
            for (FlowMapEventListener e : listeners) {
                e.flowStateChanged(id, oldState, state);
            }
        } else {
            throw new IllegalStateException("Updated illegal value: " + value.toString());
        }
    }

    @Override
    public void entryEvicted(EntryEvent<String, byte[]> event) {
        // do nothing.
    }

}
