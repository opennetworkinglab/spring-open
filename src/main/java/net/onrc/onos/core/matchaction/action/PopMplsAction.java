package net.onrc.onos.core.matchaction.action;

import org.projectfloodlight.openflow.types.EthType;

public class PopMplsAction implements Action {
    private final int ethtype;

    /**
     * Default constructor for Kryo deserialization.
     */
    @Deprecated
    public PopMplsAction() {
        this.ethtype = EthType.IPv4.getValue();
    }

    public PopMplsAction(EthType ethtype) {
        this.ethtype = ethtype.getValue();
    }

    public EthType getEthType() {
        return EthType.of(ethtype);
    }
}
