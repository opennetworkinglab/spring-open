package net.onrc.onos.core.intent;

import java.util.HashSet;
import java.util.Set;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.intent.IntentOperation.Operator;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.FlowEntryActions;
import net.onrc.onos.core.util.FlowEntryId;
import net.onrc.onos.core.util.FlowEntryUserState;

/**
 * @author Brian O'Connor <bocon@onlab.us>
 */

public class FlowEntry {
    protected long sw;
    protected Match match;
    protected Set<Action> actions;
    protected Operator operator;
    protected int hardTimeout = 0;
    protected int idleTimeout = 0;
    protected long flowEntryId;

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

    /***
     * Gets hard timeout value in seconds.
     *
     * @return the hard timeout value in seconds
     */
    public int getHardTimeout() {
        return hardTimeout;
    }

    /***
     * Gets idle timeout value in seconds.
     *
     * @return the idle timeout value in seconds
     */
    public int getIdleTimeout() {
        return idleTimeout;
    }

    /***
     * Sets hard timeout value in seconds.
     *
     * @param the hard timeout value in seconds
     */
    public void setHardTimeout(int hardTimeout) {
        this.hardTimeout = hardTimeout;
    }

    /***
     * Sets idle timeout value in seconds.
     *
     * @param the idle timeout value in seconds
     */
    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    /***
     * Gets flowEntryId.
     *
     * @param the flowEntryId to be set in cookie
     */
    public long getFlowEntryId() {
        return flowEntryId;
    }

    /***
     * Sets flowEntryId.
     *
     * @param the flowEntryId to be set in cookie
     */
    public void setFlowEntryId(long flowEntryId) {
        this.flowEntryId = flowEntryId;
    }

    @Override
    public String toString() {
        return match + "->" + actions;
    }

    public long getSwitch() {
        return sw;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator op) {
        operator = op;
    }

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


    @Override
    public int hashCode() {
        return match.hashCode();
    }

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
