package net.onrc.onos.core.packetservice;

import java.util.Map;

import net.onrc.onos.core.topology.Topology;
import net.onrc.onos.core.topology.Port;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

// TODO The generic broadcast packet shouldn't contain an IP address which is
// only for ARP packets.

/**
 * Notification to all ONOS instances to broadcast this packet out the edge of
 * the network. The edge is defined as any port that doesn't have a link to
 * another switch. The one exception is the port that the packet was received
 * on.
 */
public class BroadcastPacketOutNotification extends PacketOutNotification {

    private static final long serialVersionUID = 1L;

    private final int address;
    private final long inSwitch;
    private final short inPort;

    /**
     * Default constructor, used for deserialization.
     */
    protected BroadcastPacketOutNotification() {
        super();
        this.address = 0;
        this.inSwitch = 0;
        this.inPort = 0;
    }

    /**
     * Class constructor.
     *
     * @param packet   packet data to send in the packet-out
     * @param address  target IP address if the packet is an ARP packet
     * @param inSwitch dpid of the switch the packet was received on
     * @param inPort   port number of the receiving port
     */
    public BroadcastPacketOutNotification(byte[] packet, int address,
                                          long inSwitch, short inPort) {
        super(packet);

        this.address = address;
        this.inSwitch = inSwitch;
        this.inPort = inPort;
    }

    /**
     * Get the dpid of the switch the packet was received on.
     *
     * @return receiving switch dpid
     */
    public long getInSwitch() {
        return inSwitch;
    }

    /**
     * Get the port number of the port the packet was received on.
     *
     * @return receiving port number
     */
    public short getInPort() {
        return inPort;
    }

    /**
     * Get the target IP address if the packet is an ARP packet.
     *
     * @return the target IP address for ARP packets, or null if the packet is
     * not an ARP packet
     */
    public int getTargetAddress() {
        return address;
    }

    @Override
    public Multimap<Long, Short> calculateOutPorts(
            Multimap<Long, Short> localPorts, Topology topology) {
        Multimap<Long, Short> outPorts = HashMultimap.create();

        for (Map.Entry<Long, Short> entry : localPorts.entries()) {
            Port globalPort;
            topology.acquireReadLock();
            try {
                globalPort = topology.getPort(entry.getKey(),
                    entry.getValue().longValue());
            } finally {
                topology.releaseReadLock();
            }

            if ((!entry.getKey().equals(inSwitch) ||
                    !entry.getValue().equals(inPort)) &&
                    globalPort != null &&
                    globalPort.getOutgoingLink() == null) {

                outPorts.put(entry.getKey(), entry.getValue());
            }
        }

        return outPorts;
    }
}
