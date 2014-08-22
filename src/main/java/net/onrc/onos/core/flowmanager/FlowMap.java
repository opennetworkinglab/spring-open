package net.onrc.onos.core.flowmanager;

import java.util.Set;

import net.onrc.onos.api.flowmanager.Flow;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowState;

/**
 * Interface of the flow map.
 */
interface FlowMap {
    /**
     * Gets {@link Flow} object having {@link FlowId} as its ID from the map.
     *
     * @param id the {@link FlowId} to be used for getting the object
     * @return {@link Flow} object if exists, null otherwise
     */
    Flow get(FlowId id);

    /**
     * Puts {@link Flow} object to the map.
     *
     * @param flow the {@link Flow} object
     * @return true if the object was successfully added
     */
    boolean put(Flow flow);

    /**
     * Removes {@link Flow} object from the map.
     *
     * @param id the {@link FlowId} to be used for removing the object
     * @return the removed {@link Flow} object if exists, null otherwise
     */
    Flow remove(FlowId id);

    /**
     * Gets all {@link Flow} objects existing in the map.
     * <p>
     * The changes to the returned set does not affect the original map.
     *
     * @return a set of {@link Flow} objects
     */
    Set<Flow> getAll();

    /**
     * Sets {@link FlowState} to the specified {@link Flow} object.
     *
     * @param id the {@link FlowId} of the {@link Flow}
     * @param state the {@link FlowState} to be set
     * @param expectedState the {@link FlowState} expected as the previous state
     * @return true if the ID existed, the previous state was the expected state
     *         and successfully updated the state
     */
    boolean setState(FlowId id, FlowState state, FlowState expectedState);

    /**
     * Gets {@link FlowState} of the specified {@link Flow} object.
     *
     * @param id the {@link FlowId} of the {@link Flow}
     * @return the {@link FlowState} of the {@link Flow} or null if the
     *         {@link Flow} does not exist
     */
    FlowState getState(FlowId id);

    /**
     * Adds a listener for listening events related to the map.
     *
     * @param listener the {@link FlowMapEventListener} to be added
     */
    void addListener(FlowMapEventListener listener);

    /**
     * Removes a listener for listening events related to the map.
     *
     * @param listener the {@link FlowMapEventListener} to be removed
     */
    void removeListener(FlowMapEventListener listener);
}
