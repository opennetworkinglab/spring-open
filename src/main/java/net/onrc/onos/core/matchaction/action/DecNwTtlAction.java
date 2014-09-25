package net.onrc.onos.core.matchaction.action;

public class DecNwTtlAction implements Action {
    private final int dec;

    public DecNwTtlAction(int d) {
        this.dec = d;
    }

    public int getDec() {
        return this.dec;
    }
}
