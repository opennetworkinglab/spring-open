package net.onrc.onos.core.matchaction;

import net.onrc.onos.core.util.Dpid;

public class SwitchResult {
    private Dpid sw;
    private Status status;
    private MatchActionOperationsId matchSetId;

    protected enum Status {
        SUCCESS,
        FAILURE,
        UNKNOWN
    }

    protected SwitchResult(MatchActionOperationsId match, Dpid sw) {
        this.sw = sw;
        this.status = Status.UNKNOWN;
        this.matchSetId = match;
    }

    protected void setStatus(Status newStatus) {
        this.status = newStatus;
    }

    protected Status getStatus() {
        return this.status;
    }

    protected MatchActionOperationsId getMatchActionOperationsId() {
        return this.matchSetId;
    }

    protected Dpid getSwitch() {
        return this.sw;
    }
}
