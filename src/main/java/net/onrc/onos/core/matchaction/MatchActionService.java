package net.onrc.onos.core.matchaction;

import net.onrc.onos.api.flowmanager.ConflictDetectionPolicy;
import net.onrc.onos.core.util.IdGenerator;

import java.util.EventListener;
import java.util.Set;

/**
 * An interface for the match-action service.
 */
public interface MatchActionService {
    /**
     * Adds a new match-action entry.
     *
     * @param matchAction MatchAction object to be added.
     * @return true if succeeded, false otherwise.
     */
    boolean addMatchAction(MatchAction matchAction);

    /**
     * Gets the set of match-action entries.
     *
     * @return The set of match-action entries.
     */
    Set<MatchAction> getMatchActions();

    /**
     * Executes match-action operation plan.
     *
     * @param operations Operations to be executed.
     * @return true if succeeded, false otherwise.
     */
    boolean executeOperations(MatchActionOperations operations);

    /**
     * Sets a conflict detection policy.
     *
     * @param policy ConflictDetectionPolicy object to be set.
     */
    void setConflictDetectionPolicy(ConflictDetectionPolicy policy);

    /**
     * Gets the conflict detection policy.
     *
     * @return ConflictDetectionPolicy object being applied currently.
     */
    ConflictDetectionPolicy getConflictDetectionPolicy();

    /**
     * Gets the ID generator for MatchActionId.
     *
     * @return the ID generator for MatchActionId
     */
    IdGenerator<MatchActionId> getMatchActionIdGenerator();

    /**
     * Gets the ID generator for MatchActionOperationsId.
     *
     * @return the ID generator for MatchActionOperationsId
     */
    IdGenerator<MatchActionOperationsId> getMatchActionOperationsIdGenerator();

    /**
     * Adds event listener to this service.
     *
     * @param listener EventListener to be added.
     */
    void addEventListener(EventListener listener);

    /**
     * Removes event listener from this service.
     *
     * @param listener EventListener to be removed.
     */
    void removeEventListener(EventListener listener);
}
