/**
 *
 */
package net.onrc.onos.core.util;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * PortNumber test mainly focused on handling unsigned port no correctly.
 */
public class PortNumberTest {

    private static final PortNumber PORT_NONE = new PortNumber(0xffff);
    private static final PortNumber PORT_LOCAL = new PortNumber(0xfffe);

    /**
     * Test method for {@link PortNumber#uint16(short)}.
     */
    @Test
    public void testPortNumberUint16() {
        final PortNumber ref = new PortNumber(PORT_NONE);
        PortNumber p = PortNumber.uint16((short) 0xffff);

        assertEquals(ref, p);
        assertEquals(0xFFFFL, p.value());
    }

    /**
     * Test method for {@link PortNumber#uint32(int)}.
     */
    @Test
    public void testPortNumberUint32() {
        final PortNumber ref = new PortNumber("FFFFFFFF", 16);
        PortNumber p = PortNumber.uint32(0xFFFFFFFF);

        assertEquals(ref, p);
        assertEquals(0xFFFFFFFFL, p.value());
    }

    /**
     * Test method for {@link PortNumber#PortNumber(String)}.
     */
    @Test
    public void testPortNumberDecimalString() {
        final PortNumber ref = new PortNumber(PORT_NONE);
        PortNumber parsed = new PortNumber("65535");

        assertEquals(ref, parsed);
    }

    /**
     * Test method for {@link PortNumber#PortNumber(String, int)}.
     */
    @Test
    public void testPortNumberString() {
        final PortNumber ref = new PortNumber(PORT_NONE);
        PortNumber parsed = new PortNumber("ffff", 16);
        // Note: cannot parse "0xffff" now,
        //       which is the same behavior as Integer.parseInt.

        assertEquals(ref, parsed);
    }

    /**
     * Test method for {@link PortNumber#PortNumber(PortNumber)}.
     */
    @Test
    public void testPortNumberPortNumber() {
        PortNumber p = new PortNumber(PORT_LOCAL);
        PortNumber copy = new PortNumber(p);

        assertEquals(p, copy);
    }

    /**
     * Test method for {@link PortNumber#PortNumber(PortNumber.PortValues)}.
     */
    @Test
    public void testPortNumberPortValues() {
        PortNumber local = new PortNumber(PORT_LOCAL);
        assertEquals(0xfffeL, local.value());
        assertEquals((short) 0xfffe, local.shortValue());
    }

    /**
     * Test method for {@link PortNumber#shortToUnsignedLong(short)}.
     */
    @Test
    public void testLongValueShort() {
        assertEquals(0L, PortNumber.shortToUnsignedLong((short) 0));

        assertEquals(1L, PortNumber.shortToUnsignedLong((short) 1));

        // -1 as unsigned short
        assertEquals(0xffffL, PortNumber.shortToUnsignedLong((short) -1));
    }

    /**
     * Test method for {@link PortNumber#shortValue()}.
     */
    @Test
    public void testShortValue() {
        assertEquals(0L, new PortNumber((short) 0).shortValue());

        assertEquals(1L, new PortNumber((short) 1).shortValue());

        // user of #shortValue() needs to be careful
        // simply widening them will result in negative value
        assertEquals(-1L, new PortNumber(PORT_NONE).shortValue());

        // user of #shortValue() needs to be careful
        // should use PortNumber.shortToUnsignedLong or mask manually
        assertEquals(0xffffL, PortNumber.shortToUnsignedLong(new PortNumber(PORT_NONE).shortValue()));
        assertEquals(0xffffL, 0xffff & new PortNumber(PORT_NONE).shortValue());
}

    /**
     * Test method for {@link PortNumber#value()}.
     */
    @Test
    public void testLongValue() {
        assertEquals(0L, new PortNumber((short) 0).value());

        assertEquals(1L, new PortNumber((short) 1).value());

        assertEquals(0xffffL, new PortNumber(PORT_NONE).value());
    }

    /**
     * Test method for {@link PortNumber#toString()}.
     */
    @Test
    public void testToString() {
        assertEquals("0", new PortNumber((short) 0).toString());

        assertEquals("1", new PortNumber((short) 1).toString());

        // 0xffff in decimal
        assertEquals("65535", new PortNumber(PORT_NONE).toString());
    }

    /**
     * Test method for {@link PortNumber#equals(java.lang.Object)}.
     */
    @Test
    public void testEqualsObject() {
        // Some trivial
        assertTrue(new PortNumber(PORT_NONE).equals(new PortNumber((short) 0xffff)));
        assertFalse(new PortNumber((short) 0).equals(new PortNumber((short) 1)));

        // different type
        assertFalse(new PortNumber((short) 0).equals(Short.valueOf((short) 0)));

    }

}
