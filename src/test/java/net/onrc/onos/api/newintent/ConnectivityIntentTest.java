package net.onrc.onos.api.newintent;

import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.util.SwitchPort;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Base facilities to test various connectivity tests.
 */
public abstract class ConnectivityIntentTest {

    public static final IntentId IID = new IntentId(123);
    public static final Match MATCH = (new PacketMatchBuilder()).build();
    public static final Action NOP = new NoAction();

    public static final SwitchPort P1 = new SwitchPort(111, (short) 0x1);
    public static final SwitchPort P2 = new SwitchPort(222, (short) 0x2);
    public static final SwitchPort P3 = new SwitchPort(333, (short) 0x3);

    public static final Set<SwitchPort> PS1 = itemSet(new SwitchPort[]{P1, P3});
    public static final Set<SwitchPort> PS2 = itemSet(new SwitchPort[]{P2, P3});


    @Test
    public void equalsAndHashCode() {
        Intent one = createOne();
        Intent like = createOne();
        Intent another = createAnother();

        assertTrue("should be equal", one.equals(like));
        assertEquals("incorrect hashCode", one.hashCode(), like.hashCode());

        assertFalse("should not be equal", one.equals(another));

        assertFalse("should not be equal", one.equals(null));
        assertFalse("should not be equal", one.equals("foo"));
    }

    @Test
    public void testToString() {
        Intent one = createOne();
        Intent like = createOne();
        assertEquals("incorrect toString", one.toString(), like.toString());
    }

    /**
     * Creates a new intent, but always a like intent, i.e. all instances will
     * be equal, but should not be the same.
     *
     * @return intent
     */
    protected abstract Intent createOne();

    /**
     * Creates another intent, not equals to the one created by
     * {@link #createOne()} and with a different hash code.
     *
     * @return another intent
     */
    protected abstract Intent createAnother();


    /**
     * Produces a set of items from the supplied items.
     *
     * @param items items to be placed in set
     * @param <T>   item type
     * @return set of items
     */
    private static <T> Set<T> itemSet(T[] items) {
        return new HashSet<>(Arrays.asList(items));
    }

    // TODO: move to the match-action related package
    private static class NoAction implements Action {
    }
}
