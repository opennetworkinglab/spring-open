package net.onrc.onos.api.flowmanager;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.api.flowmanager.FlowBatchOperation.Operator;
import net.onrc.onos.core.matchaction.MatchAction;
import net.onrc.onos.core.matchaction.MatchActionIdGeneratorWithIdBlockAllocator;
import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.matchaction.MatchActionOperations;
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

import com.google.common.collect.Sets;

/**
 * Unit tests for {@link SingleDstTreeFlow} class.
 */
public class SingleDstTreeFlowTest {
    private PacketMatch match;
    private Set<SwitchPort> ingressPorts;
    private Tree tree;
    private List<Action> egressActions;
    private IdBlockAllocator allocator;

    @Before
    public void setUp() throws Exception {
        allocator = createMock(IdBlockAllocator.class);
        expect(allocator.allocateUniqueIdBlock())
                .andReturn(new IdBlock(0, 100))
                .andReturn(new IdBlock(100, 100));
        replay(allocator);

        PacketMatchBuilder builder = new PacketMatchBuilder();
        builder.setDstMac(MACAddress.valueOf(54321));
        match = builder.build();

        ingressPorts = Sets.newHashSet(
                new SwitchPort(1L, (short) 101),
                new SwitchPort(1L, (short) 102),
                new SwitchPort(2L, (short) 103),
                new SwitchPort(3L, (short) 104),
                new SwitchPort(5L, (short) 105));

        tree = new Tree();
        tree.addLink(new FlowLink(
                new SwitchPort(1L, (short) 10),
                new SwitchPort(3L, (short) 12)));
        tree.addLink(new FlowLink(
                new SwitchPort(2L, (short) 11),
                new SwitchPort(3L, (short) 13)));
        tree.addLink(new FlowLink(
                new SwitchPort(3L, (short) 14),
                new SwitchPort(5L, (short) 15)));
        tree.addLink(new FlowLink(
                new SwitchPort(4L, (short) 16),
                new SwitchPort(5L, (short) 17)));

        egressActions = Arrays.asList(
                new ModifyDstMacAction(MACAddress.valueOf(12345)),
                new OutputAction(PortNumber.uint32(100)));
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Tests if constructor initialized fields properly.
     */
    @Test
    public void testConstructor() {
        SingleDstTreeFlow treeFlow = new SingleDstTreeFlow(
                new FlowId(1L), match, ingressPorts, tree, egressActions);

        assertNotNull(treeFlow);
        assertEquals(new FlowId(1L), treeFlow.getId());
        assertEquals(match, treeFlow.getMatch());
        assertEquals(ingressPorts, treeFlow.getIngressPorts());
        assertEquals(tree, treeFlow.getTree());
        assertEquals(egressActions, treeFlow.getEgressActions());
    }

    /**
     * Generates a list of {@link Action} contains {@link OutputAction} with
     * specified output port.
     *
     * @param outputPort the output port number
     * @return a list of {@link Action} contains one {@link OutputAction}
     */
    private List<Action> outputAction(int outputPort) {
        return Arrays.asList(
                (Action) new OutputAction(PortNumber.uint16((short) outputPort)));
    }

    /**
     * Tests if compile method with {@link Operator}.ADD generates two
     * {@link MatchActionOperations} objects and they have
     * {@link MatchActionOperationEntry} properly based on specified match,
     * ingress ports, tree, and egress actinos.
     */
    @Test
    public void testCompileWithAddOperation() {
        SingleDstTreeFlow treeFlow = new SingleDstTreeFlow(
                new FlowId(1L), match, ingressPorts, tree, egressActions);

        List<MatchActionOperations> maOpsList =
                treeFlow.compile(Operator.ADD,
                        new MatchActionIdGeneratorWithIdBlockAllocator(allocator),
                        new MatchActionOperationsIdGeneratorWithIdBlockAllocator(
                                allocator));

        assertEquals(2, maOpsList.size());

        MatchActionOperations firstOp = maOpsList.get(0);
        MatchActionOperations secondOp = maOpsList.get(1);

        assertEquals(4, firstOp.size());
        Map<SwitchPort, MatchAction> maMap1 = new HashMap<>();
        for (MatchActionOperationEntry entry : firstOp.getOperations()) {
            assertEquals(MatchActionOperations.Operator.ADD, entry.getOperator());
            MatchAction ma = entry.getTarget();
            maMap1.put(ma.getSwitchPort(), ma);
        }
        assertEquals(4, maMap1.size());

        assertEquals(5, secondOp.size());
        Map<SwitchPort, MatchAction> maMap2 = new HashMap<>();
        for (MatchActionOperationEntry entry : secondOp.getOperations()) {
            assertEquals(MatchActionOperations.Operator.ADD, entry.getOperator());
            MatchAction ma = entry.getTarget();
            maMap2.put(ma.getSwitchPort(), ma);
        }
        assertEquals(5, maMap2.size());

        assertEquals(Sets.newHashSet(
                new SwitchPort(3L, (short) 12),
                new SwitchPort(3L, (short) 13),
                new SwitchPort(5L, (short) 15),
                new SwitchPort(5L, (short) 17)
                ), maMap1.keySet());

        MatchAction ma11 = maMap1.get(new SwitchPort(3L, (short) 12));
        assertEquals(match, ma11.getMatch());
        assertEquals(outputAction(14), ma11.getActions());

        MatchAction ma12 = maMap1.get(new SwitchPort(3L, (short) 13));
        assertEquals(match, ma12.getMatch());
        assertEquals(outputAction(14), ma12.getActions());

        MatchAction ma13 = maMap1.get(new SwitchPort(5L, (short) 15));
        assertEquals(match, ma13.getMatch());
        assertEquals(egressActions, ma13.getActions());

        MatchAction ma14 = maMap1.get(new SwitchPort(5L, (short) 17));
        assertEquals(match, ma14.getMatch());
        assertEquals(egressActions, ma14.getActions());

        assertEquals(Sets.newHashSet(
                new SwitchPort(1L, (short) 101),
                new SwitchPort(1L, (short) 102),
                new SwitchPort(2L, (short) 103),
                new SwitchPort(3L, (short) 104),
                new SwitchPort(5L, (short) 105)
                ), maMap2.keySet());

        MatchAction ma21 = maMap2.get(new SwitchPort(1L, (short) 101));
        assertEquals(match, ma21.getMatch());
        assertEquals(outputAction(10), ma21.getActions());

        MatchAction ma22 = maMap2.get(new SwitchPort(1L, (short) 102));
        assertEquals(match, ma22.getMatch());
        assertEquals(outputAction(10), ma22.getActions());

        MatchAction ma23 = maMap2.get(new SwitchPort(2L, (short) 103));
        assertEquals(match, ma23.getMatch());
        assertEquals(outputAction(11), ma23.getActions());

        MatchAction ma24 = maMap2.get(new SwitchPort(3L, (short) 104));
        assertEquals(match, ma24.getMatch());
        assertEquals(outputAction(14), ma24.getActions());

        MatchAction ma25 = maMap2.get(new SwitchPort(5L, (short) 105));
        assertEquals(match, ma25.getMatch());
        assertEquals(egressActions, ma25.getActions());
    }

    /**
     * Tests if the object can be serialized and deserialized properly with
     * Kryo.
     */
    @Test
    public void testKryo() {
        SingleDstTreeFlow originalFlow = new SingleDstTreeFlow(
                new FlowId(1L), match, ingressPorts, tree, egressActions);

        assertNotNull(originalFlow);
        byte[] buf = KryoFactory.serialize(originalFlow);

        final SingleDstTreeFlow obtainedFlow = KryoFactory.deserialize(buf);

        assertEquals(new FlowId(1L), obtainedFlow.getId());
        assertEquals(match, obtainedFlow.getMatch());
        assertEquals(ingressPorts, obtainedFlow.getIngressPorts());
        assertEquals(tree, obtainedFlow.getTree());
        assertEquals(egressActions, obtainedFlow.getEgressActions());
    }
}
