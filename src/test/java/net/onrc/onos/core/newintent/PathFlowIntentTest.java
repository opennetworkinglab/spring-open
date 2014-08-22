package net.onrc.onos.core.newintent;

import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.PacketPathFlow;
import net.onrc.onos.api.flowmanager.Path;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.api.newintent.IntentTest;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.util.PortNumber;

import java.util.Collections;

/**
 * Suite of tests of {@link PathFlowIntent}.
 */
public class PathFlowIntentTest extends IntentTest {

    private final IntentId intentId1 = new IntentId(123);
    private final IntentId intentId2 = new IntentId(456);
    private final FlowId flowId1 = new FlowId(1L);
    private final PacketMatch match = new PacketMatchBuilder().build();
    private final PortNumber port = PortNumber.uint16((short) 1);

    /**
     * Creates a PathFlowIntent.
     *
     * @return PathFlowIntent
     */
    @Override
    protected PathFlowIntent createOne() {
        return new PathFlowIntent(
                intentId1,
                new PacketPathFlow(flowId1, match, port,
                        new Path(), Collections.<Action>emptyList(), 0, 0)
        );
    }

    /**
     * Creates another PathFlowIntent, which is different from the intent created by {@link #createOne()}.
     *
     * @return another PathFlowIntent
     */
    @Override
    protected PathFlowIntent createAnother() {
        return new PathFlowIntent(
                intentId2,
                new PacketPathFlow(flowId1, match, port,
                        new Path(), Collections.<Action>emptyList(), 0, 0)
        );
    }
}
