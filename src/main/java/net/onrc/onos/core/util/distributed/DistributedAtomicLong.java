package net.onrc.onos.core.util.distributed;

// TODO Should it extend Number?
// TODO Only minimum set required for sequencer is defined now. Add CAS, etc.
/**
 * Distributed version of AtomicLong.
 */
public interface DistributedAtomicLong {

    /**
     * Gets the current value.
     *
     * @return current value
     */
    long get();

    /**
     * Atomically adds the given value to the current value.
     *
     * @param delta value to add
     * @return updated value
     */
    long addAndGet(long delta);


    /**
     * Sets to the given value.
     *
     * @param newValue value to set.
     */
    public void set(long newValue);

    /**
     * Atomically adds one to the current value.
     *
     * @return updated value
     */
    public long incrementAndGet();
}
