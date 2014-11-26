package net.onrc.onos.core.matchaction.action;

public class DecNwTtlAction implements Action {
    private final int dec;

    /**
     * Default constructor for Kryo deserialization.
     */
    @Deprecated
    public DecNwTtlAction() {
        this.dec = 0;
    }

    public DecNwTtlAction(int d) {
        this.dec = d;
    }

    public int getDec() {
        return this.dec;
    }
}
