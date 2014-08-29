package net.onrc.onos.api.flowmanager;

import static net.onrc.onos.core.util.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.junit.Assert.assertEquals;
import net.onrc.onos.core.util.TestUtils;

import org.junit.Before;
import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for {@link FlowBatchId} class.
 */
public class FlowBatchIdTest {
    private FlowBatchId flowBatchId1;
    private FlowBatchId flowBatchId2;
    private FlowBatchId flowBatchId3;
    private FlowBatchId flowBatchId4;
    private FlowBatchId flowBatchId5;

    @Before
    public void setUp() {
        flowBatchId1 = new FlowBatchId(0L);
        flowBatchId2 = new FlowBatchId(1L);
        flowBatchId3 = new FlowBatchId(2L);
        flowBatchId4 = new FlowBatchId(1L);
        flowBatchId5 = new FlowBatchId(0xABCDEFL);
    }

    /**
     * Tests {@link FlowBatchId#FlowBatchId(long)} constructor.
     */
    @Test
    public void testConstructor() {
        assertEquals(0xABCDEFL, TestUtils.getField(flowBatchId5, "id"));
    }

    /**
     * Tests the equality of {@link FlowBatchId} objects.
     */
    @Test
    public void testEqualsAndHashCode() {
        new EqualsTester()
                .addEqualityGroup(flowBatchId1)
                .addEqualityGroup(flowBatchId2, flowBatchId4)
                .addEqualityGroup(flowBatchId3)
                .addEqualityGroup(flowBatchId5)
                .testEquals();
    }

    /**
     * Tests {@link FlowBatchId#toString()} method.
     */
    @Test
    public void testToString() {
        assertEquals("0x0", flowBatchId1.toString());
        assertEquals("0xabcdef", flowBatchId5.toString());
    }

    /**
     * Tests if {@link FlowBatchId} is immutable.
     */
    @Test
    public void testImmutable() {
        assertThatClassIsImmutable(FlowBatchId.class);
    }
}
