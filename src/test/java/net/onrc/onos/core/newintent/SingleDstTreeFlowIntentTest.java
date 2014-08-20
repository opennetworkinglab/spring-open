package net.onrc.onos.core.newintent;

import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowLink;
import net.onrc.onos.api.flowmanager.SingleDstTreeFlow;
import net.onrc.onos.api.flowmanager.Tree;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.api.newintent.IntentTest;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import java.util.Arrays;

/**
 * Suites of test of {@link SingleDstTreeFlowIntent}.
 */
public class SingleDstTreeFlowIntentTest extends IntentTest {

    private final IntentId intentId1 = new IntentId(1L);
    private final IntentId intentId2 = new IntentId(2L);
    private final FlowId flowId1 = new FlowId(1L);
    private final PacketMatch match = new PacketMatchBuilder().build();
    private final short port1 = (short) 1;
    private final short port2 = (short) 2;
    private final short port3 = (short) 3;
    private final SwitchPort switchPort1 = new SwitchPort(1, port1);
    private final SwitchPort switchPort2 = new SwitchPort(2, port2);
    private final SwitchPort switchPort3 = new SwitchPort(3, port3);

    @Override
    protected SingleDstTreeFlowIntent createOne() {
        SingleDstTreeFlow treeFlow = new SingleDstTreeFlow(flowId1,
                match,
                Arrays.asList(switchPort1, switchPort2),
                createTree(),
                Arrays.asList((Action) new OutputAction(new PortNumber(port3))));
        return new SingleDstTreeFlowIntent(intentId1, treeFlow);
    }

    @Override
    protected SingleDstTreeFlowIntent createAnother() {
        SingleDstTreeFlow treeFlow = new SingleDstTreeFlow(flowId1,
                match,
                Arrays.asList(switchPort1, switchPort3),
                createTree(),
                Arrays.asList((Action) new OutputAction(new PortNumber(port2))));
        return new SingleDstTreeFlowIntent(intentId2, treeFlow);
    }

    private Tree createTree() {
        Tree tree = new Tree();
        tree.add(new FlowLink(switchPort1, switchPort2));
        tree.add(new FlowLink(switchPort1, switchPort3));

        return tree;
    }
}
