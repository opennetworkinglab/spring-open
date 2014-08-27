package net.onrc.onos.apps.proxyarp;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyShort;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
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
import net.onrc.onos.core.flowprogrammer.IFlowPusherService;
import net.onrc.onos.core.hostmanager.IHostService;
import net.onrc.onos.core.main.config.IConfigInfoService;
import net.onrc.onos.core.packet.ARP;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.packet.IPv4;
import net.onrc.onos.core.topology.Host;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.MutableTopology;
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
    IHostService hostService;
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

    MutableTopology mutableTopology;
    IEventChannel eg;
    IEventChannelListener el;
    Host host1;
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
        sw1Inport = PortNumber.uint16((short) 1);
        sw1Outport = PortNumber.uint16((short) 2);
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
        context = createMock(FloodlightModuleContext.class);
        floodligthProviderService = createMock(IFloodlightProviderService.class);
        configInfoService = createMock(IConfigInfoService.class);
        restApiService = createMock(IRestApiService.class);
        datagridService = createMock(IDatagridService.class);
        flowPusherService = createMock(IFlowPusherService.class);
        topologyService = createMock(ITopologyService.class);
        hostService = createMock(IHostService.class);
        packetService = createMock(IPacketService.class);
        eg = createMock(IEventChannel.class);
        el = createMock(IEventChannelListener.class);

        //Mock Topology related data
        mutableTopology = createMock(MutableTopology.class);
        host1 = createMock(Host.class);
        inPort1 = createMock(Port.class);
        outPort1 = createMock(Port.class);
        sw1 = createMock(Switch.class);
    }

    private void prepareExpectForGeneral() {
        expect(inPort1.getNumber()).andReturn(sw1Inport).anyTimes();
        expect(outPort1.getNumber()).andReturn(sw1Outport).anyTimes();
        expect(outPort1.getOutgoingLink()).andReturn(null).anyTimes();
        expect(outPort1.getIncomingLink()).andReturn(null).anyTimes();
        expect(outPort1.getSwitch()).andReturn(sw1).anyTimes();
        expect(sw1.getDpid()).andReturn(sw1Dpid).anyTimes();
    }

    private void prepareExpectForInit() {
        expect(context.getServiceImpl(IFloodlightProviderService.class)).andReturn(floodligthProviderService);
        expect(context.getServiceImpl(IConfigInfoService.class)).andReturn(configInfoService);
        expect(context.getServiceImpl(IRestApiService.class)).andReturn(restApiService);
        expect(context.getServiceImpl(IDatagridService.class)).andReturn(datagridService);
        expect(context.getServiceImpl(IFlowPusherService.class)).andReturn(flowPusherService);
        expect(context.getServiceImpl(ITopologyService.class)).andReturn(topologyService);
        expect(context.getServiceImpl(IHostService.class)).andReturn(hostService);
        expect(context.getServiceImpl(IPacketService.class)).andReturn(packetService);
    }

    private void prepareExpectForStartUp() {
        // XXX Commented out as workaround for PowerMock + Hazelcast issue.
//        try {
//            PowerMock.expectNew(ArpCache.class).andReturn(arpCache);
//        } catch (Exception e) {
//            fail("Exception:" + e.getMessage());
//        }
//        PowerMock.replayAll();
        expect(configInfoService.getVlan()).andReturn(vlanId);
        restApiService.addRestletRoutable(isA(ArpWebRoutable.class));
        expectLastCall();
        packetService.registerPacketListener(arpManager);
        expectLastCall();
        expect(topologyService.getTopology()).andReturn(mutableTopology);
        expect(datagridService.addListener(EasyMock.<String>anyObject(),
                isA(IEventChannelListener.class),
                (Class) anyObject(), (Class) anyObject())).andReturn(eg).anyTimes();
        List<ArpCacheNotification> list = new ArrayList<ArpCacheNotification>();
        expect(eg.getAllEntries()).andReturn(list);
    }

    private void prepareExpectForLearnArp() {
        eg.addEntry(eq(srcIp.toString()), isA(ArpCacheNotification.class));
        expectLastCall();
    }

    @After
    public void tearDown() throws Exception {
        arpCache = null;
    }

    @Test
    public void testConfigTimeWithNoConfig() {
        Map<String, String> config = new HashMap<String, String>();
        expect(context.getConfigParams(arpManager)).andReturn(config);

        replay(context, floodligthProviderService, configInfoService,
                restApiService, datagridService, flowPusherService,
                topologyService, hostService, packetService, mutableTopology, eg,
                el, host1, inPort1, sw1);
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
        expect(context.getConfigParams(arpManager)).andReturn(config);

        replay(context, floodligthProviderService, configInfoService,
                restApiService, datagridService, flowPusherService,
                topologyService, hostService, packetService, mutableTopology,
                eg, el, host1, inPort1, sw1);
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
        expect(context.getConfigParams(arpManager)).andReturn(configMap);

        replay(context, floodligthProviderService, configInfoService,
                restApiService, datagridService, flowPusherService,
                topologyService, hostService, packetService, mutableTopology,
                eg, el, host1, inPort1, sw1);
        arpManager.init(context);
        arpManager.startUp(context);
        assertEquals(strAgingMsec, String.valueOf(arpManager.getArpEntryTimeout()));
        assertEquals(strCleanupMsec, String.valueOf(arpManager.getArpCleaningTimerPeriod()));
    }

    @Test
    public void testGetMacAddress() {
        Map<String, String> config = new HashMap<String, String>();
        expect(context.getConfigParams(arpManager)).andReturn(config);

        replay(context, floodligthProviderService, configInfoService,
                restApiService, datagridService, flowPusherService,
                topologyService, hostService, packetService, mutableTopology,
                eg, el, host1, inPort1, sw1);
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
        expect(context.getConfigParams(arpManager)).andReturn(config);

        replay(context, floodligthProviderService, configInfoService,
                restApiService, datagridService, flowPusherService,
                topologyService, hostService, packetService, mutableTopology,
                eg, el, host1, inPort1, sw1);
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
        expect(context.getConfigParams(arpManager)).andReturn(config);

        replay(context, floodligthProviderService, configInfoService,
                restApiService, datagridService, flowPusherService,
                topologyService, hostService, packetService, mutableTopology,
                eg, el, host1, inPort1, sw1);
        arpManager.init(context);
        arpManager.startUp(context);
        arpManager.receive(sw1, inPort1, ethRarpRequest);
    }

    @Test
    public void testReceivePacketWithOtherOpCode() {
        Map<String, String> config = new HashMap<String, String>();
        expect(context.getConfigParams(arpManager)).andReturn(config);

        prepareExpectForLearnArp();

        replay(context, floodligthProviderService, configInfoService,
                restApiService, datagridService, flowPusherService,
                topologyService, hostService, packetService, mutableTopology,
                eg, el, host1, inPort1, sw1);
        arpManager.init(context);
        arpManager.startUp(context);
        arpManager.receive(sw1, inPort1, ethArpOtherOp);
    }

    @Test
    public void testClassifyPacketToSendArpReplyNotification() {
        Map<String, String> config = new HashMap<String, String>();
        expect(context.getConfigParams(arpManager)).andReturn(config);

        prepareExpectForLearnArp();

        ArpReplyNotification value =
                new ArpReplyNotification(ByteBuffer.wrap(dstIp.getAddress()).getInt(), dstMac);
        eg.addTransientEntry(srcMac.toLong(), value);
        expectLastCall();
        expect(context.getServiceImpl(IDatagridService.class)).andReturn(datagridService);

        replay(context, floodligthProviderService, configInfoService,
                restApiService, datagridService, flowPusherService,
                topologyService, hostService, packetService, mutableTopology,
                eg, el, host1, inPort1, sw1);
        arpManager.init(context);
        arpManager.startUp(context);
        arpManager.receive(sw1, inPort1, ethArpReply);
    }

    @Test
    public void testClassifyPacketToHandleArpRequest() {
        Map<String, String> config = new HashMap<String, String>();
        expect(context.getConfigParams(arpManager)).andReturn(config);

        prepareExpectForLearnArp();

        expect(configInfoService.fromExternalNetwork(anyLong(), anyShort())).andReturn(true);
        expect(configInfoService.isInterfaceAddress(dstIp)).andReturn(false);

        replay(context, floodligthProviderService, configInfoService,
                restApiService, datagridService, flowPusherService,
                topologyService, hostService, packetService, mutableTopology,
                eg, el, host1, inPort1, sw1);
        arpManager.init(context);
        arpManager.startUp(context);
        arpManager.receive(sw1, inPort1, ethArpRequest);
    }

    @Test
    public void testClassifyPacketToHandleArpRequest2() {
        List<Port> portList = new ArrayList<Port>();
        portList.add(outPort1);

        Map<String, String> config = new HashMap<String, String>();
        expect(context.getConfigParams(arpManager)).andReturn(config);

        prepareExpectForLearnArp();

        expect(configInfoService.fromExternalNetwork(
                anyLong(), anyShort())).andReturn(false);

        expect(configInfoService.inConnectedNetwork(
                dstIp)).andReturn(false);
        mutableTopology.acquireReadLock();
        expectLastCall();
        expect(mutableTopology.getHostByMac(dstMac)).andReturn(host1);
        mutableTopology.releaseReadLock();
        expectLastCall();
        expect(host1.getAttachmentPoints()).andReturn(portList);
        eg.addTransientEntry(anyLong(), anyObject());
        expectLastCall();

        replay(context, configInfoService, restApiService, floodligthProviderService,
                topologyService, datagridService, eg, mutableTopology, host1, inPort1, outPort1, sw1);
        arpManager.init(context);
        arpManager.startUp(context);

        arpManager.receive(sw1, inPort1, ethArpRequest);
    }
}
