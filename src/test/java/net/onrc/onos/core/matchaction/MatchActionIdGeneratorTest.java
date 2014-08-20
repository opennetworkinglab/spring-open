package net.onrc.onos.core.matchaction;

import net.onrc.onos.core.util.IdBlock;
import net.onrc.onos.core.util.IdBlockAllocator;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests MatchActionIdGeneratorWithIdBlockAllocator.
 */
public class MatchActionIdGeneratorTest {
    private IdBlockAllocator allocator;

    @Before
    public void setUp() {
        allocator = createMock(IdBlockAllocator.class);

    }

    /**
     * Tests generated MatchActionId sequences using two {@link net.onrc.onos.core.util.IdBlock blocks}.
     */
    @Test
    public void testIds() {
        expect(allocator.allocateUniqueIdBlock())
                .andReturn(new IdBlock(0, 3))
                .andReturn(new IdBlock(4, 3));

        replay(allocator);
        final MatchActionIdGeneratorWithIdBlockAllocator matchActionIdGenerator =
            new MatchActionIdGeneratorWithIdBlockAllocator(allocator);

        assertThat(matchActionIdGenerator.getNewId(), is(new MatchActionId(0L)));
        assertThat(matchActionIdGenerator.getNewId(), is(new MatchActionId(1L)));
        assertThat(matchActionIdGenerator.getNewId(), is(new MatchActionId(2L)));

        assertThat(matchActionIdGenerator.getNewId(), is(new MatchActionId(4L)));
        assertThat(matchActionIdGenerator.getNewId(), is(new MatchActionId(5L)));
        assertThat(matchActionIdGenerator.getNewId(), is(new MatchActionId(6L)));
    }
}
