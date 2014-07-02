package net.onrc.onos.core.matchaction;

import java.util.Arrays;
import java.util.List;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.api.batchoperation.IBatchOperationTarget;
import net.onrc.onos.core.matchaction.action.IAction;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.match.IMatch;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.IPv4;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

/**
 * A filter and actions for traffic.
 */
public class MatchAction implements IBatchOperationTarget {
    protected String id;
    protected SwitchPort port;
    protected List<IMatch> matches;
    protected List<IAction> actions;

    /**
     * Constructor.
     *
     * @param id ID for this MatchAction object.
     * @param port Switch DPID
     * @param matches The list of IMatch objects as match condition on the port.
     * @param actions The list of IAction objects as actions on the switch.
     */
    public MatchAction(String id, SwitchPort port, List<IMatch> matches,
            List<IAction> actions) {
        this.id = id;
        this.port = port;
        this.matches = matches;
        this.actions = actions;
    }

    /**
     * Constructor.
     * <p>
     * MEMO: This is a sample constructor to create a packet layer match action.
     *
     * @param id ID for this MatchAction object.
     * @param dpid Switch DPID
     * @param srcPort Source Port
     * @param srcMac Source Host MAC Address
     * @param dstMac Destination Host MAC Address
     * @param srcIp Source IP Address
     * @param dstIp Destination IP Address
     * @param dstPort Destination Port
     */
    // CHECKSTYLE:OFF suppress the warning about too many parameters
    public MatchAction(String id, Dpid dpid, PortNumber srcPort,
            MACAddress srcMac, MACAddress dstMac,
            IPv4 srcIp, IPv4 dstIp, PortNumber dstPort) {
        // CHECKSTYLE:ON
        this(id, new SwitchPort(dpid, srcPort),
                Arrays.asList((IMatch) new PacketMatch(srcMac, dstMac, srcIp, dstIp)),
                Arrays.asList((IAction) new OutputAction(dstPort)));
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Gets the switch-port which is the target of this match-action.
     *
     * @return The target switch-port of this match-action.
     */
    public SwitchPort getSwitchPort() {
        return port;
    }

    /**
     * Gets the traffic filter of the match-action.
     *
     * @return The traffic filter.
     */
    public List<IMatch> getMatches() {
        return matches;
    }

    /**
     * Gets the list of actions of the match-action.
     *
     * @return The list of actions.
     */
    public List<IAction> getActions() {
        return actions;
    }
}
