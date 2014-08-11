package net.onrc.onos.core.flowprogrammer;


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

import org.easymock.EasyMock;
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

        OFMessage msg = EasyMock.createMock(OFMessage.class);
        EasyMock.expect(msg.getXid()).andReturn((long) 1).anyTimes();
        //EasyMock.expect(msg.()).andReturn((short) 100).anyTimes();
        EasyMock.replay(msg);

        IOFSwitch sw = createConnectedSwitchMock(1, false);



        try {
            sw.write(EasyMock.eq(msg), EasyMock.eq((FloodlightContext) null));
            EasyMock.expectLastCall().once();
            EasyMock.replay(sw);
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
        EasyMock.verify(msg);
        EasyMock.verify(sw);
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

        IOFSwitch sw = createConnectedSwitchMock(1, false);


        List<OFMessage> messages = new ArrayList<OFMessage>();

        for (int i = 0; i < numMsg; ++i) {
            OFMessage msg = EasyMock.createMock(OFMessage.class);
            EasyMock.expect(msg.getXid()).andReturn((long) i).anyTimes();
            EasyMock.replay(msg);
            messages.add(msg);

            try {
                sw.write(EasyMock.eq(msg), EasyMock.eq((FloodlightContext) null));
                EasyMock.expectLastCall().once();
            } catch (IOException e1) {
                fail("Failed in IOFSwitch#write()");
            }
        }
        EasyMock.replay(sw);
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
            EasyMock.verify(msg);
        }
        EasyMock.verify(sw);
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
            IOFSwitch sw = createConnectedSwitchMock(i, false);

            List<OFMessage> messages = new ArrayList<OFMessage>();

            for (int j = 0; j < numMsg; ++j) {
                OFMessage msg = EasyMock.createMock(OFMessage.class);
                EasyMock.expect(msg.getXid()).andReturn((long) j).anyTimes();
                EasyMock.replay(msg);
                messages.add(msg);

                try {
                    sw.write(EasyMock.eq(msg), EasyMock.eq((FloodlightContext) null));
                    EasyMock.expectLastCall().once();
                } catch (IOException e1) {
                    fail("Failed in IOFWrite#write()");
                }
            }
            swMap.put(sw, messages);
            EasyMock.replay(sw);
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
                EasyMock.verify(msg);
            }

            EasyMock.verify(sw);
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
            IOFSwitch sw = createConnectedSwitchMock(i, false);
            //EasyMock.replay(sw);

            List<OFMessage> messages = new ArrayList<OFMessage>();

            for (int j = 0; j < numMsg; ++j) {
                OFMessage msg = EasyMock.createMock(OFMessage.class);
                EasyMock.expect(msg.getXid()).andReturn((long) j).anyTimes();

                EasyMock.replay(msg);
                messages.add(msg);

                try {
                    sw.write(EasyMock.eq(msg), EasyMock.eq((FloodlightContext) null));
                    EasyMock.expectLastCall().once();
                } catch (IOException e1) {
                    fail("Failed in IOFWrite#write()");
                }
            }
            swMap.put(sw, messages);
            EasyMock.replay(sw);
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
                EasyMock.verify(msg);
            }

            EasyMock.verify(sw);
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

        IOFSwitch sw = createConnectedSwitchMock(1, true);
        EasyMock.expect(sw.getOFVersion()).andReturn(OFVersion.OF_10).once();


        List<OFMessage> messages = new ArrayList<OFMessage>();

        for (int i = 0; i < numMsg; ++i) {
            OFMessage msg = EasyMock.createMock(OFMessage.class);
            EasyMock.expect(msg.getXid()).andReturn((long) 1).anyTimes();
            EasyMock.replay(msg);
            messages.add(msg);

            try {
                sw.write(EasyMock.eq(msg), EasyMock.eq((FloodlightContext) null));
                EasyMock.expectLastCall().once();
            } catch (IOException e1) {
                fail("Failed in IOFWrite#write()");
            }
        }

        try {
            sw.write(EasyMock.anyObject(OFBarrierRequest.class), EasyMock.eq((FloodlightContext) null));
            EasyMock.expectLastCall().once();
            barrierTime = System.currentTimeMillis();
        } catch (IOException e1) {
            fail("Failed in IOFWrite#write()");
        }

        EasyMock.replay(sw);

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
            EasyMock.verify(msg);
        }
        EasyMock.verify(sw);
        verifyAll();

        pusher.stop();
    }

    /**
     * Test barrier message is correctly sent to a switch.
     */
    @Test
    public void testBarrierMessage() {
        beginInitMock();

        IOFSwitch sw = createConnectedSwitchMock(1, true);
        EasyMock.expect(sw.getOFVersion()).andReturn(OFVersion.OF_10).once();

        try {
            sw.write((OFMessage) EasyMock.anyObject(), EasyMock.eq((FloodlightContext) null));
            EasyMock.expectLastCall().once();
        } catch (IOException e1) {
            fail("Failed in IOFWrite#write()");
        }
        EasyMock.replay(sw);
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
        flowEntry1.setInPort(new PortNumber((short) 1));
        flowEntry1.setOutPort(new PortNumber((short) 11));
        flowEntry1.setFlowEntryId(new FlowEntryId(1));
        flowEntry1.setFlowEntryMatch(new FlowEntryMatch());
        flowEntry1.setFlowEntryActions(new FlowEntryActions());
        flowEntry1.setFlowEntryErrorState(new FlowEntryErrorState());
        flowEntry1.setFlowEntryUserState(FlowEntryUserState.FE_USER_ADD);
        */

        beginInitMock();

        OFFlowModify fm = EasyMock.createMock(OFFlowModify.class);

        OFFlowModify.Builder bld = EasyMock.createMock(OFFlowModify.Builder.class);

        EasyMock.expect(bld.setIdleTimeout(EasyMock.anyInt())).andReturn(bld);
        EasyMock.expect(bld.setHardTimeout(EasyMock.anyInt())).andReturn(bld);
        EasyMock.expect(bld.setPriority(EasyMock.anyShort())).andReturn(bld);
        EasyMock.expect(bld.setBufferId(OFBufferId.NO_BUFFER)).andReturn(bld);
        EasyMock.expect(bld.setCookie(U64.of(EasyMock.anyLong()))).andReturn(bld);
        EasyMock.expect(bld.setMatch(EasyMock.anyObject(Match.class))).andReturn(bld);
        EasyMock.expect(bld.setActions((List<OFAction>) EasyMock.anyObject())).andReturn(bld);
        EasyMock.expect(bld.setOutPort(OFPort.of(EasyMock.anyInt()))).andReturn(bld).atLeastOnce();
        EasyMock.expect(bld.build()).andReturn(fm);

        EasyMock.expect(fm.getXid()).andReturn(XID_TO_VERIFY).anyTimes();
        EasyMock.expect(fm.getType()).andReturn(OFType.FLOW_MOD).anyTimes();




        IOFSwitch sw = createConnectedSwitchMock(DPID_TO_VERIFY, false);
        EasyMock.expect(sw.getStringId()).andReturn("1").anyTimes();
        EasyMock.expect(sw.getOFVersion()).andReturn(OFVersion.OF_10).once();

        try {
            sw.write(EasyMock.anyObject(OFMessage.class), EasyMock.eq((FloodlightContext) null));
            EasyMock.expectLastCall().once();
        } catch (IOException e1) {
            fail("Failed in IOFWrite#write()");
        }

        EasyMock.replay(bld, fm);
        EasyMock.replay(sw);

        endInitMock();
        initPusher(1);

        pusher.pushFlowEntry(sw, flowEntry1);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail("Failed to sleep");
        }

        EasyMock.verify(sw);
        verifyAll();

        pusher.stop();
    }

    private void beginInitMock() {
        context = EasyMock.createMock(FloodlightContext.class);
        modContext = EasyMock.createMock(FloodlightModuleContext.class);
        // AAS: I don't think we should mock factories... the rabbit whole is too deep.
        //factory10 = EasyMock.createMock(OFFactories.getFactory(OFVersion.OF_10).getClass());
        flProviderService = EasyMock.createMock(IFloodlightProviderService.class);
        threadPoolService = EasyMock.createMock(IThreadPoolService.class);

        EasyMock.expect(modContext.getServiceImpl(EasyMock.eq(IThreadPoolService.class)))
                .andReturn(threadPoolService).once();
        EasyMock.expect(modContext.getServiceImpl(EasyMock.eq(IFloodlightProviderService.class)))
                .andReturn(flProviderService).once();
        // AAS: FlowPusher doesn't call the following anymore.
        flProviderService.addOFMessageListener(EasyMock.eq(OFType.BARRIER_REPLY),
                EasyMock.anyObject(FlowPusher.class));
        EasyMock.expectLastCall().once();

        ScheduledExecutorService executor = EasyMock.createMock(ScheduledExecutorService.class);
        EasyMock.expect(executor.schedule((Runnable) EasyMock.anyObject(), EasyMock.anyLong(),
                (TimeUnit) EasyMock.anyObject())).andReturn(null).once();
        EasyMock.replay(executor);
        EasyMock.expect(threadPoolService.getScheduledExecutor()).andReturn(executor).anyTimes();
    }

    private void endInitMock() {
        EasyMock.replay(threadPoolService);
        EasyMock.replay(flProviderService);
        //EasyMock.replay(factory10);
        EasyMock.replay(modContext);
        EasyMock.replay(context);
    }

    private void verifyAll() {
        EasyMock.verify(threadPoolService);
        EasyMock.verify(flProviderService);
        //EasyMock.verify(factory10);
        EasyMock.verify(modContext);
        EasyMock.verify(context);
    }

    private void initPusher(int numThread) {
        pusher = new FlowPusher(numThread);
        pusher.init(modContext);
        pusher.start();
    }

    private IOFSwitch createConnectedSwitchMock(long dpid, boolean useBarrier) {
        IOFSwitch sw = EasyMock.createMock(IOFSwitch.class);
        EasyMock.expect(sw.isConnected()).andReturn(true).anyTimes();
        EasyMock.expect(sw.getId()).andReturn(dpid).anyTimes();
        sw.flush();
        EasyMock.expectLastCall().anyTimes();
        if (useBarrier) {
            prepareBarrier(sw);
        }

        return sw;
    }

    private void prepareBarrier(IOFSwitch sw) {
        OFBarrierRequest.Builder bld = EasyMock.createMock(factory10.buildBarrierRequest().getClass());
        EasyMock.expect(bld.setXid(EasyMock.anyInt())).andReturn(bld);
        EasyMock.expect(bld.getXid()).andReturn((long) 1).anyTimes();
        EasyMock.expect(bld.getType()).andReturn(OFType.BARRIER_REQUEST).anyTimes();

        OFBarrierRequest req = EasyMock.createMock(OFBarrierRequest.class);
        EasyMock.expect(bld.build()).andReturn(req).anyTimes();
        EasyMock.replay(bld);
        EasyMock.expect(sw.getNextTransactionId()).andReturn(1);
    }

}
