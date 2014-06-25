package net.onrc.onos.apps.proxyarp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.api.packet.IPacketService;
import net.onrc.onos.apps.proxyarp.web.ArpWebRoutable;
import net.onrc.onos.core.datagrid.IDatagridService;
import net.onrc.onos.core.datagrid.IEventChannel;
import net.onrc.onos.core.datagrid.IEventChannelListener;
import net.onrc.onos.core.devicemanager.IOnosDeviceService;
import net.onrc.onos.core.flowprogrammer.IFlowPusherService;
import net.onrc.onos.core.main.config.IConfigInfoService;
import net.onrc.onos.core.packet.ARP;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.packet.IPv4;
import net.onrc.onos.core.topology.Device;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.Topology;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// XXX Commented out as workaround for PowerMock + Hazelcast issue.
//@RunWith(PowerMockRunner.class)
//@PowerMockIgnore({ "net.onrc.onos.core.datastore.*", "com.hazelcast.*" })
//@PrepareOnlyThisForTest({ ProxyArpManager.class })
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ProxyArpManagerTest {

    static {
        // configuration to quickly fall back to instance mode for faster test run
        System.setProperty("net.onrc.onos.core.datastore.hazelcast.client.attemptLimit", "0");
    }

    String defaultStrAgingMsec = "60000";
    String defaultStrCleanupMsec = "60000";

    ProxyArpManager arpManager;
    FloodlightModuleContext context;
    IFloodlightProviderService floodligthProviderService;
    IConfigInfoService configInfoService;
    IRestApiService restApiService;
    IDatagridService datagridService;
    IFlowPusherService flowPusherService;
    ITopologyService topologyService;
    IOnosDeviceService onosDeviceService;
    IPacketService packetService;
    Map<String, String> configMap;

    String srcStrMac, dstStrMac, cachedStrMac1, cachedStrMac2, srcStrIp, dstStrIp, cachedStrIp1, cachedStrIp2;
    byte[] srcByteMac, dstByteMac;
    MACAddress dstMac, srcMac, cachedMac1, cachedMac2;
    InetAddress srcIp, dstIp, cachedIp1, cachedIp2;
    Dpid sw1Dpid;
    PortNumber sw1Inport, sw1Outport;
    Short vlanId;
    ARP arpRequest, arpReply, rarpRequest;
    Ethernet ethArpRequest, ethArpReply, ethRarpRequest, ethArpOtherOp;

    Topology topology;
    IEventChannel eg;
    IEventChannelListener el;
    Device dev1;
    Port inPort1, outPort1;
    Switch sw1;
    ArpCache arpCache;
    List<String> arpCacheComparisonList;

    @Before
    public void setUp() throws Exception {
        makeTestedObject();
        makeMock();
        prepareExpectForInit();
        prepareExpectForStartUp();
        prepareExpectForGeneral();
    }

    private void makeTestedObject() throws UnknownHostException {
        //Made tested values
        srcStrMac = "00:00:00:00:00:01";
        dstStrMac = "00:00:00:00:00:02";
        cachedStrMac1 = "00:00:00:00:00:03";
        cachedStrMac2 = "00:00:00:00:00:04";
        srcStrIp = "192.168.0.1";
        dstStrIp = "192.168.0.2";
        cachedStrIp1 = "192.168.0.3";
        cachedStrIp2 = "192.168.0.4";
        srcByteMac = Ethernet.toMACAddress(srcStrMac);
        dstByteMac = Ethernet.toMACAddress(dstStrMac);
        dstMac = new MACAddress(dstByteMac);
        srcMac = new MACAddress(srcByteMac);
        cachedMac1 = new MACAddress(Ethernet.toMACAddress(cachedStrMac1));
        cachedMac2 = new MACAddress(Ethernet.toMACAddress(cachedStrMac2));

        srcIp = InetAddress.getByAddress(IPv4.toIPv4AddressBytes(srcStrIp));
        dstIp = InetAddress.getByAddress(IPv4.toIPv4AddressBytes(dstStrIp));
        cachedIp1 = InetAddress.getByAddress(IPv4.toIPv4AddressBytes(cachedStrIp1));
        cachedIp2 = InetAddress.getByAddress(IPv4.toIPv4AddressBytes(cachedStrIp2));

        sw1Dpid = new Dpid(1L);
        sw1Inport = new PortNumber((short) 1);
        sw1Outport = new PortNumber((short) 2);
        vlanId = 1;

        //Made tested packets
        arpRequest = new ARP()
        .setHardwareType(ARP.HW_TYPE_ETHERNET)
        .setProtocolType(ARP.PROTO_TYPE_IP)
        .setHardwareAddressLength((byte) 6)
        .setProtocolAddressLength((byte) 4)
        .setOpCode(ARP.OP_REQUEST)
        .setSenderHardwareAddress(srcByteMac)
        .setSenderProtocolAddress(srcIp.getAddress())
        .setTargetProtocolAddress(dstIp.getAddress())
        .setTargetHardwareAddress(dstByteMac);

        ethArpRequest = (Ethernet) new Ethernet()
        .setSourceMACAddress(srcStrMac)
        .setDestinationMACAddress(dstStrMac)
        .setEtherType(Ethernet.TYPE_ARP)
        .setVlanID((short) 0)
        .setPayload(arpRequest);

        arpReply = new ARP()
        .setHardwareType(ARP.HW_TYPE_ETHERNET)
        .setProtocolType(ARP.PROTO_TYPE_IP)
        .setHardwareAddressLength((byte) 6)
        .setProtocolAddressLength((byte) 4)
        .setOpCode(ARP.OP_RARP_REPLY)
        .setSenderHardwareAddress(srcByteMac)
        .setSenderProtocolAddress(srcIp.getAddress())
        .setTargetProtocolAddress(dstIp.getAddress())
        .setTargetHardwareAddress(dstByteMac);

        ethArpReply = (Ethernet) new Ethernet()
        .setSourceMACAddress(srcStrMac)
        .setDestinationMACAddress(dstStrMac)
        .setEtherType(Ethernet.TYPE_ARP)
        .setVlanID((short) 0)
        .setPayload(arpReply);

        rarpRequest = new ARP()
        .setHardwareType(ARP.HW_TYPE_ETHERNET)
        .setProtocolType(ARP.PROTO_TYPE_IP)
        .setHardwareAddressLength((byte) 6)
        .setProtocolAddressLength((byte) 4)
        .setOpCode(ARP.OP_RARP_REQUEST)
        .setSenderHardwareAddress(srcByteMac)
        .setSenderProtocolAddress(srcIp.getAddress())
        .setTargetProtocolAddress(dstIp.getAddress())
        .setTargetHardwareAddress(dstByteMac);

        ethRarpRequest = (Ethernet) new Ethernet()
        .setSourceMACAddress(srcStrMac)
        .setDestinationMACAddress(dstStrMac)
        .setEtherType(Ethernet.TYPE_RARP)
        .setVlanID((short) 0)
        .setPayload(rarpRequest);

        ethArpOtherOp = (Ethernet) new Ethernet()
        .setSourceMACAddress(srcStrMac)
        .setDestinationMACAddress(dstStrMac)
        .setEtherType(Ethernet.TYPE_ARP)
        .setVlanID((short) 0)
        .setPayload(rarpRequest);

        //Made tested objects
        arpCache = new ArpCache();
        arpCache.setArpEntryTimeoutConfig(Long.parseLong(defaultStrCleanupMsec));
        arpCache.update(cachedIp1, cachedMac1);
        arpCache.update(cachedIp2, cachedMac2);

        arpCacheComparisonList = new ArrayList<String>();
        arpCacheComparisonList.add(cachedStrIp1
                + " => "
                + cachedStrMac1
                + " : VALID");
        arpCacheComparisonList.add(cachedStrIp2
                + " => "
                + cachedStrMac2
                + " : VALID");

        arpManager = new ProxyArpManager();
        configMap = new HashMap<String, String>();
    }

    private void makeMock() {
        //Mock floodlight modules
        context = EasyMock.createMock(FloodlightModuleContext.class);
        floodligthProviderService = EasyMock.createMock(IFloodlightProviderService.class);
        configInfoService = EasyMock.createMock(IConfigInfoService.class);
        restApiService = EasyMock.createMock(IRestApiService.class);
        datagridService = EasyMock.createMock(IDatagridService.class);
        flowPusherService = EasyMock.createMock(IFlowPusherService.class);
        topologyService = EasyMock.createMock(ITopologyService.class);
        onosDeviceService = EasyMock.createMock(IOnosDeviceService.class);
        packetService = EasyMock.createMock(IPacketService.class);
        eg = EasyMock.createMock(IEventChannel.class);
        el = EasyMock.createMock(IEventChannelListener.class);

        //Mock Topology related data
        topology = EasyMock.createMock(Topology.class);
        dev1 = EasyMock.createMock(Device.class);
        inPort1 = EasyMock.createMock(Port.class);
        outPort1 = EasyMock.createMock(Port.class);
        sw1 = EasyMock.createMock(Switch.class);
    }

    private void prepareExpectForGeneral() {
        EasyMock.expect(inPort1.getNumber()).andReturn(sw1Inport).anyTimes();
        EasyMock.expect(outPort1.getNumber()).andReturn(sw1Outport).anyTimes();
        EasyMock.expect(outPort1.getOutgoingLink()).andReturn(null).anyTimes();
        EasyMock.expect(outPort1.getIncomingLink()).andReturn(null).anyTimes();
        EasyMock.expect(outPort1.getSwitch()).andReturn(sw1).anyTimes();
        EasyMock.expect(sw1.getDpid()).andReturn(sw1Dpid).anyTimes();
    }

    private void prepareExpectForInit() {
        EasyMock.expect(context.getServiceImpl(IFloodlightProviderService.class)).andReturn(floodligthProviderService);
        EasyMock.expect(context.getServiceImpl(IConfigInfoService.class)).andReturn(configInfoService);
        EasyMock.expect(context.getServiceImpl(IRestApiService.class)).andReturn(restApiService);
        EasyMock.expect(context.getServiceImpl(IDatagridService.class)).andReturn(datagridService);
        EasyMock.expect(context.getServiceImpl(IFlowPusherService.class)).andReturn(flowPusherService);
        EasyMock.expect(context.getServiceImpl(ITopologyService.class)).andReturn(topologyService);
        EasyMock.expect(context.getServiceImpl(IOnosDeviceService.class)).andReturn(onosDeviceService);
        EasyMock.expect(context.getServiceImpl(IPacketService.class)).andReturn(packetService);
    }

    private void prepareExpectForStartUp() {
        // XXX Commented out as workaround for PowerMock + Hazelcast issue.
//        try {
//            PowerMock.expectNew(ArpCache.class).andReturn(arpCache);
//        } catch (Exception e) {
//            fail("Exception:" + e.getMessage());
//        }
//        PowerMock.replayAll();
        EasyMock.expect(configInfoService.getVlan()).andReturn(vlanId);
        restApiService.addRestletRoutable(EasyMock.isA(ArpWebRoutable.class));
        EasyMock.expectLastCall();
        packetService.registerPacketListener(arpManager);
        EasyMock.expectLastCall();
        EasyMock.expect(topologyService.getTopology()).andReturn(topology);
        EasyMock.expect(datagridService.addListener(EasyMock.<String>anyObject(),
                EasyMock.isA(IEventChannelListener.class),
                (Class) EasyMock.anyObject(), (Class) EasyMock.anyObject())).andReturn(eg).anyTimes();
        List<ArpCacheNotification> list = new ArrayList<ArpCacheNotification>();
        EasyMock.expect(eg.getAllEntries()).andReturn(list);
    }

    private void prepareExpectForLearnArp() {
        eg.addEntry(EasyMock.eq(srcIp.toString()), EasyMock.isA(ArpCacheNotification.class));
        EasyMock.expectLastCall();
    }

    @After
    public void tearDown() throws Exception {
        arpCache = null;
    }

    @Test
    public void testConfigTimeWithNoConfig() {
        Map<String, String> config = new HashMap<String, String>();
        EasyMock.expect(context.getConfigParams(arpManager)).andReturn(config);

        EasyMock.replay(context, floodligthProviderService, configInfoService,
                restApiService, datagridService, flowPusherService,
                topologyService, onosDeviceService, packetService, topology, eg,
                el, dev1, inPort1, sw1);
        arpManager.init(context);
        arpManager.startUp(context);
        assertEquals(defaultStrAgingMsec, String.valueOf(arpManager.getArpEntryTimeout()));
        assertEquals(defaultStrCleanupMsec, String.valueOf(arpManager.getArpCleaningTimerPeriod()));
    }

    @Test
    public void testConfigTimeWithWrongParameter() {
        Map<String, String> config = new HashMap<String, String>();
        String strAgingMsec = "aaaaa";
        String strCleanupMsec = "bbbbb";
        config.put("agingmsec", strAgingMsec);
        config.put("cleanupmsec", strCleanupMsec);
        EasyMock.expect(context.getConfigParams(arpManager)).andReturn(config);

        EasyMock.replay(context, floodligthProviderService, configInfoService,
                restApiService, datagridService, flowPusherService,
                topologyService, onosDeviceService, packetService, topology,
                eg, el, dev1, inPort1, sw1);
        arpManager.init(context);
        arpManager.startUp(context);
        assertEquals(defaultStrAgingMsec, String.valueOf(arpManager.getArpEntryTimeout()));
        assertEquals(defaultStrCleanupMsec, String.valueOf(arpManager.getArpCleaningTimerPeriod()));
    }

    @Test
    public void testConfigTime() {
        String strAgingMsec = "10000";
        String strCleanupMsec = "10000";
        configMap.put("agingmsec", strAgingMsec);
        configMap.put("cleanupmsec", strCleanupMsec);
        EasyMock.expect(context.getConfigParams(arpManager)).andReturn(configMap);

        EasyMock.replay(context, floodligthProviderService, configInfoService,
                restApiService, datagridService, flowPusherService,
                topologyService, onosDeviceService, packetService, topology,
                eg, el, dev1, inPort1, sw1);
        arpManager.init(context);
        arpManager.startUp(context);
        assertEquals(strAgingMsec, String.valueOf(arpManager.getArpEntryTimeout()));
        assertEquals(strCleanupMsec, String.valueOf(arpManager.getArpCleaningTimerPeriod()));
    }

    @Test
    public void testGetMacAddress() {
        Map<String, String> config = new HashMap<String, String>();
        EasyMock.expect(context.getConfigParams(arpManager)).andReturn(config);

        EasyMock.replay(context, floodligthProviderService, configInfoService,
                restApiService, datagridService, flowPusherService,
                topologyService, onosDeviceService, packetService, topology,
                eg, el, dev1, inPort1, sw1);
        arpManager.init(context);
        arpManager.startUp(context);

        // XXX workaround for PowerMock + Hazelcast issue.
        this.arpManager.debugReplaceArpCache(arpCache);

        MACAddress mac = arpManager.getMacAddress(cachedIp1);
        assertEquals(cachedMac1, mac);
    }

    @Test
    public void testGetMappings() {
        Map<String, String> config = new HashMap<String, String>();
        EasyMock.expect(context.getConfigParams(arpManager)).andReturn(config);

        EasyMock.replay(context, floodligthProviderService, configInfoService,
                restApiService, datagridService, flowPusherService,
                topologyService, onosDeviceService, packetService, topology,
                eg, el, dev1, inPort1, sw1);
        arpManager.init(context);
        arpManager.startUp(context);
        List<String> list = arpManager.getMappings();
        for (String str : list) {
            assertTrue(arpCacheComparisonList.contains(str));
        }
    }

    @Test
    public void testReceivePacketWithNoArpPacket() {
        Map<String, String> config = new HashMap<String, String>();
        EasyMock.expect(context.getConfigParams(arpManager)).andReturn(config);

        EasyMock.replay(context, floodligthProviderService, configInfoService,
                restApiService, datagridService, flowPusherService,
                topologyService, onosDeviceService, packetService, topology,
                eg, el, dev1, inPort1, sw1);
        arpManager.init(context);
        arpManager.startUp(context);
        arpManager.receive(sw1, inPort1, ethRarpRequest);
    }

    @Test
    public void testReceivePacketWithOtherOpCode() {
        Map<String, String> config = new HashMap<String, String>();
        EasyMock.expect(context.getConfigParams(arpManager)).andReturn(config);

        prepareExpectForLearnArp();

        EasyMock.replay(context, floodligthProviderService, configInfoService,
                restApiService, datagridService, flowPusherService,
                topologyService, onosDeviceService, packetService, topology,
                eg, el, dev1, inPort1, sw1);
        arpManager.init(context);
        arpManager.startUp(context);
        arpManager.receive(sw1, inPort1, ethArpOtherOp);
    }

    @Test
    public void testClassifyPacketToSendArpReplyNotification() {
        Map<String, String> config = new HashMap<String, String>();
        EasyMock.expect(context.getConfigParams(arpManager)).andReturn(config);

        prepareExpectForLearnArp();

        ArpReplyNotification value =
                new ArpReplyNotification(ByteBuffer.wrap(dstIp.getAddress()).getInt(), dstMac);
        eg.addTransientEntry(srcMac.toLong(), value);
        EasyMock.expectLastCall();
        EasyMock.expect(context.getServiceImpl(IDatagridService.class)).andReturn(datagridService);

        EasyMock.replay(context, floodligthProviderService, configInfoService,
                restApiService, datagridService, flowPusherService,
                topologyService, onosDeviceService, packetService, topology,
                eg, el, dev1, inPort1, sw1);
        arpManager.init(context);
        arpManager.startUp(context);
        arpManager.receive(sw1, inPort1, ethArpReply);
    }

    @Test
    public void testClassifyPacketToHandleArpRequest() {
        Map<String, String> config = new HashMap<String, String>();
        EasyMock.expect(context.getConfigParams(arpManager)).andReturn(config);

        prepareExpectForLearnArp();

        EasyMock.expect(configInfoService.fromExternalNetwork(EasyMock.anyLong(), EasyMock.anyShort())).andReturn(true);
        EasyMock.expect(configInfoService.isInterfaceAddress(dstIp)).andReturn(false);

        EasyMock.replay(context, floodligthProviderService, configInfoService,
                restApiService, datagridService, flowPusherService,
                topologyService, onosDeviceService, packetService, topology,
                eg, el, dev1, inPort1, sw1);
        arpManager.init(context);
        arpManager.startUp(context);
        arpManager.receive(sw1, inPort1, ethArpRequest);
    }

    @Test
    public void testClassifyPacketToHandleArpRequest2() {
        List<Port> portList = new ArrayList<Port>();
        portList.add(outPort1);

        Map<String, String> config = new HashMap<String, String>();
        EasyMock.expect(context.getConfigParams(arpManager)).andReturn(config);

        prepareExpectForLearnArp();

        EasyMock.expect(configInfoService.fromExternalNetwork(
                    EasyMock.anyLong(), EasyMock.anyShort())).andReturn(false);
        topology.acquireReadLock();
        EasyMock.expectLastCall();
        EasyMock.expect(topology.getDeviceByMac(dstMac)).andReturn(dev1);
        topology.releaseReadLock();
        EasyMock.expectLastCall();
        EasyMock.expect(dev1.getAttachmentPoints()).andReturn(portList);
        eg.addTransientEntry(EasyMock.anyLong(), EasyMock.anyObject());
        EasyMock.expectLastCall();

        EasyMock.replay(context, configInfoService, restApiService, floodligthProviderService,
                topologyService, datagridService, eg, topology, dev1, inPort1, outPort1, sw1);
        arpManager.init(context);
        arpManager.startUp(context);
        arpManager.receive(sw1, inPort1, ethArpRequest);
    }
}
