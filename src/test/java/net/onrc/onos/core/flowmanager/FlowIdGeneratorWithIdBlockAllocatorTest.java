package net.onrc.onos.core.flowmanager;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.core.util.IdBlock;
import net.onrc.onos.core.util.IdBlockAllocator;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link FlowIdGeneratorWithIdBlockAllocator} class.
 */
public class FlowIdGeneratorWithIdBlockAllocatorTest {
    private IdBlockAllocator allocator;
    private FlowIdGeneratorWithIdBlockAllocator flowIdGenerator;

    @Before
    public void setUp() {
        allocator = createMock(IdBlockAllocator.class);

    }

    /**
     * Tests generated FlowId sequences using two {@link IdBlock blocks}.
     */
    @Test
    public void testIds() {
        expect(allocator.allocateUniqueIdBlock())
                .andReturn(new IdBlock(0, 3))
                .andReturn(new IdBlock(4, 3));

        replay(allocator);
        flowIdGenerator = new FlowIdGeneratorWithIdBlockAllocator(allocator);

        assertThat(flowIdGenerator.getNewId(), is(new FlowId(0L)));
        assertThat(flowIdGenerator.getNewId(), is(new FlowId(1L)));
        assertThat(flowIdGenerator.getNewId(), is(new FlowId(2L)));

        assertThat(flowIdGenerator.getNewId(), is(new FlowId(4L)));
        assertThat(flowIdGenerator.getNewId(), is(new FlowId(5L)));
        assertThat(flowIdGenerator.getNewId(), is(new FlowId(6L)));
    }
}
