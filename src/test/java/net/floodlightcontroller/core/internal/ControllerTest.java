/**
 *    Copyright 2011, Big Switch Networks, Inc.
 *    Originally created by David Erickson, Stanford University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package net.floodlightcontroller.core.internal;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.FloodlightProvider;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.floodlightcontroller.core.IListener;
import net.floodlightcontroller.core.IListener.Command;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitch.PortChangeType;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.IUpdate;
import net.floodlightcontroller.core.internal.Controller.SwitchUpdate;
import net.floodlightcontroller.core.internal.Controller.SwitchUpdateType;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.test.MockThreadPoolService;
import net.floodlightcontroller.debugcounter.DebugCounter;
import net.floodlightcontroller.debugcounter.IDebugCounterService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.restserver.RestApiServer;
import net.floodlightcontroller.test.FloodlightTestCase;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.onrc.onos.core.linkdiscovery.ILinkDiscoveryService;
import net.onrc.onos.core.linkdiscovery.LinkDiscoveryManager;
import net.onrc.onos.core.packet.ARP;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.packet.IPacket;
import net.onrc.onos.core.packet.IPv4;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.registry.StandaloneRegistry;

import org.junit.Before;
import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketInReason;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortState;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.ver10.OFStatsReplyFlagsSerializerVer10;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.util.HexString;

/**
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class ControllerTest extends FloodlightTestCase {

    private Controller controller;
    private MockThreadPoolService tp;
    protected OFFactory factory10 = OFFactories.getFactory(OFVersion.OF_10);
    private IPacket testPacket;
    private OFPacketIn pi;

    @Override
    @Before
    public void setUp() throws Exception {
        doSetUp(Role.MASTER);
    }

    public void doSetUp(Role role) throws Exception {
        super.setUp();
        FloodlightModuleContext fmc = new FloodlightModuleContext();

        FloodlightProvider cm = new FloodlightProvider();

        controller = (Controller) cm.getServiceImpls().get(
                IFloodlightProviderService.class);
        fmc.addService(IFloodlightProviderService.class, controller);

        RestApiServer restApi = new RestApiServer();
        fmc.addService(IRestApiService.class, restApi);

        // TODO replace with mock if further testing is needed.
        DebugCounter counterService = new DebugCounter();
        fmc.addService(IDebugCounterService.class, counterService);

        tp = new MockThreadPoolService();
        fmc.addService(IThreadPoolService.class, tp);

        // Following added by ONOS
        // TODO replace with mock if further testing is needed.
        StandaloneRegistry sr = new StandaloneRegistry();
        fmc.addService(IControllerRegistryService.class, sr);
        LinkDiscoveryManager linkDiscovery = new LinkDiscoveryManager();
        fmc.addService(ILinkDiscoveryService.class, linkDiscovery);

        restApi.init(fmc);
        cm.init(fmc);
        tp.init(fmc);
        sr.init(fmc);
        linkDiscovery.init(fmc);
        restApi.startUp(fmc);
        cm.startUp(fmc);
        tp.startUp(fmc);
        sr.startUp(fmc);
        // linkDiscovery.startUp(fmc);

        testPacket = new Ethernet()
                .setSourceMACAddress("00:44:33:22:11:00")
                .setDestinationMACAddress("00:11:22:33:44:55")
                .setEtherType(Ethernet.TYPE_ARP)
                .setPayload(
                        new ARP()
                                .setHardwareType(ARP.HW_TYPE_ETHERNET)
                                .setProtocolType(ARP.PROTO_TYPE_IP)
                                .setHardwareAddressLength((byte) 6)
                                .setProtocolAddressLength((byte) 4)
                                .setOpCode(ARP.OP_REPLY)
                                .setSenderHardwareAddress(
                                        Ethernet.toMACAddress("00:44:33:22:11:00"))
                                .setSenderProtocolAddress(
                                        IPv4.toIPv4AddressBytes("192.168.1.1"))
                                .setTargetHardwareAddress(
                                        Ethernet.toMACAddress("00:11:22:33:44:55"))
                                .setTargetProtocolAddress(
                                        IPv4.toIPv4AddressBytes("192.168.1.2")));
        byte[] testPacketSerialized = testPacket.serialize();

        pi = factory10.buildPacketIn()
                .setBufferId(OFBufferId.NO_BUFFER)
                .setInPort(OFPort.of(1))
                .setData(testPacketSerialized)
                .setReason(OFPacketInReason.NO_MATCH)
                .setTotalLen((short) testPacketSerialized.length).build();

    }

    public Controller getController() {
        return controller;
    }

    protected OFStatsReply getStatisticsReply(int transactionId,
            int count, boolean moreReplies) {
        List<OFFlowStatsEntry> statistics = new ArrayList<OFFlowStatsEntry>();
        for (int i = 0; i < count; ++i) {
            statistics.add(factory10.buildFlowStatsEntry().build());
        }
        assertEquals(statistics.size(), count);
        OFStatsReply sr;
        if (moreReplies) {
            sr = (factory10.buildFlowStatsReply()
                    .setXid(transactionId)
                    .setEntries(statistics)
                    .setFlags(OFStatsReplyFlagsSerializerVer10.ofWireValue((short) 1))
                    .build());
        }
        else {
            sr = (factory10.buildFlowStatsReply()
                    .setXid(transactionId)
                    .setEntries(statistics).build());
        }

        return sr;
    }

    private OFDescStatsReply createOFDescStatsReply() {
        OFDescStatsReply desc = factory10.buildDescStatsReply()
                .setHwDesc("")
                .setMfrDesc("")
                .setDpDesc("")
                .setMfrDesc("")
                .setSwDesc("")
                .setSerialNum("").build();
        return desc;
    }

    private OFFeaturesReply createOFFeaturesReply() {
        OFFeaturesReply fr = factory10.buildFeaturesReply()
                .setPorts(new ArrayList<OFPortDesc>())
                .build();
        return fr;

    }

    /**
     * Set the mock expectations for sw when sw is passed to addSwitch The same
     * expectations can be used when a new SwitchSyncRepresentation is created
     * from the given mocked switch
     */
    protected void setupSwitchForAddSwitch(IOFSwitch sw, long dpid,
            OFDescStatsReply desc, OFFeaturesReply featuresReply) {
        String dpidString = HexString.toHexString(dpid);

        if (desc == null) {
            desc = createOFDescStatsReply();
        }
        if (featuresReply == null) {
            featuresReply = createOFFeaturesReply();
            featuresReply.createBuilder().setDatapathId(DatapathId.of(dpid));

        }
        expect(sw.getId()).andReturn(dpid).anyTimes();
        expect(sw.getStringId()).andReturn(dpidString).anyTimes();
    }

    /**
     * Run the controller's main loop so that updates are processed
     */
    protected class ControllerRunThread extends Thread {
        @Override
        public void run() {
            controller.openFlowPort = 0; // Don't listen
            controller.run();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void setupListenerOrdering(IListener<T> listener) {
        listener.isCallbackOrderingPostreq((T) anyObject(),
                anyObject(String.class));
        expectLastCall().andReturn(false).anyTimes();

        listener.isCallbackOrderingPrereq((T) anyObject(),
                anyObject(String.class));
        expectLastCall().andReturn(false).anyTimes();
    }

    /**
     * Verify that a listener that throws an exception halts further execution,
     * and verify that the Commands STOP and CONTINUE are honored.
     *
     * @throws Exception
     */

    @Test
    public void testHandleMessagesNoListeners() throws Exception {
        IOFSwitch sw = createMock(IOFSwitch.class);
        expect(sw.getId()).andReturn(0L).anyTimes();
        expect(sw.getStringId()).andReturn("00:00:00:00:00:00:00").anyTimes();
        expect(sw.getOFVersion()).andReturn(OFVersion.OF_10).anyTimes();
        replay(sw);
        controller.handleMessage(sw, pi, null);
        verify(sw);
    }

    /**
     * Test message dispatching to OFMessageListeners. Test ordering of
     * listeners for different types (we do this implicitly by using STOP and
     * CONTINUE and making sure the processing stops at the right place) Verify
     * that a listener that throws an exception halts further execution, and
     * verify that the Commands STOP and CONTINUE are honored.
     *
     * @throws Exception
     */
    @Test
    public void testHandleMessages() throws Exception {
        controller.removeOFMessageListeners(OFType.PACKET_IN);

        IOFSwitch sw = createMock(IOFSwitch.class);
        expect(sw.getId()).andReturn(0L).anyTimes();
        expect(sw.getStringId()).andReturn("00:00:00:00:00:00:00").anyTimes();
        expect(sw.getOFVersion()).andReturn(OFVersion.OF_10).anyTimes();
        // Setup listener orderings
        IOFMessageListener test1 = createMock(IOFMessageListener.class);
        expect(test1.getName()).andReturn("test1").anyTimes();
        setupListenerOrdering(test1);

        IOFMessageListener test2 = createMock(IOFMessageListener.class);
        expect(test2.getName()).andReturn("test2").anyTimes();
        // using a postreq and a prereq ordering here
        expect(test2.isCallbackOrderingPrereq(OFType.PACKET_IN, "test1"))
                .andReturn(true).atLeastOnce();
        expect(test2.isCallbackOrderingPostreq(OFType.FLOW_MOD, "test1"))
                .andReturn(true).atLeastOnce();
        setupListenerOrdering(test2);

        IOFMessageListener test3 = createMock(IOFMessageListener.class);
        expect(test3.getName()).andReturn("test3").anyTimes();
        expect(test3.isCallbackOrderingPrereq((OFType) anyObject(), eq("test1")))
                .andReturn(true).atLeastOnce();
        expect(test3.isCallbackOrderingPrereq((OFType) anyObject(), eq("test2")))
                .andReturn(true).atLeastOnce();
        setupListenerOrdering(test3);

        // Ordering: PacketIn: test1 -> test2 -> test3
        // FlowMod: test2 -> test1
        replay(test1, test2, test3);
        controller.addOFMessageListener(OFType.PACKET_IN, test1);
        controller.addOFMessageListener(OFType.PACKET_IN, test3);
        controller.addOFMessageListener(OFType.PACKET_IN, test2);
        controller.addOFMessageListener(OFType.FLOW_MOD, test1);
        controller.addOFMessageListener(OFType.FLOW_MOD, test2);
        verify(test1);
        verify(test2);
        verify(test3);

        replay(sw);

        // ------------------
        // Test PacketIn handling: all listeners return CONTINUE
        reset(test1, test2, test3);
        expect(test1.receive(eq(sw), eq(pi), isA(FloodlightContext.class)))
                .andReturn(Command.CONTINUE);
        expect(test2.receive(eq(sw), eq(pi), isA(FloodlightContext.class)))
                .andReturn(Command.CONTINUE);
        expect(test3.receive(eq(sw), eq(pi), isA(FloodlightContext.class)))
                .andReturn(Command.CONTINUE);
        replay(test1, test2, test3);
        controller.handleMessage(sw, pi, null);
        verify(test1);
        verify(test2);
        verify(test3);

        // ------------------
        // Test PacketIn handling: with a thrown exception.
        reset(test1, test2, test3);
        expect(test1.receive(eq(sw), eq(pi), isA(FloodlightContext.class)))
                .andReturn(Command.CONTINUE);
        expect(test2.receive(eq(sw), eq(pi), isA(FloodlightContext.class)))
                .andThrow(new RuntimeException("This is NOT an error! We " +
                        "are testing exception catching."));
        // expect no calls to test3.receive() since test2.receive throws
        // an exception
        replay(test1, test2, test3);
        try {
            controller.handleMessage(sw, pi, null);
            fail("Expected exception was not thrown!");
        } catch (RuntimeException e) {
            assertTrue("The caught exception was not the expected one",
                    e.getMessage().startsWith("This is NOT an error!"));
        }
        verify(test1);
        verify(test2);
        verify(test3);

        // ------------------
        // Test PacketIn handling: test1 return Command.STOP
        reset(test1, test2, test3);
        expect(test1.receive(eq(sw), eq(pi), isA(FloodlightContext.class)))
                .andReturn(Command.STOP);
        // expect no calls to test3.receive() and test2.receive since
        // test1.receive returns STOP
        replay(test1, test2, test3);
        controller.handleMessage(sw, pi, null);
        verify(test1);
        verify(test2);
        verify(test3);

        OFFlowMod fm = factory10.buildFlowAdd().build();

        // ------------------
        // Test FlowMod handling: all listeners return CONTINUE
        reset(test1, test2, test3);
        expect(test1.receive(eq(sw), eq(fm), isA(FloodlightContext.class)))
                .andReturn(Command.CONTINUE);
        expect(test2.receive(eq(sw), eq(fm), isA(FloodlightContext.class)))
                .andReturn(Command.CONTINUE);
        // test3 is not a listener for FlowMod
        replay(test1, test2, test3);
        controller.handleMessage(sw, fm, null);
        verify(test1);
        verify(test2);
        verify(test3);

        // ------------------
        // Test FlowMod handling: test2 (first listener) return STOP
        reset(test1, test2, test3);
        expect(test2.receive(eq(sw), eq(fm), isA(FloodlightContext.class)))
                .andReturn(Command.STOP);
        // test2 will not be called
        // test3 is not a listener for FlowMod
        replay(test1, test2, test3);
        controller.handleMessage(sw, fm, null);
        verify(test1);
        verify(test2);
        verify(test3);

        verify(sw);
    }

    @Test
    public void testHandleMessageWithContext() throws Exception {
        IOFSwitch sw = createMock(IOFSwitch.class);
        expect(sw.getId()).andReturn(0L).anyTimes();
        expect(sw.getStringId()).andReturn("00:00:00:00:00:00:00").anyTimes();
        expect(sw.getOFVersion()).andReturn(OFVersion.OF_10).anyTimes();

        IOFMessageListener test1 = createMock(IOFMessageListener.class);
        expect(test1.getName()).andReturn("test1").anyTimes();
        expect(test1.isCallbackOrderingPrereq((OFType) anyObject(),
                (String) anyObject()))
                .andReturn(false).anyTimes();
        expect(test1.isCallbackOrderingPostreq((OFType) anyObject(),
                (String) anyObject()))
                .andReturn(false).anyTimes();
        FloodlightContext cntx = new FloodlightContext();
        expect(test1.receive(same(sw), same(pi), same(cntx)))
                .andReturn(Command.CONTINUE);

        IOFMessageListener test2 = createMock(IOFMessageListener.class);
        expect(test2.getName()).andReturn("test2").anyTimes();
        expect(test2.isCallbackOrderingPrereq((OFType) anyObject(),
                (String) anyObject()))
                .andReturn(false).anyTimes();
        expect(test2.isCallbackOrderingPostreq((OFType) anyObject(),
                (String) anyObject()))
                .andReturn(false).anyTimes();
        // test2 will not receive any message!

        replay(test1, test2, sw);
        controller.addOFMessageListener(OFType.PACKET_IN, test1);
        controller.addOFMessageListener(OFType.ERROR, test2);
        controller.handleMessage(sw, pi, cntx);
        verify(test1, test2, sw);

        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
                IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        assertArrayEquals(testPacket.serialize(), eth.serialize());
    }

    public class FutureFetcher<E> implements Runnable {
        public E value;
        public Future<E> future;

        public FutureFetcher(Future<E> future) {
            this.future = future;
        }

        @Override
        public void run() {
            try {
                value = future.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * @return the value
         */
        public E getValue() {
            return value;
        }

        /**
         * @return the future
         */
        public Future<E> getFuture() {
            return future;
        }
    }

    /**
     * @throws Exception
     */
    @Test
    public void testOFStatisticsFuture() throws Exception {
        // Test for a single stats reply
        OFSwitchImplBase sw = createMock(OFSwitchImplBase.class);
        sw.cancelStatisticsReply(1);
        OFStatisticsFuture sf = new OFStatisticsFuture(tp, sw, 1);

        replay(sw);
        List<OFStatsReply> stats;
        FutureFetcher<List<OFStatsReply>> ff = new FutureFetcher<List<OFStatsReply>>(sf);
        Thread t = new Thread(ff);
        t.start();
        sf.deliverFuture(sw, getStatisticsReply(1, 10, false));

        t.join();
        stats = ff.getValue();
        verify(sw);
        // TODO: temporary fix: size = 1 ?
        assertEquals(1, stats.size());

        // Test multiple stats replies
        reset(sw);
        sw.cancelStatisticsReply(1);

        sf = new OFStatisticsFuture(tp, sw, 1);

        replay(sw);
        ff = new FutureFetcher<List<OFStatsReply>>(sf);
        t = new Thread(ff);
        t.start();
        sf.deliverFuture(sw, getStatisticsReply(1, 10, true));
        sf.deliverFuture(sw, getStatisticsReply(1, 5, false));
        t.join();

        stats = sf.get();
        verify(sw);
        // TODO: temporary fix: size = 2 ?
        assertEquals(2, stats.size());

        // Test cancellation
        reset(sw);
        sw.cancelStatisticsReply(1);
        sf = new OFStatisticsFuture(tp, sw, 1);

        replay(sw);
        ff = new FutureFetcher<List<OFStatsReply>>(sf);
        t = new Thread(ff);
        t.start();
        sf.cancel(true);
        t.join();

        stats = sf.get();
        verify(sw);
        assertEquals(0, stats.size());

        // Test self timeout
        reset(sw);
        sw.cancelStatisticsReply(1);
        sf = new OFStatisticsFuture(tp, sw, 1, 75, TimeUnit.MILLISECONDS);

        replay(sw);
        ff = new FutureFetcher<List<OFStatsReply>>(sf);
        t = new Thread(ff);
        t.start();
        t.join(2000);

        stats = sf.get();
        verify(sw);
        assertEquals(0, stats.size());
    }

    /**
     * Test switchActivated for a new switch, i.e., a switch that was not
     * previously known to the controller cluser. We expect that all flow mods
     * are cleared and we expect a switchAdded
     */
    @Test
    public void testNewSwitchActivated() throws Exception {
        controller.setAlwaysClearFlowsOnSwActivate(false);
        controller.setAlwaysClearFlowsOnSwAdd(false);

        IOFSwitch sw = createMock(IOFSwitch.class);
        expect(sw.getPorts()).andReturn(new HashSet<OFPortDesc>()).anyTimes();
        setupSwitchForAddSwitch(sw, 0L, null, null);

        // strict mock. Order of events matters!
        IOFSwitchListener listener = createStrictMock(IOFSwitchListener.class);
        listener.switchActivatedMaster(0L);
        expectLastCall().once();
        replay(listener);
        controller.addOFSwitchListener(listener);

        replay(sw);
        controller.addConnectedSwitch(0L, new OFChannelHandler(controller));
        controller.addActivatedMasterSwitch(0L, sw);
        verify(sw);
        assertEquals(sw, controller.getMasterSwitch(0L));
        controller.processUpdateQueueForTesting();
        verify(listener);
    }

    /**
     * Test switchActivated for a new switch while in equal: a no-op
     */
    @Test
    public void testNewSwitchActivatedWhileSlave() throws Exception {
        doSetUp(Role.EQUAL);
        IOFSwitch sw = createMock(IOFSwitch.class);

        IOFSwitchListener listener = createMock(IOFSwitchListener.class);
        controller.addOFSwitchListener(listener);

        replay(sw, listener); // nothing recorded
        controller.addConnectedSwitch(0L, new OFChannelHandler(controller));
        controller.addActivatedEqualSwitch(0L, sw);
        verify(sw);
        verify(listener);
    }

    /**
     * Disconnect a switch. normal program flow
     */
    @Test
    private void doTestSwitchConnectReconnect(boolean reconnect)
            throws Exception {
        IOFSwitch sw = doActivateNewSwitch(1L, null, null);
        expect(sw.getId()).andReturn(1L).anyTimes();
        expect(sw.getStringId()).andReturn(HexString.toHexString(1L)).anyTimes();
        sw.setConnected(false);
        expectLastCall().once();
        sw.cancelAllStatisticsReplies();
        expectLastCall().once();
        IOFSwitchListener listener = createMock(IOFSwitchListener.class);
        listener.switchDisconnected(1L);
        expectLastCall().once();
        controller.addOFSwitchListener(listener);
        replay(sw, listener);
        controller.removeConnectedSwitch(1L);
        controller.processUpdateQueueForTesting();
        verify(sw, listener);

        assertNull(controller.getSwitch(1L));
        if (reconnect) {
            controller.removeOFSwitchListener(listener);
            sw = doActivateOldSwitch(1L, null, null);
        }
    }

    @Test
    public void testSwitchDisconnected() throws Exception {
        doTestSwitchConnectReconnect(false);
    }

    /**
     * Disconnect a switch and reconnect, verify no clearAllFlowmods()
     */
    @Test
    public void testSwitchReconnect() throws Exception {
        doTestSwitchConnectReconnect(true);
    }

    /* /**
     * Remove a nonexisting switch. should be ignored
     */
    @Test
    public void testNonexistingSwitchDisconnected() throws Exception {
        IOFSwitch sw = createMock(IOFSwitch.class);
        expect(sw.getId()).andReturn(1L).anyTimes();
        expect(sw.getStringId()).andReturn(HexString.toHexString(1L)).anyTimes();
        IOFSwitchListener listener = createMock(IOFSwitchListener.class);
        controller.addOFSwitchListener(listener);
        replay(sw, listener);
        controller.removeConnectedSwitch(sw.getId());
        // controller.processUpdateQueueForTesting();
        verify(sw, listener);

        assertNull(controller.getSwitch(1L));
    }

    /**
     * Try to activate a switch that's already active (which can happen if two
     * different switches have the same DPIP or if a switch reconnects while the
     * old TCP connection is still alive
     */
    // TODO: I do not if it represents the expected behaviour
    @Test
    public void testSwitchActivatedWithAlreadyActiveSwitch() throws Exception {
        OFDescStatsReply oldDesc = createOFDescStatsReply();
        oldDesc.createBuilder().setDpDesc("Ye Olde Switch");
        OFDescStatsReply newDesc = createOFDescStatsReply();
        oldDesc.createBuilder().setDpDesc("The new Switch");
        OFFeaturesReply featuresReply = createOFFeaturesReply();

        // Setup: add a switch to the controller
        IOFSwitch oldsw = createMock(IOFSwitch.class);
        setupSwitchForAddSwitch(oldsw, 0L, oldDesc, featuresReply);
        expect(oldsw.getPorts()).andReturn(new HashSet<OFPortDesc>()).anyTimes();
        // oldsw.clearAllFlowMods();
        // expectLastCall().once();
        replay(oldsw);
        controller.addConnectedSwitch(oldsw.getId(), new OFChannelHandler(controller));
        controller.addActivatedMasterSwitch(oldsw.getId(), oldsw);
        verify(oldsw);
        // drain the queue, we don't care what's in it
        controller.processUpdateQueueForTesting();
        assertEquals(oldsw, controller.getSwitch(0L));

        // Now the actual test: add a new switch with the same dpid to
        // the controller
        reset(oldsw);
        expect(oldsw.getId()).andReturn(0L).anyTimes();
        // oldsw.cancelAllStatisticsReplies();
        // expectLastCall().once();
        // oldsw.disconnectOutputStream();
        // expectLastCall().once();

        IOFSwitch newsw = createMock(IOFSwitch.class);
        setupSwitchForAddSwitch(newsw, 0L, newDesc, featuresReply);
        // newsw.clearAllFlowMods();
        // expectLastCall().once();

        // Strict mock. We need to get the removed notification before the
        // add notification
        IOFSwitchListener listener = createStrictMock(IOFSwitchListener.class);
        // listener.switchDisconnected(0L);
        // listener.switchActivatedMaster(0L);
        replay(listener);
        controller.addOFSwitchListener(listener);

        replay(newsw, oldsw);
        controller.addActivatedMasterSwitch(0L, newsw);
        verify(newsw, oldsw);

        assertEquals(oldsw, controller.getSwitch(0L));
        controller.processUpdateQueueForTesting();
        verify(listener);
    }

    /**
     * Tests that you can't remove a switch from the map returned by
     * getSwitches() (because getSwitches should return an unmodifiable map)
     */
    @Test
    public void testRemoveActiveSwitch() {
        IOFSwitch sw = createNiceMock(IOFSwitch.class);
        expect(sw.getPorts()).andReturn(new ArrayList<OFPortDesc>()).anyTimes();
        setupSwitchForAddSwitch(sw, 1L, null, null);
        replay(sw);
        controller.addConnectedSwitch(1L, new OFChannelHandler(controller));
        controller.addActivatedMasterSwitch(1L, sw);
        assertEquals(sw, getController().getSwitch(1L));
        controller.getAllSwitchDpids().remove(1L);
        assertEquals(sw, getController().getSwitch(1L));
        verify(sw);
        // we don't care for updates. drain queue.
        controller.processUpdateQueueForTesting();
    }

    /**
     * Create and activate a switch, either completely new or reconnected The
     * mocked switch instance will be returned. It wil be reset.
     */
    private IOFSwitch doActivateSwitchInt(long dpid,
            OFDescStatsReply desc,
            OFFeaturesReply featuresReply,
            boolean clearFlows)
            throws Exception {
        controller.setAlwaysClearFlowsOnSwActivate(false);

        IOFSwitch sw = createMock(IOFSwitch.class);
        if (featuresReply == null) {
            featuresReply = createOFFeaturesReply();
            featuresReply.createBuilder().setDatapathId(DatapathId.of(dpid));
        }
        if (desc == null) {
            desc = createOFDescStatsReply();
        }
        setupSwitchForAddSwitch(sw, dpid, desc, featuresReply);
        if (clearFlows) {
            sw.clearAllFlowMods();
            expectLastCall().once();
        }
        expect(sw.getPorts()).andReturn(new HashSet<OFPortDesc>()).anyTimes();

        replay(sw);
        controller.addConnectedSwitch(dpid, new OFChannelHandler(controller));
        controller.addActivatedMasterSwitch(dpid, sw);
        verify(sw);
        assertEquals(sw, controller.getSwitch(dpid));
        // drain updates and ignore
        controller.processUpdateQueueForTesting();

        // SwitchSyncRepresentation storedSwitch = storeClient.getValue(dpid);
        // assertEquals(featuresReply, storedSwitch.getFeaturesReply());
        // assertEquals(desc, storedSwitch.getDescription());
        reset(sw);
        return sw;
    }

    /**
     * Create and activate a new switch with the given dpid, features reply and
     * description. If description and/or features reply are null we'll allocate
     * the default one The mocked switch instance will be returned. It wil be
     * reset.
     */
    private IOFSwitch doActivateNewSwitch(long dpid,
            OFDescStatsReply desc,
            OFFeaturesReply featuresReply)
            throws Exception {
        return doActivateSwitchInt(dpid, desc, featuresReply, false);
    }

    /**
     * Create and activate a switch that's just been disconnected. The mocked
     * switch instance will be returned. It wil be reset.
     */
    private IOFSwitch doActivateOldSwitch(long dpid,
            OFDescStatsReply desc,
            OFFeaturesReply featuresReply)
            throws Exception {
        return doActivateSwitchInt(dpid, desc, featuresReply, false);
    }

    @Test
    public void testUpdateQueue() throws Exception {
        class DummySwitchListener implements IOFSwitchListener {
            public int nAddedMaster;
            public int nAddedEqual;
            public int nDisconnected;
            public int nPortChanged;
            public int nPortAdded;
            public int nPortDeleted;

            public DummySwitchListener() {
                nAddedMaster = 0;
                nAddedEqual = 0;
                nDisconnected = 0;
                nPortChanged = 0;
                nPortAdded = 0;
                nPortDeleted = 0;
            }

            @Override
            public String getName() {
                return "dummy";
            }

            @Override
            public void switchActivatedMaster(long swId) {
                nAddedMaster++;
                notifyAll();

            }

            @Override
            public void switchActivatedEqual(long swId) {
                nAddedEqual++;
                notifyAll();

            }

            @Override
            public void switchMasterToEqual(long swId) {
                // TODO Auto-generated method stub

            }

            @Override
            public void switchEqualToMaster(long swId) {
                // TODO Auto-generated method stub
            }

            @Override
            public void switchDisconnected(long swId) {
                nDisconnected++;
                notifyAll();

            }

            @Override
            public void switchPortChanged(long swId, OFPortDesc port,
                    PortChangeType changeType) {
                switch (changeType) {
                case ADD:
                    nPortAdded++;
                    notifyAll();
                    break;
                case DELETE:
                    nPortDeleted++;
                    notifyAll();
                    break;

                case OTHER_UPDATE:
                    nPortChanged++;
                    notifyAll();
                    break;

                }
            }
        }
        DummySwitchListener switchListener = new DummySwitchListener();
        IOFSwitch sw = createMock(IOFSwitch.class);
        expect(sw.getId()).andReturn(1L).anyTimes();
        expect(sw.getPort(1)).andReturn(factory10.buildPortDesc().build()).anyTimes();
        replay(sw);
        ControllerRunThread t = new ControllerRunThread();
        t.start();

        controller.addOFSwitchListener(switchListener);
        synchronized (switchListener) {
            controller.updates.put(controller.new SwitchUpdate(sw.getId(),
                    Controller.SwitchUpdateType.ACTIVATED_MASTER));
            switchListener.wait(500);
            assertTrue("IOFSwitchListener.addedSwitch() was not called",
                    switchListener.nAddedMaster == 1);
            controller.addOFSwitchListener(switchListener);
            synchronized (switchListener) {
                controller.updates.put(controller.new SwitchUpdate(sw.getId(),
                        Controller.SwitchUpdateType.ACTIVATED_EQUAL));
                switchListener.wait(500);
                assertTrue("IOFSwitchListener.addedSwitch() was not called",
                        switchListener.nAddedEqual == 1);
                controller.updates.put(controller.new SwitchUpdate(sw.getId(),
                        Controller.SwitchUpdateType.DISCONNECTED));
                switchListener.wait(500);
                assertTrue("IOFSwitchListener.removedSwitch() was not called",
                        switchListener.nDisconnected == 1);
                controller.updates.put(controller.new SwitchUpdate(sw.getId(),
                        Controller.SwitchUpdateType.PORTCHANGED, sw.getPort(1),
                        PortChangeType.ADD));
                switchListener.wait(500);
                assertTrue(
                        "IOFSwitchListener.switchPortChanged() with PortChangeType.ADD was not called",
                        switchListener.nPortAdded == 1);
                controller.updates.put(controller.new SwitchUpdate(sw.getId(),
                        Controller.SwitchUpdateType.PORTCHANGED, sw.getPort(1),
                        PortChangeType.DELETE));
                switchListener.wait(500);
                assertTrue(
                        "IOFSwitchListener.switchPortChanged() with PortChangeType.DELETE was not called",
                        switchListener.nPortDeleted == 1);
                controller.updates.put(controller.new SwitchUpdate(sw.getId(),
                        Controller.SwitchUpdateType.PORTCHANGED, sw.getPort(1),
                        PortChangeType.OTHER_UPDATE));
                switchListener.wait(500);
                assertTrue(
                        "IOFSwitchListener.switchPortChanged() with PortChangeType.OTHER_UPDATE was not called",
                        switchListener.nPortChanged == 1);
            }
        }
    }


    public void verifyPortChangedUpdateInQueue(IOFSwitch sw) throws Exception {
        assertEquals(1, controller.updates.size());
        IUpdate update = controller.updates.take();
        assertEquals(true, update instanceof SwitchUpdate);
        SwitchUpdate swUpdate = (SwitchUpdate) update;
        assertEquals(sw.getId(), swUpdate.getSwId());
        assertEquals(SwitchUpdateType.PORTCHANGED, swUpdate.getSwitchUpdateType());
        assertEquals(PortChangeType.OTHER_UPDATE, swUpdate.getPortChangeType());
    }

    public void verifyPortDownUpdateInQueue(IOFSwitch sw) throws Exception {
        assertEquals(1, controller.updates.size());
        IUpdate update = controller.updates.take();
        assertEquals(true, update instanceof SwitchUpdate);
        SwitchUpdate swUpdate = (SwitchUpdate) update;
        assertEquals(sw.getId(), swUpdate.getSwId());
        assertEquals(SwitchUpdateType.PORTCHANGED, swUpdate.getSwitchUpdateType());
        assertEquals(PortChangeType.DOWN, swUpdate.getPortChangeType());
    }

    public void verifyPortAddedUpdateInQueue(IOFSwitch sw) throws Exception {
        assertEquals(1, controller.updates.size());
        IUpdate update = controller.updates.take();
        assertEquals(true, update instanceof SwitchUpdate);
        SwitchUpdate swUpdate = (SwitchUpdate) update;
        assertEquals(sw.getId(), swUpdate.getSwId());
        assertEquals(SwitchUpdateType.PORTCHANGED, swUpdate.getSwitchUpdateType());
        assertEquals(PortChangeType.ADD, swUpdate.getPortChangeType());
    }

    public void verifyPortRemovedUpdateInQueue(IOFSwitch sw) throws Exception {
        assertEquals(1, controller.updates.size());
        IUpdate update = controller.updates.take();
        assertEquals(true, update instanceof SwitchUpdate);
        SwitchUpdate swUpdate = (SwitchUpdate) update;
        assertEquals(sw.getId(), swUpdate.getSwId());
        assertEquals(SwitchUpdateType.PORTCHANGED, swUpdate.getSwitchUpdateType());
        assertEquals(PortChangeType.DELETE, swUpdate.getPortChangeType());
    }

    // * Test handlePortStatus()
    // *
    @Test
    public void testHandlePortStatus() throws Exception {
        IOFSwitch sw = createMock(IOFSwitch.class);
        expect(sw.getId()).andReturn(1L).anyTimes();
        //expect(sw.getPorts()).andReturn(new HashSet<OFPortDesc>()).anyTimes();
        OFPortDesc port = factory10.buildPortDesc()
                .setName("myPortName1")
                .setPortNo(OFPort.of(42))
                .build();

        controller.connectedSwitches.put(1L, new OFChannelHandler(controller));
        controller.activeMasterSwitches.put(1L, sw);

        replay(sw);
        controller.notifyPortChanged(sw.getId(), port, PortChangeType.ADD);
        verify(sw);
        verifyPortAddedUpdateInQueue(sw);
        reset(sw);

        expect(sw.getId()).andReturn(1L).anyTimes();

        Set<OFPortState> ofPortStates = new HashSet<OFPortState>();
        ofPortStates.add(OFPortState.LINK_DOWN);
        port.createBuilder().setState(ofPortStates);
        replay(sw);
        controller.notifyPortChanged(sw.getId(), port, PortChangeType.OTHER_UPDATE);
        verify(sw);
        verifyPortChangedUpdateInQueue(sw);
        reset(sw);
        ofPortStates = new HashSet<OFPortState>();
        port.createBuilder().setState(ofPortStates);

        expect(sw.getId()).andReturn(1L).anyTimes();

        port.createBuilder().setState(ofPortStates);
        replay(sw);
        controller.notifyPortChanged(sw.getId(), port, PortChangeType.DOWN);
        verify(sw);
        verifyPortDownUpdateInQueue(sw);
        reset(sw);

        expect(sw.getId()).andReturn(1L).anyTimes();
        replay(sw);
        controller.notifyPortChanged(sw.getId(), port, PortChangeType.DELETE);
        verify(sw);
        verifyPortRemovedUpdateInQueue(sw);
        reset(sw);

    }
}
