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

    public FlowEntry(long sw, long srcPort, long dstPort,
                     MACAddress srcMac, MACAddress dstMac,
                     Operator operator) {
        this.sw = sw;
        this.match = new Match(sw, srcPort, srcMac, dstMac);
        this.actions = new HashSet<Action>();
        this.actions.add(new ForwardAction(dstPort));
        this.operator = operator;

    }

    /***
     * Gets hard timeout value in seconds.
     *
     * @return hardTimeout
     */
    public int getHardTimeout() {
        return hardTimeout;
    }

    /***
     * Gets idle timeout value in seconds.
     *
     * @return idleTimeout
     */
    public int getIdleTimeout() {
        return idleTimeout;
    }

    /***
     * Sets hard timeout value in seconds.
     *
     * @param hardTimeout
     */
    public void setHardTimeout(int hardTimeout) {
        this.hardTimeout = hardTimeout;
    }

    /***
     * Sets idle timeout value in seconds.
     *
     * @param idleTimeout
     */
    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
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
        entry.setFlowEntryId(new FlowEntryId(hashCode())); // naive, but useful for now
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
