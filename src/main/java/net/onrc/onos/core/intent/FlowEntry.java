package net.onrc.onos.core.intent;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.intent.IntentOperation.Operator;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.FlowEntryActions;
import net.onrc.onos.core.util.FlowEntryId;
import net.onrc.onos.core.util.FlowEntryUserState;

/**
 * A class to represent an OpenFlow FlowMod.
 * It is OpenFlow v.1.0-centric and contains a Match and an Action.
 */

public class FlowEntry {
    protected long sw;
    protected Match match;
    protected Set<Action> actions;
    protected Operator operator;
    protected int hardTimeout = 0;
    protected int idleTimeout = 0;
    protected long flowEntryId;

    /**
     * Constructor.
     *
     * @param sw switch's DPID
     * @param srcPort source port on switch
     * @param dstPort output port on switch
     * @param srcMac source Ethernet MAC address
     * @param dstMac destination Ethernet MAC address
     * @param srcIpAddress source IP address
     * @param dstIpAddress destination IP address
     * @param operator OpenFlow operation/command (add, remove, etc.)
     */
// CHECKSTYLE:OFF suppress the warning about too many parameters
    public FlowEntry(long sw, long srcPort, long dstPort,
                     MACAddress srcMac, MACAddress dstMac,
                     int srcIpAddress, int dstIpAddress,
                     Operator operator) {
// CHECKSTYLE:ON
        this.sw = sw;
        this.match = new Match(sw, srcPort, srcMac, dstMac, srcIpAddress, dstIpAddress);
        this.actions = new HashSet<Action>();
        this.actions.add(new ForwardAction(dstPort));
        this.operator = operator;
        this.flowEntryId = hashCode();
    }

    /**
     * Gets the switch for this FlowEntry.
     *
     * @return the switch's DPID
     */
    public long getSwitch() {
        return sw;
    }

    /**
     * Gets the operator (i.e. add, remove, error).
     *
     * @return the operator
     */
    public Operator getOperator() {
        return operator;
    }

    /**
     * Sets the FlowMod operation (i.e. add, remove, error).
     *
     * @param op the operator
     */
    public void setOperator(Operator op) {
        operator = op;
    }

    /**
     * Gets hard timeout value in seconds.
     *
     * @return the hard timeout value in seconds
     */
    public int getHardTimeout() {
        return hardTimeout;
    }

    /**
     * Sets hard timeout value in seconds.
     *
     * @param hardTimeout hard timeout value in seconds
     */
    public void setHardTimeout(int hardTimeout) {
        this.hardTimeout = hardTimeout;
    }

    /**
     * Gets idle timeout value in seconds.
     *
     * @return the idle timeout value in seconds
     */
    public int getIdleTimeout() {
        return idleTimeout;
    }

    /**
     * Sets idle timeout value in seconds.
     *
     * @param idleTimeout idle timeout value in seconds
     */
    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    /**
     * Gets flowEntryId.
     *
     * @return the flowEntryId to be set in cookie
     */
    public long getFlowEntryId() {
        return flowEntryId;
    }

    /**
     * Sets flowEntryId.
     *
     * @param flowEntryId flowEntryId to be set in cookie
     */
    public void setFlowEntryId(long flowEntryId) {
        this.flowEntryId = flowEntryId;
    }

    /**
     * Converts the FlowEntry in to a legacy FlowEntry object.
     *
     * @return an equivalent legacy FlowEntry object
     */
    public net.onrc.onos.core.util.FlowEntry getFlowEntry() {
        net.onrc.onos.core.util.FlowEntry entry = new net.onrc.onos.core.util.FlowEntry();
        entry.setDpid(new Dpid(sw));
        entry.setFlowEntryId(new FlowEntryId(flowEntryId));
        entry.setFlowEntryMatch(match.getFlowEntryMatch());
        FlowEntryActions flowEntryActions = new FlowEntryActions();
        for (Action action : actions) {
            flowEntryActions.addAction(action.getFlowEntryAction());
        }
        entry.setFlowEntryActions(flowEntryActions);
        switch (operator) {
            case ADD:
                entry.setFlowEntryUserState(FlowEntryUserState.FE_USER_MODIFY);
                break;
            case REMOVE:
                entry.setFlowEntryUserState(FlowEntryUserState.FE_USER_DELETE);
                break;
            default:
                break;
        }
        entry.setIdleTimeout(idleTimeout);
        entry.setHardTimeout(hardTimeout);
        return entry;
    }

    /**
     * Returns a String representation of this FlowEntry.
     *
     * @return the FlowEntry as a String
     */
    @Override
    public String toString() {
        return match + "->" + actions;
    }

    /**
     * Generates hash using Objects.hash() on the match and actions.
     */
    @Override
    public int hashCode() {
        return Objects.hash(match, actions);
    }

    /**
     * Flow Entries are equal if their matches and action sets are equal.
     *
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FlowEntry)) {
            return false;
        }
        FlowEntry other = (FlowEntry) o;
        // Note: we should not consider the operator for this comparison
        return this.match.equals(other.match)
                && this.actions.containsAll(other.actions)
                && other.actions.containsAll(this.actions);
    }
}
