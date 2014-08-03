package net.onrc.onos.core.util.distributed.sharedlog;

import com.google.common.annotations.Beta;

/**
 * Listener interface for SharedLogObject consumer.
 */
@Beta
public interface LogEventListener {

    // TODO Whether to expose logValue is TBD
    // if exposed, one may manually apply logValue without going through runtime
    /**
     * Notification for .
     *
     * @param seq updated log entry sequence number
     */
    public void logAdded(SeqNum seq/*, ByteValue logValue*/);
}
