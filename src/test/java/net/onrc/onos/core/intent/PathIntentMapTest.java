package net.onrc.onos.core.intent;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import net.onrc.onos.core.intent.IntentOperation.Operator;
import net.onrc.onos.core.topology.Link;
import net.onrc.onos.core.topology.LinkEvent;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.LinkTuple;
import net.onrc.onos.core.util.PortNumber;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PathIntentMapTest {
    private static final PortNumber PORT_NUMBER_1 = new PortNumber((short) 1);
    private static final PortNumber PORT_NUMBER_3 = new PortNumber((short) 3);
    private static final Dpid DPID_1 = new Dpid(1L);
    private static final Dpid DPID_2 = new Dpid(2L);
    private static final Dpid DPID_3 = new Dpid(3L);
    private static final Dpid DPID_4 = new Dpid(4L);
    Link link12, link23, link24;
    Switch sw1, sw2, sw3, sw4;
    Port port11, port22, port21, port23, port31, port41;
    Path path1, path2;
    PathIntent intent1, intent2;

    @Before
    public void setUp() throws Exception {
        sw1 = createMock(Switch.class);
        sw2 = createMock(Switch.class);
        sw3 = createMock(Switch.class);
        sw4 = createMock(Switch.class);
        expect(sw1.getDpid()).andReturn(new Dpid(1L)).anyTimes();
        expect(sw2.getDpid()).andReturn(new Dpid(2L)).anyTimes();
        expect(sw3.getDpid()).andReturn(new Dpid(3L)).anyTimes();
        expect(sw4.getDpid()).andReturn(new Dpid(4L)).anyTimes();
        replay(sw1);
        replay(sw2);
        replay(sw3);
        replay(sw4);

        port11 = createMock(Port.class);
        port22 = createMock(Port.class);
        port21 = createMock(Port.class);
        port23 = createMock(Port.class);
        port31 = createMock(Port.class);
        port41 = createMock(Port.class);
        expect(port11.getNumber()).andReturn(new PortNumber((short) 1)).anyTimes();
        expect(port22.getNumber()).andReturn(new PortNumber((short) 2)).anyTimes();
        expect(port21.getNumber()).andReturn(new PortNumber((short) 1)).anyTimes();
        expect(port23.getNumber()).andReturn(new PortNumber((short) 3)).anyTimes();
        expect(port31.getNumber()).andReturn(new PortNumber((short) 1)).anyTimes();
        expect(port41.getNumber()).andReturn(new PortNumber((short) 1)).anyTimes();
        replay(port11);
        replay(port22);
        replay(port21);
        replay(port23);
        replay(port31);
        replay(port41);

        link12 = createMock(Link.class);
        link23 = createMock(Link.class);
        link24 = createMock(Link.class);
        expect(link12.getCapacity()).andReturn(1000.0).anyTimes();
        expect(link12.getSrcSwitch()).andReturn(sw1).anyTimes();
        expect(link12.getSrcPort()).andReturn(port11).anyTimes();
        expect(link12.getDstSwitch()).andReturn(sw2).anyTimes();
        expect(link12.getDstPort()).andReturn(port22).anyTimes();
        expect(link12.getLinkTuple()).andReturn(
                new LinkTuple(DPID_1, port11.getNumber(),
                              DPID_2, port22.getNumber())).anyTimes();

        expect(link23.getCapacity()).andReturn(1000.0).anyTimes();
        expect(link23.getSrcSwitch()).andReturn(sw2).anyTimes();
        expect(link23.getSrcPort()).andReturn(port21).anyTimes();
        expect(link23.getDstSwitch()).andReturn(sw3).anyTimes();
        expect(link23.getDstPort()).andReturn(port31).anyTimes();
        expect(link23.getLinkTuple()).andReturn(
                new LinkTuple(DPID_2, port21.getNumber(),
                              DPID_3, port31.getNumber())).anyTimes();

        expect(link24.getCapacity()).andReturn(1000.0).anyTimes();
        expect(link24.getSrcSwitch()).andReturn(sw2).anyTimes();
        expect(link24.getSrcPort()).andReturn(port23).anyTimes();
        expect(link24.getDstSwitch()).andReturn(sw4).anyTimes();
        expect(link24.getDstPort()).andReturn(port41).anyTimes();
        expect(link24.getLinkTuple()).andReturn(
                new LinkTuple(DPID_2, port23.getNumber(),
                              DPID_4, port41.getNumber())).anyTimes();

        replay(link12);
        replay(link23);
        replay(link24);

        path1 = new Path();
        path1.add(new LinkEvent(link12));
        path1.add(new LinkEvent(link23));

        path2 = new Path();
        path2.add(new LinkEvent(link12));
        path2.add(new LinkEvent(link24));

        intent1 = new PathIntent("1", path1, 400.0, new Intent("_1"));
        intent2 = new PathIntent("2", path2, 400.0, new Intent("_2"));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testCreate() {
        PathIntentMap intents = new PathIntentMap();
        assertEquals(0, intents.getAllIntents().size());
    }

    @Test
    public void testGetIntentsByDpid() {
        IntentOperationList operations = new IntentOperationList();
        operations.add(Operator.ADD, intent1);
        operations.add(Operator.ADD, intent2);
        assertEquals(2, operations.size());

        PathIntentMap intents = new PathIntentMap();
        intents.executeOperations(operations);
        assertEquals(2, intents.getAllIntents().size());

        Collection<PathIntent> pathIntents = intents.getIntentsByDpid(DPID_1);
        assertEquals(2, pathIntents.size());
        assertTrue(pathIntents.contains(intent1));
        assertTrue(pathIntents.contains(intent2));

        pathIntents = intents.getIntentsByDpid(DPID_2);
        assertEquals(2, pathIntents.size());
        assertTrue(pathIntents.contains(intent1));
        assertTrue(pathIntents.contains(intent2));

        pathIntents = intents.getIntentsByDpid(DPID_3);
        assertEquals(1, pathIntents.size());
        assertTrue(pathIntents.contains(intent1));

        pathIntents = intents.getIntentsByDpid(DPID_4);
        assertEquals(1, pathIntents.size());
        assertTrue(pathIntents.contains(intent2));
    }

    @Test
    public void testGetPathIntentsByPort() {
        IntentOperationList operations = new IntentOperationList();
        operations.add(Operator.ADD, intent1);
        operations.add(Operator.ADD, intent2);
        assertEquals(2, operations.size());

        PathIntentMap intents = new PathIntentMap();
        intents.executeOperations(operations);
        assertEquals(2, intents.getAllIntents().size());

        Collection<PathIntent> pathIntents = intents.getIntentsByPort(DPID_1, PORT_NUMBER_1);
        assertEquals(2, pathIntents.size());
        assertTrue(pathIntents.contains(intent1));
        assertTrue(pathIntents.contains(intent2));

        pathIntents = intents.getIntentsByPort(DPID_2, PORT_NUMBER_1);
        assertEquals(1, pathIntents.size());
        assertTrue(pathIntents.contains(intent1));

        pathIntents = intents.getIntentsByPort(DPID_2, PORT_NUMBER_3);
        assertEquals(1, pathIntents.size());
        assertTrue(pathIntents.contains(intent2));
    }

    @Test
    public void testGetPathIntentsByLink() {
        IntentOperationList operations = new IntentOperationList();
        operations.add(Operator.ADD, intent1);
        operations.add(Operator.ADD, intent2);
        assertEquals(2, operations.size());

        PathIntentMap intents = new PathIntentMap();
        intents.executeOperations(operations);
        assertEquals(2, intents.getAllIntents().size());

        Collection<PathIntent> pathIntents = intents.getIntentsByLink(new LinkEvent(link12));
        assertEquals(2, pathIntents.size());
        assertTrue(pathIntents.contains(intent1));
        assertTrue(pathIntents.contains(intent2));

        pathIntents = intents.getIntentsByLink(new LinkEvent(link23));
        assertEquals(1, pathIntents.size());
        assertTrue(pathIntents.contains(intent1));

        pathIntents = intents.getIntentsByLink(new LinkEvent(link24));
        assertEquals(1, pathIntents.size());
        assertTrue(pathIntents.contains(intent2));
    }

    @Test
    public void testGetAvailableBandwidth() {
        IntentOperationList operations = new IntentOperationList();
        operations.add(Operator.ADD, intent1);
        operations.add(Operator.ADD, intent2);
        assertEquals(2, operations.size());

        PathIntentMap intents = new PathIntentMap();
        intents.executeOperations(operations);
        assertEquals(2, intents.getAllIntents().size());

        assertEquals(200.0, intents.getAvailableBandwidth(link12), 0.0);
    }
}
