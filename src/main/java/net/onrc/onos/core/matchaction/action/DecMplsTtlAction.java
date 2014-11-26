package net.onrc.onos.core.matchaction.action;

public class DecMplsTtlAction implements Action {
    private final int dec;

    /**
     * Default constructor for Kryo deserialization.
     */
    @Deprecated
    public DecMplsTtlAction() {
        this.dec = 0;
    }

    public DecMplsTtlAction(int d) {
        this.dec = d;
    }

    public int getDec() {
        return dec;
    }

}
