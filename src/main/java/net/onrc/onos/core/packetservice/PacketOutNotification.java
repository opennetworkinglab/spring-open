package net.onrc.onos.core.packetservice;

import java.io.Serializable;
import java.util.Arrays;

import net.onrc.onos.core.topology.NetworkGraph;

import com.google.common.collect.Multimap;

/**
 * A PacketOutNotification contains data sent between ONOS instances that
 * directs other instances to send a packet out a set of ports. This is an
 * abstract base class that will be subclassed by specific types of
 * notifications.
 */
public abstract class PacketOutNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    private final byte[] packet;

    /**
     * Default constructor.
     */
    protected PacketOutNotification() {
        packet = null;
    }

    /**
     * Class constructor.
     *
     * @param packet the packet data to send in the packet-out
     */
    public PacketOutNotification(byte[] packet) {
        this.packet = Arrays.copyOf(packet, packet.length);
    }

    /**
     * Gets the packet that needs to be sent into the network.
     *
     * @return the packet data as a serialized byte array
     */
    public byte[] getPacketData() {
        return Arrays.copyOf(packet, packet.length);
    }

    /**
     * Calculate a list of local ports that the packet should be sent out.
     * <p/>
     * A {@link PacketOutNotification} contains a high-level description of the
     * where to send the packet. The receiver of the notification needs to know
     * an explicit list of ports to send the packet out. This function will
     * calculate that list, given the list of edge ports controlled by this
     * instance.
     *
     * @param localPorts the map of locally-controlled ports
     * @param networkGraph an instance of the global network graph
     * @return a multimap of ports that the packet should be sent out,
     * in the form
     * {@code {dpid1 => {portnum1, portnum2, ...}, dpid2 => {portnum1}, ...}}
     */
    public abstract Multimap<Long, Short> calculateOutPorts(
            Multimap<Long, Short> localPorts, NetworkGraph networkGraph);
}
