package net.onrc.onos.core.newintent;

import net.onrc.onos.api.flowmanager.FakeFlowManagerService;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowLink;
import net.onrc.onos.api.flowmanager.FlowManagerService;
import net.onrc.onos.api.flowmanager.SingleSrcTreeFlow;
import net.onrc.onos.api.flowmanager.Tree;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.SwitchPort;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static net.onrc.onos.api.flowmanager.FlowState.COMPILED;
import static net.onrc.onos.api.flowmanager.FlowState.INSTALLED;
import static net.onrc.onos.api.flowmanager.FlowState.SUBMITTED;
import static net.onrc.onos.api.flowmanager.FlowState.WITHDRAWING;

/**
 * Suites of test of {@link SingleSrcTreeFlowIntentInstaller}.
 */
public class SingleSrcTreeFlowIntentInstallerTest {
    private final FlowId flowId = new FlowId(1);
    private final SwitchPort port12 = new SwitchPort(1, (short) 1);
    private final SwitchPort port13 = new SwitchPort(1, (short) 3);
    private final SwitchPort port21 = new SwitchPort(2, (short) 2);
    private final SwitchPort port31 = new SwitchPort(3, (short) 3);
    private final SwitchPort ingress1 = new SwitchPort(1, (short) 3);
    private final SwitchPort egress1 = new SwitchPort(2, (short) 100);
    private final SwitchPort egress2 = new SwitchPort(3, (short) 101);

    /**
     * Tests intent installation that the state is changed
     * to SUBMITTED, COMPILED, then INSTALLED.
     */
    @Test
    public void testNormalStateTransition() {
        FlowManagerService flowManager =
                new FakeFlowManagerService(flowId, false, SUBMITTED, COMPILED, INSTALLED);
        SingleSrcTreeFlowIntentInstaller sut =
                new SingleSrcTreeFlowIntentInstaller(flowManager);

        SingleSrcTreeFlow flow = createFlow();
        SingleSrcTreeFlowIntent intent = new SingleSrcTreeFlowIntent(new IntentId(1), flow);

        sut.install(intent);
    }

    /**
     * Tests intent installation that addFlow() returns null.
     */
    @Test(expected = IntentInstallationException.class)
    public void testInstallationFails() {
        FlowManagerService flowManager =
                new FakeFlowManagerService(flowId, true, SUBMITTED);
        SingleSrcTreeFlowIntentInstaller sut =
                new SingleSrcTreeFlowIntentInstaller(flowManager);

        SingleSrcTreeFlow flow = createFlow();
        SingleSrcTreeFlowIntent intent = new SingleSrcTreeFlowIntent(new IntentId(1), flow);

        sut.install(intent);
    }

    /**
     * Tests intent removal that removeFlow() returns null.
     */
    @Test(expected = IntentRemovalException.class)
    public void testRemovalFails() {
        FlowManagerService flowManager =
                new FakeFlowManagerService(flowId, true, WITHDRAWING);
        SingleSrcTreeFlowIntentInstaller sut =
                new SingleSrcTreeFlowIntentInstaller(flowManager);

        SingleSrcTreeFlow flow = createFlow();
        SingleSrcTreeFlowIntent intent = new SingleSrcTreeFlowIntent(new IntentId(1), flow);

        sut.remove(intent);
    }

    private SingleSrcTreeFlow createFlow() {
        Tree tree = new Tree();
        tree.add(new FlowLink(port12, port21));
        tree.add(new FlowLink(port13, port31));

        Set<Pair<Dpid, OutputAction>> actions = new HashSet<>();
        actions.add(Pair.of(egress1.getDpid(), new OutputAction(egress1.getPortNumber())));
        actions.add(Pair.of(egress2.getDpid(), new OutputAction(egress2.getPortNumber())));

        return new SingleSrcTreeFlow(
                flowId,
                new PacketMatchBuilder().build(),
                ingress1,
                tree,
                actions
        );
    }
}
