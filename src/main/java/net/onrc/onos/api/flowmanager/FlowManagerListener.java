package net.onrc.onos.api.flowmanager;

/**
 * An interface to the FlowManager's listener.
 */
public interface FlowManagerListener {
    /**
     * Handles flow state changes.
     *
     * @param event the state changes of the flow objects
     */
    public void flowStatesChanged(FlowStatesChangedEvent event);

    /**
     * Handles flow batch operation's state changes.
     *
     * @param event the state changes of the flow batch operations
     */
    public void flowBatchStateChanged(FlowBatchStateChangedEvent event);
}
