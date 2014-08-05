package net.onrc.onos.api.newintent;

import com.google.common.base.Objects;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.util.SwitchPort;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of point-to-point connectivity.
 */
public class PointToPointIntent extends ConnectivityIntent {

    private final SwitchPort ingressPort;
    private final SwitchPort egressPort;

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports.
     *
     * @param id          intent identifier
     * @param match       traffic match
     * @param action      action
     * @param ingressPort ingress port
     * @param egressPort  egress port
     * @throws NullPointerException if {@code ingressPort} or {@code egressPort} is null.
     */
    public PointToPointIntent(IntentId id, Match match, Action action,
                              SwitchPort ingressPort, SwitchPort egressPort) {
        super(id, match, action);
        this.ingressPort = checkNotNull(ingressPort);
        this.egressPort = checkNotNull(egressPort);
    }


    /**
     * Returns the port on which the ingress traffic should be connected to
     * the egress.
     *
     * @return ingress port
     */
    public SwitchPort getIngressPort() {
        return ingressPort;
    }

    /**
     * Returns the port on which the traffic should egress.
     *
     * @return egress port
     */
    public SwitchPort getEgressPort() {
        return egressPort;
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

        PointToPointIntent that = (PointToPointIntent) o;
        return Objects.equal(this.ingressPort, that.ingressPort)
                && Objects.equal(this.egressPort, that.egressPort);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), ingressPort, egressPort);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("id", getId())
                .add("match", getMatch())
                .add("action", getAction())
                .add("ingressPort", ingressPort)
                .add("egressPort", egressPort)
                .toString();
    }

}
