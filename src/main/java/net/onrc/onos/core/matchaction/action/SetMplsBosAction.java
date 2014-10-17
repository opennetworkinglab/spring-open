package net.onrc.onos.core.matchaction.action;

public class SetMplsBosAction implements Action {
    private final boolean mplsBos;

    public SetMplsBosAction(boolean setBos) {
        mplsBos = setBos;
    }

    public boolean isSet() {
        return mplsBos;
    }
}
