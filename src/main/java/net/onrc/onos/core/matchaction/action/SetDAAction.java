package net.onrc.onos.core.matchaction.action;

import org.projectfloodlight.openflow.types.MacAddress;

public class SetDAAction implements Action {
    MacAddress macAddress;

    public SetDAAction(MacAddress macAddress) {
        this.macAddress = macAddress;
    }

    public MacAddress getAddress() {
        return this.macAddress;
    }
}
