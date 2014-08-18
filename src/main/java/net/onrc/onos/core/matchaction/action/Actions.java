package net.onrc.onos.core.matchaction.action;

/**
 * Utility for creating an instance of action.
 */
public final class Actions {
    private Actions() {}

    // TODO: consider if it is meaningful to return NullAction
    // instead of just Action
    /**
     * Returns an action representing null action.
     *
     * @return action representing null action
     */
    public static NullAction nullAction() {
        return NullAction.getInstance();
    }
}
