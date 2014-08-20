package net.onrc.onos.api.flowmanager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FlowIdTest {

    /**
     * Tests FlowId's equals method.
     */
    @Test
    public void testEquals() {
        FlowId flow1 = new FlowId(0L);
        FlowId flow2 = new FlowId(1L);
        FlowId flow3 = new FlowId(2L);
        FlowId flow4 = new FlowId(1L);

        assertTrue(flow1.equals(flow1));
        assertTrue(flow2.equals(flow2));
        assertTrue(flow3.equals(flow3));
        assertTrue(flow4.equals(flow4));

        assertFalse(flow1.equals(flow2));
        assertFalse(flow1.equals(flow3));
        assertFalse(flow1.equals(flow4));
        assertFalse(flow2.equals(flow1));
        assertFalse(flow2.equals(flow3));
        assertFalse(flow3.equals(flow1));
        assertFalse(flow3.equals(flow2));
        assertFalse(flow3.equals(flow4));
        assertFalse(flow4.equals(flow1));
        assertFalse(flow4.equals(flow3));

        assertTrue(flow2.equals(flow4));
        assertTrue(flow4.equals(flow2));
    }

}
