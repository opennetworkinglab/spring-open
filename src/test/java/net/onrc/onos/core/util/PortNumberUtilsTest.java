package net.onrc.onos.core.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.OFPort;

/**
 * Tests for {@link PortNumberUtils}.
 */
public class PortNumberUtilsTest {


    /**
     * Tests for {@link PortNumberUtils#of(OFVersion, int))}.
     */
    @Test
    public void testOfOFVersionInt() {

        assertEquals(PortNumber.uint32(0x1ffff),
                PortNumberUtils.openFlow(OFVersion.OF_13, 0x1FFFF));

        assertEquals(PortNumber.uint16((short) 0xabc),
                PortNumberUtils.openFlow(OFVersion.OF_10, 0xabc));

        try {
            PortNumberUtils.openFlow(OFVersion.OF_10, 0x1FFFF);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) { // CHECKSTYLE IGNORE THIS LINE
            // should throw
        }
    }

    /**
     * Tests for {@link PortNumberUtils#openFlow(OFPortDesc)}.
     */
    @Test
    public void testOfOFPortDesc() {
        final OFPortDesc desc10 = OFFactories.getFactory(OFVersion.OF_10)
                .buildPortDesc()
                .setPortNo(OFPort.of(123))
                .build();

        assertEquals(PortNumber.uint16((short) 123),
                PortNumberUtils.openFlow(desc10));

        final OFPortDesc desc13 = OFFactories.getFactory(OFVersion.OF_13)
                .buildPortDesc()
                .setPortNo(OFPort.of(0x1FFFF))
                .build();

        assertEquals(PortNumber.uint32(0x1FFFF),
                PortNumberUtils.openFlow(desc13));
    }

    /**
     * Tests for {@link PortNumberUtils#toOF10(int)}.
     */
    @Test
    public void testToOF10() {
        assertEquals((short) 0, PortNumberUtils.toOF10(0));
        assertEquals((short) 1, PortNumberUtils.toOF10(1));
        assertEquals((short) 0xFF00, PortNumberUtils.toOF10(0xFF00));
        for (int i = 0xFF00 + 1; i < 0xFFf8; ++i) {
            try {
                PortNumberUtils.toOF10(i);
                fail("Should have thrown IllegalArgumentException");
            } catch (IllegalArgumentException e) { // CHECKSTYLE IGNORE THIS LINE
                // should throw
            }
        }
        assertEquals((short) 0xFFf8, PortNumberUtils.toOF10(0xFFf8));
        assertEquals((short) 0xFFff, PortNumberUtils.toOF10(0xFFff));


        // OFPort#getPortNumber can return int value outside OF1.0
        // verifty that toOF10 converts them into valid OF1.0 range
        assertEquals((short) 0xFF00,
                PortNumberUtils.toOF10(OFPort.MAX.getPortNumber()));
        assertEquals((short) 0xFFf8,
                PortNumberUtils.toOF10(OFPort.IN_PORT.getPortNumber()));
        assertEquals((short) 0xFFf9,
                PortNumberUtils.toOF10(OFPort.TABLE.getPortNumber()));
        assertEquals((short) 0xFFfa,
                PortNumberUtils.toOF10(OFPort.NORMAL.getPortNumber()));
        assertEquals((short) 0xFFfb,
                PortNumberUtils.toOF10(OFPort.FLOOD.getPortNumber()));
        assertEquals((short) 0xFFfc,
                PortNumberUtils.toOF10(OFPort.ALL.getPortNumber()));
        assertEquals((short) 0xFFfd,
                PortNumberUtils.toOF10(OFPort.CONTROLLER.getPortNumber()));
        assertEquals((short) 0xFFfe,
                PortNumberUtils.toOF10(OFPort.LOCAL.getPortNumber()));
        assertEquals((short) 0xFFff, // OFPP_NONE
                PortNumberUtils.toOF10(OFPort.ANY.getPortNumber()));
    }
}
