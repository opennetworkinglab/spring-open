package net.onrc.onos.core.matchaction;

/**
 * States for MatchActionOperations objects.
 */
public enum MatchActionOperationsState {
    /** Being initialized. */
    INIT,

    /** All operations that we depend on are finished. */
    RESOLVED,

    /** Operation is pending waiting for dependencies. */
    PENDING,

    /** Operations successfully installed. */
    INSTALLED,

    /** Operations installation failed. */
    FAILED
}

