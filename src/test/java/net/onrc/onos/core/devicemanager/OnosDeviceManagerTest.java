package net.onrc.onos.core.devicemanager;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Date;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IListener.Command;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IUpdate;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.test.FloodlightTestCase;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.datagrid.IDatagridService;
import net.onrc.onos.core.datagrid.IEventChannel;
import net.onrc.onos.core.datagrid.IEventChannelListener;
import net.onrc.onos.core.intent.MockTopology;
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

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFType;

/**
 * @author patrick.liu@huawei.com
 *         <p/>
 *         Unit tests for the Device Manager module (OnosDeviceManger).
 *         These test cases check the result of add/delete device and
 *         verify the result of processPacketIn through inject faked packets
 *         floodLightProvider, datagridService, networkGraphService,
 *         controllerRegistryService, eventChannel are mocked out.
 */
public class OnosDeviceManagerTest extends FloodlightTestCase {
    private IPacket pkt0, pkt1, pkt2, pkt3, pkt4;
    private IOFSwitch sw1;
    private long sw1Dpid;
    private short sw1DevPort;
    private OnosDeviceManager odm;
    private OFPacketIn pktIn;
    private FloodlightModuleContext modContext;
    private ITopologyService networkGraphService;
    private IEventChannel<Long, OnosDevice> eventChannel;
    private IFloodlightProviderService floodLightProvider;
    private Date lastSeenTimestamp;

    @Override
    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();
        MockTopology topology = new MockTopology();
        IDatagridService datagridService;
        IControllerRegistryService controllerRegistryService;

        topology.createSampleTopology1();
        modContext = new FloodlightModuleContext();

        floodLightProvider = createMock(IFloodlightProviderService.class);
        datagridService = createMock(IDatagridService.class);
        networkGraphService = createMock(ITopologyService.class);
        controllerRegistryService = createMock(IControllerRegistryService.class);
        eventChannel = createMock(IEventChannel.class);
        expect(networkGraphService.getTopology()).andReturn(topology).anyTimes();
        networkGraphService.registerTopologyListener(anyObject(ITopologyListener.class));
        expectLastCall();

        expect(datagridService.createChannel("onos.device", Long.class, OnosDevice.class))
        .andReturn(eventChannel).once();
        expect(topology.getOutgoingLink(1L, 100L)).andReturn(null).anyTimes();
        expect(datagridService.addListener(
                eq("onos.device"),
                anyObject(IEventChannelListener.class),
                eq(Long.class),
                eq(OnosDevice.class)))
                .andReturn(eventChannel).once();

        replay(datagridService);
        replay(networkGraphService);
        replay(controllerRegistryService);

        modContext.addService(IDatagridService.class, datagridService);
        modContext.addService(ITopologyService.class, networkGraphService);
        modContext.addService(IFloodlightProviderService.class, floodLightProvider);
        modContext.getServiceImpl(IFloodlightProviderService.class);
        sw1Dpid = 1L;
        sw1 = createMockSwitch(sw1Dpid);
        replay(sw1);

        sw1DevPort = 100;

        odm = new OnosDeviceManager();
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
                .setPayload(new UDP()
                .setSourcePort((short) 5000)
                .setDestinationPort((short) 5001)
                .setPayload(new Data(new byte[]{0x01}))));
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
                .setPayload(new UDP()
                .setSourcePort((short) 5000)
                .setDestinationPort((short) 5001)
                .setPayload(new Data(new byte[]{0x01}))));
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
                .setPayload(new UDP()
                .setSourcePort((short) 5000)
                .setDestinationPort((short) 5001)
                .setPayload(new Data(new byte[]{0x01}))));
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
                .setPayload(new UDP()
                .setSourcePort((short) 5000)
                .setDestinationPort((short) 5001)
                .setChecksum((short) 0)
                .setPayload(
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


        this.pktIn = new OFPacketIn()
        .setInPort(sw1DevPort);

        lastSeenTimestamp = new Date(1);

        odm.init(modContext);
        odm.startUp(modContext);
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
     * Test set operation on lastSeenTimstamp field in OnosDevice.
     */
    @Test
    public void testSetLastSeenTimestamp() {
        Ethernet eth = (Ethernet) pkt1;
        OnosDevice srcDevice = odm.getSourceDeviceFromPacket(eth, sw1Dpid, sw1DevPort);

        floodLightProvider.addOFMessageListener(EasyMock.eq(OFType.PACKET_IN), EasyMock.isA(OnosDeviceManager.class));
        srcDevice.setLastSeenTimestamp(lastSeenTimestamp);
        assertEquals(lastSeenTimestamp, srcDevice.getLastSeenTimestamp());
    }
    /**
     * test the functionality to get the source device from Packet header
     * information.
     */
    @Test
    public void testGetSourceDeviceFromPacket() {
        byte[] address = new byte[] {0x00, 0x44, 0x33, 0x22, 0x11, 0x01};
        MACAddress srcMac = new MACAddress(address);
        OnosDevice dev1 = new OnosDevice(srcMac,
                null,
                sw1Dpid,
                sw1DevPort,
                null);

        /*
         * test DHCP packet case
         */
        Ethernet eth = (Ethernet) pkt3;
        OnosDevice dev2 = odm.getSourceDeviceFromPacket(eth, sw1Dpid, sw1DevPort);
        assertEquals(dev1, dev2);

        /*
         * test ARP packet case
         */
        eth = (Ethernet) pkt4;
        dev2 = odm.getSourceDeviceFromPacket(eth, sw1Dpid, sw1DevPort);
        assertEquals(dev1, dev2);
    }

    /**
     * This test will invoke addOnosDevice to add a new device through Packet pkt1.
     */
    @Test
    public void testProcessPacketInAddNewDevice() {
        Ethernet eth = (Ethernet) pkt1;
        Long longmac = eth.getSourceMAC().toLong();
        OnosDevice srcDevice = odm.getSourceDeviceFromPacket(eth, sw1Dpid, sw1DevPort);

        floodLightProvider.addOFMessageListener(EasyMock.eq(OFType.PACKET_IN), EasyMock.isA(OnosDeviceManager.class));
        EasyMock.expectLastCall();
        eventChannel.addEntry(longmac, srcDevice);
        EasyMock.expectLastCall();
        floodLightProvider.publishUpdate(EasyMock.isA(IUpdate.class));
        EasyMock.expectLastCall();
        replay(floodLightProvider, eventChannel);

        Command cmd = odm.processPacketIn(sw1, pktIn, (Ethernet) pkt1);
        assertEquals(Command.CONTINUE, cmd);

        verify(eventChannel);
    }

    /**
     * This test will test return Command.STOP path in processPacketIn method
     * by injecting a broadcast packet.
     */
    @Test
    public void testProcessPacketInStop() {
        Command cmd = odm.processPacketIn(sw1, pktIn, (Ethernet) pkt0);
        assertEquals(Command.STOP, cmd);
    }

    /**
     * This tests same packet received case.
     */
    @Test
    public void testProcessPacketInSamePacket() {
        Ethernet eth = (Ethernet) pkt2;
        OnosDevice srcDevice = odm.getSourceDeviceFromPacket(eth, sw1Dpid, sw1DevPort);
        odm.entryAdded(srcDevice);
        srcDevice.setLastSeenTimestamp(lastSeenTimestamp);

        Command cmd = odm.processPacketIn(sw1, pktIn, (Ethernet) pkt2);
        assertEquals(Command.CONTINUE, cmd);
        assertTrue(lastSeenTimestamp.before(srcDevice.getLastSeenTimestamp()));
    }

    /**
     * This tests the packet with the same MAC but the second one without IP address.
     */
    @Test
    public void testProcessPacketInNoIpAddress() {
        Ethernet eth = (Ethernet) pkt3;
        OnosDevice srcDevice = odm.getSourceDeviceFromPacket(eth, sw1Dpid, sw1DevPort);
        odm.entryAdded(srcDevice);
        srcDevice.setLastSeenTimestamp(lastSeenTimestamp);

        Command cmd = odm.processPacketIn(sw1, pktIn, (Ethernet) pkt2);
        assertEquals(Command.CONTINUE, cmd);
        assertTrue(lastSeenTimestamp.before(srcDevice.getLastSeenTimestamp()));
    }

    /**
     * Test add a device from the information from packet.
     */
    @Test
    public void testAddOnosDevice() {
        Ethernet eth = (Ethernet) pkt1;
        Long longmac = eth.getSourceMAC().toLong();
        OnosDevice srcDevice = odm.getSourceDeviceFromPacket(eth, sw1Dpid, sw1DevPort);

        floodLightProvider.addOFMessageListener(EasyMock.eq(OFType.PACKET_IN), EasyMock.isA(OnosDeviceManager.class));
        EasyMock.expectLastCall();
        eventChannel.addEntry(longmac, srcDevice);
        EasyMock.expectLastCall();
        floodLightProvider.publishUpdate(EasyMock.isA(IUpdate.class));
        EasyMock.expectLastCall();
        replay(floodLightProvider, eventChannel);

        odm.addOnosDevice(longmac, srcDevice);

        verify(eventChannel);
    }

    /**
     * Test delete a device.
     */
    @Test
    public void testDeleteOnosDevice() {
        Ethernet eth = (Ethernet) pkt1;
        Long longmac = eth.getSourceMAC().toLong();
        OnosDevice srcDevice = odm.getSourceDeviceFromPacket(eth, sw1Dpid, sw1DevPort);

        floodLightProvider.addOFMessageListener(EasyMock.eq(OFType.PACKET_IN), EasyMock.isA(OnosDeviceManager.class));
        EasyMock.expectLastCall();
        eventChannel.removeEntry(longmac);
        EasyMock.expectLastCall();
        floodLightProvider.publishUpdate(EasyMock.isA(IUpdate.class));
        EasyMock.expectLastCall();
        replay(floodLightProvider, eventChannel);

        odm.deleteOnosDevice(srcDevice);

        verify(eventChannel);
    }

    /**
     * Test delete a device by using its source mac address.
     */
    @Test
    public void testDeleteOnosDeviceByMac() {
        Ethernet eth = (Ethernet) pkt1;
        MACAddress mac = eth.getSourceMAC();
        Long longmac = mac.toLong();
        OnosDevice srcDevice = odm.getSourceDeviceFromPacket(eth, sw1Dpid, sw1DevPort);

        floodLightProvider.addOFMessageListener(EasyMock.eq(OFType.PACKET_IN), EasyMock.isA(OnosDeviceManager.class));
        EasyMock.expectLastCall();
        eventChannel.removeEntry(longmac);
        EasyMock.expectLastCall();
        floodLightProvider.publishUpdate(EasyMock.isA(IUpdate.class));
        EasyMock.expectLastCall();
        replay(floodLightProvider, eventChannel);

        odm.entryAdded(srcDevice);
        odm.deleteOnosDeviceByMac(mac);
        verify(eventChannel);
    }
}
