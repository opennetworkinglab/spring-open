package net.onrc.onos.core.matchaction.action;

import org.projectfloodlight.openflow.types.MacAddress;

public class SetSAAction implements Action {

    MacAddress macAddress;

    public SetSAAction (MacAddress macAddress) {
        this.macAddress = macAddress;
    }

    public MacAddress getAddress() {
        return this.macAddress;
    }

}
