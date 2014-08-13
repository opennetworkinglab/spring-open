package net.onrc.onos.core.newintent;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import net.onrc.onos.api.intent.Intent;
import net.onrc.onos.api.intent.IntentId;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.util.SwitchPort;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The class represents multiple sources, single destination tree like connectivity.
 *
 * <p>
 * This class is intended to be used for the SDN-IP application.
 * </p>
 */
public class MultiPointToSinglePointIntent extends Intent {
    private final ImmutableList<SwitchPort> ingressPorts;
    private final SwitchPort egressPort;
    private final Match match;
    private final ImmutableList<Action> modifications;

    /**
     * Constructs an intent representing multiple sources, single destination
     * tree like connectivity.
     *
     * @param id the ID of the intent.
     * @param ingressPorts the ingress ports.
     * @param egressPort the egress port.
     * @param match the filter condition of the incoming traffic which can go through.
     * @param modifications the modification actions to the outgoing traffic.
     */
    public MultiPointToSinglePointIntent(IntentId id,
                                         List<SwitchPort> ingressPorts,
                                         SwitchPort egressPort,
                                         Match match,
                                         List<Action> modifications) {
        super(id);

        this.ingressPorts = ImmutableList.copyOf(checkNotNull(ingressPorts));
        this.egressPort = checkNotNull(egressPort);
        this.match = checkNotNull(match);
        this.modifications = ImmutableList.copyOf(checkNotNull(modifications));
    }

    /**
     * Returns the ingress ports.
     *
     * @return the ingress ports.
     */
    public ImmutableList<SwitchPort> getIngressPorts() {
        return ingressPorts;
    }

    /**
     * Returns the egress port.
     *
     * @return the egress port.
     */
    public SwitchPort getEgressPort() {
        return egressPort;
    }

    /**
     * Returns the filter condition of the incoming traffic which can go through.
     *
     * @return the filter condition of the incoming traffic which can go through.
     */
    public Match getMatch() {
        return match;
    }

    /**
     * Returns the modification actions to the outgoing traffic.
     *
     * @return the modification actions to the outgoing traffic.
     */
    public ImmutableList<Action> getModifications() {
        return modifications;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId(), ingressPorts, egressPort, match, modifications);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof MultiPointToSinglePointIntent)) {
            return false;
        }

        MultiPointToSinglePointIntent that = (MultiPointToSinglePointIntent) obj;
        return Objects.equal(this.getId(), that.getId())
                && Objects.equal(this.ingressPorts, that.ingressPorts)
                && Objects.equal(this.egressPort, that.egressPort)
                && Objects.equal(this.match, that.match)
                && Objects.equal(this.modifications, that.modifications);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", getId())
                .add("ingressPorts", ingressPorts)
                .add("egressPort", egressPort)
                .add("match", match)
                .add("modifications", modifications)
                .toString();
    }
}
