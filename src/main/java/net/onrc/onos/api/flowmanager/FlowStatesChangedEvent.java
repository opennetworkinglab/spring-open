package net.onrc.onos.api.flowmanager;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * The event class which notifies the set of the flow state transitions.
 */
public class FlowStatesChangedEvent {
    private final long time;
    private final Set<FlowStateChange> changes;

    /**
     * Creates the {@link FlowStatesChangedEvent} instance.
     *
     * @param time the time at which the event was created in milliseconds since start of epoch
     * @param changes the set of {@link FlowStateChange} objects
     */
    FlowStatesChangedEvent(long time, Set<FlowStateChange> changes) {
        this.time = time;
        this.changes = ImmutableSet.copyOf(checkNotNull(changes));
    }

    /**
     * Gets the time at which the event was created.
     *
     * @return the time at which the event was created in milliseconds since start of epoch
     */
    public long getTime() {
        return time;
    }

    /**
     * Gets the set of state changes happened at once.
     *
     * @return the set of {@link FlowStateChange} objects
     */
    public Set<FlowStateChange> getStateChanges() {
        return changes;
    }
}
