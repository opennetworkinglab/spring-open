package net.onrc.onos.core.matchaction.match;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.IPv4;

/**
 * A match object (traffic specifier) for packet nodes, flow-paths and intents.
 * <p>
 * This class does not have a switch ID and a port number. They are handled by
 * MatchAction, IFlow or Intent class.
 */
public class PacketMatch implements IMatch {

    // Match fields
    protected MACAddress srcMacAddress;
    protected MACAddress dstMacAddress;
    protected IPv4 srcIpAddress;
    protected IPv4 dstIpAddress;

    /**
     * Constructor.
     */
    public PacketMatch() {
        this(null, null, null, null);
    }

    /**
     * Constructor.
     *
     * @param srcMac Source Host MAC Address
     * @param dstMac Destination Host MAC Address
     * @param srcIp Source IP Address
     * @param dstIp Destination IP Address
     */
    public PacketMatch(MACAddress srcMac, MACAddress dstMac,
            IPv4 srcIp, IPv4 dstIp) {
        this.srcMacAddress = srcMac;
        this.dstMacAddress = dstMac;
        this.srcIpAddress = srcIp;
        this.dstIpAddress = dstIp;
    }

    /**
     * Gets the source host MAC address.
     *
     * @return The source host MAC address.
     */
    public MACAddress getSrcMacAddress() {
        return srcMacAddress;
    }

    /**
     * Gets the destination host MAC address.
     *
     * @return The destination host MAC address.
     */
    public MACAddress getDstMacAddress() {
        return dstMacAddress;
    }

    /**
     * Gets the source host IP address.
     *
     * @return The source host IP address.
     */
    public IPv4 getSrcIpAddress() {
        return srcIpAddress;
    }

    /**
     * Gets the destination host IP address.
     *
     * @return The destination host IP address.
     */
    public IPv4 getDstIpAddress() {
        return dstIpAddress;
    }
}
