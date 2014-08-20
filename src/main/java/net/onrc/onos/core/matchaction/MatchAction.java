package net.onrc.onos.core.matchaction;

import java.util.List;

import net.onrc.onos.api.batchoperation.BatchOperationTarget;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.util.SwitchPort;

/**
 * A filter and actions for traffic.  Objects of this class are immutable.
 */
public final class MatchAction implements BatchOperationTarget {
    private final MatchActionId id;
    private final SwitchPort port;
    private final Match match;
    private final List<Action> actions;

    /**
     * Constructor.
     *
     * @param id ID for this MatchAction object
     * @param port switch port to apply changes to
     * @param match the Match object as match condition on the port
     * @param actions the list of Action objects as actions on the switch
     */
    public MatchAction(MatchActionId id, SwitchPort port, Match match, List<Action> actions) {
        this.id = id;
        this.port = port;
        this.match = match;
        this.actions = actions;
    }

    /**
     * Constructor. TEMPORARY
     *
     * @param id ID for this MatchAction object
     * @param port switch port to apply changes to
     * @param match the Match object as match condition on the port
     * @param actions the list of Action objects as actions on the switch
     */
    public MatchAction(String id, SwitchPort port, Match match, List<Action> actions) {
        this.id = null;
        this.port = port;
        this.match = match;
        this.actions = actions;
    }

    /**
     * Gets ID for this object.
     *
     * @return the ID for this object
     */
    public MatchActionId getId() {
        return id;
    }

    /**
     * Gets the switch-port which is the target of this match-action.
     *
     * @return the target switch-port of this match-action
     */
    public SwitchPort getSwitchPort() {
        return port;
    }

    /**
     * Gets the traffic filter of the match-action.
     *
     * @return the traffic filter
     */
    public Match getMatch() {
        return match;
    }

    /**
     * Gets the list of actions of the match-action.
     *
     * @return the list of actions
     */
    public List<Action> getActions() {
        return actions;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MatchAction) {
            MatchAction other = (MatchAction) obj;
            return (id.equals(other.id));
        }
        return false;
    }
}
