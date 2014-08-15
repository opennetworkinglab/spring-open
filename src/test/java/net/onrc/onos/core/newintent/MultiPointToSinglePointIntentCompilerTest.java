package net.onrc.onos.core.newintent;

import net.onrc.onos.api.flowmanager.FlowLink;
import net.onrc.onos.api.flowmanager.SingleDstTreeFlow;
import net.onrc.onos.api.flowmanager.Tree;
import net.onrc.onos.api.newintent.Intent;
import net.onrc.onos.api.newintent.MultiPointToSinglePointIntent;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.Actions;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.util.SwitchPort;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Suites of test of {@link MultiPointToSinglePointIntentCompiler}.
 */
public class MultiPointToSinglePointIntentCompilerTest extends IntentCompilerTest {

    private MultiPointToSinglePointIntentCompiler sut;

    @Before
    public void setUp() {
        sut = new MultiPointToSinglePointIntentCompiler(
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
        MultiPointToSinglePointIntent intent = new MultiPointToSinglePointIntent(
                intentIdGenerator.getNewId(),
                new PacketMatchBuilder().build(),
                Actions.nullAction(),
                new HashSet<>(Arrays.asList(
                        new SwitchPort(dpid4, port43),
                        new SwitchPort(dpid2, port23)
                )),
                new SwitchPort(dpid1, port1h)
        );
        List<Intent> compiled = sut.compile(intent);

        assertThat(compiled, hasSize(1));

        SingleDstTreeFlowIntent lower = (SingleDstTreeFlowIntent) compiled.get(0);
        assertTree(lower.getTree());
    }

    private void assertTree(SingleDstTreeFlow actual) {
        assertThat(actual.getId(), is(flowId));
        assertThat(actual.getIngressPorts(), hasSize(2));
        assertThat(actual.getIngressPorts(), hasItems(new SwitchPort(dpid4, port43), new SwitchPort(dpid2, port23)));
        assertThat(actual.getMatch(), is((Match) new PacketMatchBuilder().build()));
        assertThat(actual.getEgressActions(), hasSize(1));
        assertThat(actual.getEgressActions().get(0), is((Action) new OutputAction(port1h)));

        Tree tree = new Tree();
        tree.add(new FlowLink(dpid1, port14, dpid4, port41));
        tree.add(new FlowLink(dpid1, port12, dpid2, port21));
        assertThat(actual.getTree(), is(tree));
    }
}
