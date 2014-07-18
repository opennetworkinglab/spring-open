package net.onrc.onos.core.topology;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.Validate;

import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

/**
 * Handler to Port object stored in In-memory Topology snapshot.
 * <p/>
 */
public class PortImpl extends TopologyObject implements Port {

    private final SwitchPort id;


    /**
     * Creates a Port handler object.
     *
     * @param topology Topology instance this object belongs to
     * @param switchPort SwitchPort
     */
    PortImpl(TopologyInternal topology, SwitchPort switchPort) {
        super(topology);
        Validate.notNull(switchPort);
        this.id = switchPort;
    }

    /**
     * Creates a Port handler object.
     *
     * @param topology Topology instance this object belongs to
     * @param dpid DPID
     * @param number PortNumber
     */
    PortImpl(TopologyInternal topology, Dpid dpid, PortNumber number) {
        this(topology, new SwitchPort(dpid, number));
    }

    @Override
    public Dpid getDpid() {
        return asSwitchPort().getDpid();
    }

    @Override
    public PortNumber getNumber() {
        return asSwitchPort().getPortNumber();
    }

    @Override
    public SwitchPort asSwitchPort() {
        return id;
    }

    @Override
    public String getDescription() {
        return getStringAttribute(PortEvent.DESCRIPTION, "");
    }

    @Override
    public Long getHardwareAddress() {
        // TODO implement using attributes?
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Switch getSwitch() {
        topology.acquireReadLock();
        try {
            return topology.getSwitch(getDpid());
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Link getOutgoingLink() {
        topology.acquireReadLock();
        try {
            return topology.getOutgoingLink(asSwitchPort());
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Link getOutgoingLink(String type) {
        topology.acquireReadLock();
        try {
            return topology.getOutgoingLink(asSwitchPort(), type);
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Collection<Link> getOutgoingLinks() {
        topology.acquireReadLock();
        try {
            return topology.getOutgoingLinks(asSwitchPort());
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Link getIncomingLink() {
        topology.acquireReadLock();
        try {
            return topology.getIncomingLink(asSwitchPort());
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Link getIncomingLink(String type) {
        topology.acquireReadLock();
        try {
            return topology.getIncomingLink(asSwitchPort(), type);
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Collection<Link> getIncomingLinks() {
        topology.acquireReadLock();
        try {
            return topology.getIncomingLinks(asSwitchPort());
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Collection<Host> getHosts() {
        topology.acquireReadLock();
        try {
            return topology.getHosts(this.asSwitchPort());
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public String getStringAttribute(String attr) {
        return this.topology.getPortEvent(id).getStringAttribute(attr);
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
        return this.topology.getPortEvent(id).getAllStringAttributes();
    }

    @Override
    public String toString() {
        return String.format("%s:%s",
                getSwitch().getDpid(),
                getNumber());
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
