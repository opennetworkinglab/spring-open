package net.onrc.onos.api.flowmanager;

/**
 * An generator of {@link FlowId}.
 */
public interface FlowIdGenerator {
    /**
     * Generates a globally unique {@link FlowId} instance.
     *
     * @return a globally unique {@link FlowId} instance.
     */
    FlowId getNextId();
}
