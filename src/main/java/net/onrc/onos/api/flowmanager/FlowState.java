package net.onrc.onos.api.flowmanager;

/**
 * Represents the state of the Flow object.
 */
public enum FlowState {

    /**
     * The flow object is submitted, but not compiled yet.
     */
    SUBMITTED,

    /**
     * The match-action plan has been compiled from the flow object, but not
     * installed yet.
     */
    COMPILED,

    /**
     * The compiled match-action plan has been installed successfully.
     */
    INSTALLED,

    /**
     * The installed flow is withdrawing.
     */
    WITHDRAWING,

    /**
     * The installed flow has been withdrawn successfully.
     */
    WITHDRAWN,

    /**
     * The FlowManager has failed to compile, install or withdraw the flow.
     */
    FAILED,
}
