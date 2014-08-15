package net.onrc.onos.core.matchaction;

/**
 * TODO.
 */
public interface MatchActionOperationsEvent {

    /**
     * TODO.
     *
     * @return Match Action Operations Id (this is a WIP).
     */
    public MatchActionOperationsId getId();

    /**
     * TODO.
     *
     * @return Match Action Operations State (this is a WIP).
     */
    public MatchActionOperationsState getState();

    /**
     * Returns the time at which the event was created.
     *
     * @return the time in milliseconds since start of epoch
     */
    long getTime();
}
