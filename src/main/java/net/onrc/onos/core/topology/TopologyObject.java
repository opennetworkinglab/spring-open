package net.onrc.onos.core.topology;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Base class for Topology Objects.
 */
public abstract class TopologyObject implements ITopologyElement {

    /**
     * Topology snapshot this object belongs to.
     */
    protected volatile BaseInternalTopology topology;

    // XXX Updater to be used once we implement snapshot update.
    // Should it be static or not.
    //     static: Low memory consumption, but higher contention on atomic update
    // non-static: Updater per instance, but less chance of contention
//    private static final AtomicReferenceFieldUpdater<TopologyObject, TopologyImpl>
//        TOPOLOGY_UPDATER =
//            AtomicReferenceFieldUpdater.newUpdater(
//                        TopologyObject.class, TopologyImpl.class, "topology");

    /**
     * Constructor.
     *
     * @param topology Topology instance this object belongs to
     */
    protected TopologyObject(BaseInternalTopology topology) {
        this.topology = checkNotNull(topology);
    }

    // TODO Add method to replace topology snapshot
    //  - Request TopologyManager for latest TopologyImpl and swap?
    //  - Make caller specify TopologyImpl instance?


    /**
     * Returns the type of topology object.
     *
     * @return the type of the topology object
     */
    @Override
    public abstract String getType();
}
