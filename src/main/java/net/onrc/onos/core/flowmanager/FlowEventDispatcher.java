package net.onrc.onos.core.flowmanager;

import java.util.EventListener;
import java.util.concurrent.CopyOnWriteArraySet;

import net.onrc.onos.api.flowmanager.Flow;
import net.onrc.onos.api.flowmanager.FlowBatchHandle;
import net.onrc.onos.api.flowmanager.FlowBatchId;
import net.onrc.onos.api.flowmanager.FlowBatchOperation;
import net.onrc.onos.api.flowmanager.FlowBatchState;
import net.onrc.onos.api.flowmanager.FlowBatchStateChangedEvent;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowManagerListener;
import net.onrc.onos.api.flowmanager.FlowState;
import net.onrc.onos.api.flowmanager.FlowStateChange;
import net.onrc.onos.api.flowmanager.FlowStatesChangedEvent;
import net.onrc.onos.core.matchaction.MatchActionService;

import com.google.common.collect.Sets;

/**
 * Manages flow manager related events.
 */
public class FlowEventDispatcher implements
        EventListener, FlowMapEventListener, FlowBatchMapEventListener {
    private final FlowMap flowMap;
    private final FlowBatchMap flowBatchMap;
    private final MatchActionService maService;
    private CopyOnWriteArraySet<FlowManagerListener> listeners;

    /**
     * Creates an instance using {@link FlowMap}, {@link FlowBatchMap} and
     * {@link MatchActionService}.
     *
     * @param flowMap the {@link FlowMap} object
     * @param flowBatchMap the {@link FlowBatchMap} object
     * @param maService the {@link MatchActionService} object
     */
    FlowEventDispatcher(FlowMap flowMap, FlowBatchMap flowBatchMap,
            MatchActionService maService) {
        this.flowMap = flowMap;
        this.flowBatchMap = flowBatchMap;
        this.maService = maService;
        this.listeners = new CopyOnWriteArraySet<>();
    }

    /**
     * Adds the event listener.
     *
     * @param listener the listener to be added
     */
    void addListener(FlowManagerListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the event listener.
     *
     * @param listener the listener to be removed
     */
    void removeListener(FlowManagerListener listener) {
        listeners.remove(listener);
    }

    /**
     * Starts listening flow manager related events.
     */
    void start() {
        maService.addEventListener(this);
        flowMap.addListener(this);
        flowBatchMap.addListener(this);
    }

    /**
     * Stops listening flow manager related events.
     */
    void stop() {
        maService.removeEventListener(this);
        flowMap.removeListener(this);
        flowBatchMap.removeListener(this);
    }

    @Override
    public void flowAdded(FlowId id, Flow flow) {
        // do nothing. (This event is not used for now)
    }

    @Override
    public void flowRemoved(FlowId id) {
        // do nothing. (This event is not used for now)
    }

    @Override
    public void flowStateChanged(FlowId id, FlowState oldState, FlowState currentState) {
        FlowStateChange stateChange = new FlowStateChange(id, currentState, oldState);
        long time = System.currentTimeMillis();
        FlowStatesChangedEvent event =
                new FlowStatesChangedEvent(time, Sets.newHashSet(stateChange));
        for (FlowManagerListener e : listeners) {
            e.flowStatesChanged(event);
        }
    }

    @Override
    public void flowBatchOperationAdded(FlowBatchId id, FlowBatchOperation flowOp) {
        // do nothing. (This event is not used for now)
    }

    @Override
    public void flowBatchOperationRemoved(FlowBatchId id) {
        // do nothing. (This event is not used for now)
    }

    @Override
    public void flowBatchOperationStateChanged(FlowBatchId id, FlowBatchState oldState,
            FlowBatchState currentState) {
        FlowBatchHandle handle = new FlowBatchHandleImpl(flowBatchMap, id);
        long time = System.currentTimeMillis();
        FlowBatchStateChangedEvent event = new FlowBatchStateChangedEvent(time, handle,
                currentState, currentState);

        for (FlowManagerListener e : listeners) {
            e.flowBatchStateChanged(event);
        }
    }
}
