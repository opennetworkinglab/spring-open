package net.onrc.onos.core.util;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Basic SwitchPort test.
 */
public class SwitchPortTest {

    private static final Dpid DPID = new Dpid(9);
    private static final PortNumber PORT_NUM = new PortNumber((short) 56);

    private static final SwitchPort SWP1 = new SwitchPort(DPID, PORT_NUM);
    private static final SwitchPort SWP2 = new SwitchPort(new Dpid(1),
                                                 new PortNumber((short) 65535));

    /**
     * Tests to confirm 2-arg constructor.
     */
    @Test
    public void testSwitchPortDpidPortNumber() {
        SwitchPort swp = new SwitchPort(DPID, PORT_NUM);

        assertEquals(DPID, swp.getDpid());
        assertEquals(PORT_NUM, swp.getPortNumber());
    }

    /**
     * Tests to confirm constructors input validation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSwitchPortDpidPortNumberFailOnNull1() {
        new SwitchPort(null, PORT_NUM);
    }

    /**
     * Tests to confirm constructors input validation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSwitchPortDpidPortNumberFailOnNull2() {
        new SwitchPort(DPID, null);
    }

    /**
     * Test to detect string representation change.
     */
    @Test
    public void testToString() {
        assertEquals("00:00:00:00:00:00:00:01/65535", SWP2.toString());
    }

    /**
     * Tests confirming equals returning true.
     */
    @Test
    public void testEqualsTrue() {
        SwitchPort swp = new SwitchPort(DPID, PORT_NUM);

        assertTrue(SWP1.equals(swp));
        assertTrue(swp.equals(SWP1));
        assertEquals(SWP1.hashCode(), swp.hashCode());

        assertTrue(swp.equals(swp));
    }

    /**
     * Tests confirming equals returning false.
     */
    @Test
    public void testEqualsFalse() {
        SwitchPort swp = new SwitchPort(DPID, PORT_NUM);

        assertFalse(SWP2.equals(swp));
        assertFalse(swp.equals(SWP2));
        assertNotEquals(SWP2.hashCode(), swp.hashCode());

        assertFalse(swp.equals(null));

        assertFalse(swp.equals(PORT_NUM));
    }

}
