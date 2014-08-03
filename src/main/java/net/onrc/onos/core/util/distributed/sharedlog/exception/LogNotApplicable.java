package net.onrc.onos.core.util.distributed.sharedlog.exception;

import com.google.common.annotations.Beta;

/**
 * Exception thrown, when log cannot be applied to the shared log object.
 */
@Beta
public class LogNotApplicable extends Exception {

    private static final long serialVersionUID = 670547401260137360L;

    /**
     * {@link Exception#Exception()}.
     */
    public LogNotApplicable() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message.
     * {@link Exception#Exception(String)}
     *
     * @param message failure description
     */
    public LogNotApplicable(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     * {@link Exception#Exception(Throwable)}
     *
     * @param cause exception causing this.
     */
    public LogNotApplicable(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * {@link Exception#Exception(String, Throwable)}
     *
     * @param message failure description
     * @param cause exception causing this.
     */
    public LogNotApplicable(String message, Throwable cause) {
        super(message, cause);
    }

}
