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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.FloodlightProvider;
import net.floodlightcontroller.core.IFloodlightProviderService;
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

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketIn.Builder;
import org.projectfloodlight.openflow.protocol.OFPacketInReason;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsReplyFlags;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.util.HexString;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * Unit tests for the Controller class.
 */
public class ControllerTest extends FloodlightTestCase {

    private Controller controller;
    private MockThreadPoolService threadPool;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        FloodlightModuleContext fmc = new FloodlightModuleContext();

        FloodlightProvider floodlightProvider = new FloodlightProvider();

        controller = (Controller) floodlightProvider.getServiceImpls().get(
                IFloodlightProviderService.class);
        fmc.addService(IFloodlightProviderService.class, controller);

        RestApiServer restApi = new RestApiServer();
        fmc.addService(IRestApiService.class, restApi);

        DebugCounter counterService = new DebugCounter();
        fmc.addService(IDebugCounterService.class, counterService);

        threadPool = new MockThreadPoolService();
        fmc.addService(IThreadPoolService.class, threadPool);

        IControllerRegistryService registry =
                createMock(IControllerRegistryService.class);
        fmc.addService(IControllerRegistryService.class, registry);
        LinkDiscoveryManager linkDiscovery = new LinkDiscoveryManager();
        fmc.addService(ILinkDiscoveryService.class, linkDiscovery);

        restApi.init(fmc);
        floodlightProvider.init(fmc);
        threadPool.init(fmc);
        linkDiscovery.init(fmc);
        restApi.startUp(fmc);
        floodlightProvider.startUp(fmc);
        threadPool.startUp(fmc);
    }

    private OFPacketIn buildPacketIn(short inPort, OFVersion version) {
        IPacket testPacket = new Ethernet()
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

        OFFactory factory = OFFactories.getFactory(version);

        Builder piBuilder = factory.buildPacketIn()
                .setBufferId(OFBufferId.NO_BUFFER)
                .setData(testPacketSerialized)
                .setReason(OFPacketInReason.NO_MATCH);

        if (version == OFVersion.OF_10) {
            piBuilder.setInPort(OFPort.of(inPort));
        } else {
            Match match = factory.buildMatch()
                        .setExact(MatchField.IN_PORT, OFPort.ofShort(inPort))
                        .build();
            piBuilder.setMatch(match);
        }

        return piBuilder.build();
    }

    public Controller getController() {
        return controller;
    }

    private OFStatsReply getStatisticsReply(int transactionId,
            int count, boolean moreReplies, OFVersion version) {
        OFFactory factory = OFFactories.getFactory(version);

        List<OFFlowStatsEntry> statistics = new ArrayList<OFFlowStatsEntry>();
        for (int i = 0; i < count; ++i) {
            statistics.add(factory.buildFlowStatsEntry().build());
        }
        assertEquals(statistics.size(), count);

        org.projectfloodlight.openflow.protocol.OFStatsReply.Builder
                statsReplyBuilder = factory.buildFlowStatsReply()
                    .setXid(transactionId)
                    .setEntries(statistics);

        if (moreReplies) {
            statsReplyBuilder.setFlags(
                    Collections.singleton(OFStatsReplyFlags.REPLY_MORE));
        }

        return statsReplyBuilder.build();
    }

    private IOFSwitch createMockSwitch(long dpid, OFVersion version) {
        IOFSwitch sw = createMock(IOFSwitch.class);
        expect(sw.getId()).andReturn(dpid).anyTimes();
        expect(sw.getStringId()).andReturn(HexString.toHexString(dpid)).anyTimes();
        expect(sw.getPorts()).andReturn(
                Collections.<OFPortDesc>emptySet()).anyTimes();
        expect(sw.getOFVersion()).andReturn(version).anyTimes();
        return sw;
    }

    /**
     * Set up expectations for the callback ordering methods for mocked
     * listener classes.
     *
     * @param listener the mock listener to set up expectations for
     */
    private void setUpNoOrderingRequirements(IListener<OFType> listener) {
        listener.isCallbackOrderingPostreq(EasyMock.<OFType>anyObject(),
                anyObject(String.class));
        expectLastCall().andReturn(false).anyTimes();

        listener.isCallbackOrderingPrereq(EasyMock.<OFType>anyObject(),
                anyObject(String.class));
        expectLastCall().andReturn(false).anyTimes();
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

    /**
     * Verifies that a listener that throws an exception halts further
     * execution, and verifies that the Commands STOP and CONTINUE are honored.
     *
     * @throws Exception
     */
    @Test
    public void testHandleMessages() throws Exception {
        Controller controller = getController();
        controller.removeOFMessageListeners(OFType.PACKET_IN);

        // Just test 1.0 here. We test with 1.3 in testHandleMessageWithContext.
        OFVersion version = OFVersion.OF_10;

        IOFSwitch sw = createMockSwitch(1L, version);
        OFPacketIn pi = buildPacketIn((short) 1, version);

        IOFMessageListener test1 = createMock(IOFMessageListener.class);
        expect(test1.getName()).andReturn("test1").anyTimes();
        setUpNoOrderingRequirements(test1);
        expect(test1.receive(eq(sw), eq(pi), isA(FloodlightContext.class)))
                .andThrow(new RuntimeException(
                "This is NOT an error! We are testing exception catching."));
        IOFMessageListener test2 = createMock(IOFMessageListener.class);
        expect(test2.getName()).andReturn("test2").anyTimes();
        setUpNoOrderingRequirements(test2);
        // expect no calls to test2.receive() since test1.receive() threw an
        // exception

        replay(test1, test2, sw);
        controller.addOFMessageListener(OFType.PACKET_IN, test1);
        controller.addOFMessageListener(OFType.PACKET_IN, test2);
        try {
            controller.handleMessage(sw, pi, null);
        } catch (RuntimeException e) {
            assertEquals(e.getMessage().startsWith("This is NOT an error!"), true);
        }

        verify(test1, test2);

        // verify STOP works
        reset(test1, test2);
        expect(test1.receive(eq(sw), eq(pi), isA(FloodlightContext.class)))
                .andReturn(Command.STOP);

        replay(test1, test2);
        controller.handleMessage(sw, pi, null);
        verify(test1, test2);
    }


    /**
     * Tests message handling when providing a FloodlightContext object.
     * Checks that the correct values are set in the context before the message
     * is dispatched to the listeners.
     *
     * @throws Exception
     */
    @Test
    public void testHandleMessageWithContext() throws Exception {
        doTestHandleMessageWithContext(OFVersion.OF_10);
        doTestHandleMessageWithContext(OFVersion.OF_13);
    }

    private void doTestHandleMessageWithContext(OFVersion version)
                                                throws IOException {
        controller.messageListeners.clear();

        short inPort = (short) 1;
        FloodlightContext cntx = new FloodlightContext();

        IOFSwitch sw = createMockSwitch(1L, version);
        OFPacketIn pi = buildPacketIn(inPort, version);

        IOFMessageListener test1 = createMock(IOFMessageListener.class);
        expect(test1.getName()).andReturn("test1").anyTimes();
        setUpNoOrderingRequirements(test1);

        expect(test1.receive(same(sw), same(pi), same(cntx)))
                .andReturn(Command.CONTINUE);

        IOFMessageListener test2 = createMock(IOFMessageListener.class);
        expect(test2.getName()).andReturn("test2").anyTimes();
        setUpNoOrderingRequirements(test2);
        // test2 will not receive any message!

        replay(test1, test2, sw);
        controller.addOFMessageListener(OFType.PACKET_IN, test1);
        controller.addOFMessageListener(OFType.ERROR, test2);
        controller.handleMessage(sw, pi, cntx);
        verify(test1, test2, sw);

        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
                IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        assertArrayEquals(pi.getData(), eth.serialize());

        short actualInPort = (short) cntx.getStorage()
                .get(IFloodlightProviderService.CONTEXT_PI_INPORT);
        assertEquals(inPort, actualInPort);
    }

    /**
     * Task used to get the value from a Future in a different thread.
     *
     * @param <E> the type of value returned by the Future
     */
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
         * Gets the value from the Future.
         *
         * @return the value
         */
        public E getValue() {
            return value;
        }

        /**
         * Gets the Future.
         *
         * @return the future
         */
        public Future<E> getFuture() {
            return future;
        }
    }

    /**
     * Test that the OFStatisticsFuture correctly returns statistics replies.
     *
     * @throws Exception
     */
    @Test
    public void testOFStatisticsFuture() throws Exception {
        // Test both OF 1.0 and 1.3 stats messages
        doTestOFStatisticsFuture(OFVersion.OF_10);
        doTestOFStatisticsFuture(OFVersion.OF_13);
    }

    private void doTestOFStatisticsFuture(OFVersion version) throws Exception {
        // Test for a single stats reply
        IOFSwitch sw = createMock(IOFSwitch.class);
        sw.cancelStatisticsReply(1);
        OFStatisticsFuture sf = new OFStatisticsFuture(threadPool, sw, 1);

        replay(sw);
        List<OFStatsReply> stats;
        FutureFetcher<List<OFStatsReply>> ff =
                new FutureFetcher<List<OFStatsReply>>(sf);
        Thread t = new Thread(ff);
        t.start();
        sf.deliverFuture(sw, getStatisticsReply(1, 10, false, version));

        t.join();
        stats = ff.getValue();
        verify(sw);

        // We expect 1 flow stats reply message containing 10 flow stats entries
        assertEquals(1, stats.size());
        assertEquals(OFStatsType.FLOW, stats.get(0).getStatsType());
        assertEquals(Collections.EMPTY_SET, stats.get(0).getFlags());
        OFFlowStatsReply flowStatsReply = (OFFlowStatsReply) stats.get(0);
        assertEquals(10, flowStatsReply.getEntries().size());

        // Test multiple stats replies
        reset(sw);
        sw.cancelStatisticsReply(1);

        sf = new OFStatisticsFuture(threadPool, sw, 1);

        replay(sw);
        ff = new FutureFetcher<List<OFStatsReply>>(sf);
        t = new Thread(ff);
        t.start();
        sf.deliverFuture(sw, getStatisticsReply(1, 10, true, version));
        sf.deliverFuture(sw, getStatisticsReply(1, 5, false, version));
        t.join();

        stats = sf.get();
        verify(sw);

        // We expect 2 flow stats replies. The first one has 10 entries and has
        // the REPLY_MORE flag set, and the second one has 5 entries with no flag.
        assertEquals(2, stats.size());
        assertEquals(OFStatsType.FLOW, stats.get(0).getStatsType());
        assertEquals(Collections.singleton(OFStatsReplyFlags.REPLY_MORE),
                stats.get(0).getFlags());
        assertEquals(OFStatsType.FLOW, stats.get(1).getStatsType());
        assertEquals(Collections.EMPTY_SET, stats.get(1).getFlags());
        OFFlowStatsReply flowStatsReply2 = (OFFlowStatsReply) stats.get(0);
        assertEquals(10, flowStatsReply2.getEntries().size());
        OFFlowStatsReply flowStatsReply3 = (OFFlowStatsReply) stats.get(1);
        assertEquals(5, flowStatsReply3.getEntries().size());

        // Test cancellation
        reset(sw);
        sw.cancelStatisticsReply(1);
        sf = new OFStatisticsFuture(threadPool, sw, 1);

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
        sf = new OFStatisticsFuture(threadPool, sw, 1, 75, TimeUnit.MILLISECONDS);

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
     * Test adding a new switch in the MASTER role.
     * We expect a switchActivatedMaster event fired to the switch listeners.
     */
    @Test
    public void testSwitchActivatedMaster() throws Exception {
        long dpid = 1L;

        controller.setAlwaysClearFlowsOnSwActivate(false);
        controller.setAlwaysClearFlowsOnSwAdd(false);

        // Create a 1.0 switch. There's no difference between 1.0 and 1.3 here.
        IOFSwitch sw = createMockSwitch(dpid, OFVersion.OF_10);

        // strict mock. Order of events matters!
        IOFSwitchListener listener = createStrictMock(IOFSwitchListener.class);
        listener.switchActivatedMaster(dpid);
        expectLastCall().once();
        replay(listener);
        controller.addOFSwitchListener(listener);

        replay(sw);
        controller.addConnectedSwitch(dpid, new OFChannelHandler(controller));
        controller.addActivatedMasterSwitch(dpid, sw);
        verify(sw);
        assertEquals(sw, controller.getMasterSwitch(dpid));
        controller.processUpdateQueueForTesting();
        verify(listener);
    }

    /**
     * Test adding a new switch in the EQUAL role.
     * We expect a switchActivatedEqual event fired to the switch listeners.
     */
    @Test
    public void testSwitchActivatedEqual() throws Exception {
        long dpid = 1L;
        // Create a 1.0 switch. There's no difference between 1.0 and 1.3 here.
        IOFSwitch sw = createMockSwitch(dpid, OFVersion.OF_10);

        IOFSwitchListener listener = createStrictMock(IOFSwitchListener.class);
        controller.addOFSwitchListener(listener);

        listener.switchActivatedSlave(dpid);
        replay(sw, listener); // nothing recorded
        controller.addConnectedSwitch(dpid, new OFChannelHandler(controller));
        controller.addActivatedSlaveSwitch(dpid, sw);
        verify(sw);
        controller.processUpdateQueueForTesting();
        verify(listener);
    }

    /**
     * Disconnect a switch which was connected in the MASTER role.
     * Check the correct cleanup methods are called on the switch, that the
     * switch is removed from the Controller data structures, and that the
     * correct switch listener method is called.
     */
    @Test
    public void testDisconnectMasterSwitch() {
        long dpid = 1L;

        // Create a 1.0 switch. There's no difference between 1.0 and 1.3 here.
        IOFSwitch sw = createMockSwitch(dpid, OFVersion.OF_10);
        replay(sw);

        // Add switch to controller as MASTER
        controller.addConnectedSwitch(dpid, new OFChannelHandler(controller));
        controller.addActivatedMasterSwitch(dpid, sw);

        // Check the switch is in the controller's lists
        assertEquals(sw, controller.getMasterSwitch(dpid));

        IOFSwitchListener listener = createStrictMock(IOFSwitchListener.class);
        listener.switchDisconnected(dpid);
        expectLastCall().once();
        replay(listener);
        // Drain the update queue
        controller.processUpdateQueueForTesting();
        // Add the listener
        controller.addOFSwitchListener(listener);

        reset(sw);
        sw.cancelAllStatisticsReplies();
        expectLastCall().once();
        sw.setConnected(false);
        expectLastCall().once();
        replay(sw);

        // Disconnect switch
        controller.removeConnectedSwitch(dpid);

        assertNull(controller.getMasterSwitch(dpid));

        controller.processUpdateQueueForTesting();
        verify(listener, sw);
    }

    /**
     * Disconnect a switch which was connected in the EQUAL role.
     * Check the correct cleanup methods are called on the switch, that the
     * switch is removed from the Controller data structures, and that the
     * correct switch listener method is called.
     */
    @Test
    public void testDisconnectEqualSwitch() {
        long dpid = 1L;

        // Create a 1.0 switch. There's no difference between 1.0 and 1.3 here.
        IOFSwitch sw = createMockSwitch(dpid, OFVersion.OF_10);
        replay(sw);

        // Add switch to controller as EQUAL
        controller.addConnectedSwitch(dpid, new OFChannelHandler(controller));
        controller.addActivatedSlaveSwitch(dpid, sw);

        // Check the switch is in the controller's lists
        assertEquals(sw, controller.getSlaveSwitch(dpid));

        IOFSwitchListener listener = createStrictMock(IOFSwitchListener.class);
        listener.switchDisconnected(dpid);
        expectLastCall().once();
        replay(listener);
        // Drain the update queue
        controller.processUpdateQueueForTesting();
        // Add the listener
        controller.addOFSwitchListener(listener);

        reset(sw);
        sw.cancelAllStatisticsReplies();
        expectLastCall().once();
        sw.setConnected(false);
        expectLastCall().once();
        replay(sw);

        // Disconnect switch
        controller.removeConnectedSwitch(dpid);

        assertNull(controller.getSlaveSwitch(dpid));

        controller.processUpdateQueueForTesting();
        verify(listener, sw);
    }

    /**
     * Remove a nonexistent switch.
     * Check the switch listeners don't receive a disconnected event.
     */
    @Test
    public void testNonexistingSwitchDisconnected() throws Exception {
        long dpid = 1L;
        // Create a 1.0 switch. There's no difference between 1.0 and 1.3 here.
        IOFSwitch sw = createMockSwitch(1L, OFVersion.OF_10);
        IOFSwitchListener listener = createStrictMock(IOFSwitchListener.class);
        controller.addOFSwitchListener(listener);
        replay(sw, listener);
        controller.removeConnectedSwitch(dpid);
        controller.processUpdateQueueForTesting();
        verify(sw, listener);

        assertNull(controller.getSwitch(dpid));
    }

    /**
     * Try to connect a switch with the same DPID as an already active switch.
     * This could happen if two different switches have the same DPID or if a
     * switch reconnects while the old TCP connection is still alive.
     * Check that {@link Controller#addConnectedSwitch(long, OFChannelHandler)}
     * returns false and that no modification is made to the connectedSwitches
     * map.
     */
    @Test
    public void testConnectSwitchWithSameDpid() {
        long dpid = 1L;

        // Setup: add a switch to the controller
        // Create a 1.0 switch. There's no difference between 1.0 and 1.3 here.
        IOFSwitch oldsw = createMockSwitch(dpid, OFVersion.OF_10);

        replay(oldsw);
        OFChannelHandler oldChannel = new OFChannelHandler(controller);

        assertTrue(controller.addConnectedSwitch(dpid, oldChannel));
        controller.addActivatedMasterSwitch(dpid, oldsw);

        assertSame(oldChannel, controller.connectedSwitches.get(dpid));
        assertEquals(oldsw, controller.getSwitch(dpid));

        // Now try to add a new switch (OFChannelHandler) with the same dpid to
        // the controller. #addConnectedSwitch should return false and no
        // modification should be made to the connected switches map.
        assertFalse(controller.addConnectedSwitch(
                dpid, new OFChannelHandler(controller)));
        assertSame(oldChannel, controller.connectedSwitches.get(dpid));
    }

    /**
     * Tests that you can't remove a switch from the map returned by
     * getSwitches() (because getSwitches should return an unmodifiable map)
     */
    @Test
    public void testRemoveActiveSwitch() {
        long dpid = 1L;

        // Create a 1.0 switch. There's no difference between 1.0 and 1.3 here.
        IOFSwitch sw = createMockSwitch(dpid, OFVersion.OF_10);
        replay(sw);
        controller.addConnectedSwitch(1L, new OFChannelHandler(controller));
        controller.addActivatedMasterSwitch(1L, sw);
        assertEquals(sw, getController().getSwitch(1L));
        controller.getAllSwitchDpids().remove(1L);
        assertEquals(sw, getController().getSwitch(1L));
        verify(sw);
    }

    /**
     * Implementation of an IOFSwitchListener that counts the number of events
     * it has received of each different event type.
     */
    private static class DummySwitchListener implements IOFSwitchListener {
        // The multisets will record a count of the number of times each update
        // has been seen by the listener
        private Multiset<SwitchUpdateType> updateCount = HashMultiset.create();
        private Multiset<PortChangeType> portUpdateCount = HashMultiset.create();

        /**
         * Gets the number of times a switch update event of the specified type
         * has been received.
         *
         * @param type SwitchUpdateType to get the count for
         * @return number of times the event has been received
         */
        public int getSwitchUpdateCount(SwitchUpdateType type) {
            return updateCount.count(type);
        }

        /**
         * Gets the number of times a port update event of the specified type
         * has been received.
         *
         * @param type PortChangeType to get the count for
         * @return number of times the event has been received
         */
        public int getPortUpdateCount(PortChangeType type) {
            return portUpdateCount.count(type);
        }

        @Override
        public String getName() {
            return "dummy";
        }

        @Override
        public synchronized void switchActivatedMaster(long swId) {
            updateCount.add(SwitchUpdateType.ACTIVATED_MASTER);
            notifyAll();
        }

        @Override
        public synchronized void switchActivatedSlave(long swId) {
            updateCount.add(SwitchUpdateType.ACTIVATED_SLAVE);
            notifyAll();
        }

        @Override
        public synchronized void switchMasterToSlave(long swId) {
            updateCount.add(SwitchUpdateType.MASTER_TO_SLAVE);
            notifyAll();
        }

        @Override
        public synchronized void switchSlaveToMaster(long swId) {
            updateCount.add(SwitchUpdateType.SLAVE_TO_MASTER);
            notifyAll();
        }

        @Override
        public synchronized void switchDisconnected(long swId) {
            updateCount.add(SwitchUpdateType.DISCONNECTED);
            notifyAll();
        }

        @Override
        public synchronized void switchPortChanged(long swId, OFPortDesc port,
                PortChangeType changeType) {
            portUpdateCount.add(changeType);
            notifyAll();
        }
    }

    /**
     * Tests that updates sent into the Controller updates queue are dispatched
     * to the listeners correctly.
     *
     * @throws InterruptedException
     */
    @Test
    public void testUpdateQueue() throws InterruptedException {
        // No difference between OpenFlow versions here
        OFVersion version = OFVersion.OF_10;
        OFPortDesc port = OFFactories.getFactory(version)
                .buildPortDesc().build();
        long dpid = 1L;

        DummySwitchListener switchListener = new DummySwitchListener();
        IOFSwitch sw = createMockSwitch(dpid, version);
        replay(sw);
        ControllerRunThread t = new ControllerRunThread();
        t.start();

        controller.addOFSwitchListener(switchListener);

        // Switch updates
        doTestUpdateQueueWithUpdate(dpid, SwitchUpdateType.ACTIVATED_MASTER,
                switchListener);
        doTestUpdateQueueWithUpdate(dpid, SwitchUpdateType.ACTIVATED_SLAVE,
                switchListener);
        doTestUpdateQueueWithUpdate(dpid, SwitchUpdateType.SLAVE_TO_MASTER,
                switchListener);
        doTestUpdateQueueWithUpdate(dpid, SwitchUpdateType.MASTER_TO_SLAVE,
                switchListener);
        doTestUpdateQueueWithUpdate(dpid, SwitchUpdateType.DISCONNECTED,
                switchListener);

        // Port updates
        doTestUpdateQueueWithPortUpdate(dpid, port, PortChangeType.ADD,
                switchListener);
        doTestUpdateQueueWithPortUpdate(dpid, port, PortChangeType.OTHER_UPDATE,
                switchListener);
        doTestUpdateQueueWithPortUpdate(dpid, port, PortChangeType.DELETE,
                switchListener);
        doTestUpdateQueueWithPortUpdate(dpid, port, PortChangeType.UP,
                switchListener);
        doTestUpdateQueueWithPortUpdate(dpid, port, PortChangeType.DOWN,
                switchListener);
    }

    private void doTestUpdateQueueWithUpdate(long dpid, SwitchUpdateType type,
            DummySwitchListener listener) throws InterruptedException {
        controller.updates.put(controller.new SwitchUpdate(dpid, type));
        synchronized (listener) {
            listener.wait(500);
        }
        // Test that the update was seen by the listener 1 time
        assertEquals(1, listener.getSwitchUpdateCount(type));
    }

    private void doTestUpdateQueueWithPortUpdate(long dpid, OFPortDesc port,
            PortChangeType type,
            DummySwitchListener listener) throws InterruptedException {
        controller.updates.put(controller.new SwitchUpdate(dpid,
                SwitchUpdateType.PORTCHANGED, port, type));
        synchronized (listener) {
            listener.wait(500);
        }
        // Test that the update was seen by the listener 1 time
        assertEquals(1, listener.getPortUpdateCount(type));
    }

    /**
     * Test the method to notify the controller of a port change.
     * The controller should send out an update to the switch listeners.
     *
     * @throws InterruptedException
     */
    @Test
    public void testNotifyPortChanged() throws InterruptedException {
        long dpid = 1L;

        // No difference between OpenFlow versions here
        OFVersion version = OFVersion.OF_10;
        IOFSwitch sw = createMockSwitch(dpid, version);
        OFPortDesc port = OFFactories.getFactory(version).buildPortDesc()
                .setName("myPortName1")
                .setPortNo(OFPort.of(42))
                .build();

        replay(sw);

        controller.connectedSwitches.put(1L, new OFChannelHandler(controller));
        controller.activeMasterSwitches.put(1L, sw);

        doTestNotifyPortChanged(dpid, port, PortChangeType.ADD);
        doTestNotifyPortChanged(dpid, port, PortChangeType.OTHER_UPDATE);
        doTestNotifyPortChanged(dpid, port, PortChangeType.DELETE);
        doTestNotifyPortChanged(dpid, port, PortChangeType.UP);
        doTestNotifyPortChanged(dpid, port, PortChangeType.DOWN);

    }

    private void doTestNotifyPortChanged(long dpid, OFPortDesc port,
            PortChangeType changeType) throws InterruptedException {
        controller.notifyPortChanged(dpid, port, changeType);

        assertEquals(1, controller.updates.size());
        IUpdate update = controller.updates.take();
        assertEquals(true, update instanceof SwitchUpdate);
        SwitchUpdate swUpdate = (SwitchUpdate) update;
        assertEquals(dpid, swUpdate.getSwId());
        assertEquals(SwitchUpdateType.PORTCHANGED, swUpdate.getSwitchUpdateType());
        assertEquals(changeType, swUpdate.getPortChangeType());
    }
}
