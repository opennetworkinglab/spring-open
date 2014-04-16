package net.onrc.onos.api.packet;

import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.Switch;

/**
 * An object that wishes to receive notifications of packets received from the
 * network.
 */
public interface IPacketListener {
    /**
     * Called to notify the object that a packet has been received and pass the
     * payload of the packet to the listener module.
     *
     * @param sw the switch the packet was received from
     * @param inPort the port the packet was received from
     * @param payload the payload of the packet
     */
    public void receive(Switch sw, Port inPort, Ethernet payload);
}
