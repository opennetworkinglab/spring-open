package net.onrc.onos.core.intent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.intent.IntentOperation.Operator;

import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match.Builder;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;

/**
 * A class to represent an OpenFlow FlowMod. <br>
 * It is OpenFlow v.1.0-centric and contains a Match and an Action.
 */

public class FlowEntry {
    public static final int PRIORITY_DEFAULT = 32768; // Default Flow Priority

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
     * Builds and returns an OFFlowMod given an OFFactory.
     *
     * @param factory the OFFactory to use for building
     * @return the OFFlowMod
     */
    public OFFlowMod buildFlowMod(OFFactory factory) {
        OFFlowMod.Builder builder = null;

        switch (operator) {
        case ADD:
            builder = factory.buildFlowModifyStrict();
            break;
        case REMOVE:
            builder = factory.buildFlowDeleteStrict();
            break;
        default:
            // TODO throw error?
            return null;
        }

        // Build OFMatch
        Builder matchBuilder = match.getOFMatchBuilder(factory);

        // Build OFAction Set
        List<OFAction> actionList = new ArrayList<>(actions.size());
        for (Action action : actions) {
            actionList.add(action.getOFAction(factory));
        }

        OFPort outp = OFPort.of((short) 0xffff); // OF1.0 OFPP.NONE
        if (operator == Operator.REMOVE) {
            if (actionList.size() == 1) {
                if (actionList.get(0).getType() == OFActionType.OUTPUT) {
                    OFActionOutput oa = (OFActionOutput) actionList.get(0);
                    outp = oa.getPort();
                }
            }
        }

        // Build OFFlowMod
        builder.setMatch(matchBuilder.build())
                .setActions(actionList)
                .setIdleTimeout(idleTimeout)
                .setHardTimeout(hardTimeout)
                .setCookie(U64.of(flowEntryId))
                .setBufferId(OFBufferId.NO_BUFFER)
                .setPriority(PRIORITY_DEFAULT)
                .setOutPort(outp);

        /* Note: The following are NOT USED.
         * builder.setFlags()
         * builder.setInstructions()
         * builder.setOutGroup()
         * builder.setTableId()
         * builder.setXid()
         */

        // TODO from Flow Pusher
        // Set the OFPFF_SEND_FLOW_REM flag if the Flow Entry is not
        // permanent.
        //
        // if ((flowEntry.idleTimeout() != 0) ||
        // (flowEntry.hardTimeout() != 0)) {
        // fm.setFlags(OFFlowMod.OFPFF_SEND_FLOW_REM);
        // }

        // TODO do we care?
        // fm.setOutPort(OFPort.OFPP_NONE.getValue());
        // if ((flowModCommand == OFFlowMod.OFPFC_DELETE)
        // || (flowModCommand == OFFlowMod.OFPFC_DELETE_STRICT)) {
        // if (actionOutputPort.portNumber != null) {
        // fm.setOutPort(actionOutputPort.portNumber);
        // }
        // }

        // TODO
        // Set the OFPFF_SEND_FLOW_REM flag if the Flow Entry is not
        // permanent.
        //
        // if ((flowEntry.idleTimeout() != 0) ||
        // (flowEntry.hardTimeout() != 0)) {
        // fm.setFlags(OFFlowMod.OFPFF_SEND_FLOW_REM);
        // }

        return builder.build();
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
    public final int hashCode() {
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
