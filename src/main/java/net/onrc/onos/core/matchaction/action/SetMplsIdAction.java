package net.onrc.onos.core.matchaction.action;

public class SetMplsIdAction implements Action {
    private final int mplsId;

    /**
     * Default constructor for Kryo deserialization.
     */
    @Deprecated
    public SetMplsIdAction() {
        this.mplsId = -1;
    }

    public SetMplsIdAction(int id) {
        this.mplsId = id;
    }

    public int getMplsId() {
        return this.mplsId;
    }
}
