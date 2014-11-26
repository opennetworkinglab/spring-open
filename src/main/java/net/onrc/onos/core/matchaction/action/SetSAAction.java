package net.onrc.onos.core.matchaction.action;

import net.floodlightcontroller.util.MACAddress;

import org.projectfloodlight.openflow.types.MacAddress;

public class SetSAAction implements Action {

    /* Changing OF MacAddress type to ONOS MACAddress to facilitate
     * kyro serialization though APIs still use OF MacAddress
     */
    // MacAddress macAddress;
    MACAddress macAddress;

    /**
     * Default constructor for Kryo deserialization.
     */
    @Deprecated
    public SetSAAction() {
        this.macAddress = null;
    }

    public SetSAAction (MacAddress macAddress) {
        this.macAddress = MACAddress.valueOf(macAddress.getLong());
    }

    public MacAddress getAddress() {
        return MacAddress.of(this.macAddress.toLong());
    }

}
