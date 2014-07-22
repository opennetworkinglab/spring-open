package net.onrc.onos.core.newintent;

import com.google.common.base.Objects;
import net.onrc.onos.api.intent.Intent;
import net.onrc.onos.api.intent.IntentId;
import net.onrc.onos.core.matchaction.match.IMatch;
import net.onrc.onos.core.util.Pair;
import net.onrc.onos.core.util.SwitchPort;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class represents point-to-point connectivity.
 */
public class PointToPointIntent extends Intent {
    private final SwitchPort ingressPort;
    private final SwitchPort egressPort;
    private final IMatch match;
    private final long idleTimeout;
    private final TimeUnit unit;

    /**
     * Constructs an intent representing ingress port to egress port
     * connectivity with a given match condition.
     *
     * @param id ID of this Intent object.
     * @param ingressPort the ingress port where a path is originated.
     * @param egressPort the egress port where a path is terminated.
     * @param match the match condition of a path established by this intent.
     */
    public PointToPointIntent(IntentId id,
                              SwitchPort ingressPort, SwitchPort egressPort,
                              IMatch match) {
        this(id, ingressPort, egressPort, match, 0, TimeUnit.SECONDS);
    }

    /**
     * Constructs an intent representing ingress port to egress port
     * connectivity with a given match condition
     * and idle timeout.
     *
     * @param id ID of this Intent object.
     * @param ingressPort the ingress port where a path is originated.
     * @param egressPort the egress port where a path is terminated.
     * @param match the match condition of a path established by this intent.
     * @param idleTimeout the maximum time to be idle.
     * @param unit the unit of the idleTimeout argument.
     */
    public PointToPointIntent(IntentId id,
                              SwitchPort ingressPort, SwitchPort egressPort,
                              IMatch match, long idleTimeout, TimeUnit unit) {
        super(id);

        checkArgument(idleTimeout >= 0, "idleTimeout should not be negative");

        this.ingressPort = checkNotNull(ingressPort);
        this.egressPort = checkNotNull(egressPort);
        this.match = checkNotNull(match);
        this.idleTimeout = idleTimeout;
        this.unit = checkNotNull(unit);
    }

    /**
     * Returns the ingress port.
     *
     * @return the ingress port.
     */
    public SwitchPort getIngressPort() {
        return ingressPort;
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
     * Returns the match condition.
     *
     * @return the match condition.
     */
    public IMatch getMatch() {
        return match;
    }

    /**
     * Returns the idle timeout.
     *
     * @return the idle timeout.
     */
    public Pair<Long, TimeUnit> getIdleTimeout() {
        return new Pair<>(idleTimeout, unit);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId(), ingressPort, egressPort, match, idleTimeout, unit);
    }

    /**
     * Compares the specified object with this intent for equality.
     *
     * Note: Comparison of idleTimeout value is done in micro-second precision.
     * Then the value less than a micro second is truncated. In addition, the comparison
     * is done between long values. It causes overflow if the idleTimeout is large.
     *
     * @param obj the object to be compared with this intent for equality.
     * @return true if the specified object is equal to this intent.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof PointToPointIntent)) {
            return false;
        }

        PointToPointIntent that = (PointToPointIntent) obj;
        long thisIdleTimeoutInMicro = this.unit.toMicros(this.idleTimeout);
        long thatIdleTimeoutInMicro = that.unit.toMicros(that.idleTimeout);

        return Objects.equal(this.getId(), that.getId())
                && Objects.equal(this.ingressPort, that.ingressPort)
                && Objects.equal(this.egressPort, that.egressPort)
                && Objects.equal(this.match, that.match)
                && Objects.equal(thisIdleTimeoutInMicro, thatIdleTimeoutInMicro);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", getId())
                .add("ingressPort", ingressPort)
                .add("egressPort", egressPort)
                .add("match", match)
                .add("idleTimeout", idleTimeout)
                .add("unit", unit)
                .toString();
    }
}
