package net.onrc.onos.core.newintent;

import net.onrc.onos.api.flowmanager.FlowLink;
import net.onrc.onos.api.flowmanager.Path;
import net.onrc.onos.api.flowmanager.PathFlow;
import net.onrc.onos.api.newintent.Intent;
import net.onrc.onos.api.newintent.PointToPointIntent;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.Actions;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.util.SwitchPort;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Suites of test of {@link PointToPointIntentCompiler}.
 */
public class PointToPointIntentCompilerTest extends IntentCompilerTest {

    private PointToPointIntentCompiler sut;

    @Before
    public void setUp() {
        sut = new PointToPointIntentCompiler(
                intentIdGenerator,
                flowIdGenerator,
                topologyService
        );
    }

    /**
     * Checks the compilation result.
     */
    @Test
    public void testCompilation() {
        PointToPointIntent input = new PointToPointIntent(
                intentIdGenerator.getNewId(),
                new PacketMatchBuilder().build(),
                Actions.nullAction(),
                new SwitchPort(dpid1, port1h),
                new SwitchPort(dpid3, port3h)
        );
        List<Intent> compiled = sut.compile(input);

        assertThat(compiled, hasSize(1));

        Intent one = compiled.get(0);
        assertThat(one.getId(), is(intentId2));
        assertThat(one, is(instanceOf(PathFlowIntent.class)));

        PathFlowIntent lower = (PathFlowIntent) one;
        assertPathFlow(lower.getFlow());
    }

    private void assertPathFlow(PathFlow actual) {
        assertThat(actual.getId(), is(flowId));
        assertThat(actual.getIngressPortNumber(), is(port1h));
        assertThat(actual.getMatch(), is((Match) new PacketMatchBuilder().build()));
        assertThat(actual.getEgressActions(), hasSize(1));
        assertThat(actual.getEgressActions().get(0), is((Action) new OutputAction(port3h)));

        Path path = new Path(Arrays.asList(
                new FlowLink(dpid1, port12, dpid2, port21),
                new FlowLink(dpid2, port23, dpid3, port32)
        ));
        assertThat(actual.getPath(), is(path));
    }
}
