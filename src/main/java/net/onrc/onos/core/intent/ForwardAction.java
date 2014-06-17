package net.onrc.onos.core.intent;

import net.onrc.onos.core.util.FlowEntryAction;

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
    public String toString() {
        return Long.toString(dstPort);
    }

    /**
     * Converts the FowardAction into a legacy FlowEntryAction object.
     *
     * @return an equivalent FlowEntryAction object
     */
    @Override
    public FlowEntryAction getFlowEntryAction() {
        FlowEntryAction action = new FlowEntryAction();
        action.setActionOutput(new net.onrc.onos.core.util.Port((short) dstPort));
        return action;
    }

    /**
     * A simple hash function that just used the destination port.
     *
     * @return hashcode
     */
    public int hashCode() {
        return (int) dstPort;
    }

    /**
     * Objects are equal if they share a destination port.
     *
     * @param o another object to compare to this
     * @return true if equal, false otherwise
     */
    public boolean equals(Object o) {
        if (!(o instanceof ForwardAction)) {
            return false;
        }
        ForwardAction action = (ForwardAction) o;
        return this.dstPort == action.dstPort;
    }
}
