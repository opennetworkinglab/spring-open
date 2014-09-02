package net.onrc.onos.core.newintent;

import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.SingleDstTreeFlow;
import net.onrc.onos.api.flowmanager.Tree;
import net.onrc.onos.api.newintent.Intent;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.api.newintent.MultiPointToSinglePointIntent;
import net.onrc.onos.core.intent.ConstrainedBFSTree;
import net.onrc.onos.core.intent.Path;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.topology.BaseTopology;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.util.IdGenerator;
import net.onrc.onos.core.util.SwitchPort;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.onrc.onos.core.newintent.PointToPointIntentCompiler.convertPath;

/**
 * An intent compiler for {@link MultiPointToSinglePointIntent}.
 */
public class MultiPointToSinglePointIntentCompiler
        extends AbstractFlowGeneratingIntentCompiler<MultiPointToSinglePointIntent> {

    private final ITopologyService topologyService;

    /**
     * Constructs an intent compiler for {@link MultiPointToSinglePointIntent}.
     *
     * @param intentIdGenerator intent ID generator
     * @param flowIdGenerator flow ID generator
     * @param topologyService topology service
     */
    public MultiPointToSinglePointIntentCompiler(IdGenerator<IntentId> intentIdGenerator,
                                                 IdGenerator<FlowId> flowIdGenerator,
                                                 ITopologyService topologyService) {
        super(intentIdGenerator, flowIdGenerator);
        this.topologyService = checkNotNull(topologyService);
    }

    @Override
    public List<Intent> compile(MultiPointToSinglePointIntent intent) {
        Match match = intent.getMatch();
        if (!(match instanceof PacketMatch)) {
            throw new IntentCompilationException(
                    "intent has unsupported type of match object: " + match
            );
        }

        SingleDstTreeFlow treeFlow = new SingleDstTreeFlow(
                getNextFlowId(),
                (PacketMatch) intent.getMatch(), // down-cast, but it is safe due to the guard above
                intent.getIngressPorts(),
                calculateTree(intent.getIngressPorts(), intent.getEgressPort()),
                packActions(intent, intent.getEgressPort())
        );
        Intent compiled = new SingleDstTreeFlowIntent(getNextId(), treeFlow);
        return Arrays.asList(compiled);
    }

    /**
     * Calculates a tree with the specified ingress ports and egress port.
     *
     * {@link PathNotFoundException} is thrown when no tree found or
     * the specified egress port is not found in the topology.
     *
     * @param ingressPorts ingress ports
     * @param egressPort egress port
     * @return tree
     * @throws PathNotFoundException if the specified egress switch is not
     * found or no tree is found.
     */
    private Tree calculateTree(Set<SwitchPort> ingressPorts, SwitchPort egressPort) {
        BaseTopology topology = topologyService.getTopology();
        Switch egressSwitch = topology.getSwitch(egressPort.getDpid());
        if (egressSwitch == null) {
            throw new PathNotFoundException("destination switch not found: " + egressPort.getDpid());
        }

        ConstrainedBFSTree bfs = new ConstrainedBFSTree(egressSwitch);
        Tree tree = new Tree();

        for (SwitchPort ingressPort : ingressPorts) {
            Switch ingressSwitch = topology.getSwitch(ingressPort.getDpid());
            if (ingressSwitch == null) {
                continue;
            }

            Path path = bfs.getPath(ingressSwitch);
            if (path.isEmpty()) {
                continue;
            }

            tree.addAll(convertPath(path));
        }

        if (tree.isEmpty()) {
            throw new PathNotFoundException(
                    String.format("No tree found (ingress: %s, egress: %s", ingressPorts, egressPort)
            );
        }

        return tree;
    }
}
