package net.onrc.onos.api.batchoperation;

/**
 * Operators used by BatchOperation classes.
 */
public enum BatchOperator {
    /**
     * Adds new target object.
     */
    ADD,

    /**
     * Removes existing object.
     */
    REMOVE,

    /**
     * Unknown operator type.
     */
    UNKNOWN,
}
