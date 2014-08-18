package net.onrc.onos.api.newintent;

import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.Actions;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.util.SwitchPort;

import java.util.Set;

/**
 * Base facilities to test various connectivity tests.
 */
public abstract class ConnectivityIntentTest extends IntentTest {

    public static final IntentId IID = new IntentId(123);
    public static final Match MATCH = (new PacketMatchBuilder()).build();
    public static final Action NOP = Actions.nullAction();

    public static final SwitchPort P1 = new SwitchPort(111, (short) 0x1);
    public static final SwitchPort P2 = new SwitchPort(222, (short) 0x2);
    public static final SwitchPort P3 = new SwitchPort(333, (short) 0x3);

    public static final Set<SwitchPort> PS1 = itemSet(new SwitchPort[]{P1, P3});
    public static final Set<SwitchPort> PS2 = itemSet(new SwitchPort[]{P2, P3});
}
