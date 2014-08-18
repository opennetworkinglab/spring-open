package net.onrc.onos.core.matchaction.action;

/**
 * An action which does not affect anything at all.
 * In other words, NOP.
 *
 * This action can be used for intent only. Flow Manager and Match Action Manager
 * may not handle correctly.
 */
public final class NullAction implements Action {
    private static final NullAction INSTANCE = new NullAction();

    private NullAction() {}

    /**
     * Returns singleton of this class.
     *
     * @return singleton of this class
     */
    static NullAction getInstance() {
        return INSTANCE;
    }
}
