package net.onrc.onos.core.newintent;

import net.onrc.onos.api.newintent.IntentException;

/**
 * An exception thrown when intent installation fails.
 */
public class IntentInstallationException extends IntentException {
    private static final long serialVersionUID = 3720268258616014168L;

    public IntentInstallationException() {
        super();
    }

    public IntentInstallationException(String message) {
        super(message);
    }

    public IntentInstallationException(String message, Throwable cause) {
        super(message, cause);
    }
}
