package net.onrc.onos.core.util.distributed.sharedlog.exception;

import com.google.common.annotations.Beta;

import net.onrc.onos.core.util.distributed.sharedlog.SeqNum;

/**
 * Exception thrown, when log can no longer be replayed.
 * Caller need to jump to the available snapshot and restart from snapshot.
 */
@Beta
public class LogNotContiguous extends Exception {

    /**
     * Construct a new exception.
     *
     * @param failed sequence number which failed
     */
    public LogNotContiguous(final SeqNum failed) {
        this(failed + " cannot be read");
    }

    /**
     * Constructs a new exception with the specified detail message.
     * {@link Exception#Exception(String)}
     *
     * @param message failure description
     */
    protected LogNotContiguous(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     * {@link Exception#Exception(Throwable)}
     *
     * @param failed sequence number which failed
     * @param cause exception causing this.
     */
    public LogNotContiguous(SeqNum failed, Throwable cause) {
        this(failed + " cannot be read", cause);
    }

    /**
     * Constructs a new exception with the specified cause.
     * {@link Exception#Exception(Throwable)}
     *
     * @param cause exception causing this.
     */
    protected LogNotContiguous(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * {@link Exception#Exception(String, Throwable)}
     *
     * @param message failure description
     * @param cause exception causing this.
     */
    public LogNotContiguous(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }
}
