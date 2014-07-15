package net.onrc.onos.core.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;

import org.apache.commons.lang.Validate;

/**
 * Handler to Switch object stored in In-memory Topology snapshot.
 * <p/>
 *
 */
public class SwitchImpl extends TopologyObject implements Switch {

    private final Dpid id;


    /**
     * Creates a Switch handler object.
     *
     * @param topology Topology instance this object belongs to
     * @param dpid DPID
     */
    SwitchImpl(TopologyInternal topology, Dpid dpid) {
        super(topology);
        Validate.notNull(dpid);
        this.id = dpid;
    }

    @Override
    public Dpid getDpid() {
        return this.id;
    }

    @Override
    public Collection<Port> getPorts() {
        topology.acquireReadLock();
        try {
            return topology.getPorts(getDpid());
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Port getPort(PortNumber number) {
        topology.acquireReadLock();
        try {
            return topology.getPort(getDpid(), number);
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Iterable<Switch> getNeighbors() {
        Set<Switch> neighbors = new HashSet<>();
        for (Link link : getOutgoingLinks()) {
            neighbors.add(link.getDstSwitch());
        }
        // XXX should incoming considered neighbor?
        for (Link link : getIncomingLinks()) {
            neighbors.add(link.getSrcSwitch());
        }
        return neighbors;
    }

    @Override
    public Link getLinkToNeighbor(Dpid neighborDpid) {
        for (Link link : getOutgoingLinks()) {
            if (link.getDstSwitch().getDpid().equals(neighborDpid)) {
                return link;
            }
        }
        return null;
    }

    @Override
    public Collection<Host> getHosts() {
        // TODO Should switch also store a list of attached devices to avoid
        // calculating this every time?
        List<Host> hosts = new ArrayList<Host>();

        for (Port port : getPorts()) {
            for (Host host : port.getHosts()) {
                hosts.add(host);
            }
        }

        return hosts;
    }

    /**
     * Returns the switch type of this switch.
     *
     * @return switch type {@link net.onrc.onos.core.topology.SwitchType} of this switch.
     */
    @Override
    public SwitchType getSwitchType() {
        return SwitchType.valueOf(getStringAttribute(TopologyElement.ELEMENT_TYPE,
                SwitchType.ETHERNET_SWITCH.toString()));
    }

    @Override
    public Iterable<Link> getOutgoingLinks() {
        LinkedList<Link> links = new LinkedList<Link>();
        for (Port port : getPorts()) {
            Link link = port.getOutgoingLink();
            if (link != null) {
                links.add(link);
            }
        }
        return links;
    }

    @Override
    public Iterable<Link> getIncomingLinks() {
        LinkedList<Link> links = new LinkedList<Link>();
        for (Port port : getPorts()) {
            Link link = port.getIncomingLink();
            if (link != null) {
                links.add(link);
            }
        }
        return links;
    }

    @Override
    public String getStringAttribute(String attr) {
        return this.topology.getSwitchEvent(getDpid()).getStringAttribute(attr);
    }

    @Override
    public String getStringAttribute(String attr, String def) {
        final String v = getStringAttribute(attr);
        if (v == null) {
            return def;
        } else {
            return v;
        }
    }

    @Override
    public Map<String, String> getAllStringAttributes() {
        return this.topology.getSwitchEvent(getDpid()).getAllStringAttributes();
    }

    @Override
    public String toString() {
        return getDpid().toString();
    }

    /**
     * Returns the type of topology object.
     *
     * @return the type of the topology object
     */
    @Override
    public String getType() {
        return getStringAttribute(TopologyElement.TYPE, TopologyElement.TYPE_PACKET_LAYER);
    }

    /**
     * Returns the config state of topology element.
     *
     * @return ConfigState
     */
    @Override
    public ConfigState getConfigState() {
        return ConfigState.valueOf(getStringAttribute(TopologyElement.ELEMENT_CONFIG_STATE));
    }

    /**
     * Returns the status of topology element.
     *
     * @return AdminStatus
     */
    @Override
    public AdminStatus getStatus() {
        return AdminStatus.valueOf(getStringAttribute(TopologyElement.ELEMENT_ADMIN_STATUS));
    }
}
