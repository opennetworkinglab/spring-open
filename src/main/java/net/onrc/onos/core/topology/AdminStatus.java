package net.onrc.onos.core.topology;

/**
 * Management status of this element.
 * <p/>
 * Note: This status only resembles ONOS's recognition of the element.
 * This status is orthogonal to the operational state of the D-plane.
 */
public enum AdminStatus {
    /**
     * ONOS has discovered the element.
     */
    ACTIVE,
    /**
     * ONOS has not yet discovered the element
     * or has observed that the element has disappeared.
     */
    INACTIVE
}
