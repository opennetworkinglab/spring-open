package net.onrc.onos.api.newintent;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.util.SwitchPort;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of single source, multiple destination connectivity intent.
 */
public class SinglePointToMultiPointIntent extends ConnectivityIntent {

    private final SwitchPort ingressPort;
    private final Set<SwitchPort> egressPorts;

    /**
     * Creates a new single-to-multi point connectivity intent.
     *
     * @param id          intent identifier
     * @param match       traffic match
     * @param action      action
     * @param ingressPort port on which traffic will ingress
     * @param egressPorts set of ports on which traffic will egress
     * @throws NullPointerException if {@code ingressPort} or {@code egressPorts} is null
     * @throws IllegalArgumentException if the size of {@code egressPorts} is not more than 1
     */
    public SinglePointToMultiPointIntent(IntentId id, Match match, Action action,
                                         SwitchPort ingressPort,
                                         Set<SwitchPort> egressPorts) {
        super(id, match, action);

        checkNotNull(egressPorts);
        checkArgument(egressPorts.size() > 1, "the number of egress ports should be more than 1, " +
                "but actually %s", egressPorts.size());

        this.ingressPort = checkNotNull(ingressPort);
        this.egressPorts = ImmutableSet.copyOf(egressPorts);
    }

    /**
     * Returns the port on which the ingress traffic should be connected to the egress.
     *
     * @return ingress port
     */
    public SwitchPort getIngressPort() {
        return ingressPort;
    }

    /**
     * Returns the set of ports on which the traffic should egress.
     *
     * @return set of egress ports
     */
    public Set<SwitchPort> getEgressPorts() {
        return egressPorts;
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

        SinglePointToMultiPointIntent that = (SinglePointToMultiPointIntent) o;
        return Objects.equal(this.ingressPort, that.ingressPort)
                && Objects.equal(this.egressPorts, that.egressPorts);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), ingressPort, egressPorts);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("id", getId())
                .add("match", getMatch())
                .add("action", getAction())
                .add("ingressPort", ingressPort)
                .add("egressPort", egressPorts)
                .toString();
    }

}
