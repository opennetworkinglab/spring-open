package net.onrc.onos.core.matchaction.match;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.IPv4;
import net.onrc.onos.core.util.IPv4Net;

/**
 * A builder to instantiate PacketMatch class.
 */
public class PacketMatchBuilder {
    private MACAddress srcMac;
    private MACAddress dstMac;
    private Short etherType;
    private IPv4Net srcIp;
    private IPv4Net dstIp;
    private Byte ipProto;
    private Short srcTcpPort;
    private Short dstTcpPort;

    /**
     * Sets source MAC address.
     *
     * @param mac the source MAC address
     * @return this builder
     */
    public PacketMatchBuilder setSrcMac(MACAddress mac) {
        srcMac = mac;
        return this;
    }

    /**
     * Sets destination MAC address.
     *
     * @param mac the destination MAC address
     * @return this builder
     */
    public PacketMatchBuilder setDstMac(MACAddress mac) {
        dstMac = mac;
        return this;
    }

    /**
     * Sets Ethernet type.
     *
     * @param etherTypeNo the Ethernet type
     * @return this builder
     */
    public PacketMatchBuilder setEtherType(short etherTypeNo) {
        etherType = etherTypeNo;
        return this;
    }

    /**
     * Sets source IP address with prefix length.
     *
     * @param ip the source IP address
     * @param prefixLen the prefix length
     * @return this builder
     */
    public PacketMatchBuilder setSrcIp(IPv4 ip, short prefixLen) {
        srcIp = new IPv4Net(ip, prefixLen);
        return this;
    }

    /**
     * Sets source IP address.
     *
     * @param ip the source IP address
     * @return this builder
     */
    public PacketMatchBuilder setSrcIp(IPv4 ip) {
        return setSrcIp(ip, (short) 32);
    }

    /**
     * Sets destination IP address with prefix length.
     *
     * @param ip the destination IP address
     * @param prefixLen the prefix length
     * @return this builder
     */
    public PacketMatchBuilder setDstIp(IPv4 ip, short prefixLen) {
        dstIp = new IPv4Net(ip, prefixLen);
        return this;
    }

    /**
     * Sets destination IP address.
     *
     * @param ip the destination IP address
     * @return this builder
     */
    public PacketMatchBuilder setDstIp(IPv4 ip) {
        return setDstIp(ip, (short) 32);
    }

    /**
     * Sets IP protocol number.
     *
     * @param ipProtoNo the IP protocol
     * @return this builder
     */
    public PacketMatchBuilder setIpProto(byte ipProtoNo) {
        ipProto = ipProtoNo;
        return this;
    }

    /**
     * Sets source TCP port number.
     *
     * @param tcpPort the source TCP port number
     * @return this builder
     */
    public PacketMatchBuilder setSrcTcpPort(short tcpPort) {
        srcTcpPort = tcpPort;
        return this;
    }

    /**
     * Sets destination TCP port number.
     *
     * @param tcpPort the destination TCP port number
     * @return this builder
     */
    public PacketMatchBuilder setDstTcpPort(short tcpPort) {
        dstTcpPort = tcpPort;
        return this;
    }

    /**
     * Builds the PacketMatch instance.
     *
     * @return the PacketMatch instance
     */
    public PacketMatch build() {
        // TODO: check consistency among fields

        return new PacketMatch(srcMac, dstMac,
                etherType,
                srcIp, dstIp, ipProto,
                srcTcpPort, dstTcpPort);
    }
}
