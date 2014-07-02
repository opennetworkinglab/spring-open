package net.onrc.onos.api.batchoperation;

/**
 * Operators used by BatchOperation classes.
 */
public enum BatchOperator {
    /**
     * Adds new intent.
     */
    ADD,

    /**
     * Removes existing intent specified by intent ID.
     */
    REMOVE,

    /**
     * Overwrites existing intent using new intent.
     */
    UPDATE,

    /**
     * Unknown type.
     */
    UNKNOWN,
}
