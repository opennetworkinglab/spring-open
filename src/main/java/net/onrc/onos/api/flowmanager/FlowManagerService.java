package net.onrc.onos.api.flowmanager;

import java.util.Collection;

/**
 * An interface class for flow manager. The role of the flow manager is to
 * manage a set of Match-Action entries based on the specified Flow objects.
 * <p>
 * It compiles accepted Flow objects to Match-Action entries by calculating the
 * match-action operation phases and allocating resources based on the
 * constrains described in the Flow objects, and executes calculated phases
 * using Match-Action Service.
 * <p>
 * TODO: add more getter with filter for Flow objects.
 */
public interface FlowManagerService {
    /**
     * Adds Flow object, calculates match-action plan and executes it
     * asynchronously.
     * <p>
     * To track the execution result, use the returned FlowBatchHandle object.
     *
     * @param flow Flow object to be added
     * @return FlowBatchHandle object if succeeded, null otherwise
     */
    FlowBatchHandle addFlow(Flow flow);

    /**
     * Removes Flow object, calculates match-action plan and executes it
     * asynchronously.
     * <p>
     * To track the execution result, use the returned FlowBatchHandle object.
     *
     * @param id ID for Flow object to be removed
     * @return FlowBatchHandle object if succeeded, null otherwise
     */
    FlowBatchHandle removeFlow(FlowId id);

    /**
     * Gets Flow object.
     *
     * @param id ID of Flow object
     * @return Flow object if found, null otherwise
     */
    Flow getFlow(FlowId id);

    /**
     * Gets All Flow objects.
     *
     * @return the collection of Flow objects
     */
    Collection<Flow> getFlows();

    /**
     * Executes batch operation of Flow object asynchronously.
     * <p>
     * To track the execution result, use the returned FlowBatchHandle object.
     *
     * @param ops flow operations to be executed
     * @return FlowBatchHandle object if succeeded, null otherwise
     */
    FlowBatchHandle executeBatch(FlowBatchOperation ops);

    /**
     * Gets ID generator for Flow objects.
     * @return the ID generator for Flow objects
     */
    FlowIdGenerator getFlowIdGenerator();

    /**
     * Sets a conflict detection policy.
     *
     * @param policy ConflictDetectionPolicy object to be set
     */
    void setConflictDetectionPolicy(ConflictDetectionPolicy policy);

    /**
     * Gets the conflict detection policy.
     *
     * @return ConflictDetectionPolicy object being applied currently
     */
    ConflictDetectionPolicy getConflictDetectionPolicy();

    /**
     * Adds event listener to this service.
     *
     * @param listener the listener to be added
     */
    void addListener(FlowManagerListener listener);

    /**
     * Removes event listener from this service.
     *
     * @param listener the listener to be removed
     */
    void removeListener(FlowManagerListener listener);
}
