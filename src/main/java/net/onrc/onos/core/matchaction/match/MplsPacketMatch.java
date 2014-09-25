package net.onrc.onos.core.matchaction.match;

public class MplsPacketMatch implements Match {

    private final int mplsLabel;

    public MplsPacketMatch(int label) {
        this.mplsLabel = label;
    }

    public int getMplsLabel() {
        return mplsLabel;
    }

}
