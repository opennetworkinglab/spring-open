package net.onrc.onos.core.newintent;

import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowLink;
import net.onrc.onos.api.flowmanager.SingleSrcTreeFlow;
import net.onrc.onos.api.flowmanager.Tree;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.api.newintent.IntentTest;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Suites of test of {@link SingleSrcTreeFlowIntent}.
 */
public class SingleSrcTreeFlowIntentTest extends IntentTest {

    private final IntentId intentId1 = new IntentId(1L);
    private final IntentId intentId2 = new IntentId(2L);
    private final FlowId flowId1 = new FlowId(1L);
    private final FlowId flowId2 = new FlowId(2L);
    private final Dpid dpid1 = new Dpid(1);
    private final Dpid dpid2 = new Dpid(2);
    private final Dpid dpid3 = new Dpid(3);
    private final PortNumber port1 = PortNumber.uint32(1);
    private final PortNumber port2 = PortNumber.uint32(2);
    private final PortNumber port3 = PortNumber.uint32(3);
    private final OutputAction action1 = new OutputAction(port1);
    private final OutputAction action2 = new OutputAction(port2);
    private final OutputAction action3 = new OutputAction(port3);
    private final PacketMatch match = new PacketMatchBuilder().build();

    @Override
    protected SingleSrcTreeFlowIntent createOne() {
        Set<Pair<Dpid, OutputAction>> actions = new HashSet<>(Arrays.asList(
                Pair.of(dpid2, action2),
                Pair.of(dpid3, action3)
        ));
        SingleSrcTreeFlow tree = new SingleSrcTreeFlow(flowId1, match,
                new SwitchPort(dpid1, port3), createTree(), actions
        );
        return new SingleSrcTreeFlowIntent(intentId1, tree);
    }

    @Override
    protected SingleSrcTreeFlowIntent createAnother() {
        Set<Pair<Dpid, OutputAction>> actions = new HashSet<>(Arrays.asList(
                Pair.of(dpid1, action1),
                Pair.of(dpid3, action3)
        ));
        SingleSrcTreeFlow tree = new SingleSrcTreeFlow(flowId2, match,
                new SwitchPort(dpid2, port3), createTree(), actions
        );
        return new SingleSrcTreeFlowIntent(intentId2, tree);
    }

    private Tree createTree() {
        Tree tree = new Tree();
        tree.add(new FlowLink(dpid1, port1, dpid2, port2));
        tree.add(new FlowLink(dpid1, port2, dpid3, port3));

        return tree;
    }
}
