package net.onrc.onos.api.packet;

import java.util.List;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.util.SwitchPort;

/**
 * Provides packet services to ONOS applications. This includes both the
 * ability to receive packets from the network, and to send packets out
 * ports in the network. The packet service provides a global context for
 * packet operations; an application can send/receive packets to/from any
 * port in the network.
 * <p/>
 * NOTE: Global packet-ins are currently not implemented. An application can
 * subscribe to local packet-ins only at the moment.
 */
public interface IPacketService extends IFloodlightService {
    /**
     * Register to listen for packet-in events.
     *
     * @param listener the function to call when a packet-in event is received
     */
    public void registerPacketListener(IPacketListener listener);

    // TODO investigate using Port objects again when Ports can be safely
    // passed around.
    /**
     * Send a packet out a specific port in the network.
     * @param eth the packet to send
     * @param switchPort the port to send the packet out
     */
    public void sendPacket(Ethernet eth, SwitchPort switchPort);

    /**
     * Send a packet out multiple ports in the network.
     * <p/>
     * NOTE: currently unimplemented.
     *
     * @param eth the packet to send
     * @param switchPorts a list of ports to send the packet out
     */
    public void sendPacket(Ethernet eth, List<SwitchPort> switchPorts);

    /**
     * Broadcast the packet out all edge ports in the network. An edge port is
     * defined as any port that doesn't have a link to another switch.
     * <p/>
     * By default, this function does not broadcast to external networks.
     * <p/>
     * NOTE: currently unimplemented.
     *
     * @param eth the packet to broadcast
     */
    public void broadcastPacketOutInternalEdge(Ethernet eth);

    /**
     * Broadcast the packet out all edge ports in the network, except for the
     * specified excluded port. An edge port is defined as any port that
     * doesn't have a link to another switch.
     * <p/>
     * This is useful for packets that are received from a host in the
     * dataplane, where we want to broadcast the packet to everyone apart from
     * the host that sent it.
     * <p/>
     * By default, this function does not broadcast to external networks.
     *
     * @param eth the packet to broadcast
     * @param inSwitchPort the exception port that the packet is not
     * broadcast out
     */
    public void broadcastPacketOutInternalEdge(Ethernet eth, SwitchPort inSwitchPort);
}
