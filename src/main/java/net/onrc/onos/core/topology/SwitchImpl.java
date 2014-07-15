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
 * Switch Object stored in In-memory Topology.
 * <p/>
 * TODO REMOVE following design memo: This object itself may hold the DBObject,
 * but this Object itself will not issue any read/write to the DataStore.
 */
public class SwitchImpl extends TopologyObject implements Switch {

    //////////////////////////////////////////////////////
    /// Topology element attributes
    ///  - any changes made here needs to be replicated.
    //////////////////////////////////////////////////////
    private SwitchEvent switchObj;

    ///////////////////
    /// In-memory index
    ///////////////////

    // none

    /**
     * Creates a Switch object with empty attributes.
     *
     * @param topology Topology instance this object belongs to
     * @param dpid DPID
     */
    public SwitchImpl(Topology topology, Dpid dpid) {
        this(topology, new SwitchEvent(dpid).freeze());
    }

    /**
     * Creates a Switch object based on {@link SwitchEvent}.
     *
     * @param topology Topology instance this object belongs to
     * @param scSw self contained {@link SwitchEvent}
     */
    public SwitchImpl(Topology topology, SwitchEvent scSw) {
        super(topology);
        Validate.notNull(scSw);

        // TODO should we assume switchObj is already frozen before this call
        //      or expect attribute update will happen after .
        if (scSw.isFrozen()) {
            this.switchObj = scSw;
        } else {
            this.switchObj = new SwitchEvent(scSw);
            this.switchObj.freeze();
        }
    }

    @Override
    public Dpid getDpid() {
        return switchObj.getDpid();
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

    void replaceStringAttributes(SwitchEvent updated) {
        Validate.isTrue(this.getDpid().equals(updated.getDpid()),
                "Wrong SwitchEvent given.");

        // XXX simply replacing whole self-contained object for now
        if (updated.isFrozen()) {
            this.switchObj = updated;
        } else {
            this.switchObj = new SwitchEvent(updated).freeze();
        }
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
        return this.switchObj.getStringAttribute(attr);
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
        return this.switchObj.getAllStringAttributes();
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
}
