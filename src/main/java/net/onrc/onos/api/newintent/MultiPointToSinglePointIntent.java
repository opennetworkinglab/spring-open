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
 * Abstraction of multiple source to single destination connectivity intent.
 */
public class MultiPointToSinglePointIntent extends ConnectivityIntent {

    private final Set<SwitchPort> ingressPorts;
    private final SwitchPort egressPort;

    /**
     * Creates a new multi-to-single point connectivity intent for the specified
     * traffic match and action.
     *
     * @param id           intent identifier
     * @param match        traffic match
     * @param action       action
     * @param ingressPorts set of ports from which ingress traffic originates
     * @param egressPort   port to which traffic will egress
     * @throws NullPointerException if {@code ingressPorts} or {@code egressPort} is null.
     * @throws IllegalArgumentException if the size of {@code ingressPorts} is not more than 1
     */
    public MultiPointToSinglePointIntent(IntentId id, Match match, Action action,
                                         Set<SwitchPort> ingressPorts, SwitchPort egressPort) {
        super(id, match, action);

        checkNotNull(ingressPorts);
        checkArgument(ingressPorts.size() > 1, "the number of ingress ports should be more than 1, " +
                "but actually %s", ingressPorts.size());

        this.ingressPorts = ImmutableSet.copyOf(ingressPorts);
        this.egressPort = checkNotNull(egressPort);
    }

    /**
     * Returns the set of ports on which ingress traffic should be connected to the egress port.
     *
     * @return set of ingress ports
     */
    public Set<SwitchPort> getIngressPorts() {
        return ingressPorts;
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

        MultiPointToSinglePointIntent that = (MultiPointToSinglePointIntent) o;
        return Objects.equal(this.ingressPorts, that.ingressPorts)
                && Objects.equal(this.egressPort, that.egressPort);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), ingressPorts, egressPort);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("id", getId())
                .add("match", getMatch())
                .add("aciton", getAction())
                .add("ingressPorts", getIngressPorts())
                .add("egressPort", getEgressPort())
                .toString();
    }
}
