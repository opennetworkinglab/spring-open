package net.onrc.onos.core.packetservice;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.onrc.onos.core.topology.MutableTopology;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

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

    private final int address;
    private final Set<SwitchPort> blacklistSwitchPorts;

    /**
     * Default constructor, used for deserialization.
     */
    protected BroadcastPacketOutNotification() {
        super();
        this.address = 0;
        this.blacklistSwitchPorts = null;
    }

    /**
     * Class constructor.
     *
     * @param packet   packet data to send in the packet-out
     * @param address  target IP address if the packet is an ARP packet
     * @param inSwitchPort switch port the packet was received on
     */
    public BroadcastPacketOutNotification(byte[] packet, int address,
            SwitchPort inSwitchPort) {
        super(packet);

        this.address = address;
        this.blacklistSwitchPorts =  new HashSet<SwitchPort>();
        blacklistSwitchPorts.add(inSwitchPort);
    }

    /**
     * Class constructor.
     *
     * @param packet   packet data to send in the packet-out
     * @param address  target IP address if the packet is an ARP packet
     * @param blacklistSwitchPorts switch ports will not be broadcasted to
     */
    public BroadcastPacketOutNotification(byte[] packet, int address,
            Set<SwitchPort> blacklistSwitchPorts) {
        super(packet);

        this.address = address;
        this.blacklistSwitchPorts = new HashSet<SwitchPort>(blacklistSwitchPorts);
    }

    /**
     * Get the blacklist SwitchPorts.
     *
     * @return blacklist SwitchPorts
     */
    public Set<SwitchPort> getInSwitchPort() {
        return Collections.unmodifiableSet(blacklistSwitchPorts);
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
            Multimap<Long, Short> localPorts, MutableTopology mutableTopology) {
        Multimap<Long, Short> outPorts = HashMultimap.create();

        for (Map.Entry<Long, Short> entry : localPorts.entries()) {
            Port globalPort;
            mutableTopology.acquireReadLock();
            try {
                globalPort = mutableTopology.getPort(new Dpid(entry.getKey()),
                    PortNumber.uint16(entry.getValue()));
            } finally {
                mutableTopology.releaseReadLock();
            }
            SwitchPort switchPort =  new SwitchPort(entry.getKey(), entry.getValue());


            if ((blacklistSwitchPorts == null || !blacklistSwitchPorts
                    .contains(switchPort)) &&
                    globalPort != null &&
                    globalPort.getOutgoingLink() == null) {

                outPorts.put(entry.getKey(), entry.getValue());
            }
        }

        return outPorts;
    }

}
