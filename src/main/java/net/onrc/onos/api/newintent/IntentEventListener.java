package net.onrc.onos.api.newintent;

/**
 * Listener for {@link IntentEvent intent events}.
 */
public interface IntentEventListener {
    /**
     * Processes the specified intent event.
     *
     * @param event the event to process
     */
    void event(IntentEvent event);
}
