package net.onrc.onos.core.topology;

/**
 * Exception thrown, when topology could not mutated due to unmet preconditions.
 */
public class TopologyMutationFailed extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     * {@link RuntimeException#RuntimeException(String)}
     *
     * @param message failure description
     */
    public TopologyMutationFailed(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     * {@link RuntimeException#RuntimeException(Throwable)}
     *
     * @param cause exception causing this.
     */
    public TopologyMutationFailed(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * {@link RuntimeException#RuntimeException(String, Throwable)}
     *
     * @param message failure description
     * @param cause exception causing this.
     */
    public TopologyMutationFailed(String message, Throwable cause) {
        super(message, cause);
    }
}
