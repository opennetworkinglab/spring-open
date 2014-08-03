package net.onrc.onos.core.util.distributed.sharedlog.runtime;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.annotations.Beta;

import net.onrc.onos.core.util.distributed.sharedlog.Sequencer;
import net.onrc.onos.core.util.distributed.sharedlog.SharedLogObjectID;

/**
 * Runtime to get Sequencer implementation for given SharedLogObjectID.
 */
@Beta
@ThreadSafe
public interface SequencerRuntime {

    /**
     * Gets the Sequencer for specified SharedLogObjectID.
     *
     * @param id SharedLogObjectID
     * @return Sequencer for specified id
     */
    public Sequencer getSequencer(SharedLogObjectID id);
}
