package net.onrc.onos.core.topology;


/**
 * Base class for Topology Objects.
 */
public class TopologyObject {

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
        this.topology = topology;
    }

}
