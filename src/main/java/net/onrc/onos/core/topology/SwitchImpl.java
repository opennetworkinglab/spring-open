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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Switch Object stored in In-memory Topology.
 * <p/>
 * TODO REMOVE following design memo: This object itself may hold the DBObject,
 * but this Object itself will not issue any read/write to the DataStore.
 */
public class SwitchImpl extends TopologyObject implements Switch {

    private Dpid dpid;

    public SwitchImpl(Topology topology, Long dpid) {
        this(topology, new Dpid(dpid));
    }

    public SwitchImpl(Topology topology, Dpid dpid) {
        super(topology);
        this.dpid = dpid;
    }

    @Override
    public Dpid getDpid() {
        return dpid;
    }

    @Override
    public Collection<Port> getPorts() {
        return topology.getPorts(getDpid());
    }

    @Override
    public Port getPort(PortNumber number) {
        return topology.getPort(getDpid(), number);
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
    public Collection<Device> getDevices() {
        // TODO Should switch also store a list of attached devices to avoid
        // calculating this every time?
        List<Device> devices = new ArrayList<Device>();

        for (Port port : getPorts()) {
            for (Device device : port.getDevices()) {
                devices.add(device);
            }
        }

        return devices;
    }

    public Port addPort(Long portNumber) {
        PortImpl port = new PortImpl(topology, this, portNumber);
        ((TopologyImpl) topology).putPort(port);
        return port;
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
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
       justification = "getStringAttribute might return null once implemented")
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
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String toString() {
        return dpid.toString();
    }
}
