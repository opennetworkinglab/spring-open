package net.onrc.onos.core.topology;

/**
 * State to show configuration state of this element.
 */
public enum ConfigState {
    /**
     * Existence of the element was not configured, but discovered.
     */
    NOT_CONFIGURED,
    /**
     * Existence of the element was configured by operator.
     */
    CONFIGURED
}
