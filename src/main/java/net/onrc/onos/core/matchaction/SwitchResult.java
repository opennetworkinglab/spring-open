package net.onrc.onos.core.matchaction;

import net.onrc.onos.core.util.Dpid;

/**
 * The result of applying a MatchAction operation to a switch.
 */
public class SwitchResult {
    private Dpid sw;
    private Status status;
    private MatchActionOperationsId matchSetId;

    /**
     * Status of the switch operation.
     */
    public enum Status {
        /** Installation of the MatchAction was successful. */
        SUCCESS,

        /** Installation of the MatchAction failed. */
        FAILURE,

        /** No status has been assigned. */
        UNKNOWN
    }

    /**
     * Creates a new SwitchResult object.
     *
     * @param match identifier for MatchActionsOperations that was requested
     * @param sw Dpid of the switch that the operations were applied to
     */
    protected SwitchResult(MatchActionOperationsId match, Dpid sw) {
        this.sw = sw;
        this.status = Status.UNKNOWN;
        this.matchSetId = match;
    }

    /**
     * no-arg constructor for Kryo.
     */
    protected SwitchResult() {
        // Needed for Kryo
    }

    /**
     * Sets the status of the SwitchResult.
     *
     * @param newStatus new status
     */
    protected void setStatus(Status newStatus) {
        this.status = newStatus;
    }

    /**
     * Gets the status of the SwitchResult.
     *
     * @return status
     */
    protected Status getStatus() {
        return this.status;
    }

    /**
     * Gets the identifier for the set of operations that was requested.
     *
     * @return MatchActionOperationsId of the requested set of operations
     */
    protected MatchActionOperationsId getMatchActionOperationsId() {
        return this.matchSetId;
    }

    /**
     * Gets the identifier for the switch that was requested.
     *
     * @return Dpid of the requested switch
     */
    protected Dpid getSwitch() {
        return this.sw;
    }
}
