package net.onrc.onos.core.matchaction.match;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.IPv4Net;

import com.google.common.base.Objects;

/**
 * A match object (traffic specifier) for packet nodes, flow-paths and intents.
 * <p>
 * This class does not have a switch ID and a in-port number. They are handled
 * by MatchAction, Flow and Intent classes (for example MatchAction class has a
 * separate SwitchPort field). Also, this class does not handle vlans or ttls.
 * <p>
 * TODO: This class should be extensible.
 */
public class PacketMatch implements Match {

    // Match fields
    private final MACAddress srcMac;
    private final MACAddress dstMac;
    private final Short etherType;
    private final IPv4Net srcIp;
    private final IPv4Net dstIp;
    private final Byte ipProto;
    private final Short srcTcpPort;
    private final Short dstTcpPort;

    /**
     * Default constructor for Kryo deserialization.
     */
    @Deprecated
    protected PacketMatch() {
        srcMac = null;
        dstMac = null;
        etherType = null;
        srcIp = null;
        dstIp = null;
        ipProto = null;
        srcTcpPort = null;
        dstTcpPort = null;
    }

    /**
     * Package private constructor.
     * <p>
     * This class should be instantiated by the builder.
     *
     * @param srcMac the source host MAC address
     * @param dstMac the destination host MAC address
     * @param etherType the Ether type
     * @param srcIp the source IP address with IP prefix
     * @param dstIp the destination IP address with IP prefix
     * @param ipProto
     * @param srcTcpPort the source TCP port number
     * @param dstTcpPort the destination TCP port number
     */
    PacketMatch(MACAddress srcMac, MACAddress dstMac,
            Short etherType,
            IPv4Net srcIp, IPv4Net dstIp, Byte ipProto,
            Short srcTcpPort, Short dstTcpPort) {
        this.srcMac = srcMac;
        this.dstMac = dstMac;
        this.etherType = etherType;
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.ipProto = ipProto;
        this.srcTcpPort = srcTcpPort;
        this.dstTcpPort = dstTcpPort;
    }

    /**
     * Gets the source host MAC address.
     *
     * @return the source host MAC address
     */
    public MACAddress getSrcMacAddress() {
        return srcMac;
    }

    /**
     * Gets the destination host MAC address.
     *
     * @return the destination host MAC address
     */
    public MACAddress getDstMacAddress() {
        return dstMac;
    }

    /**
     * Gets the Ether type.
     *
     * @return the Ether type
     */
    public Short getEtherType() {
        return etherType;
    }

    /**
     * Gets the source host IP address.
     *
     * @return the source host IP address
     */
    public IPv4Net getSrcIpAddress() {
        return srcIp;
    }

    /**
     * Gets the destination host IP address.
     *
     * @return the destination host IP address
     */
    public IPv4Net getDstIpAddress() {
        return dstIp;
    }

    /**
     * Gets the IP protocol number.
     *
     * @return the IP protocol number
     */
    public Byte getIpProtocolNumber() {
        return ipProto;
    }

    /**
     * Gets the source TCP port number.
     *
     * @return the source TCP port number
     */
    public Short getSrcTcpPortNumber() {
        return srcTcpPort;
    }

    /**
     * Gets the destination TCP port number.
     *
     * @return the destination TCP port number
     */
    public Short getDstTcpPortNumber() {
        return dstTcpPort;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(srcMac, dstMac, etherType,
                srcIp, dstIp, ipProto,
                srcTcpPort, dstTcpPort);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof PacketMatch)) {
            return false;
        }

        PacketMatch that = (PacketMatch) obj;
        return Objects.equal(this.srcMac, that.srcMac)
                && Objects.equal(this.dstMac, that.dstMac)
                && Objects.equal(this.etherType, that.etherType)
                && Objects.equal(this.srcIp, that.srcIp)
                && Objects.equal(this.dstIp, that.dstIp)
                && Objects.equal(this.ipProto, that.ipProto)
                && Objects.equal(this.srcTcpPort, that.srcTcpPort)
                && Objects.equal(this.dstTcpPort, that.dstTcpPort);
    }

    private Integer toUnsignedInt(Byte number) {
        return number == null ? null : Integer.valueOf(number & 0xFF);
    }

    private Integer toUnsignedInt(Short number) {
        return number == null ? null : Integer.valueOf(number & 0xFFFF);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("srcMac", srcMac)
                .add("dstMac", dstMac)
                .add("etherType", toUnsignedInt(etherType))
                .add("srcIp", srcIp)
                .add("dstIp", dstIp)
                .add("ipProto", toUnsignedInt(ipProto))
                .add("srcTcpPort", toUnsignedInt(srcTcpPort))
                .add("dstTcpPort", toUnsignedInt(dstTcpPort))
                .toString();
    }
}
