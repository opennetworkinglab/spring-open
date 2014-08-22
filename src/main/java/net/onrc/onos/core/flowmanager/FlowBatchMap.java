package net.onrc.onos.core.flowmanager;

import java.util.Set;

import net.onrc.onos.api.flowmanager.FlowBatchId;
import net.onrc.onos.api.flowmanager.FlowBatchOperation;
import net.onrc.onos.api.flowmanager.FlowBatchState;

/**
 * Interface of the flow batch map.
 */
interface FlowBatchMap {
    /**
     * Gets {@link FlowBatchOperation} object having {@link FlowBatchId} as its
     * ID from the map.
     *
     * @param id the {@link FlowBatchId} to be used for getting the object
     * @return {@link FlowBatchOperation} object if exists, null otherwise
     */
    FlowBatchOperation get(FlowBatchId id);

    /**
     * Puts {@link FlowBatchOperation} object to the map.
     *
     * @param id the {@link FlowBatchId} to be used for the object
     * @param flowOp the {@link FlowBatchOperation} object
     * @return true if the object was successfully added
     */
    boolean put(FlowBatchId id, FlowBatchOperation flowOp);

    /**
     * Removes {@link FlowBatchOperation} object from the map.
     *
     * @param id the {@link FlowBatchId} to be used for removing the object
     * @return the removed {@link FlowBatchOperation} object if exists, null
     *         otherwise
     */
    FlowBatchOperation remove(FlowBatchId id);

    /**
     * Gets all {@link FlowBatchOperation} objects existing in the map.
     * <p>
     * The changes to the returned set does not affect the original map.

     * @return a set of {@link FlowBatchOperation} objects
     */
    Set<FlowBatchOperation> getAll();

    /**
     * Sets {@link FlowBatchState} to the specified {@link FlowBatchOperation}
     * object.
     *
     * @param id the {@link FlowBatchId} of the {@link FlowBatchOperation}
     * @param state the {@link FlowBatchState} to be set
     * @param expectedState the {@link FlowBatchState} expected as the previous
     *        state
     * @return true if the ID existed, the previous state was the expected state
     *         and successfully updated the state
     */
    boolean setState(FlowBatchId id, FlowBatchState state, FlowBatchState expectedState);

    /**
     * Gets {@link FlowBatchState} of the specified {@link FlowBatchOperation}
     * object.
     *
     * @param id the {@link FlowBatchId} of the {@link FlowBatchOperation}
     * @return the {@link FlowBatchState} of the {@link FlowBatchOperation} or
     *         null if the object does not exist
     */
    FlowBatchState getState(FlowBatchId id);

    /**
     * Adds a listener for listening events related to the map.
     *
     * @param listener the {@link FlowBatchMapEventListener} to be added
     */
    void addListener(FlowBatchMapEventListener listener);

    /**
     * Removes a listener for listening events related to the map.
     *
     * @param listener the {@link FlowBatchMapEventListener} to be removed
     */
    void removeListener(FlowBatchMapEventListener listener);

    /**
     * Checks if this instance is a leader of the map.
     *
     * @return true if it is leader, false otherwise
     */
    boolean isLeader();
}
