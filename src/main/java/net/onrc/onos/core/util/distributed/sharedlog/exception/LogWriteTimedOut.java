package net.onrc.onos.core.util.distributed.sharedlog.exception;

import com.google.common.annotations.Beta;

/**
 * Exception thrown, when allocated sequence number timed out.
 */
@Beta
public class LogWriteTimedOut extends Exception {

    /**
     * {@link Exception#Exception()}.
     */
    public LogWriteTimedOut() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     * {@link Exception#Exception(String)}
     *
     * @param message failure description
     */
    public LogWriteTimedOut(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     * {@link Exception#Exception(Throwable)}
     *
     * @param cause exception causing this.
     */
    public LogWriteTimedOut(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * {@link Exception#Exception(String, Throwable)}
     *
     * @param message failure description
     * @param cause exception causing this.
     */
    public LogWriteTimedOut(String message, Throwable cause) {
        super(message, cause);
    }
}
