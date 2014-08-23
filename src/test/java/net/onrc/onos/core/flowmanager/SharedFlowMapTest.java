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
import net.onrc.onos.api.flowmanager.Flow;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowLink;
import net.onrc.onos.api.flowmanager.FlowState;
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

/**
 * Unit test for {@link SharedFlowMap}.
 */
public class SharedFlowMapTest {
    private ISharedCollectionsService scs;
    private Path path;
    private PacketMatch match;
    private List<Action> actions;
    private Flow flow;

    @Before
    public void setUp() throws Exception {
        scs = new DummySharedCollectionsService();

        path = new Path();
        path.add(new FlowLink(
                new SwitchPort(1, (short) 10), new SwitchPort(2, (short) 11)));

        PacketMatchBuilder builder = new PacketMatchBuilder();
        builder.setDstMac(MACAddress.valueOf(54321));
        match = builder.build();

        actions = Arrays.asList(
                new ModifyDstMacAction(MACAddress.valueOf(12345)),
                new OutputAction(PortNumber.uint16((short) 101)));

        flow = new PacketPathFlow(new FlowId(1L), match, PortNumber.uint32(12345), path,
                actions, 0, 0);
    }

    /**
     * Tests if the constructor initializes its field correctly.
     */
    @Test
    public void testConstructor() {
        SharedFlowMap map = new SharedFlowMap(scs);
        Set<Flow> flows = map.getAll();
        assertNotNull(flows);
        assertTrue(flows.isEmpty());
    }

    /**
     * Tests the basic functionality of the flow map. This test puts, gets and
     * removes a flow and checks these operations are executed properly.
     */
    @Test
    public void testAddGetRemoveFlow() {
        SharedFlowMap map = new SharedFlowMap(scs);

        // Check if the stored flow can be retrieved its ID
        assertTrue(map.put(flow));
        final Flow obtainedFlow = map.get(flow.getId());
        assertNotNull(obtainedFlow);
        assertEquals(flow.getId(), obtainedFlow.getId());
        assertEquals(match, obtainedFlow.getMatch());

        // Check if it will not return flow with nonexistent ID
        FlowId nonexistentId = new FlowId(99999L);
        assertFalse(nonexistentId.equals(flow.getId()));
        assertNull(map.get(nonexistentId));

        // Check if it will remove the flow and it will not return the flow
        // after the removal
        final Flow removedFlow = map.remove(flow.getId());
        assertNotNull(removedFlow);
        assertEquals(flow.getId(), removedFlow.getId());
        final Flow nullFlow = map.get(flow.getId());
        assertNull(nullFlow);
    }

    /**
     * Tests the basic functionality of the flow state on the map. This test put
     * the flow and changes state of it.
     */
    @Test
    public void testStateChangeOfFlow() {
        SharedFlowMap map = new SharedFlowMap(scs);

        // Check if the state of nonexistent flow is not exist
        assertNull(map.getState(flow.getId()));

        // Check if it won't change the state of nonexistent flow
        assertFalse(map.setState(flow.getId(), FlowState.COMPILED, FlowState.SUBMITTED));
        assertNull(map.getState(flow.getId()));

        // Check if the initial state is SUBMITTED
        assertTrue(map.put(flow));
        assertEquals(FlowState.SUBMITTED, map.getState(flow.getId()));

        // Check if it won't change the state if the expected state was wrong
        assertFalse(map.setState(flow.getId(), FlowState.INSTALLED, FlowState.COMPILED));
        assertEquals(FlowState.SUBMITTED, map.getState(flow.getId()));

        // Check if it changes the state if the expected state was correct
        assertTrue(map.setState(flow.getId(), FlowState.COMPILED, FlowState.SUBMITTED));
        assertEquals(FlowState.COMPILED, map.getState(flow.getId()));

        // Check if it won't return the state if the flow was removed
        assertEquals(flow, map.remove(flow.getId()));
        assertNull(map.getState(flow.getId()));
    }
}
