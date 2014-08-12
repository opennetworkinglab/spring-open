package net.onrc.onos.core.intent;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.types.OFPort;

/**
 * A class to represent the OpenFlow forwarding action.
 */

class ForwardAction extends Action {
    protected long dstPort;

    /**
     * Constructor.
     *
     * @param dstPort the destination port to forward packets
     */
    public ForwardAction(long dstPort) {
        this.dstPort = dstPort;
    }

    /**
     * Returns a String representation of this ForwardAction.
     *
     * @return the destination port as a String
     */
    @Override
    public String toString() {
        return Long.toString(dstPort);
    }

    @Override
    public OFAction getOFAction(OFFactory factory) {
        return factory.actions().output(OFPort.of((int) dstPort), Short.MAX_VALUE);
    }

    /**
     * A simple hash function that just used the destination port.
     *
     * @return hashcode
     */
    @Override
    public int hashCode() {
        return (int) dstPort;
    }

    /**
     * Objects are equal if they share a destination port.
     *
     * @param o another object to compare to this
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ForwardAction)) {
            return false;
        }
        ForwardAction action = (ForwardAction) o;
        return this.dstPort == action.dstPort;
    }
}
