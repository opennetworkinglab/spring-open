package net.onrc.onos.core.topology;

import java.util.Map;

import net.onrc.onos.core.util.LinkTuple;

import org.apache.commons.lang.Validate;

/**
 * Link Object stored in In-memory Topology.
 * <p/>
 * TODO REMOVE following design memo: This object itself may hold the DBObject,
 * but this Object itself will not issue any read/write to the DataStore.
 */
public class LinkImpl extends TopologyObject implements Link {

    //////////////////////////////////////////////////////
    /// Topology element attributes
    ///  - any changes made here needs to be replicated.
    //////////////////////////////////////////////////////
    private LinkEvent linkObj;

    // TODO remove?
    protected static final Double DEFAULT_CAPACITY = Double.POSITIVE_INFINITY;
    protected Double capacity = DEFAULT_CAPACITY;

    ///////////////////
    /// In-memory index
    ///////////////////

    // none

    /**
     * Creates a Link object based on {@link LinkEvent}.
     *
     * @param topology Topology instance this object belongs to
     * @param scPort self contained {@link LinkEvent}
     */
    public LinkImpl(Topology topology, LinkEvent scPort) {
        super(topology);
        Validate.notNull(scPort);

        // TODO should we assume linkObj is already frozen before this call
        //      or expect attribute update will happen after .
        if (scPort.isFrozen()) {
            this.linkObj = scPort;
        } else {
            this.linkObj = new LinkEvent(scPort);
            this.linkObj.freeze();
        }
    }

    /**
     * Creates a Link object with empty attributes.
     *
     * @param topology Topology instance this object belongs to
     * @param srcPort source port
     * @param dstPort destination port
     */
    public LinkImpl(Topology topology, Port srcPort, Port dstPort) {
        this(topology,
             new LinkEvent(srcPort.asSwitchPort(),
                           dstPort.asSwitchPort()).freeze());
    }

    @Override
    public LinkTuple getLinkTuple() {
        return linkObj.getLinkTuple();
    }

    @Override
    public Switch getSrcSwitch() {
        topology.acquireReadLock();
        try {
            return topology.getSwitch(linkObj.getSrc().getDpid());
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Port getSrcPort() {
        topology.acquireReadLock();
        try {
            return topology.getPort(linkObj.getSrc());
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Switch getDstSwitch() {
        topology.acquireReadLock();
        try {
            return topology.getSwitch(linkObj.getDst().getDpid());
        } finally {
            topology.releaseReadLock();
        }
    }

    @Override
    public Port getDstPort() {
        topology.acquireReadLock();
        try {
            return topology.getPort(linkObj.getDst());
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
        return capacity;
    }

    void setCapacity(Double capacity) {
        this.capacity = capacity;
    }

    void replaceStringAttributes(LinkEvent updated) {
        Validate.isTrue(this.linkObj.getSrc().equals(updated.getSrc()),
                "Wrong LinkEvent given.");
        Validate.isTrue(this.linkObj.getDst().equals(updated.getDst()),
                "Wrong LinkEvent given.");

        // XXX simply replacing whole self-contained object for now
        if (updated.isFrozen()) {
            this.linkObj = updated;
        } else {
            this.linkObj = new LinkEvent(updated).freeze();
        }
    }


    @Override
    public String getStringAttribute(String attr) {
        return linkObj.getStringAttribute(attr);
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
        return linkObj.getAllStringAttributes();
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
}
