package net.floodlightcontroller.core.test;

import java.util.ArrayList;
import java.util.List;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.packet.DHCP;
import net.onrc.onos.core.packet.DHCPOption;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.packet.IPv4;
import net.onrc.onos.core.packet.UDP;

import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketInReason;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;

/**
 * A class to that creates many types of L2/L3/L4 or OpenFlow packets. This is
 * used in testing.
 * @author alexreimers
 */
public class PacketFactory {
    public static String broadcastMac = "ff:ff:ff:ff:ff:ff";
    public static String broadcastIp = "255.255.255.255";
    protected static OFFactory factory13 = OFFactories.getFactory(OFVersion.OF_13);
    protected static OFFactory factory10 = OFFactories.getFactory(OFVersion.OF_10);

    // protected static BasicFactory OFMessageFactory = new BasicFactory();

    /**
     * Generates a DHCP request OFPacketIn.
     *
     * @param hostMac The host MAC address of for the request.
     * @return An OFPacketIn that contains a DHCP request packet.
     */

    public static OFPacketIn DhcpDiscoveryRequestOFPacketIn10(MACAddress hostMac) {
        byte[] serializedPacket = DhcpDiscoveryRequestEthernet(hostMac).serialize();
        return (factory10.buildPacketIn()
                .setBufferId(OFBufferId.NO_BUFFER)
                .setInPort(OFPort.of(1))
                .setData(serializedPacket)
                .setReason(OFPacketInReason.NO_MATCH)
                .setTotalLen(serializedPacket.length).build());
    }

    public static OFPacketIn DhcpDiscoveryRequestOFPacketIn13(MACAddress hostMac) {
        byte[] serializedPacket = DhcpDiscoveryRequestEthernet(hostMac).serialize();
        return (factory13.buildPacketIn()
                .setBufferId(OFBufferId.NO_BUFFER)
                .setInPort(OFPort.of(1))
                .setData(serializedPacket)
                .setReason(OFPacketInReason.NO_MATCH)
                .setTotalLen(serializedPacket.length).build());
    }

    /**
     * Generates a DHCP request Ethernet frame.
     * @param hostMac The host MAC address of for the request.
     * @returnAn An Ethernet frame that contains a DHCP request packet.
     */
    public static Ethernet DhcpDiscoveryRequestEthernet(MACAddress hostMac) {
        List<DHCPOption> optionList = new ArrayList<DHCPOption>();

        byte[] requestValue = new byte[4];
        requestValue[0] = requestValue[1] = requestValue[2] = requestValue[3] = 0;
        DHCPOption requestOption =
                new DHCPOption()
                        .setCode(DHCP.DHCPOptionCode.OptionCode_RequestedIP.
                                getValue())
                        .setLength((byte) 4)
                        .setData(requestValue);

        byte[] msgTypeValue = new byte[1];
        msgTypeValue[0] = 1; // DHCP request
        DHCPOption msgTypeOption =
                new DHCPOption()
                        .setCode(DHCP.DHCPOptionCode.OptionCode_MessageType.
                                getValue())
                        .setLength((byte) 1)
                        .setData(msgTypeValue);

        byte[] reqParamValue = new byte[4];
        reqParamValue[0] = 1; // subnet mask
        reqParamValue[1] = 3; // Router
        reqParamValue[2] = 6; // Domain Name Server
        reqParamValue[3] = 42; // NTP Server
        DHCPOption reqParamOption =
                new DHCPOption()
                        .setCode(DHCP.DHCPOptionCode.OptionCode_RequestedParameters.
                                getValue())
                        .setLength((byte) 4)
                        .setData(reqParamValue);

        byte[] clientIdValue = new byte[7];
        clientIdValue[0] = 1; // Ethernet
        System.arraycopy(hostMac.toBytes(), 0,
                clientIdValue, 1, 6);
        DHCPOption clientIdOption =
                new DHCPOption()
                        .setCode(DHCP.DHCPOptionCode.OptionCode_ClientID.
                                getValue())
                        .setLength((byte) 7)
                        .setData(clientIdValue);

        DHCPOption endOption =
                new DHCPOption()
                        .setCode(DHCP.DHCPOptionCode.OptionCode_END.
                                getValue())
                        .setLength((byte) 0)
                        .setData(null);

        optionList.add(requestOption);
        optionList.add(msgTypeOption);
        optionList.add(reqParamOption);
        optionList.add(clientIdOption);
        optionList.add(endOption);

        Ethernet requestPacket = new Ethernet();
        requestPacket
                .setSourceMACAddress(hostMac.toBytes())
                .setDestinationMACAddress(broadcastMac)
                .setEtherType(Ethernet.TYPE_IPV4)
                .setPayload(
                        new IPv4()
                                .setVersion((byte) 4)
                                .setDiffServ((byte) 0)
                                .setIdentification((short) 100)
                                .setFlags((byte) 0)
                                .setFragmentOffset((short) 0)
                                .setTtl((byte) 250)
                                .setProtocol(IPv4.PROTOCOL_UDP)
                                .setChecksum((short) 0)
                                .setSourceAddress(0)
                                .setDestinationAddress(broadcastIp)
                                .setPayload(
                                        new UDP()
                                                .setSourcePort(UDP.DHCP_CLIENT_PORT)
                                                .setDestinationPort(UDP.DHCP_SERVER_PORT)
                                                .setChecksum((short) 0)
                                                .setPayload(
                                                        new DHCP()
                                                                .setOpCode(
                                                                        DHCP.OPCODE_REQUEST)
                                                                .setHardwareType(
                                                                        DHCP.HWTYPE_ETHERNET)
                                                                .setHardwareAddressLength(
                                                                        (byte) 6)
                                                                .setHops((byte) 0)
                                                                .setTransactionId(
                                                                        0x00003d1d)
                                                                .setSeconds((short) 0)
                                                                .setFlags((short) 0)
                                                                .setClientIPAddress(0)
                                                                .setYourIPAddress(0)
                                                                .setServerIPAddress(0)
                                                                .setGatewayIPAddress(0)
                                                                .setClientHardwareAddress(
                                                                        hostMac.toBytes())
                                                                .setOptions(optionList))));

        return requestPacket;
    }
}
