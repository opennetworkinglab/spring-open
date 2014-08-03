package net.onrc.onos.core.util.distributed.sharedlog.hazelcast;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.hazelcast.core.IAtomicLong;

import net.onrc.onos.core.util.distributed.sharedlog.SeqNum;
import net.onrc.onos.core.util.distributed.sharedlog.Sequencer;

/**
 * Sequencer implemented using Hazelcast IAtomicLong.
 */
@Beta
public final class HazelcastSequencer implements Sequencer {

    private final IAtomicLong value;

    /**
     * Constructor.
     *
     * @param value IAtomicLong to use as Sequencer
     */
    public HazelcastSequencer(IAtomicLong value) {
        this.value = checkNotNull(value);
    }

    @Override
    public SeqNum get() {
        return SeqNum.anyValueOf(value.get());
    }

    @Override
    public SeqNum next() {
        long next = value.incrementAndGet();
        if (next == SeqNum.ZERO) {
            next = value.incrementAndGet();
        }
        return SeqNum.valueOf(next);
    }
}
