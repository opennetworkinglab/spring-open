package net.onrc.onos.api.flowmanager;

import static com.google.common.base.Preconditions.checkNotNull;

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

    /**
     * Creates new instance.
     */
    public Tree() {
        super();
    }

    /**
     * Creates new instance from the specified Path object.
     *
     * @param path the Path object
     * @throws IllegalArgumentException if the path object does not form a
     *         single connected directed path topology
     */
    public Tree(Path path) {
        super();
        checkNotNull(path);

        for (FlowLink link : path) {
            if (!addLink(link)) {
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Creates new instance using FlowLinks object.
     *
     * @param links the FlowLinks object
     * @throws IllegalArgumentException if the links object does not form a
     *         single connected directed tree topology
     */
    public Tree(FlowLinks links) {
        super();
        checkNotNull(links);

        for (FlowLink link : links) {
            if (!addLink(link)) {
                throw new IllegalArgumentException();
            }
        }
    }

    private void addPort(SwitchPort port) {
        checkNotNull(port);
        if (!ports.containsKey(port.getDpid())) {
            ports.put(port.getDpid(), new HashSet<PortNumber>());
        }
        ports.get(port.getDpid()).add(port.getPortNumber());
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
        checkNotNull(link);
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

        return true;
    }

    /**
     * Checks if specified dpid exists in this tree.
     *
     * @param dpid DPID to be checked
     * @return true if found, false otherwise
     */
    public boolean hasDpid(Dpid dpid) {
        return ports.containsKey(dpid);
    }

    /**
     * Gets a set of {@link PortNumber}s associated with specified DPID.
     *
     * @param dpid the DPID
     * @return a set of {@link PortNumber}s associated with the DPID, or null if
     *         the dpid does not exist in this tree
     */
    public Set<PortNumber> getPortNumbersOf(Dpid dpid) {
        return ports.get(dpid);
    }

    /**
     * Checks if specified switch-port exists in this tree.
     *
     * @param port SwitchPort object to be checked
     * @return true if found, false otherwise
     */
    public boolean hasSwitchPort(SwitchPort port) {
        Set<PortNumber> portNumbers = getPortNumbersOf(port.getDpid());
        return portNumbers != null && portNumbers.contains(port.getPortNumber());
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
