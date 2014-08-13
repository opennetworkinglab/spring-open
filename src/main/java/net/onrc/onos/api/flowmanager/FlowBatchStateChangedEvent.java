package net.onrc.onos.api.flowmanager;

/**
 * Event class which notifies the state change of flow batch operation.
 */
public class FlowBatchStateChangedEvent {
    private final long time;
    private final FlowBatchHandle handle;
    private final FlowBatchState current;
    private final FlowBatchState previous;

    /**
     * Creates the {@link FlowBatchStateChangedEvent} instance.
     *
     * @param time the time at which the event was created in milliseconds since
     *        start of epoch
     * @param handle the handle for the flow batch operation
     * @param current the current state of the flow batch operation
     * @param previous the previous state of the flow batch operation
     */
    FlowBatchStateChangedEvent(long time, FlowBatchHandle handle,
            FlowBatchState current, FlowBatchState previous) {
        this.time = time;
        this.handle = handle;
        this.current = current;
        this.previous = previous;
    }

    /**
     * Gets the time at which the event was created.
     *
     * @return the time in milliseconds since start of epoch
     */
    public long getTime() {
        return time;
    }

    /**
     * Gets the handle for the flow batch operation.
     *
     * @return the handle for the flow batch operation
     */
    public FlowBatchHandle getFlowBatchHandle() {
        return handle;
    }

    /**
     * Gets the current state of the flow batch operation.
     *
     * @return the current state of the flow batch operation
     */
    public FlowBatchState getCurrentState() {
        return current;
    }

    /**
     * Gets the previous state of the flow batch operation.
     *
     * @return the previous state of the flow batch operation
     */
    public FlowBatchState getPreviousState() {
        return previous;
    }
}
