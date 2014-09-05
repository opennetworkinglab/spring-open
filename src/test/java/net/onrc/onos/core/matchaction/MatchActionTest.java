package net.onrc.onos.core.matchaction;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.ModifyDstMacAction;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.util.SwitchPort;

import org.junit.Test;

/**
 * Unit tests for MatchAction objects.
 */
public class MatchActionTest {

    /**
     * Tests that a MatchAction object can be constructed properly.
     */
    @Test
    public void testConstructor() {
        SwitchPort port = new SwitchPort(123L, (short) 55);
        PacketMatchBuilder builder = new PacketMatchBuilder();
        builder.setDstTcpPort((short) 80);
        List<Action> actions = new LinkedList<Action>();
        actions.add(new ModifyDstMacAction(MACAddress.valueOf("00:01:02:03:04:05")));
        MatchAction ma = new MatchAction(new MatchActionId(1L), port, builder.build(), actions);

        assertEquals(actions, ma.getActions());
        assertEquals("1", ma.getId().toString());
        assertEquals(builder.build(), ma.getMatch());
        assertEquals(port, ma.getSwitchPort());
    }

}
