package net.onrc.onos.core.matchaction;

import net.onrc.onos.core.util.ImmutableClassChecker;
import org.junit.Test;

/**
 * Tests to verify that immutable MatchAction classes are immutable.
 */
public class TestImmutableClasses {

    /**
     * MatchAction objects should be immutable.
     */
    @Test
    public void checkMatchActionImmutable() {
        ImmutableClassChecker.assertThatClassIsImmutable(MatchAction.class);
    }

    /**
     * MatchActionOperationEntry objects should be immutable.
     */
    @Test
    public void checkMatchActionOperationEntryImmutable() {
        ImmutableClassChecker.assertThatClassIsImmutable(MatchActionOperationEntry.class);
    }

    /**
     * MatchActionId objects should be immutable.
     */
    @Test
    public void checkMatchActionId() {
        ImmutableClassChecker.assertThatClassIsImmutable(MatchActionId.class);
    }

    /**
     * MatchActionOperationsId objects should be immutable.
     */
    @Test
    public void checkMatchActionOperationsId() {
        ImmutableClassChecker.assertThatClassIsImmutable(MatchActionOperationsId.class);
    }
}
