package net.onrc.onos.core.matchaction;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A match-action plan to be executed on the match-action module.
 * <p>
 * The plan is a list of phases, and the phase is a batch operation of
 * match-actions.
 */
public class MatchActionPlan {
    List<MatchActionPhase> phases;

    /**
     * Constructor.
     */
    public MatchActionPlan() {
        phases = new LinkedList<>();
    }

    /**
     * Adds the specified phase to the plan.
     *
     * @param phase The batch operation of match-actions to be added to the
     *        plan.
     */
    public void addPhase(MatchActionPhase phase) {
        phases.add(phase);
    }

    /**
     * Gets the list of phases of the plan.
     *
     * @return The list of phases, batch operations of match-actions.
     */
    public List<MatchActionPhase> getPhases() {
        return Collections.unmodifiableList(phases);
    }
}
