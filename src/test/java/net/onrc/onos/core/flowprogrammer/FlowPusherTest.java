package net.onrc.onos.core.flowprogrammer;


import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyShort;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.OFMessageFuture;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.onrc.onos.core.intent.FlowEntry;
import net.onrc.onos.core.intent.IntentOperation.Operator;
import net.onrc.onos.core.util.IntegrationTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.projectfloodlight.openflow.protocol.OFBarrierReply;
import org.projectfloodlight.openflow.protocol.OFBarrierRequest;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowModify;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.util.HexString;


@Category(IntegrationTest.class)
public class FlowPusherTest {
    private FlowPusher pusher;
    private FloodlightContext context;
    private FloodlightModuleContext modContext;
    private IFloodlightProviderService flProviderService;
    private IThreadPoolService threadPoolService;

    private OFFactory factory10 = OFFactories.getFactory(OFVersion.OF_10);

    /**
     * Test single OFMessage is correctly sent to single switch.
     */
    @Test
    public void testAddMessage() {
        beginInitMock();

        OFMessage msg = createMock(OFMessage.class);
        expect(msg.getXid()).andReturn((long) 1).anyTimes();
        replay(msg);

        IOFSwitch sw = createConnectedSwitchMock(1);

        try {
            sw.write(eq(msg), eq((FloodlightContext) null));
            expectLastCall().once();
            replay(sw);
        } catch (IOException e1) {
            fail("Failed in IOFSwitch#write()");
        }

        endInitMock();
        initPusher(1);

        boolean addResult = pusher.add(sw, msg);
        assertTrue(addResult);

        try {
            // wait until message is processed.
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail("Failed in Thread.sleep()");
        }
        verify(msg);
        verify(sw);
        verifyAll();

        pusher.stop();
    }

    /**
     * Test bunch of OFMessages are correctly sent to single switch.
     */
    @Test
    public void testMassiveAddMessage() {
        // Some number larger than FlowPusher.MAX_MESSAGE_SEND
        final int numMsg = FlowPusher.MAX_MESSAGE_SEND * 2;

        beginInitMock();

        IOFSwitch sw = createConnectedSwitchMock(1);


        List<OFMessage> messages = new ArrayList<OFMessage>();

        for (int i = 0; i < numMsg; ++i) {
            OFMessage msg = createMock(OFMessage.class);
            expect(msg.getXid()).andReturn((long) i).anyTimes();
            replay(msg);
            messages.add(msg);

            try {
                sw.write(eq(msg), eq((FloodlightContext) null));
                expectLastCall().once();
            } catch (IOException e1) {
                fail("Failed in IOFSwitch#write()");
            }
        }
        replay(sw);
        endInitMock();
        initPusher(1);

        for (OFMessage msg : messages) {
            boolean addResult = pusher.add(sw, msg);
            assertTrue(addResult);
        }

        try {
            // wait until message is processed.
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail("Failed in Thread.sleep()");
        }

        for (OFMessage msg : messages) {
            verify(msg);
        }
        verify(sw);
        verifyAll();

        pusher.stop();
    }

    /**
     * Test bunch of OFMessages are correctly sent to multiple switches with single threads.
     */
    @Test
    public void testMultiSwitchAddMessage() {
        final int numSwitch = 10;
        final int numMsg = 100;    // messages per thread

        beginInitMock();

        Map<IOFSwitch, List<OFMessage>> swMap = new HashMap<IOFSwitch, List<OFMessage>>();
        for (int i = 0; i < numSwitch; ++i) {
            IOFSwitch sw = createConnectedSwitchMock(i);

            List<OFMessage> messages = new ArrayList<OFMessage>();

            for (int j = 0; j < numMsg; ++j) {
                OFMessage msg = createMock(OFMessage.class);
                expect(msg.getXid()).andReturn((long) j).anyTimes();
                replay(msg);
                messages.add(msg);

                try {
                    sw.write(eq(msg), eq((FloodlightContext) null));
                    expectLastCall().once();
                } catch (IOException e1) {
                    fail("Failed in IOFWrite#write()");
                }
            }
            swMap.put(sw, messages);
            replay(sw);
        }

        endInitMock();
        initPusher(1);

        for (IOFSwitch sw : swMap.keySet()) {
            for (OFMessage msg : swMap.get(sw)) {
                boolean addResult = pusher.add(sw, msg);
                assertTrue(addResult);
            }
        }

        try {
            // wait until message is processed.
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail("Failed in Thread.sleep()");
        }

        for (IOFSwitch sw : swMap.keySet()) {
            for (OFMessage msg : swMap.get(sw)) {
                verify(msg);
            }

            verify(sw);
        }
        verifyAll();

        pusher.stop();
    }

    /**
     * Test bunch of OFMessages are correctly sent to multiple switches using multiple threads.
     */
    @Test
    public void testMultiThreadedAddMessage() {
        final int numThreads = 10;
        final int numMsg = 100;    // messages per thread

        beginInitMock();

        Map<IOFSwitch, List<OFMessage>> swMap = new HashMap<IOFSwitch, List<OFMessage>>();
        for (int i = 0; i < numThreads; ++i) {
            IOFSwitch sw = createConnectedSwitchMock(i);
            //EasyMock.replay(sw);

            List<OFMessage> messages = new ArrayList<OFMessage>();

            for (int j = 0; j < numMsg; ++j) {
                OFMessage msg = createMock(OFMessage.class);
                expect(msg.getXid()).andReturn((long) j).anyTimes();

                replay(msg);
                messages.add(msg);

                try {
                    sw.write(eq(msg), eq((FloodlightContext) null));
                    expectLastCall().once();
                } catch (IOException e1) {
                    fail("Failed in IOFWrite#write()");
                }
            }
            swMap.put(sw, messages);
            replay(sw);
        }

        endInitMock();
        initPusher(numThreads);
        for (IOFSwitch sw : swMap.keySet()) {
            for (OFMessage msg : swMap.get(sw)) {
                boolean addResult = pusher.add(sw, msg);
                assertTrue(addResult);
            }
        }

        try {
            // wait until message is processed.
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail("Failed in Thread.sleep()");
        }

        for (IOFSwitch sw : swMap.keySet()) {
            for (OFMessage msg : swMap.get(sw)) {
                verify(msg);
            }

            verify(sw);
        }
        verifyAll();

        pusher.stop();
    }

    private long barrierTime = 0;

    /**
     * Test rate limitation of messages works correctly.
     */
    @Test
    public void testRateLimitedAddMessage() {
        final long limitRate = 100; // [bytes/ms]
        final int numMsg = 1000;

        // Accuracy of FlowPusher's rate calculation can't be measured by unit test
        // because switch doesn't return BARRIER_REPLY.
        // In unit test we use approximate way to measure rate. This value is
        // acceptable margin of measured rate.
        final double acceptableRate = limitRate * 1.2;

        beginInitMock();

        IOFSwitch sw = createConnectedSwitchMock(1);

        List<OFMessage> messages = new ArrayList<OFMessage>();

        for (int i = 0; i < numMsg; ++i) {
            OFMessage msg = createMock(OFMessage.class);
            expect(msg.getXid()).andReturn((long) 1).anyTimes();
            replay(msg);
            messages.add(msg);

            try {
                sw.write(eq(msg), eq((FloodlightContext) null));
                expectLastCall().once();
            } catch (IOException e1) {
                fail("Failed in IOFWrite#write()");
            }
        }

        try {
            sw.write(anyObject(OFBarrierRequest.class), eq((FloodlightContext) null));
            expectLastCall().once();
            barrierTime = System.currentTimeMillis();
        } catch (IOException e1) {
            fail("Failed in IOFWrite#write()");
        }

        replay(sw);

        endInitMock();
        initPusher(1);

        pusher.createQueue(sw);
        pusher.setRate(sw, limitRate);

        long beginTime = System.currentTimeMillis();
        for (OFMessage msg : messages) {
            boolean addResult = pusher.add(sw, msg);
            assertTrue(addResult);
        }

        pusher.barrierAsync(sw);

        try {
            do {
                Thread.sleep(1000);
            } while (barrierTime == 0);
        } catch (InterruptedException e) {
            fail("Failed to sleep");
        }

        double measuredRate = numMsg * 100 / (barrierTime - beginTime);
        assertTrue(measuredRate < acceptableRate);

        for (OFMessage msg : messages) {
            verify(msg);
        }
        verify(sw);
        verifyAll();

        pusher.stop();
    }

    /**
     * Test barrier message is correctly sent to a switch.
     */
    @Test
    public void testBarrierMessage() {
        beginInitMock();

        IOFSwitch sw = createConnectedSwitchMock(1);
        expect(sw.getOFVersion()).andReturn(OFVersion.OF_10).once();

        try {
            sw.write((OFMessage) anyObject(), eq((FloodlightContext) null));
            expectLastCall().once();
        } catch (IOException e1) {
            fail("Failed in IOFWrite#write()");
        }
        replay(sw);
        endInitMock();
        initPusher(1);

        OFMessageFuture<OFBarrierReply> future = pusher.barrierAsync(sw);

        assertNotNull(future);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail("Failed to sleep");
        }

        verifyAll();

        pusher.stop();
    }

    static final long XID_TO_VERIFY = 100;
    static final long DPID_TO_VERIFY = 10;

    /**
     * Test FlowObject is correctly converted to message and is sent to a switch.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAddFlow() {
        // instantiate required objects
        FlowEntry flowEntry1 = new FlowEntry(DPID_TO_VERIFY, 1, 11, null, null, 0, 0, Operator.ADD);
        /*
        flowEntry1.setDpid(new Dpid(DPID_TO_VERIFY));
        flowEntry1.setFlowId(new FlowId(1));
        flowEntry1.setInPort(PortNumber.uint16((short) 1));
        flowEntry1.setOutPort(PortNumber.uint16((short) 11));
        flowEntry1.setFlowEntryId(new FlowEntryId(1));
        flowEntry1.setFlowEntryMatch(new FlowEntryMatch());
        flowEntry1.setFlowEntryActions(new FlowEntryActions());
        flowEntry1.setFlowEntryErrorState(new FlowEntryErrorState());
        flowEntry1.setFlowEntryUserState(FlowEntryUserState.FE_USER_ADD);
        */

        beginInitMock();

        OFFlowModify fm = createMock(OFFlowModify.class);

        OFFlowModify.Builder bld = createMock(OFFlowModify.Builder.class);

        expect(bld.setIdleTimeout(anyInt())).andReturn(bld);
        expect(bld.setHardTimeout(anyInt())).andReturn(bld);
        expect(bld.setPriority(anyShort())).andReturn(bld);
        expect(bld.setBufferId(OFBufferId.NO_BUFFER)).andReturn(bld);
        expect(bld.setCookie(U64.of(anyLong()))).andReturn(bld);
        expect(bld.setMatch(anyObject(Match.class))).andReturn(bld);
        expect(bld.setActions((List<OFAction>) anyObject())).andReturn(bld);
        expect(bld.setOutPort(OFPort.of(anyInt()))).andReturn(bld).atLeastOnce();
        expect(bld.build()).andReturn(fm);

        expect(fm.getXid()).andReturn(XID_TO_VERIFY).anyTimes();
        expect(fm.getType()).andReturn(OFType.FLOW_MOD).anyTimes();

        IOFSwitch sw = createConnectedSwitchMock(DPID_TO_VERIFY);

        try {
            sw.write(anyObject(OFMessage.class), eq((FloodlightContext) null));
            expectLastCall().once();
        } catch (IOException e1) {
            fail("Failed in IOFWrite#write()");
        }

        replay(bld, fm);
        replay(sw);

        endInitMock();
        initPusher(1);

        pusher.pushFlowEntry(sw, flowEntry1);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail("Failed to sleep");
        }

        verify(sw);
        verifyAll();

        pusher.stop();
    }

    private void beginInitMock() {
        context = createMock(FloodlightContext.class);
        modContext = createMock(FloodlightModuleContext.class);
        // AAS: I don't think we should mock factories... the rabbit whole is too deep.
        //factory10 = EasyMock.createMock(OFFactories.getFactory(OFVersion.OF_10).getClass());
        flProviderService = createMock(IFloodlightProviderService.class);
        threadPoolService = createMock(IThreadPoolService.class);

        expect(modContext.getServiceImpl(eq(IThreadPoolService.class)))
                .andReturn(threadPoolService).once();
        expect(modContext.getServiceImpl(eq(IFloodlightProviderService.class)))
                .andReturn(flProviderService).once();
        // AAS: FlowPusher doesn't call the following anymore.
        flProviderService.addOFMessageListener(eq(OFType.BARRIER_REPLY),
                anyObject(FlowPusher.class));
        expectLastCall().once();

        ScheduledExecutorService executor = createMock(ScheduledExecutorService.class);
        expect(executor.schedule((Runnable) anyObject(), anyLong(),
                (TimeUnit) anyObject())).andReturn(null).once();
        replay(executor);
        expect(threadPoolService.getScheduledExecutor()).andReturn(executor).anyTimes();
    }

    private void endInitMock() {
        replay(threadPoolService);
        replay(flProviderService);
        //EasyMock.replay(factory10);
        replay(modContext);
        replay(context);
    }

    private void verifyAll() {
        verify(threadPoolService);
        verify(flProviderService);
        //EasyMock.verify(factory10);
        verify(modContext);
        verify(context);
    }

    private void initPusher(int numThread) {
        pusher = new FlowPusher(numThread);
        pusher.init(modContext);
        pusher.start();
    }

    private IOFSwitch createConnectedSwitchMock(long dpid) {
        IOFSwitch sw = createMock(IOFSwitch.class);
        expect(sw.isConnected()).andReturn(true).anyTimes();
        expect(sw.getId()).andReturn(dpid).anyTimes();
        expect(sw.getStringId()).andReturn(HexString.toHexString(dpid))
                .anyTimes();
        expect(sw.getNextTransactionId()).andReturn(1).times(0, 1);
        // TODO 1.3ize
        expect(sw.getFactory()).andReturn(factory10).anyTimes();
        sw.flush();
        expectLastCall().anyTimes();

        return sw;
    }

}
