package net.onrc.onos.core.matchaction;

import java.util.EventListener;

/**
 * The listener for MatchActionEvents and MatchActionOperationsEvents.
 * <p>
 * Callers of the MatchActionService should implement both types of listeners to
 * receive updates to asynchronous calls.
 */
public interface MatchActionListener extends EventListener {

    /**
     * Processes the MatchActionOperationsEvent.
     *
     * @param event the event
     */
    public void opertaionsUpdated(MatchActionOperationsEvent event);

    /**
     * Processes the MatchActionEvent.
     *
     * @param event the event
     */
    public void matchActionUpdated(MatchActionEvent event);
}
