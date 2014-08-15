package net.onrc.onos.core.newintent;

import net.onrc.onos.api.newintent.IntentException;

/**
 * An exception thrown when a intent compilation fails.
 */
public class IntentCompilationException extends IntentException {
    private static final long serialVersionUID = 235237603018210810L;

    public IntentCompilationException() {
        super();
    }

    public IntentCompilationException(String message) {
        super(message);
    }

    public IntentCompilationException(String message, Throwable cause) {
        super(message, cause);
    }
}
