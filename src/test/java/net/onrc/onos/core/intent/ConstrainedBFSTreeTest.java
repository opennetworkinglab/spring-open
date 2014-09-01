package net.onrc.onos.core.intent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import net.onrc.onos.core.intent.IntentOperation.Operator;
import net.onrc.onos.core.topology.LinkData;
import net.onrc.onos.core.topology.MockTopology;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for ConstrainedBFSTree class.
 */
public class ConstrainedBFSTreeTest {
    private static final Dpid DPID_1 = new Dpid(1L);
    private static final Dpid DPID_2 = new Dpid(2L);
    private static final Dpid DPID_3 = new Dpid(3L);
    private static final Dpid DPID_4 = new Dpid(4L);

    private static final PortNumber PORT_NUMBER_12 = PortNumber.uint16((short) 12);
    private static final PortNumber PORT_NUMBER_14 = PortNumber.uint16((short) 14);
    private static final PortNumber PORT_NUMBER_21 = PortNumber.uint16((short) 21);
    private static final PortNumber PORT_NUMBER_23 = PortNumber.uint16((short) 23);
    private static final PortNumber PORT_NUMBER_41 = PortNumber.uint16((short) 41);
    private static final PortNumber PORT_NUMBER_42 = PortNumber.uint16((short) 42);
    private static final PortNumber PORT_NUMBER_43 = PortNumber.uint16((short) 43);

    static final long LOCAL_PORT = 0xFFFEL;

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testCreate() {
        MockTopology topology = new MockTopology();
        topology.createSampleTopology1();
        ConstrainedBFSTree tree = new ConstrainedBFSTree(topology.getSwitch(DPID_1));
        assertNotNull(tree);
    }

    @Test
    public void testCreateConstrained() {
        MockTopology topology = new MockTopology();
        topology.createSampleTopology1();
        PathIntentMap intents = new PathIntentMap();
        ConstrainedBFSTree tree = new ConstrainedBFSTree(topology.getSwitch(DPID_1), intents, 1000.0);
        assertNotNull(tree);
    }

    @Test
    public void testGetPath() {
        MockTopology topology = new MockTopology();
        topology.createSampleTopology1();
        ConstrainedBFSTree tree = new ConstrainedBFSTree(topology.getSwitch(DPID_1));
        Path path11 = tree.getPath(topology.getSwitch(DPID_1));
        Path path12 = tree.getPath(topology.getSwitch(DPID_2));
        Path path13 = tree.getPath(topology.getSwitch(DPID_3));
        Path path14 = tree.getPath(topology.getSwitch(DPID_4));

        assertNotNull(path11);
        assertEquals(0, path11.size());

        assertNotNull(path12);
        assertEquals(1, path12.size());
        assertEquals(new LinkData(topology.getOutgoingLink(DPID_1, PORT_NUMBER_12)), path12.get(0));

        assertNotNull(path13);
        assertEquals(2, path13.size());
        if (path13.get(0).getDst().getDpid().value() == 2L) {
            assertEquals(new LinkData(topology.getOutgoingLink(DPID_1, PORT_NUMBER_12)), path13.get(0));
            assertEquals(new LinkData(topology.getOutgoingLink(DPID_2, PORT_NUMBER_23)), path13.get(1));
        } else {
            assertEquals(new LinkData(topology.getOutgoingLink(DPID_1, PORT_NUMBER_14)), path13.get(0));
            assertEquals(new LinkData(topology.getOutgoingLink(DPID_4, PORT_NUMBER_43)), path13.get(1));
        }

        assertNotNull(path14);
        assertEquals(1, path14.size());
        assertEquals(new LinkData(topology.getOutgoingLink(DPID_1, PORT_NUMBER_14)), path14.get(0));
    }

    @Test
    public void testGetPathNull() {
        MockTopology topology = new MockTopology();
        topology.createSampleTopology1();
        topology.removeLink(DPID_1, PORT_NUMBER_12, DPID_2, PORT_NUMBER_21);
        topology.removeLink(DPID_1, PORT_NUMBER_14, DPID_4, PORT_NUMBER_41);
        // now, there is no path from switch 1, but to switch1

        ConstrainedBFSTree tree1 = new ConstrainedBFSTree(topology.getSwitch(DPID_1));
        Path path12 = tree1.getPath(topology.getSwitch(DPID_2));
        Path path13 = tree1.getPath(topology.getSwitch(DPID_3));
        Path path14 = tree1.getPath(topology.getSwitch(DPID_4));

        ConstrainedBFSTree tree2 = new ConstrainedBFSTree(topology.getSwitch(DPID_2));
        Path path21 = tree2.getPath(topology.getSwitch(DPID_1));

        assertNull(path12);
        assertNull(path13);
        assertNull(path14);
        assertNotNull(path21);
        assertEquals(1, path21.size());
        assertEquals(new LinkData(topology.getOutgoingLink(DPID_2, PORT_NUMBER_21)), path21.get(0));
    }

    @Test
    public void testGetConstrainedPath() {
        MockTopology topology = new MockTopology();
        topology.createSampleTopology1();
        PathIntentMap intents = new PathIntentMap();
        IntentOperationList intentOps = new IntentOperationList();

        // create constrained shortest path intents that have the same source destination ports
        ConstrainedShortestPathIntent intent1 = new ConstrainedShortestPathIntent(
                "1", 1L, LOCAL_PORT, 0x111L, 2L, LOCAL_PORT, 0x222L, 600.0);
        ConstrainedShortestPathIntent intent2 = new ConstrainedShortestPathIntent(
                "2", 1L, LOCAL_PORT, 0x333L, 2L, LOCAL_PORT, 0x444L, 600.0);

        // calculate path of the intent1
        ConstrainedBFSTree tree = new ConstrainedBFSTree(topology.getSwitch(DPID_1), intents, 600.0);
        Path path1 = tree.getPath(topology.getSwitch(DPID_2));

        assertNotNull(path1);
        assertEquals(1, path1.size());
        assertEquals(new LinkData(topology.getOutgoingLink(DPID_1, PORT_NUMBER_12)), path1.get(0));

        PathIntent pathIntent1 = new PathIntent("pi1", path1, 600.0, intent1);
        intentOps.add(Operator.ADD, pathIntent1);
        intents.executeOperations(intentOps);

        // calculate path of the intent2
        tree = new ConstrainedBFSTree(topology.getSwitch(DPID_1), intents, 600.0);
        Path path2 = tree.getPath(topology.getSwitch(DPID_2));

        assertNotNull(path2);
        assertEquals(2, path2.size());
        assertEquals(new LinkData(topology.getOutgoingLink(DPID_1, PORT_NUMBER_14)), path2.get(0));
        assertEquals(new LinkData(topology.getOutgoingLink(DPID_4, PORT_NUMBER_42)), path2.get(1));

        PathIntent pathIntent2 = new PathIntent("pi2", path2, 600.0, intent2);
        intentOps.add(Operator.ADD, pathIntent2);
        intents.executeOperations(intentOps);

        // calculate path of the intent3
        tree = new ConstrainedBFSTree(topology.getSwitch(DPID_1), intents, 600.0);
        Path path3 = tree.getPath(topology.getSwitch(DPID_2));

        assertNull(path3);
    }
}
