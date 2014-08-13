package net.onrc.onos.core.flowprogrammer;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.floodlightcontroller.core.IOFSwitch;
import net.onrc.onos.core.flowprogrammer.IFlowPusherService.MsgPriority;
import net.onrc.onos.core.flowprogrammer.IFlowSyncService.SyncResult;
import net.onrc.onos.core.intent.FlowEntry;

import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModCommand;
import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFFlowStatsRequest;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.U64;

// Test should be fixed to fit RAMCloud basis
@Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest({FlowSynchronizer.class })
public class FlowSynchronizerTest {
    private FlowPusher pusher;
    private FlowSynchronizer sync;
    private List<Long> idAdded;
    private List<Long> idRemoved;

    /*
     * OF1.0 Factory for now. Change when we move to
     * OF 1.3.
     */
    private static OFFactory factory10;

    @Before
    public void setUp() throws Exception {
        factory10 = OFFactories.getFactory(OFVersion.OF_10);
        idAdded = new ArrayList<Long>();
        idRemoved = new ArrayList<Long>();

        pusher = createMock(FlowPusher.class);
        expect(pusher.suspend(anyObject(IOFSwitch.class))).andReturn(true).anyTimes();
        expect(pusher.resume(anyObject(IOFSwitch.class))).andReturn(true).anyTimes();
        pusher.add(anyObject(IOFSwitch.class), anyObject(OFMessage.class),
                eq(MsgPriority.HIGH));
        expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws Throwable {
                OFMessage msg = (OFMessage) getCurrentArguments()[1];
                if (msg.getType().equals(OFType.FLOW_MOD)) {
                    OFFlowMod fm = (OFFlowMod) msg;
                    if (fm.getCommand() == OFFlowModCommand.DELETE_STRICT) {
                        idRemoved.add(fm.getCookie().getValue());
                    }
                }
                return null;
            }
        }).anyTimes();
        pusher.pushFlowEntry(anyObject(IOFSwitch.class), anyObject(FlowEntry.class),
                eq(MsgPriority.HIGH));
        expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws Throwable {
                FlowEntry flow = (FlowEntry) getCurrentArguments()[1];
                idAdded.add(flow.getFlowEntryId());
                return null;
            }
        }).anyTimes();
        replay(pusher);
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test that synchronization doesn't affect anything in case either DB and
     * flow table has the same entries.
     */
    @Test
    public void testStable() {
        // Create mock of flow table : flow 1
        IOFSwitch sw = createMockSwitch(new long[]{1});

        // Create mock of flow entries : flow 1
        initMockGraph(new long[]{1});

        // synchronize
        doSynchronization(sw);

        // check if flow is not changed
        assertEquals(0, idAdded.size());
        assertEquals(0, idRemoved.size());
    }

    /**
     * Test that an flow is added in case DB has an extra FlowEntry.
     */
    @Test
    public void testSingleAdd() {
        // Create mock of flow table : null
        IOFSwitch sw = createMockSwitch(new long[]{});

        // Create mock of flow entries : flow 1
        initMockGraph(new long[]{1});

        // synchronize
        doSynchronization(sw);

        // check if single flow is installed
        assertEquals(1, idAdded.size());
        assertTrue(idAdded.contains((long) 1));
        assertEquals(0, idRemoved.size());
    }

    /**
     * Test that an flow is deleted in case switch has an extra FlowEntry.
     */
    @Test
    public void testSingleDelete() {
        // Create mock of flow table : flow 1
        IOFSwitch sw = createMockSwitch(new long[]{1});

        // Create mock of flow entries : null
        initMockGraph(new long[]{});

        // synchronize
        doSynchronization(sw);

        // check if single flow is deleted
        assertEquals(0, idAdded.size());
        assertEquals(1, idRemoved.size());
        assertTrue(idRemoved.contains((long) 1));
    }

    /**
     * Test that appropriate flows are added and other appropriate flows are deleted
     * in case flows in DB are overlapping flows in switch.
     */
    @Test
    public void testMixed() {
        // Create mock of flow table : flow 1,2,3
        IOFSwitch sw = createMockSwitch(new long[]{1, 2, 3});

        // Create mock of flow entries : flow 2,3,4,5
        initMockGraph(new long[]{2, 3, 4, 5});

        // synchronize
        doSynchronization(sw);

        // check if two flows {4,5} is installed and one flow {1} is deleted
        assertEquals(2, idAdded.size());
        assertTrue(idAdded.contains((long) 4));
        assertTrue(idAdded.contains((long) 5));
        assertEquals(1, idRemoved.size());
        assertTrue(idRemoved.contains((long) 1));
    }


    @Test
    public void testMassive() {
        // Create mock of flow table : flow 0-1999
        long[] swIdList = new long[2000];
        for (long i = 0; i < 2000; ++i) {
            swIdList[(int) i] = i;
        }
        IOFSwitch sw = createMockSwitch(swIdList);

        // Create mock of flow entries : flow 1500-3499
        long[] dbIdList = new long[2000];
        for (long i = 0; i < 2000; ++i) {
            dbIdList[(int) i] = 1500 + i;
        }
        initMockGraph(dbIdList);

        // synchronize
        doSynchronization(sw);

        // check if 1500 flows {2000-3499} is installed and 1500 flows {0,...,1499} is deleted
        assertEquals(1500, idAdded.size());
        for (long i = 2000; i < 3500; ++i) {
            assertTrue(idAdded.contains(i));
        }
        assertEquals(1500, idRemoved.size());
        for (long i = 0; i < 1500; ++i) {
            assertTrue(idRemoved.contains(i));
        }
    }

    /**
     * Create mock IOFSwitch with flow table which has arbitrary flows.
     *
     * @param cookieList List of FlowEntry IDs switch has.
     * @return Mock object.
     */
    private IOFSwitch createMockSwitch(long[] cookieList) {
        IOFSwitch sw = createMock(IOFSwitch.class);
        expect(sw.getId()).andReturn((long) 1).anyTimes();

        List<OFStatsReply> stats = new ArrayList<OFStatsReply>();
        for (long cookie : cookieList) {
            stats.add(createReply(cookie));
        }

        @SuppressWarnings("unchecked")
        Future<List<OFStatsReply>> future = createMock(Future.class);
        try {
            expect(future.get()).andReturn(stats).once();
        } catch (InterruptedException e1) {
            fail("Failed in Future#get()");
        } catch (ExecutionException e1) {
            fail("Failed in Future#get()");
        }
        replay(future);

        try {
            expect(sw.getStatistics(anyObject(OFFlowStatsRequest.class)))
                    .andReturn(future).once();
        } catch (IOException e) {
            fail("Failed in IOFSwitch#getStatistics()");
        }

        replay(sw);
        return sw;
    }

    /**
     * Create single OFFlowStatisticsReply object which is actually obtained from switch.
     *
     * @param cookie Cookie value, which indicates ID of FlowEntry installed to switch.
     * @return Created object.
     */
    private OFFlowStatsReply createReply(long cookie) {
        OFFlowStatsEntry entry = factory10.buildFlowStatsEntry()
                .setCookie(U64.of(cookie))
                .setPriority(1)
                .setMatch(factory10.buildMatch().build())
                .build();
        OFFlowStatsReply stat = factory10.buildFlowStatsReply()
                .setEntries(Collections.singletonList(entry)).build();

        return stat;
    }

    /**
     * Create mock FlowDatabaseOperation to mock DB.
     *
     * @param idList List of FlowEntry IDs stored in DB.
     */
    private void initMockGraph(long[] idList) {
        /*
         * TODO: The old FlowDatabaseOperation class is gone, so the method
         * below needs to be rewritten.
         */
        /*
        List<IFlowEntry> flowEntryList = new ArrayList<IFlowEntry>();

        for (long id : idList) {
            IFlowEntry entry = EasyMock.createMock(IFlowEntry.class);
            EasyMock.expect(entry.getFlowEntryId()).andReturn(String.valueOf(id)).anyTimes();
            EasyMock.replay(entry);
            flowEntryList.add(entry);
        }

        ISwitchObject swObj = EasyMock.createMock(ISwitchObject.class);
        EasyMock.expect(swObj.getFlowEntries()).andReturn(flowEntryList).once();
        EasyMock.replay(swObj);

        DBOperation mockOp = PowerMock.createMock(DBOperation.class);
        EasyMock.expect(mockOp.searchSwitch(EasyMock.anyObject(String.class))).andReturn(swObj).once();

        PowerMock.mockStatic(FlowDatabaseOperation.class);
        for (IFlowEntry entry : flowEntryList) {
            EasyMock.expect(FlowDatabaseOperation.extractFlowEntry(EasyMock.eq(entry)))
            .andAnswer(new IAnswer<FlowEntry>() {
                @Override
                public FlowEntry answer() throws Throwable {
                    IFlowEntry iflow = (IFlowEntry)EasyMock.getCurrentArguments()[0];
                    long flowEntryId = Long.valueOf(iflow.getFlowEntryId());

                    FlowEntry flow = EasyMock.createMock(FlowEntry.class);
                    EasyMock.expect(flow.flowEntryId()).andReturn(new FlowEntryId(flowEntryId)).anyTimes();
                    EasyMock.replay(flow);
                    return flow;
                }

            }).anyTimes();
            EasyMock.expect(mockOp.searchFlowEntry(EasyMock.eq(new FlowEntryId(entry.getFlowEntryId()))))
            .andReturn(entry);
        }
        PowerMock.replay(FlowDatabaseOperation.class);
        EasyMock.replay(mockOp);

        try {
            PowerMock.expectNew(DBOperation.class).andReturn(mockOp);
        } catch (Exception e) {
            fail("Failed to create DBOperation");
        }
        PowerMock.replay(DBOperation.class);
        */
    }

    /**
     * Instantiate FlowSynchronizer and sync flows.
     *
     * @param sw Target IOFSwitch object
     */
    private void doSynchronization(IOFSwitch sw) {
        sync = new FlowSynchronizer();
        sync.init(pusher);
        Future<SyncResult> future = sync.synchronize(sw);
        try {
            future.get();
        } catch (Exception e) {
            fail("Failed to Future#get()");
        }
    }
}
