package net.onrc.onos.core.util;

import org.junit.Test;

import static net.onrc.onos.core.util.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * PairTest class tests the immutability of Pair class when immutable objects are supplied.
 */
public class PairTest {
    /**
     * Tests Pair class satisfies the guideline for immutable objects
     * by using ImmutableClassChecker framework.
     */
    @Test
    public void pairClassFollowsGuidelineForImmutableObject() {
        assertThatClassIsImmutable(Pair.class);
    }
}
