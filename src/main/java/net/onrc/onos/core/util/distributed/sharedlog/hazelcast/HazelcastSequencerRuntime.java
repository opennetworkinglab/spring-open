package net.onrc.onos.core.util.distributed.sharedlog.hazelcast;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.hazelcast.core.HazelcastInstance;

import net.onrc.onos.core.util.distributed.sharedlog.Sequencer;
import net.onrc.onos.core.util.distributed.sharedlog.SharedLogObjectID;
import net.onrc.onos.core.util.distributed.sharedlog.runtime.SequencerRuntime;

/**
 * Runtime which hands out the Sequencer backed by Hazelcast IAtomicLong.
 */
@Beta
public class HazelcastSequencerRuntime implements SequencerRuntime {

    private final HazelcastInstance instance;

    /**
     * Constructs SequencerRuntime.
     *
     * @param instance HazelcastInstance to use.
     */
    public HazelcastSequencerRuntime(HazelcastInstance instance) {
        this.instance = checkNotNull(instance);
    }

    @Override
    public Sequencer getSequencer(SharedLogObjectID id) {
        return new HazelcastSequencer(
                instance.getAtomicLong(
                        "sequencer://" + id.getObjectName()));
    }
}
