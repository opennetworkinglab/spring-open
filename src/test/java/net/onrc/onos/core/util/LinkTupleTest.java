package net.onrc.onos.core.util;

import static net.onrc.onos.core.util.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Basic LinkTupleTest.
 */
public class LinkTupleTest {

    private static final Dpid SRC_DPID = new Dpid(9);
    private static final PortNumber SRC_PORT_NUM = new PortNumber((short) 56);
    private static final Dpid DST_DPID = new Dpid(12);
    private static final PortNumber DST_PORT_NUM = new PortNumber((short) 81);

    private static final SwitchPort SRC = new SwitchPort(SRC_DPID, SRC_PORT_NUM);
    private static final SwitchPort DST = new SwitchPort(DST_DPID, DST_PORT_NUM);


    private static final LinkTuple L1 = new LinkTuple(SRC, DST);
    private static final LinkTuple L2 = new LinkTuple(
            new SwitchPort(new Dpid(1), new PortNumber((short) 65535)),
            new SwitchPort(new Dpid(2), new PortNumber((short) 65534)));

    /**
     * Test to confirm class definition is immutable.
     */
    @Test
    public void testImmutable() {
        assertThatClassIsImmutable(LinkTuple.class);
    }

    /**
     * Tests to confirm 2-arg constructor.
     */
    @Test
    public void testLinkTupleSwitchPortSwitchPort() {
        LinkTuple link = new LinkTuple(SRC, DST);
        assertEquals(SRC, link.getSrc());
        assertEquals(SRC_DPID, link.getSrc().getDpid());
        assertEquals(SRC_PORT_NUM, link.getSrc().getPortNumber());
        assertEquals(DST, link.getDst());
        assertEquals(DST_DPID, link.getDst().getDpid());
        assertEquals(DST_PORT_NUM, link.getDst().getPortNumber());
    }

    /**
     * Tests to confirm constructors input validation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testLinkTupleSwitchPortSwitchPortFailsOnNull1() {
        new LinkTuple(null, DST);
    }

    /**
     * Tests to confirm constructors input validation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testLinkTupleSwitchPortSwitchPortFailsOnNull2() {
        new LinkTuple(SRC, null);
    }

    /**
     * Tests to confirm 4-arg constructor.
     */
    @Test
    public void testLinkTupleDpidPortNumberDpidPortNumber() {
        LinkTuple link = new LinkTuple(SRC_DPID, SRC_PORT_NUM, DST_DPID, DST_PORT_NUM);
        assertEquals(SRC, link.getSrc());
        assertEquals(SRC_DPID, link.getSrc().getDpid());
        assertEquals(SRC_PORT_NUM, link.getSrc().getPortNumber());
        assertEquals(DST, link.getDst());
        assertEquals(DST_DPID, link.getDst().getDpid());
        assertEquals(DST_PORT_NUM, link.getDst().getPortNumber());
    }

    /**
     * Tests to confirm constructors input validation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testLinkTupleDpidPortNumberDpidPortNumberFailOnNull1() {
        new LinkTuple(null, SRC_PORT_NUM, DST_DPID, DST_PORT_NUM);
    }

    /**
     * Tests to confirm constructors input validation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testLinkTupleDpidPortNumberDpidPortNumberFailOnNull2() {
        new LinkTuple(SRC_DPID, null, DST_DPID, DST_PORT_NUM);
    }

    /**
     * Tests to confirm constructors input validation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testLinkTupleDpidPortNumberDpidPortNumberFailOnNull3() {
        new LinkTuple(SRC_DPID, SRC_PORT_NUM, null, DST_PORT_NUM);
    }

    /**
     * Tests to confirm constructors input validation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testLinkTupleDpidPortNumberDpidPortNumberFailOnNull4() {
        new LinkTuple(SRC_DPID, SRC_PORT_NUM, DST_DPID, null);
    }

    /**
     * Tests confirming equals returning true.
     */
    @Test
    public void testEqualsTrue() {
        LinkTuple link = new LinkTuple(SRC, DST);

        assertTrue(L1.equals(link));
        assertTrue(link.equals(L1));
        assertEquals(L1.hashCode(), link.hashCode());

        assertTrue(link.equals(link));
    }

    /**
     * Tests confirming equals returning false.
     */
    @Test
    public void testEqualsFalse() {
        LinkTuple link = new LinkTuple(SRC, DST);

        assertFalse(L2.equals(link));
        assertFalse(link.equals(L2));
        assertNotEquals(L2.hashCode(), link.hashCode());

        assertFalse(link.equals(null));

        assertFalse(link.equals(DST));
    }

    /**
     * Test to detect string representation change.
     */
    @Test
    public void testToString() {
        // FIXME when we start handling unsigned integer properly
        assertEquals("(00:00:00:00:00:00:00:01/-1=>00:00:00:00:00:00:00:02/-2)",
                     L2.toString());
    }

}
