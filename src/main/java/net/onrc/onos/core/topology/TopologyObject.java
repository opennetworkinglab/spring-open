package net.onrc.onos.core.topology;

import org.apache.commons.lang.Validate;



/**
 * Base class for Topology Objects.
 */
public abstract class TopologyObject implements ITopologyElement {

    // XXX This will be a snapshot, thus should be replaceable
    /**
     * Topology instance this object belongs to.
     */
    protected final Topology topology;

    /**
     * Constructor.
     *
     * @param topology Topology instance this object belongs to
     */
    protected TopologyObject(Topology topology) {
        Validate.notNull(topology);
        this.topology = topology;
    }


    /**
     * Returns the type of topology object.
     *
     * @return the type of the topology object
     */
    @Override
    public abstract String getType();


}
