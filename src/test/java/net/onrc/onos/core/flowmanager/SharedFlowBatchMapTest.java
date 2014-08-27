package net.onrc.onos.core.flowmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.api.flowmanager.FlowBatchId;
import net.onrc.onos.api.flowmanager.FlowBatchOperation;
import net.onrc.onos.api.flowmanager.FlowBatchState;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowLink;
import net.onrc.onos.api.flowmanager.PacketPathFlow;
import net.onrc.onos.api.flowmanager.Path;
import net.onrc.onos.core.datagrid.ISharedCollectionsService;
import net.onrc.onos.core.datastore.hazelcast.DummySharedCollectionsService;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.ModifyDstMacAction;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.junit.Before;
import org.junit.Test;

public class SharedFlowBatchMapTest {
    private ISharedCollectionsService scs;
    private FlowBatchOperation flowOp;

    @Before
    public void setUp() throws Exception {
        scs = new DummySharedCollectionsService();

        Path path = new Path();
        path.add(new FlowLink(
                new SwitchPort(1, (short) 10), new SwitchPort(2, (short) 11)));

        PacketMatchBuilder builder = new PacketMatchBuilder();
        builder.setDstMac(MACAddress.valueOf(54321));
        PacketMatch match1 = builder.build();

        builder = new PacketMatchBuilder();
        builder.setDstMac(MACAddress.valueOf(12345));
        PacketMatch match2 = builder.build();

        List<Action> actions1 = Arrays.asList(
                new ModifyDstMacAction(MACAddress.valueOf(12345)),
                new OutputAction(PortNumber.uint16((short) 101)));

        List<Action> actions2 = Arrays.asList(
                new ModifyDstMacAction(MACAddress.valueOf(54321)),
                new OutputAction(PortNumber.uint16((short) 101)));

        PacketPathFlow flow1 = new PacketPathFlow(new FlowId(1L), match1,
                PortNumber.uint32(12345), path, actions1, 0, 0);

        PacketPathFlow flow2 = new PacketPathFlow(new FlowId(2L), match2,
                PortNumber.uint32(54321), path, actions2, 0, 0);

        flowOp = new FlowBatchOperation();
        flowOp.addAddFlowOperation(flow1);
        flowOp.addAddFlowOperation(flow2);
    }

    /**
     * Tests if the constructor initializes its field correctly.
     */
    @Test
    public void testConstructor() {
        SharedFlowBatchMap map = new SharedFlowBatchMap(scs);
        Set<FlowBatchOperation> flowOps = map.getAll();
        assertNotNull(flowOps);
        assertTrue(flowOps.isEmpty());
    }

    /**
     * Tests the basic functionality of the flow batch map. This test puts gets
     * and removes a batch operation and checks these operations are executed
     * properly.
     */
    @Test
    public void testAddGetRemoveFlowOp() {
        SharedFlowBatchMap map = new SharedFlowBatchMap(scs);
        final FlowBatchId id = new FlowBatchId(100L);

        // Check if the stored flow batch operation can be retrieved its ID
        assertTrue(map.put(id, flowOp));
        final FlowBatchOperation obtainedFlowOp = map.get(id);
        assertNotNull(obtainedFlowOp);
        assertEquals(2, obtainedFlowOp.size());

        // Check if it will not return flow with nonexistent ID
        FlowBatchId nonexistentId = new FlowBatchId(99999L);
        assertFalse(nonexistentId.equals(id));
        assertNull(map.get(nonexistentId));

        // Check if it will remove the operation and it will not return the
        // operation after the removal
        final FlowBatchOperation removedFlowOp = map.remove(id);
        assertNotNull(removedFlowOp);
        assertEquals(2, removedFlowOp.size());
        final FlowBatchOperation nullFlowOp = map.get(id);
        assertNull(nullFlowOp);
    }

    @Test
    public void testChangeStateOfFlowOp() {
        SharedFlowBatchMap map = new SharedFlowBatchMap(scs);

        final FlowBatchId id = new FlowBatchId(100L);

        // Check if the state of nonexistent flow is not exist
        assertNull(map.getState(id));

        // Check if it won't change the state of nonexistent flow
        assertFalse(map.setState(id, FlowBatchState.EXECUTING, FlowBatchState.SUBMITTED));
        assertNull(map.getState(id));

        // Check if the initial state is SUBMITTED
        assertTrue(map.put(id, flowOp));
        assertEquals(FlowBatchState.SUBMITTED, map.getState(id));

        // Check if it won't change the state if the expected state was wrong
        assertFalse(map.setState(id, FlowBatchState.COMPLETED, FlowBatchState.EXECUTING));
        assertEquals(FlowBatchState.SUBMITTED, map.getState(id));

        // Check if it changes the state if the expected state was correct
        assertTrue(map.setState(id, FlowBatchState.EXECUTING, FlowBatchState.SUBMITTED));
        assertEquals(FlowBatchState.EXECUTING, map.getState(id));

        // Check if it won't return the state if the flow was removed
        assertEquals(flowOp, map.remove(id));
        assertNull(map.getState(id));
    }
}
