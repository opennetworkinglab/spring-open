package net.onrc.onos.core.util;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * Suites of test of {@link IdBlock}.
 */
public class IdBlockTest {

    private final IdBlock sut = new IdBlock(0, 3);

    /**
     * Tests generated sequences. Also checks occurrence of {@link UnavailableIdException},
     * when the number of generated IDs exceeds the block size.
     */
    @Test
    public void basics() {
        assertThat(sut.getNextId(), is(0L));
        assertThat(sut.getNextId(), is(1L));
        assertThat(sut.getNextId(), is(2L));

        try {
            sut.getNextId();
            fail("UnavailableIdException should be thrown");
        } catch (UnavailableIdException e) {
            assertTrue(true);
        }
    }
}
