package net.onrc.onos.core.newintent;

import net.onrc.onos.api.flowmanager.FakeFlowManagerService;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowLink;
import net.onrc.onos.api.flowmanager.FlowManagerService;
import net.onrc.onos.api.flowmanager.PacketPathFlow;
import net.onrc.onos.api.flowmanager.Path;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.match.PacketMatch;
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
 * Suites of test of {@link PathFlowIntentInstaller}.
 */
public class PathFlowIntentInstallerTest {
    private final FlowId flowId = new FlowId(1);
    private final IntentId intentId = new IntentId(1);
    private final PacketMatch match = new PacketMatchBuilder().build();
    private SwitchPort src = new SwitchPort(1, (short) 1);
    private SwitchPort dst = new SwitchPort(2, (short) 2);

    /**
     * Tests intent installation that the state is changed
     * to SUBMITTED, COMPILED, then INSTALLED.
     */
    @Test
    public void testNormalStateTransition() {
        FlowManagerService flowManager =
                new FakeFlowManagerService(flowId, false, SUBMITTED, COMPILED, INSTALLED);
        PathFlowIntentInstaller sut =
                new PathFlowIntentInstaller(flowManager);

        PacketPathFlow flow = createFlow();
        PathFlowIntent intent = new PathFlowIntent(intentId, flow);

        sut.install(intent);
    }

    /**
     * Tests intent installation that addFlow() returns null.
     */
    @Test(expected = IntentInstallationException.class)
    public void testInstallationFails() {
        FlowManagerService flowManager =
                new FakeFlowManagerService(flowId, true, SUBMITTED);
        PathFlowIntentInstaller sut =
                new PathFlowIntentInstaller(flowManager);

        PacketPathFlow flow = createFlow();
        PathFlowIntent intent = new PathFlowIntent(intentId, flow);

        sut.install(intent);
    }

    /**
     * Tests intent removal that removeFlow() returns null.
     */
    @Test(expected = IntentRemovalException.class)
    public void testRemovalFails() {
        FlowManagerService flowManager =
                new FakeFlowManagerService(flowId, true, WITHDRAWING);
        PathFlowIntentInstaller sut =
                new PathFlowIntentInstaller(flowManager);

        PacketPathFlow flow = createFlow();
        PathFlowIntent intent = new PathFlowIntent(intentId, flow);

        sut.remove(intent);
    }

    private PacketPathFlow createFlow() {
        return new PacketPathFlow(flowId, match, src.getPortNumber(),
                new Path(Arrays.asList(new FlowLink(src, dst))),
                Collections.<Action>emptyList(), 0, 0);
    }
}
