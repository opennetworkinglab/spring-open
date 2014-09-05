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
 * Tests MatchActionOperationsIdGeneratorWithIdBlockAllocator.
 */
public class MatchActionOperationsIdGeneratorTest {

    private IdBlockAllocator allocator;

    /**
     * Creates a mocked IdBlockAllocator.
     */
    @Before
    public void setUp() {
        allocator = createMock(IdBlockAllocator.class);

    }

    /**
     * Tests generated MatchActionOperationsId sequences using two {@link net.onrc.onos.core.util.IdBlock blocks}.
     */
    @Test
    public void testIds() {
        expect(allocator.allocateUniqueIdBlock())
                .andReturn(new IdBlock(0, 3))
                .andReturn(new IdBlock(4, 3));

        replay(allocator);
        final MatchActionOperationsIdGeneratorWithIdBlockAllocator generator =
                new MatchActionOperationsIdGeneratorWithIdBlockAllocator(allocator);

        assertThat(generator.getNewId(), is(new MatchActionOperationsId(0L)));
        assertThat(generator.getNewId(), is(new MatchActionOperationsId(1L)));
        assertThat(generator.getNewId(), is(new MatchActionOperationsId(2L)));

        assertThat(generator.getNewId(), is(new MatchActionOperationsId(4L)));
        assertThat(generator.getNewId(), is(new MatchActionOperationsId(5L)));
        assertThat(generator.getNewId(), is(new MatchActionOperationsId(6L)));
    }
}

