package net.onrc.onos.core.matchaction;

import net.onrc.onos.core.util.ImmutableClassChecker;
import org.junit.Test;

/**
 * Tests to verify that immutable MatchAction classes are immutable.
 */
public class TestImmutableClasses {

    @Test
    public void checkMatchActionImmutable() {
        ImmutableClassChecker.assertThatClassIsImmutable(MatchAction.class);
    }

    @Test
    public void checkMatchActionOperationEntryImmutable() {
        ImmutableClassChecker.assertThatClassIsImmutable(MatchActionOperationEntry.class);
    }

    @Test
    public void checkMatchActionId() {
        ImmutableClassChecker.assertThatClassIsImmutable(MatchActionId.class);
    }

    @Test
    public void checkMatchActionOperations() {
        ImmutableClassChecker.assertThatClassIsImmutable(MatchActionOperations.class);
    }
}
