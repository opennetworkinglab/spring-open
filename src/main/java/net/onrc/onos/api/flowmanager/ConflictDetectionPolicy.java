package net.onrc.onos.api.flowmanager;

/**
 * Conflict detection policies for the flow manager.
 */
public enum ConflictDetectionPolicy {
    /**
     * Do not allow overlap flow-space on any ingress port.
     */
    STRICT,

    /**
     * Keep control of packets flowing through each specified path, tree, etc.
     */
    LOOSE,

    /**
     * No limitation (accepts all, handles by priority).
     */
    FREE
}
