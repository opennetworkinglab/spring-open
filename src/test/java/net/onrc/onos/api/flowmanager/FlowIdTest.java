package net.onrc.onos.api.flowmanager;

import static net.onrc.onos.core.util.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.junit.Assert.assertEquals;
import net.onrc.onos.core.util.TestUtils;

import org.junit.Before;
import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for {@link FlowId} class.
 */
public class FlowIdTest {
    private FlowId flowId1;
    private FlowId flowId2;
    private FlowId flowId3;
    private FlowId flowId4;
    private FlowId flowId5;

    @Before
    public void setUp() {
        flowId1 = new FlowId(0L);
        flowId2 = new FlowId(1L);
        flowId3 = new FlowId(2L);
        flowId4 = new FlowId(1L);
        flowId5 = new FlowId(0xABCDEFL);
    }

    /**
     * Tests {@link FlowId#FlowId(long)} constructor.
     */
    @Test
    public void testConstructor() {
        assertEquals(0xABCDEFL, TestUtils.getField(flowId5, "id"));
    }

    /**
     * Tests the equality of {@link FlowId} objects.
     */
    @Test
    public void testEqualsAndHashCode() {
        new EqualsTester()
                .addEqualityGroup(flowId1)
                .addEqualityGroup(flowId2, flowId4)
                .addEqualityGroup(flowId3)
                .addEqualityGroup(flowId5)
                .testEquals();
    }

    /**
     * Tests {@link FlowId#toString()} method.
     */
    @Test
    public void testToString() {
        assertEquals("0x0", flowId1.toString());
        assertEquals("0xabcdef", flowId5.toString());
    }

    /**
     * Tests if {@link FlowId} is immutable.
     */
    @Test
    public void testImmutable() {
        assertThatClassIsImmutable(FlowId.class);
    }
}
