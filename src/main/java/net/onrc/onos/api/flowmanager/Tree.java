package net.onrc.onos.api.flowmanager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

/**
 * A directed connected tree topology representation used by TreeFlow.
 */
public class Tree extends FlowLinks {
    Map<Dpid, Set<PortNumber>> ports = new HashMap<Dpid, Set<PortNumber>>();
    Map<SwitchPort, FlowLink> inLinks = new HashMap<SwitchPort, FlowLink>();
    Map<SwitchPort, FlowLink> outLinks = new HashMap<SwitchPort, FlowLink>();

    /**
     * Creates new instance.
     */
    public Tree() {
        super();
    }

    /**
     * Creates new instance using Path object.
     */
    public Tree(Path path) {
        super();
        // TODO implement
    }

    private void addPort(SwitchPort port) {
        if (!ports.containsKey(port.dpid())) {
            ports.put(port.dpid(), new HashSet<PortNumber>());
        }
        ports.get(port.dpid()).add(port.port());
    }

    /**
     * Adds FlowLink object to this tree.
     * <p>
     * This method checks specified FlowLink object to keep this Tree object a
     * connected tree.
     *
     * @param link FlowLink object to be added.
     * @return true if succeeded, false otherwise.
     */
    public boolean addLink(FlowLink link) {
        if (links.size() > 0) {
            if (!hasDpid(link.getSrcDpid()) && !hasDpid(link.getDstDpid())) {
                // no attaching point
                return false;
            }
            if (hasDpid(link.getSrcDpid()) && hasDpid(link.getDstDpid())) {
                // loop or duplicated paths
                return false;
            }
        }

        if (hasSwitchPort(link.getSrcSwitchPort())
                || hasSwitchPort(link.getDstSwitchPort())) {
            // some port has already been occupied by another link
            return false;
        }

        links.add(link);
        addPort(link.getSrcSwitchPort());
        addPort(link.getDstSwitchPort());

        inLinks.put(link.getDstSwitchPort(), link);
        outLinks.put(link.getSrcSwitchPort(), link);

        return true;
    }

    /**
     * Checks if specified dpid exists in this tree.
     *
     * @param dpid DPID to be checked.
     * @return true if found, false otherwise.
     */
    public boolean hasDpid(Dpid dpid) {
        return ports.containsKey(dpid);
    }

    /**
     * Checks if specified switch-port exists in this tree.
     *
     * @param port SwitchPort object to be checked.
     * @return true if found, false otherwise.
     */
    public boolean hasSwitchPort(SwitchPort port) {
        return inLinks.containsKey(port) || outLinks.containsKey(port);
    }

    @Override
    public int hashCode() {
        // TODO think if this is correct.
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        // TODO think if this is correct.
        // - has to check equality using a set, not a list?
        return super.equals(o);
    }
}
