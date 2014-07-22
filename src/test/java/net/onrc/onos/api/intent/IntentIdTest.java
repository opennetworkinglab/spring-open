package net.onrc.onos.api.intent;

import org.junit.Test;

import static net.onrc.onos.core.util.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * This class tests the immutability, equality, and non-equality of {@link IntentId}.
 */
public class IntentIdTest {
    /**
     * Tests the immutability of {@link IntentId}.
     */
    @Test
    public void intentIdFollowsGuidelineForImmutableObject() {
        assertThatClassIsImmutable(IntentId.class);
    }

    /**
     * Tests equality of {@link IntentId}.
     */
    @Test
    public void testEquality() {
        IntentId id1 = new IntentId(1L);
        IntentId id2 = new IntentId(1L);

        assertThat(id1, is(id2));
    }

    /**
     * Tests non-equality of {@link IntentId}.
     */
    @Test
    public void testNonEquality() {
        IntentId id1 = new IntentId(1L);
        IntentId id2 = new IntentId(2L);

        assertThat(id1, is(not(id2)));
    }
}
