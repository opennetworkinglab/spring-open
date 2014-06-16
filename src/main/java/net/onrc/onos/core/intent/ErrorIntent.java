package net.onrc.onos.core.intent;

/**
 * This class is instantiated by Run-times to express intent calculation error.
 */
public class ErrorIntent extends Intent {
    public enum ErrorType {
        /**
         * Intent not supported by runtime.
         */
        UNSUPPORTED_INTENT,

        /**
         * One or more of the switches refereneced by this Intent not found.
         */
        SWITCH_NOT_FOUND,

        /**
         * Path specified by Intent is not in the topology.
         */
        PATH_NOT_FOUND,
    }

    public ErrorType errorType;
    public String message;
    public Intent parentIntent;

    /**
     * Default constructor for Kryo deserialization.
     */
    protected ErrorIntent() {
    }

    /**
     * Constructor.
     *
     * @param errorType error type
     * @param message human-readable error string
     * @param parentIntent related parent Intent
     */
    public ErrorIntent(ErrorType errorType, String message, Intent parentIntent) {
        super(parentIntent.getId());
        this.errorType = errorType;
        this.message = message;
        this.parentIntent = parentIntent;
    }

    /**
     * Generates a hash code using the Intent ID.
     *
     * @return hashcode
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Compares two intent object by type (class) and Intent ID.
     *
     * @param obj other Intent
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
