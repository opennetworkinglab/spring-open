package net.onrc.onos.api.rest;

/**
 * Holder class for constants that describe the REST error codes.
 */
public final class RestErrorCodes {

    /**
     * Hidden default constructor for utility class.
     */
    private RestErrorCodes() { }

    /**
     * Enumeration of the ONOS defined error codes.
     */
    public enum RestErrorCode {

        // TODO: These are made up just for testing purposes.

        /** Intent not found. */
        INTENT_NOT_FOUND,

        /** Intent already exists. */
        INTENT_ALREADY_EXISTS,

        /** No path available for the Intent. */
        INTENT_NO_PATH,

        /** An object specified for an intent is invalid (parsing error). */
        INTENT_INVALID
    }

}
