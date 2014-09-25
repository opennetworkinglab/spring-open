package net.onrc.onos.core.matchaction.action;

public class DecMplsTtlAction implements Action {
    private final int dec;

    public DecMplsTtlAction(int d) {
        this.dec = d;
    }

    public int getDec() {
        return dec;
    }

}
