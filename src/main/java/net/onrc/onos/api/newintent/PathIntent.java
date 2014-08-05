package net.onrc.onos.api.newintent;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.util.LinkTuple;
import net.onrc.onos.core.util.SwitchPort;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of explicitly path specified connectivity intent.
 */
public class PathIntent extends PointToPointIntent {

    private final List<LinkTuple> path;

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports and using the specified explicit path.
     *
     * @param id          intent identifier
     * @param match       traffic match
     * @param action      action
     * @param ingressPort ingress port
     * @param egressPort  egress port
     * @param path        traversed links
     * @throws NullPointerException {@code path} is null
     */
    public PathIntent(IntentId id, Match match, Action action,
                      SwitchPort ingressPort, SwitchPort egressPort,
                      List<LinkTuple> path) {
        super(id, match, action, ingressPort, egressPort);
        this.path = ImmutableList.copyOf(checkNotNull(path));
    }

    /**
     * Returns the links which the traffic goes along.
     *
     * @return traversed links
     */
    public List<LinkTuple> getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        PathIntent that = (PathIntent) o;

        if (!path.equals(that.path)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), path);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("id", getId())
                .add("match", getMatch())
                .add("action", getAction())
                .add("ingressPort", getIngressPort())
                .add("egressPort", getEgressPort())
                .add("path", path)
                .toString();
    }
}
