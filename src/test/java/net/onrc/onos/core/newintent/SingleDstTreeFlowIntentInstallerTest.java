package net.onrc.onos.core.newintent;

import net.onrc.onos.api.flowmanager.FakeFlowManagerService;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowLink;
import net.onrc.onos.api.flowmanager.FlowManagerService;
import net.onrc.onos.api.flowmanager.SingleDstTreeFlow;
import net.onrc.onos.api.flowmanager.Tree;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.util.SwitchPort;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static net.onrc.onos.api.flowmanager.FlowState.COMPILED;
import static net.onrc.onos.api.flowmanager.FlowState.INSTALLED;
import static net.onrc.onos.api.flowmanager.FlowState.SUBMITTED;
import static net.onrc.onos.api.flowmanager.FlowState.WITHDRAWING;

/**
 * Suites of test of {@link SingleDstTreeFlowIntentInstaller}.
 */
public class SingleDstTreeFlowIntentInstallerTest {
    private final SwitchPort port12 = new SwitchPort(1, (short) 1);
    private final SwitchPort port13 = new SwitchPort(1, (short) 2);
    private final SwitchPort port21 = new SwitchPort(2, (short) 2);
    private final SwitchPort ingress1 = new SwitchPort(2, (short) 100);
    private final SwitchPort ingress2 = new SwitchPort(3, (short) 101);
    private final FlowId flowId = new FlowId(1);

    /**
     * Tests intent installation that the state is changed
     * to SUBMITTED, COMPILED, then INSTALLED.
     */
    @Test
    public void testNormalStateTransition() {
        FlowManagerService flowManager =
                new FakeFlowManagerService(flowId, false, SUBMITTED, COMPILED, INSTALLED);
        SingleDstTreeFlowIntentInstaller sut =
                new SingleDstTreeFlowIntentInstaller(flowManager);

        Tree tree = createTree();
        SingleDstTreeFlowIntent intent = new SingleDstTreeFlowIntent(new IntentId(1), createFlow(tree));

        sut.install(intent);
    }

    /**
     * Tests intent installation that addFlow() returns null.
     */
    @Test(expected = IntentInstallationException.class)
    public void testInstallationFails() {
        FlowManagerService flowManager =
                new FakeFlowManagerService(flowId, true, SUBMITTED);
        SingleDstTreeFlowIntentInstaller sut =
                new SingleDstTreeFlowIntentInstaller(flowManager);

        Tree tree = createTree();
        SingleDstTreeFlowIntent intent = new SingleDstTreeFlowIntent(new IntentId(1), createFlow(tree));

        sut.install(intent);
    }

    /**
     * Tests intent removal that removeFlow() returns null.
     */
    @Test(expected = IntentRemovalException.class)
    public void testRemovalFails() {
        FlowManagerService flowManager =
                new FakeFlowManagerService(flowId, true, WITHDRAWING);
        SingleDstTreeFlowIntentInstaller sut =
                new SingleDstTreeFlowIntentInstaller(flowManager);

        Tree tree = createTree();
        SingleDstTreeFlowIntent intent = new SingleDstTreeFlowIntent(new IntentId(1), createFlow(tree));

        sut.remove(intent);
    }

    private SingleDstTreeFlow createFlow(Tree tree) {
        return new SingleDstTreeFlow(
                flowId,
                    new PacketMatchBuilder().build(),
                    Arrays.asList(ingress1, ingress2),
                    tree,
                    Collections.<Action>emptyList()
            );
    }

    private Tree createTree() {
        Tree tree = new Tree();
        tree.add(new FlowLink(port12, port21));
        tree.add(new FlowLink(port13, port13));
        return tree;
    }
}
