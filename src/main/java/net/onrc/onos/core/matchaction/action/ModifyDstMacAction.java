package net.onrc.onos.core.matchaction.action;

import net.floodlightcontroller.util.MACAddress;

/**
 * An action object to modify destination MAC address.
 * <p>
 * This class does not have a switch ID. The switch ID is handled by
 * MatchAction, Flow or Intent class.
 */
public class ModifyDstMacAction implements Action {
    private final MACAddress dstMac;

    /**
     * Constructor.
     *
     * @param dstMac destination MAC address after the modification
     */
    public ModifyDstMacAction(MACAddress dstMac) {
        this.dstMac = dstMac;
    }

    /**
     * Gets the destination MAC address.
     *
     * @return the destination MAC address
     */
    public MACAddress getDstMac() {
        return dstMac;
    }

}
