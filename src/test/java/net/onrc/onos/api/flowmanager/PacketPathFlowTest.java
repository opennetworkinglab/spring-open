package net.onrc.onos.api.flowmanager;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.matchaction.MatchAction;
import net.onrc.onos.core.matchaction.MatchActionIdGeneratorWithIdBlockAllocator;
import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.matchaction.MatchActionOperations;
import net.onrc.onos.core.matchaction.MatchActionOperations.Operator;
import net.onrc.onos.core.matchaction.MatchActionOperationsIdGeneratorWithIdBlockAllocator;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.ModifyDstMacAction;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.util.IdBlock;
import net.onrc.onos.core.util.IdBlockAllocator;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;
import net.onrc.onos.core.util.serializers.KryoFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link PacketPathFlow} class.
 */
public class PacketPathFlowTest {
    private Path pathWith4Switches;
    private Path pathWith2Switches;
    private PacketMatch match;
    private List<Action> actions;
    private IdBlockAllocator allocator;

    @Before
    public void setUp() throws Exception {
        allocator = createMock(IdBlockAllocator.class);
        expect(allocator.allocateUniqueIdBlock())
                .andReturn(new IdBlock(0, 99))
                .andReturn(new IdBlock(100, 199));
        replay(allocator);

        pathWith4Switches = new Path();
        pathWith4Switches.add(new FlowLink(
                new SwitchPort(1, (short) 10), new SwitchPort(2, (short) 11)));
        pathWith4Switches.add(new FlowLink(
                new SwitchPort(2, (short) 12), new SwitchPort(3, (short) 13)));
        pathWith4Switches.add(new FlowLink(
                new SwitchPort(3, (short) 14), new SwitchPort(4, (short) 15)));

        pathWith2Switches = new Path();
        pathWith2Switches.add(new FlowLink(
                new SwitchPort(1, (short) 10), new SwitchPort(2, (short) 11)));

        PacketMatchBuilder builder = new PacketMatchBuilder();
        builder.setDstMac(MACAddress.valueOf(54321));
        match = builder.build();

        actions = Arrays.asList(
                new ModifyDstMacAction(MACAddress.valueOf(12345)),
                new OutputAction(PortNumber.uint32(101)));
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Checks the constructor initializes fields properly.
     */
    @Test
    public void testConstructor() {
        PacketPathFlow flow = new PacketPathFlow(
                new FlowId(1L), match, PortNumber.uint32(100),
                pathWith4Switches, actions, 0, 0);

        assertNotNull(flow);
        assertEquals(new FlowId(1L), flow.getId());
        assertEquals(match, flow.getMatch());
        assertEquals(PortNumber.uint32(100), flow.getIngressPortNumber());
        assertEquals(pathWith4Switches, flow.getPath());
        assertEquals(actions, flow.getEgressActions());
        assertEquals(0, flow.getHardTimeout());
        assertEquals(0, flow.getIdleTimeout());
    }

    /**
     * Checks the compile method with add-operation. This test creates a flow
     * object using the path with 2 switches, then checks if the compiled the
     * list of MatchActionOperations objects are generated properly.
     */
    @Test
    public void testCompileWithAddOperationFor2Switches() {
        PacketPathFlow flow = new PacketPathFlow(
                new FlowId(1L), match, PortNumber.uint32(100),
                pathWith2Switches, actions, 0, 0);

        List<MatchActionOperations> maOpsList =
                flow.compile(FlowBatchOperation.Operator.ADD,
                        new MatchActionIdGeneratorWithIdBlockAllocator(allocator),
                        new MatchActionOperationsIdGeneratorWithIdBlockAllocator(
                                allocator)
                        );

        assertEquals(2, maOpsList.size());

        MatchActionOperations firstOp = maOpsList.get(0);
        assertEquals(1, firstOp.size());
        assertEquals(1, firstOp.getOperations().size());

        MatchActionOperations secondOp = maOpsList.get(1);
        assertEquals(1, secondOp.size());
        assertEquals(1, secondOp.getOperations().size());

        MatchActionOperationEntry entry1 = secondOp.getOperations().get(0);
        MatchActionOperationEntry entry2 = firstOp.getOperations().get(0);

        assertEquals(Operator.ADD, entry1.getOperator());
        assertEquals(Operator.ADD, entry2.getOperator());

        MatchAction ma1 = entry1.getTarget();
        MatchAction ma2 = entry2.getTarget();

        assertNotNull(ma1);
        assertNotNull(ma2);

        assertEquals(new SwitchPort(1, (short) 100), ma1.getSwitchPort());
        assertEquals(new SwitchPort(2, (short) 11), ma2.getSwitchPort());

        assertEquals(match, ma1.getMatch());
        assertEquals(match, ma2.getMatch());

        assertNotNull(ma1.getActions());
        assertNotNull(ma2.getActions());

        assertEquals(1, ma1.getActions().size());
        assertEquals(2, ma2.getActions().size());

        assertEquals(new OutputAction(PortNumber.uint32(10)),
                ma1.getActions().get(0));
        assertEquals(new ModifyDstMacAction(MACAddress.valueOf(12345)),
                ma2.getActions().get(0));
        assertEquals(new OutputAction(PortNumber.uint32(101)),
                ma2.getActions().get(1));
    }

    /**
     * Checks the compile method with add-operation. This test creates a flow
     * object using the path with 4 switches, then checks if the compiled the
     * list of MatchActionOperations objects are generated properly.
     */
    @Test
    public void testCompileWithAddOperationFor4Switches() {
        PacketPathFlow flow = new PacketPathFlow(
                new FlowId(1L), match, PortNumber.uint32(100),
                pathWith4Switches, actions, 0, 0);

        List<MatchActionOperations> maOpsList =
                flow.compile(FlowBatchOperation.Operator.ADD,
                        new MatchActionIdGeneratorWithIdBlockAllocator(allocator),
                        new MatchActionOperationsIdGeneratorWithIdBlockAllocator(
                                allocator)
                        );

        assertEquals(2, maOpsList.size());

        MatchActionOperations firstOp = maOpsList.get(0);
        assertEquals(3, firstOp.size());
        assertEquals(3, firstOp.getOperations().size());

        MatchActionOperations secondOp = maOpsList.get(1);
        assertEquals(1, secondOp.size());
        assertEquals(1, secondOp.getOperations().size());

        MatchActionOperationEntry entry1 = secondOp.getOperations().get(0);
        MatchActionOperationEntry entry2 = firstOp.getOperations().get(0);
        MatchActionOperationEntry entry3 = firstOp.getOperations().get(1);
        MatchActionOperationEntry entry4 = firstOp.getOperations().get(2);

        assertEquals(Operator.ADD, entry1.getOperator());
        assertEquals(Operator.ADD, entry2.getOperator());
        assertEquals(Operator.ADD, entry3.getOperator());
        assertEquals(Operator.ADD, entry4.getOperator());

        MatchAction ma1 = entry1.getTarget();
        MatchAction ma2 = entry2.getTarget();
        MatchAction ma3 = entry3.getTarget();
        MatchAction ma4 = entry4.getTarget();

        assertNotNull(ma1);
        assertNotNull(ma2);
        assertNotNull(ma3);
        assertNotNull(ma4);

        assertEquals(new SwitchPort(1, (short) 100), ma1.getSwitchPort());
        assertEquals(new SwitchPort(2, (short) 11), ma2.getSwitchPort());
        assertEquals(new SwitchPort(3, (short) 13), ma3.getSwitchPort());
        assertEquals(new SwitchPort(4, (short) 15), ma4.getSwitchPort());

        assertEquals(match, ma1.getMatch());
        assertEquals(match, ma2.getMatch());
        assertEquals(match, ma3.getMatch());
        assertEquals(match, ma4.getMatch());

        assertNotNull(ma1.getActions());
        assertNotNull(ma2.getActions());
        assertNotNull(ma3.getActions());
        assertNotNull(ma4.getActions());

        assertEquals(1, ma1.getActions().size());
        assertEquals(1, ma2.getActions().size());
        assertEquals(1, ma3.getActions().size());
        assertEquals(2, ma4.getActions().size());

        assertEquals(new OutputAction(PortNumber.uint32(10)),
                ma1.getActions().get(0));
        assertEquals(new OutputAction(PortNumber.uint32(12)),
                ma2.getActions().get(0));
        assertEquals(new OutputAction(PortNumber.uint32(14)),
                ma3.getActions().get(0));
        assertEquals(new ModifyDstMacAction(MACAddress.valueOf(12345)),
                ma4.getActions().get(0));
        assertEquals(new OutputAction(PortNumber.uint32(101)),
                ma4.getActions().get(1));
    }

    /**
     * Tests if the object can be serialized and deserialized properly with
     * Kryo.
     */
    @Test
    public void testKryo() {
        final FlowId id = new FlowId(1);
        final PortNumber ingressPort = PortNumber.uint32(12345);

        final PacketPathFlow originalFlow =
                new PacketPathFlow(id, match, ingressPort, pathWith4Switches,
                        actions, 1000, 100);

        assertNotNull(originalFlow);
        byte[] buf = KryoFactory.serialize(originalFlow);

        final PacketPathFlow obtainedFlow = KryoFactory.deserialize(buf);

        assertEquals(id, obtainedFlow.getId());
        assertEquals(match, obtainedFlow.getMatch());
        assertEquals(ingressPort, obtainedFlow.getIngressPortNumber());
        assertEquals(pathWith4Switches, obtainedFlow.getPath());
        assertEquals(actions, obtainedFlow.getEgressActions());
        assertEquals(1000, obtainedFlow.getHardTimeout());
        assertEquals(100, obtainedFlow.getIdleTimeout());
    }
}
