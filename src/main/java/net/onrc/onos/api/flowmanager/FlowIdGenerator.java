package net.onrc.onos.api.flowmanager;

/**
 * An generator of {@link FlowId}.
 */
public interface FlowIdGenerator {
    /**
     * Generates a global unique {@link FlowId} instance.
     *
     * @return a global unique {@link FlowId} instance.
     */
    FlowId getNewId();
}
