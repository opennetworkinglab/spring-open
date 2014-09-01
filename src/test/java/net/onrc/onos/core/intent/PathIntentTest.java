package net.onrc.onos.core.intent;

import static org.junit.Assert.assertEquals;
import net.onrc.onos.core.topology.LinkData;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;
import net.onrc.onos.core.util.serializers.KryoFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Unit tests for PathIntent.
 */
public class PathIntentTest {

    private static final Dpid DPID_1 = new Dpid(1L);
    private static final Dpid DPID_2 = new Dpid(2L);
    private static final Dpid DPID_3 = new Dpid(3L);
    private static final Dpid DPID_4 = new Dpid(4L);

    private static final PortNumber PORT_NUMBER_1 = PortNumber.uint16((short) 1);
    private static final PortNumber PORT_NUMBER_2 = PortNumber.uint16((short) 2);

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testCreateFirstId() {
        String id = PathIntent.createFirstId("100");
        assertEquals("100___0", id);
    }

    @Test
    public void testCreateNextId() {
        String id = PathIntent.createNextId("100___999");
        assertEquals("100___1000", id);
    }

    @Test
    public void test() {
        KryoFactory factory = new KryoFactory();
        Kryo kryo = factory.newKryo();
        Output output = new Output(1024);

        ConstrainedShortestPathIntent cspIntent1 =
                new ConstrainedShortestPathIntent("1", 2L, 3L, 4L, 5L, 6L, 7L, 1000.0);

        Path path = new Path();
        path.add(new LinkData(new SwitchPort(1L, 1L), new SwitchPort(2L, 2L)));
        path.add(new LinkData(new SwitchPort(2L, 1L), new SwitchPort(3L, 2L)));
        path.add(new LinkData(new SwitchPort(3L, 1L), new SwitchPort(4L, 2L)));

        PathIntent pathIntent1 = new PathIntent("11", path, 123.45, cspIntent1);

        kryo.writeObject(output, pathIntent1);
        output.close();

        Input input = new Input(output.toBytes());

        // create pathIntent from bytes

        PathIntent pathIntent2 =
                kryo.readObject(input, PathIntent.class);
        input.close();

        // check

        assertEquals("11", pathIntent2.getId());
        Path path2 = pathIntent2.getPath();

        assertEquals(DPID_1, path2.get(0).getSrc().getDpid());
        assertEquals(PORT_NUMBER_1, path2.get(0).getSrc().getPortNumber());
        assertEquals(DPID_2, path2.get(0).getDst().getDpid());
        assertEquals(PORT_NUMBER_2, path2.get(0).getDst().getPortNumber());

        assertEquals(DPID_2, path2.get(1).getSrc().getDpid());
        assertEquals(PORT_NUMBER_1, path2.get(1).getSrc().getPortNumber());
        assertEquals(DPID_3, path2.get(1).getDst().getDpid());
        assertEquals(PORT_NUMBER_2, path2.get(1).getDst().getPortNumber());

        assertEquals(DPID_3, path2.get(2).getSrc().getDpid());
        assertEquals(PORT_NUMBER_1, path2.get(2).getSrc().getPortNumber());
        assertEquals(DPID_4, path2.get(2).getDst().getDpid());
        assertEquals(PORT_NUMBER_2, path2.get(2).getDst().getPortNumber());

        assertEquals(123.45, pathIntent2.getBandwidth(), 0.0);

        ConstrainedShortestPathIntent cspIntent2 =
                (ConstrainedShortestPathIntent) pathIntent2.getParentIntent();

        assertEquals("1", cspIntent2.getId());
        assertEquals(2L, cspIntent2.getSrcSwitchDpid());
        assertEquals(3L, cspIntent2.getSrcPortNumber());
        assertEquals(4L, cspIntent2.getSrcMac());
        assertEquals(5L, cspIntent2.getDstSwitchDpid());
        assertEquals(6L, cspIntent2.getDstPortNumber());
        assertEquals(7L, cspIntent2.getDstMac());
        assertEquals(1000.0, cspIntent2.getBandwidth(), 0.0);
    }
}
