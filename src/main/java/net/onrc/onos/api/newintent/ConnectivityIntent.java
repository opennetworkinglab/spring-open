package net.onrc.onos.api.newintent;

import com.google.common.base.Objects;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.match.Match;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of connectivity intent for traffic matching some criteria.
 */
public abstract class ConnectivityIntent extends AbstractIntent {

    // TODO: other forms of intents should be considered for this family:
    //   point-to-point with constraints (waypoints/obstacles)
    //   multi-to-single point with constraints (waypoints/obstacles)
    //   single-to-multi point with constraints (waypoints/obstacles)
    //   concrete path (with alternate)
    //   ...

    private final Match match;
    // TODO: should consider which is better for multiple actions,
    // defining compound action class or using list of actions.
    private final Action action;

    /**
     * Creates a connectivity intent that matches on the specified intent
     * and applies the specified action.
     *
     * @param id    intent identifier
     * @param match traffic match
     * @param action action
     * @throws NullPointerException if the match or action is null
     */
    protected ConnectivityIntent(IntentId id, Match match, Action action) {
        super(id);
        this.match = checkNotNull(match);
        this.action = checkNotNull(action);
    }

    /**
     * Constructor for serializer.
     */
    protected ConnectivityIntent() {
        super();
        this.match = null;
        this.action = null;
    }

    /**
     * Returns the match specifying the type of traffic.
     *
     * @return traffic match
     */
    public Match getMatch() {
        return match;
    }

    /**
     * Returns the action applied to the traffic.
     *
     * @return applied action
     */
    public Action getAction() {
        return action;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        ConnectivityIntent that = (ConnectivityIntent) o;
        return Objects.equal(this.match, that.match)
                && Objects.equal(this.action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), match, action);
    }

}
