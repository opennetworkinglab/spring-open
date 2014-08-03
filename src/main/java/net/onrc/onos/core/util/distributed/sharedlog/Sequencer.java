package net.onrc.onos.core.util.distributed.sharedlog;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.annotations.Beta;

/**
 * Sequencer for LogBasedRuntime.
 */
@Beta
@ThreadSafe
public interface Sequencer {

    /**
     * Gets the current sequence number.
     *
     * @return current sequence number
     */
    public SeqNum get();

    /**
     * Gets the next sequence number.
     *
     * @return next sequence number
     */
    public SeqNum next();
}
