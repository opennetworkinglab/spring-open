package net.onrc.onos.core.topology;

// TODO Not sure if we need this at all
/**
 * MutableInternalTopology, which this instance can be updated to new view.
 * <p>
 * Requires read-lock to access any information on this topology view.
 * <p>
 * Note: This is still read-only view of the topology.
 */
public interface MutableInternalTopology extends BaseInternalTopology {

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
