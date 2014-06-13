package net.onrc.onos.core.intent;

import net.onrc.onos.core.util.FlowEntryAction;

/**
 * An abstract class that represents an OpenFlow action.
 */

public abstract class Action {

    /**
     * This function converts the Action into a legacy FlowEntryAction.
     *
     * @return an equivalent FlowEntryAction object
     */
    public abstract FlowEntryAction getFlowEntryAction();
}
