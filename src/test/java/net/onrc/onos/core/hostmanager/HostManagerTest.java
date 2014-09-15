package net.onrc.onos.core.hostmanager;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Date;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IListener.Command;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IUpdate;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.test.FloodlightTestCase;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.datagrid.IDatagridService;
import net.onrc.onos.core.datagrid.IEventChannel;
import net.onrc.onos.core.datagrid.IEventChannelListener;
import net.onrc.onos.core.packet.ARP;
import net.onrc.onos.core.packet.DHCP;
import net.onrc.onos.core.packet.Data;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.packet.IPacket;
import net.onrc.onos.core.packet.IPv4;
import net.onrc.onos.core.packet.UDP;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.topology.ITopologyListener;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.MockTopology;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketInReason;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.OFPort;

/**
 * Unit tests for the Host Manager module (HostManger).
 * These test cases check the result of add/delete host and
 * verify the result of processPacketIn by injecting fake packets.
 * floodlightProvider, datagridService, topologyService,
 * controllerRegistryService, eventChannel are mocked out.
 */
public class HostManagerTest extends FloodlightTestCase {
    private IPacket pkt0, pkt1, pkt2, pkt3, pkt4;
    private IOFSwitch sw1;
    private long sw1Dpid;
    private long sw1DevPort, sw1DevPort2;
    private HostManager hostManager;
    private OFPacketIn pktIn, pktIn2;
    private FloodlightModuleContext modContext;
    private ITopologyService networkGraphService;
    private IEventChannel<Long, Host> eventChannel;
    private IFloodlightProviderService floodlightProvider;
    private Date lastSeenTimestamp;
    private OFFactory ofact;

    @Override
    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();
        MockTopology topology = new MockTopology();
        IDatagridService datagridService;
        IControllerRegistryService controllerRegistryService;

        topology.createSampleTopology2();
        modContext = new FloodlightModuleContext();

        floodlightProvider = createMock(IFloodlightProviderService.class);
        datagridService = createMock(IDatagridService.class);
        networkGraphService = createMock(ITopologyService.class);
        controllerRegistryService = createMock(IControllerRegistryService.class);
        eventChannel = createMock(IEventChannel.class);
        expect(networkGraphService.getTopology()).andReturn(topology).anyTimes();
        networkGraphService.addListener(anyObject(ITopologyListener.class), eq(false));
        expectLastCall();

        expect(datagridService.createChannel("onos.host", Long.class, Host.class))
        .andReturn(eventChannel).once();
        expect(topology.getOutgoingLink(new Dpid(1L), PortNumber.uint16((short) 100))).andReturn(null).anyTimes();
        expect(datagridService.addListener(
                eq("onos.host"),
                anyObject(IEventChannelListener.class),
                eq(Long.class),
                eq(Host.class)))
                .andReturn(eventChannel).once();

        replay(datagridService);
        replay(networkGraphService);
        replay(controllerRegistryService);

        modContext.addService(IDatagridService.class, datagridService);
        modContext.addService(ITopologyService.class, networkGraphService);
        modContext.addService(IFloodlightProviderService.class, floodlightProvider);
        modContext.getServiceImpl(IFloodlightProviderService.class);
        sw1Dpid = 1L;
        ofact = OFFactories.getFactory(OFVersion.OF_10);

        sw1 = createMockSwitch(sw1Dpid);
        replay(sw1);

        sw1DevPort = 100;
        sw1DevPort2 = 12L;

        hostManager = new HostManager();
        /*
         * Broadcast source address
         */
        this.pkt0 = new Ethernet()
        .setDestinationMACAddress("00:44:33:22:11:33")
        .setSourceMACAddress("FF:FF:FF:FF:FF:FF")
        .setEtherType(Ethernet.TYPE_IPV4)
        .setPayload(
                new IPv4()
                .setTtl((byte) 128)
                .setSourceAddress("192.168.10.1")
                .setDestinationAddress("192.168.255.255")
                .setPayload(getUDP(new Data(new byte[]{0x01}))));

        /*
         * Normal IPv4 packet
         */
        this.pkt1 = new Ethernet()
        .setDestinationMACAddress("00:11:22:33:44:55")
        .setSourceMACAddress("00:44:33:22:11:00")
        .setEtherType(Ethernet.TYPE_IPV4)
        .setPayload(
                new IPv4()
                .setTtl((byte) 128)
                .setSourceAddress("192.168.1.1")
                .setDestinationAddress("192.168.1.2")
                .setPayload(getUDP(new Data(new byte[]{0x01}))));

        /*
         * Same MAC header as pkt1,but not IP address set
         */
        this.pkt2 = new Ethernet()
        .setSourceMACAddress("00:44:33:22:11:01")
        .setDestinationMACAddress("00:11:22:33:44:55")
        .setEtherType(Ethernet.TYPE_IPV4)
        .setPayload(
                new IPv4()
                .setTtl((byte) 128)
                .setPayload(getUDP(new Data(new byte[]{0x01}))));

        /*
         * DHCP packet
         */
        this.pkt3 = new Ethernet()
        .setSourceMACAddress("00:44:33:22:11:01")
        .setDestinationMACAddress("00:11:22:33:44:55")
        .setEtherType(Ethernet.TYPE_IPV4)
        .setPayload(
                new IPv4()
                .setTtl((byte) 128)
                .setSourceAddress("192.168.1.1")
                .setDestinationAddress("192.168.1.2")
                .setPayload(getUDP(
                        new DHCP()
                        .setOpCode(DHCP.OPCODE_REPLY)
                        .setHardwareType(DHCP.HWTYPE_ETHERNET)
                        .setHardwareAddressLength((byte) 6)
                        .setHops((byte) 0)
                        .setTransactionId(0x00003d1d)
                        .setSeconds((short) 0)
                        .setFlags((short) 0)
                        .setClientIPAddress(0)
                        .setYourIPAddress(0)
                        .setServerIPAddress(0)
                        .setGatewayIPAddress(0))));
        /*
         * ARP packet
         */
        this.pkt4 = new Ethernet()
        .setSourceMACAddress("00:44:33:22:11:01")
        .setDestinationMACAddress("00:11:22:33:44:55")
        .setEtherType(Ethernet.TYPE_ARP)
        .setPayload(
                new ARP()
                .setHardwareType(ARP.HW_TYPE_ETHERNET)
                .setProtocolType(ARP.PROTO_TYPE_IP)
                .setHardwareAddressLength((byte) 6)
                .setProtocolAddressLength((byte) 4)
                .setOpCode(ARP.OP_REPLY)
                .setSenderHardwareAddress(Ethernet.toMACAddress("00:44:33:22:11:01"))
                .setSenderProtocolAddress(IPv4.toIPv4AddressBytes("192.168.1.1"))
                .setTargetHardwareAddress(Ethernet.toMACAddress("00:11:22:33:44:55"))
                .setTargetProtocolAddress(IPv4.toIPv4AddressBytes("192.168.1.2")));


        this.pktIn = getPacketIn((short) sw1DevPort);
        this.pktIn2 = getPacketIn((short) sw1DevPort2);

        lastSeenTimestamp = new Date(1);
    }

    @Override
    @After
    public void tearDown() throws Exception {
    }

    public IOFSwitch createMockSwitch(Long id) {
        IOFSwitch mockSwitch = createNiceMock(IOFSwitch.class);
        expect(mockSwitch.getId()).andReturn(id).anyTimes();
        return mockSwitch;
    }

    /**
     * Test set operation on lastSeenTimstamp field in Host.
     */
    @Test
    public void testSetLastSeenTimestamp() {
        Ethernet eth = (Ethernet) pkt1;
        Host srcHost = hostManager.getSourceHostFromPacket(eth, sw1Dpid, sw1DevPort);

        floodlightProvider.addOFMessageListener(eq(OFType.PACKET_IN),
                isA(HostManager.class));
        srcHost.setLastSeenTimestamp(lastSeenTimestamp);
        assertEquals(lastSeenTimestamp, srcHost.getLastSeenTimestamp());
    }
    /**
     * test the functionality to get the source host from Packet header
     * information.
     */
    @Test
    public void testGetSourceHostFromPacket() {
        byte[] address = new byte[] {0x00, 0x44, 0x33, 0x22, 0x11, 0x01};
        MACAddress srcMac = new MACAddress(address);
        Host host1 = new Host(srcMac,
                0,
                null,
                sw1Dpid,
                sw1DevPort,
                null);

        /*
         * test DHCP packet case
         */
        Ethernet eth = (Ethernet) pkt3;
        Host host2 = hostManager.getSourceHostFromPacket(eth, sw1Dpid, sw1DevPort);
        assertEquals(host1, host2);

        /*
         * test ARP packet case
         */
        eth = (Ethernet) pkt4;
        host2 = hostManager.getSourceHostFromPacket(eth, sw1Dpid, sw1DevPort);
        assertEquals(host1, host2);
    }

    /**
     * This test will invoke addHost to add a new host through Packet pkt1.
     * @throws FloodlightModuleException
     */
    @Test
    public void testProcessPacketInAddNewHost() throws FloodlightModuleException {
        floodlightProvider.addOFMessageListener(eq(OFType.PACKET_IN),
                isA(HostManager.class));
        expectLastCall();
        floodlightProvider.publishUpdate(isA(IUpdate.class));
        expectLastCall();
        replay(floodlightProvider);

        hostManager.init(modContext);
        hostManager.startUp(modContext);
        Command cmd = hostManager.processPacketIn(
                sw1, pktIn, (Ethernet) pkt1, (short) sw1DevPort);
        assertEquals(Command.CONTINUE, cmd);

        verify(floodlightProvider);
    }

    /**
     * Test ProcessPacket function.
     * @throws FloodlightModuleException
     */
    @Test
    public void testProcessPacketInHasLink() throws FloodlightModuleException {
        floodlightProvider.addOFMessageListener(eq(OFType.PACKET_IN),
                isA(HostManager.class));
        expectLastCall();
        replay(floodlightProvider);

        hostManager.init(modContext);
        hostManager.startUp(modContext);
        Command cmd = hostManager.processPacketIn(
                sw1, pktIn2, (Ethernet) pkt1, (short) sw1DevPort2);
        assertEquals(Command.CONTINUE, cmd);

        verify(floodlightProvider);
    }

    /**
     * Test return Command.STOP path in processPacketIn method by injecting a broadcast packet.
     */
    @Test
    public void testProcessPacketInStop() {
        Command cmd = hostManager.processPacketIn(
                sw1, pktIn, (Ethernet) pkt0, (short) sw1DevPort);
        assertEquals(Command.STOP, cmd);
    }

    /**
     * Test add a host from the information from packet.
     * @throws FloodlightModuleException
     */
    @Test
    public void testAddHost() throws FloodlightModuleException {
        Ethernet eth = (Ethernet) pkt1;
        Long longmac = eth.getSourceMAC().toLong();
        Host srcHost = hostManager.getSourceHostFromPacket(eth, sw1Dpid, sw1DevPort);

        floodlightProvider.addOFMessageListener(eq(OFType.PACKET_IN),
                isA(HostManager.class));
        expectLastCall();
        floodlightProvider.publishUpdate(isA(IUpdate.class));
        expectLastCall();
        replay(floodlightProvider);

        hostManager.init(modContext);
        hostManager.startUp(modContext);
        hostManager.addHost(longmac, srcHost);

        verify(floodlightProvider);
    }

    /**
     * Test delete a host.
     * @throws FloodlightModuleException
     */
    @Test
    public void testDeleteHost() throws FloodlightModuleException {
        Ethernet eth = (Ethernet) pkt1;
        Host srcHost = hostManager.getSourceHostFromPacket(eth, sw1Dpid, sw1DevPort);

        floodlightProvider.addOFMessageListener(eq(OFType.PACKET_IN),
                isA(HostManager.class));
        expectLastCall();
        floodlightProvider.publishUpdate(isA(IUpdate.class));
        expectLastCall();
        replay(floodlightProvider);

        hostManager.init(modContext);
        hostManager.startUp(modContext);
        hostManager.deleteHost(srcHost);

        verify(floodlightProvider);
    }

    /**
     * Test delete a host by using its source mac address.
     * @throws FloodlightModuleException
     */
    @Test
    public void testDeleteHostByMac() throws FloodlightModuleException {
        Ethernet eth = (Ethernet) pkt1;
        MACAddress mac = eth.getSourceMAC();

        floodlightProvider.addOFMessageListener(eq(OFType.PACKET_IN),
                isA(HostManager.class));
        expectLastCall();
        floodlightProvider.publishUpdate(isA(IUpdate.class));
        expectLastCall();
        replay(floodlightProvider);

        hostManager.init(modContext);
        hostManager.startUp(modContext);
        hostManager.deleteHostByMac(mac);

        verify(floodlightProvider);
    }

    /**
     * Helper for building PacketIns. Defaults to a flowtable miss.
     * @param inport the inport field value
     * @return a PacketIn
     */
    private OFPacketIn getPacketIn(int inport) {
        return ofact.buildPacketIn()
                .setInPort(OFPort.of(inport))
                .setReason(OFPacketInReason.NO_MATCH)
                .build();
    }

    private UDP getUDP(IPacket payload) {
        return (UDP) new UDP()
        .setSourcePort((short) 5000)
        .setDestinationPort((short) 5001)
        .setPayload(payload);
    }
}
