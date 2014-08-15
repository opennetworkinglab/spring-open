package net.onrc.onos.core.matchaction.action;

import net.onrc.onos.core.util.PortNumber;

/**
 * An action object to output traffic to specified port.
 * <p>
 * This class does not have a switch ID. The switch ID is handled by
 * MatchAction, Flow or Intent class.
 */
public class OutputAction implements Action {
    protected PortNumber portNumber;

    /**
     * Constructor.
     *
     * @param dstPort The port number of the target output port.
     */
    public OutputAction(PortNumber dstPort) {
        this.portNumber = dstPort;
    }

    /**
     * Gets the port number of the target output port.
     *
     * @return The port number of the target output port.
     */
    public PortNumber getPortNumber() {
        return portNumber;
    }
}
