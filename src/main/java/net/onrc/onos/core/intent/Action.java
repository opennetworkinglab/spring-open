package net.onrc.onos.core.intent;

import net.onrc.onos.core.util.FlowEntryAction;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.action.OFAction;

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

    /**
     * Builds and returns an OFAction given an OFFactory.
     *
     * @param factory the OFFactory to use for building
     * @return the OFAction
     */
    public abstract OFAction getOFAction(OFFactory factory);
}
