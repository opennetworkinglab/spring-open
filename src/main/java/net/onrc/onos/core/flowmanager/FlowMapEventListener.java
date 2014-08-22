package net.onrc.onos.core.flowmanager;

import net.onrc.onos.api.flowmanager.Flow;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowState;

/**
 * An interface to the event listener of the flow map.
 */
interface FlowMapEventListener {
    /**
     * Invoked when a {@link Flow} object is added.
     *
     * @param id the ID of the {@link Flow}.
     * @param flow the {@link Flow} object.
     */
    void flowAdded(FlowId id, Flow flow);

    /**
     * Invoked when a {@link Flow} object is removed.
     *
     * @param id the ID of the {@link Flow}.
     */
    void flowRemoved(FlowId id);

    /**
     * Invoked when a {@link FlowState} of a {@link Flow} object is changed.
     *
     * @param id the ID of the {@link Flow}
     * @param oldState the old state of the {@link Flow}
     * @param currentState the current state of the {@link Flow}
     */
    void flowStateChanged(FlowId id, FlowState oldState, FlowState currentState);
}
