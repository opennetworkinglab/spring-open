package net.onrc.onos.core.matchaction.action;

public class SetMplsIdAction implements Action {
    private final int mplsId;

    public SetMplsIdAction(int id) {
        this.mplsId = id;
    }

    public int getMplsId() {
        return this.mplsId;
    }
}
