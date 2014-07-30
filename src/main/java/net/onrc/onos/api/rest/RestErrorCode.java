package net.onrc.onos.api.rest;

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
