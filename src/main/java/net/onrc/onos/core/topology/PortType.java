package net.onrc.onos.core.topology;

public enum PortType {
    /**
     *  Ethernet port is a port on Ethernet Switch.
     */
    ETHERNET_PORT,
    /**
     * WDM port is port connecting two Optical Switches,
     * WDM port is only connected to another WDM port.
     */
    WDM_PORT,
    /**
     * Transponder port is a port on Optical Switch, which will be connected to Packet/Ethernet Switch.
     * Transponder port is connected to Ethernet port.
     */
    TRANSPONDER_PORT;
}
