package net.onrc.onos.core.newintent;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowLink;
import net.onrc.onos.api.flowmanager.PacketPathFlow;
import net.onrc.onos.api.flowmanager.Path;
import net.onrc.onos.api.newintent.Intent;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.api.newintent.PointToPointIntent;
import net.onrc.onos.core.intent.ConstrainedBFSTree;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.topology.BaseTopology;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.LinkData;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.util.IdGenerator;
import net.onrc.onos.core.util.SwitchPort;

import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A intent compiler for {@link PointToPointIntent}.
 */
public class PointToPointIntentCompiler
        extends AbstractFlowGeneratingIntentCompiler<PointToPointIntent> {

    private final ITopologyService topologyService;

    /**
     * Constructs an intent compiler for {@link PointToPointIntent} with the specified
     * ID generator and topology service.
     *
     * @param intentIdGenerator intent ID generator
     * @param topologyService topology service
     */
    public PointToPointIntentCompiler(IdGenerator<IntentId> intentIdGenerator,
                                      IdGenerator<FlowId> flowIdGenerator,
                                      ITopologyService topologyService) {
        super(intentIdGenerator, flowIdGenerator);
        this.topologyService = checkNotNull(topologyService);
    }

    @Override
    public List<Intent> compile(PointToPointIntent intent) {
        Match match = intent.getMatch();
        if (!(match instanceof PacketMatch)) {
            throw new IntentCompilationException(
                    "intent has unsupported type of match object: " + match
            );
        }

        SwitchPort ingress = intent.getIngressPort();
        SwitchPort egress = intent.getEgressPort();
        FlowId flowId = getNextFlowId();
        Path path = calculatePath(ingress, egress);

        List<Action> actions = packActions(intent, intent.getEgressPort());

        PacketPathFlow flow = new PacketPathFlow(flowId, (PacketMatch) match,
                ingress.getPortNumber(), path, actions, 0, 0);
        return Arrays.asList((Intent) new PathFlowIntent(getNextId(), flow));
    }

    /**
     * Calculates a path between the specified ingress port and the specified egress port.
     * @param ingress ingress port
     * @param egress egress port
     * @return path
     */
    private Path calculatePath(SwitchPort ingress, SwitchPort egress) {
        BaseTopology topology = topologyService.getTopology();
        Switch source = topology.getSwitch(ingress.getDpid());
        Switch destination = topology.getSwitch(egress.getDpid());

        if (source == null) {
            throw new PathNotFoundException("source switch not found: " + ingress.getDpid());
        }
        if (destination == null) {
            throw new PathNotFoundException("destination switch not found: " + egress.getDpid());
        }

        ConstrainedBFSTree tree = new ConstrainedBFSTree(source);
        net.onrc.onos.core.intent.Path path = tree.getPath(destination);
        return convertPath(path);
    }

    /**
     * Converts a {@link net.onrc.onos.core.intent.Path} to {@link Path}.
     *
     * @param path original {@link net.onrc.onos.core.intent.Path}
     * @return converted {@link Path}
     */
    static Path convertPath(net.onrc.onos.core.intent.Path path) {
        // would like to use filter and transform, but Findbugs detects
        // inconsistency of use of @Nullable annotation. Then, use of the
        // transform is avoided.
        // Ref: https://code.google.com/p/guava-libraries/issues/detail?id=1812
        // TODO: replace with transform when the above issue is resolved
        ImmutableList<LinkData> dataEntries = FluentIterable.from(path)
                .filter(Predicates.notNull())
                .toList();

        Path converted = new Path();
        for (LinkData data : dataEntries) {
            converted.add(new FlowLink(data.getSrc(), data.getDst()));
        }
        return converted;
    }
}
