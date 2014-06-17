package net.onrc.onos.core.datastore.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;

import net.onrc.onos.core.util.distributed.DistributedAtomicLong;

/**
 * Hazelcast implementation of DistributedAtomicLong.
 */
public class HZDistributedAtomicLong implements DistributedAtomicLong {
    private final IAtomicLong hzAtomicLong;

    // TODO remove dependency HZClient if possible
    /**
     * Creates or Gets the DistributedAtomicLong instance.
     *
     * @param client client to use
     * @param name the name of the DistributedAtomicLong instance
     */
    public HZDistributedAtomicLong(HZClient client, String name) {
        this(client.getHZInstance(), name);
    }

    /**
     * Creates or Gets the DistributedAtomicLong instance.
     *
     * @param instance HazelcastInstance to use
     * @param name the name of the DistributedAtomicLong instance.
     */
    public HZDistributedAtomicLong(HazelcastInstance instance, String name) {
        hzAtomicLong = instance.getAtomicLong(name);
    }

    @Override
    public long get() {
        return hzAtomicLong.get();
    }

    @Override
    public long addAndGet(long delta) {
        return hzAtomicLong.addAndGet(delta);
    }

    @Override
    public void set(long newValue) {
        hzAtomicLong.set(newValue);
    }

    @Override
    public long incrementAndGet() {
        return hzAtomicLong.incrementAndGet();
    }
}
