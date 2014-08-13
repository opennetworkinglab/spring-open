package net.onrc.onos.api.flowmanager;

/**
 * The class which expresses the state changes of Flow object.
 */
public class FlowStateChange {
    private final FlowId flowId;
    private final FlowState current;
    private final FlowState previous;

    /**
     * Creates {@link FlowStateChange} instance.
     *
     * @param flowId the ID of the target flow
     * @param current the current state of the flow
     * @param previous the previous state of the flow
     */
    FlowStateChange(FlowId flowId, FlowState current, FlowState previous) {
        this.flowId = flowId;
        this.current = current;
        this.previous = previous;
    }

    /**
     * Gets the ID of the flow.
     *
     * @return the flow ID
     */
    public FlowId getFlowId() {
        return flowId;
    }

    /**
     * Gets the current state of the flow.
     *
     * @return the current state of the flow
     */
    public FlowState getCurrentState() {
        return current;
    }

    /**
     * Gets the previous state of the flow.
     *
     * @return the previous state of the flow
     */
    public FlowState getPreviousState() {
        return previous;
    }
}
