package net.onrc.onos.core.topology;

public enum LinkType {
    /**
     * Ethernet link is link between {@link PortType#ETHERNET_PORT}.
     */
    ETHERNET_LINK,
    /**
     * WDM Link is link between {@link PortType#WDM_PORT}.
     */
    WDM_LINK,
    /**
     * This link is link between {@link PortType#ETHERNET_PORT} and {@link PortType#TRANSPONDER_PORT}.
     */
    PACKET_TPORT_LINK;
}
