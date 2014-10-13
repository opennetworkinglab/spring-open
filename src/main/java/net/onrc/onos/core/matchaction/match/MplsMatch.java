package net.onrc.onos.core.matchaction.match;

public class MplsMatch implements Match {

    private final int mplsLabel;
    private final boolean bos;

    public MplsMatch(int label, boolean bos) {
        this.mplsLabel = label;
        this.bos = bos;
    }

    public int getMplsLabel() {
        return mplsLabel;
    }

    public boolean isBos() {
        return this.bos;
    }

}
