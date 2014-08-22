package net.onrc.onos.core.matchaction.action;

import static com.google.common.base.Preconditions.checkNotNull;
import net.onrc.onos.core.util.PortNumber;

import com.google.common.base.Objects;

/**
 * An action object to output traffic to specified port.
 * <p>
 * This class does not have a switch ID. The switch ID is handled by
 * MatchAction, Flow or Intent class.
 */
public class OutputAction implements Action {
    private final PortNumber portNumber;

    /**
     * Constructor.
     *
     * @param dstPort The port number of the target output port.
     */
    public OutputAction(PortNumber dstPort) {
        this.portNumber = checkNotNull(dstPort);
    }

    /**
     * Gets the port number of the target output port.
     *
     * @return The port number of the target output port.
     */
    public PortNumber getPortNumber() {
        return portNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(portNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        OutputAction that = (OutputAction) obj;
        return Objects.equal(this.portNumber, that.portNumber);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("portNumber", portNumber)
                .toString();
    }
}
