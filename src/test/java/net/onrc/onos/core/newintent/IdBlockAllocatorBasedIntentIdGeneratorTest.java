package net.onrc.onos.core.newintent;

import net.onrc.onos.api.newintent.IntentId;
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
 * Suites of test of {@link IdBlockAllocatorBasedIntentIdGenerator}.
 */
public class IdBlockAllocatorBasedIntentIdGeneratorTest {
    private IdBlockAllocator allocator;
    private IdBlockAllocatorBasedIntentIdGenerator sut;

    @Before
    public void setUp() {
        allocator = createMock(IdBlockAllocator.class);

    }

    /**
     * Tests generated IntentId sequences using two {@link IdBlock blocks}.
     */
    @Test
    public void testIds() {
        expect(allocator.allocateUniqueIdBlock())
                .andReturn(new IdBlock(0, 3))
                .andReturn(new IdBlock(4, 3));

        replay(allocator);
        sut = new IdBlockAllocatorBasedIntentIdGenerator(allocator);

        assertThat(sut.getNewId(), is(new IntentId(0)));
        assertThat(sut.getNewId(), is(new IntentId(1)));
        assertThat(sut.getNewId(), is(new IntentId(2)));

        assertThat(sut.getNewId(), is(new IntentId(4)));
        assertThat(sut.getNewId(), is(new IntentId(5)));
        assertThat(sut.getNewId(), is(new IntentId(6)));
    }
}
