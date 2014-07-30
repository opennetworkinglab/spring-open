package net.onrc.onos.core.topology;

import java.util.Collection;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

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
        this.id = checkNotNull(switchPort);
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
        return getSwitchPort().getDpid();
    }

    @Override
    public PortNumber getNumber() {
        return getPortNumber();
    }

    @Override
    public PortNumber getPortNumber() {
        return getSwitchPort().getPortNumber();
    }

    @Override
    public SwitchPort getSwitchPort() {
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
            return topology.getOutgoingLink(getSwitchPort());
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Link getOutgoingLink(String type) {
        topology.acquireReadLock();
        try {
            return topology.getOutgoingLink(getSwitchPort(), type);
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Collection<Link> getOutgoingLinks() {
        topology.acquireReadLock();
        try {
            return topology.getOutgoingLinks(getSwitchPort());
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Link getIncomingLink() {
        topology.acquireReadLock();
        try {
            return topology.getIncomingLink(getSwitchPort());
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Link getIncomingLink(String type) {
        topology.acquireReadLock();
        try {
            return topology.getIncomingLink(getSwitchPort(), type);
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Collection<Link> getIncomingLinks() {
        topology.acquireReadLock();
        try {
            return topology.getIncomingLinks(getSwitchPort());
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Collection<Host> getHosts() {
        topology.acquireReadLock();
        try {
            return topology.getHosts(this.getSwitchPort());
        } finally {
            topology.releaseReadLock();
        }
    }

    /**
     * Returns the port type of this port.
     *
     * @return {@link net.onrc.onos.core.topology.PortType}
     */
    @Override
    public PortType getPortType() {
        return PortType.valueOf(getStringAttribute(TopologyElement.ELEMENT_TYPE,
                PortType.ETHERNET_PORT.toString()));
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
