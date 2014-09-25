package net.onrc.onos.core.matchaction.match;

public class MplsMatch implements Match {

    private final int mplsLabel;

    public MplsMatch(int label) {
        this.mplsLabel = label;
    }

    public int getMplsLabel() {
        return mplsLabel;
    }

}
