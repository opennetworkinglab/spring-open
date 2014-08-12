package net.onrc.onos.core.matchaction;

/**
 * TODO.
 */
public interface MatchActionOperationsEvent {

    /**
     * TODO.
     *
     * @return
     */
    public MatchActionOperationsId getId();

    /**
     * TODO.
     *
     * @return
     */
    public MatchActionOperationsState getState();

    /**
     * Returns the time at which the event was created.
     *
     * @return the time in milliseconds since start of epoch
     */
    long getTime();
}
