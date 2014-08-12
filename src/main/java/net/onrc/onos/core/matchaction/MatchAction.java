package net.onrc.onos.core.matchaction;

import java.util.List;

import net.onrc.onos.api.batchoperation.BatchOperationTarget;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.util.SwitchPort;

/**
 * A filter and actions for traffic.
 */
public class MatchAction implements BatchOperationTarget {
    private final MatchActionId id;
    private final SwitchPort port;
    private final Match match;
    private final List<Action> actions;

    /**
     * Constructor.
     *
     * @param id ID for this MatchAction object
     * @param port switch DPID
     * @param match the Match object as match condition on the port
     * @param actions the list of Action objects as actions on the switch
     */
    public MatchAction(String id, SwitchPort port, Match match, List<Action> actions) {
        this.id = new MatchActionId(id);
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
}
