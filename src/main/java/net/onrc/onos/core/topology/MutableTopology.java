package net.onrc.onos.core.topology;

import net.onrc.onos.core.topology.web.serializers.TopologySerializer;

import org.codehaus.jackson.map.annotate.JsonSerialize;

//TODO move to appropriate package under api
/**
 * MutableTopology, which this instance can be updated to new view.
 * <p>
 * Requires read-lock to access any information on this topology view.
 * <p>
 * Note: This is still read-only view of the topology.
 * <p>
 * The northbound interface to the topology. This interface
 * is presented to the rest of ONOS. It is currently read-only, as we want
 * only the discovery modules to be allowed to modify the topology.
 */
@JsonSerialize(using = TopologySerializer.class)
public interface MutableTopology extends BaseTopology {

    /**
     * Acquire a read lock on the entire topology. The topology will not
     * change while readers have the lock. Must be released using
     * {@link #releaseReadLock()}. This method will block until a read lock is
     * available.
     */
    public void acquireReadLock();

    /**
     * Release the read lock on the topology.
     */
    public void releaseReadLock();
}
