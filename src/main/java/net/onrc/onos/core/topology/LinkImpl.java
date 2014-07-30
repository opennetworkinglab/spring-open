package net.onrc.onos.core.topology;

import java.util.Map;

import net.onrc.onos.core.util.LinkTuple;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Handler to Link object stored in In-memory Topology snapshot.
 * <p/>
 */
public class LinkImpl extends TopologyObject implements Link {

    private final LinkTuple id;


    /**
     * Creates a Link handler object.
     *
     * @param topology Topology instance this object belongs to
     * @param linkTuple Link identifier
     */
    LinkImpl(TopologyInternal topology, LinkTuple linkTuple) {
        super(topology);
        this.id = checkNotNull(linkTuple);
    }

    @Override
    public LinkTuple getLinkTuple() {
        return id;
    }

    @Override
    public Switch getSrcSwitch() {
        topology.acquireReadLock();
        try {
            return topology.getSwitch(id.getSrc().getDpid());
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Port getSrcPort() {
        topology.acquireReadLock();
        try {
            return topology.getPort(id.getSrc());
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Switch getDstSwitch() {
        topology.acquireReadLock();
        try {
            return topology.getSwitch(id.getDst().getDpid());
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Port getDstPort() {
        topology.acquireReadLock();
        try {
            return topology.getPort(id.getDst());
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public long getLastSeenTime() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Double getCapacity() {
        return this.topology.getLinkEvent(id).getCapacity();
    }

    /**
     * Returns the link type of the link.
     *
     * @return {@link net.onrc.onos.core.topology.LinkType} for this link
     */
    @Override
    public LinkType getLinkType() {
        return LinkType.valueOf(getStringAttribute(TopologyElement.ELEMENT_TYPE,
                LinkType.ETHERNET_LINK.toString()));
    }

    @Override
    public String getStringAttribute(String attr) {
        return this.topology.getLinkEvent(id).getStringAttribute(attr);
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
        return this.topology.getLinkEvent(id).getAllStringAttributes();
    }

    @Override
    public String toString() {
        return String.format("%s --(cap:%f Mbps)--> %s",
                getSrcPort().toString(),
                getCapacity(),
                getDstPort().toString());
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
