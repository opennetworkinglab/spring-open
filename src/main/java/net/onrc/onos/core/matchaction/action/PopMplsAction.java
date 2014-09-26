package net.onrc.onos.core.matchaction.action;

import org.projectfloodlight.openflow.types.EthType;

public class PopMplsAction implements Action {
    private final EthType ethtype;

    public PopMplsAction(EthType ethtype) {
        this.ethtype = ethtype;
    }

    public EthType getEthType() {
        return ethtype;
    }
}
