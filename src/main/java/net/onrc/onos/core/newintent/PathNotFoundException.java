package net.onrc.onos.core.newintent;

import net.onrc.onos.api.newintent.IntentException;

/**
 * An exception thrown when a path is not found.
 */
public class PathNotFoundException extends IntentException {
    private static final long serialVersionUID = -2087045731049914733L;

    public PathNotFoundException() {
        super();
    }

    public PathNotFoundException(String message) {
        super(message);
    }

    public PathNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
