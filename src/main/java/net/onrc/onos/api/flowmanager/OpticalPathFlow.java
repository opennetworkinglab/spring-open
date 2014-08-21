package net.onrc.onos.api.flowmanager;

import java.util.List;

import net.onrc.onos.api.flowmanager.FlowBatchOperation.Operator;
import net.onrc.onos.core.matchaction.MatchActionIdGenerator;
import net.onrc.onos.core.matchaction.MatchActionOperations;
import net.onrc.onos.core.matchaction.MatchActionOperationsIdGenerator;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.util.PortNumber;

/**
 * Flow object representing an optical path.
 * <p>
 * TODO: Think this: How do we deal the optical path flow going through the
 * regenerators? Can we express it with multiple OpticalPathFlow objects?
 * <p>
 * NOTE: This class is not fully supported for the August release.
 */
public class OpticalPathFlow extends PathFlow {
    private final int lambda;

    /**
     * Constructor.
     *
     * @param id the ID for this new Flow object
     * @param ingressPort the Ingress port number at the ingress edge node
     * @param path the Path between ingress and egress edge node
     * @param egressActions the list of Action objects at the egress edge node
     * @param lambda the lambda to be used throughout the path
     */
    public OpticalPathFlow(FlowId id,
            PortNumber ingressPort, Path path, List<Action> egressActions, int lambda) {
        super(id, ingressPort, path, egressActions);
        this.lambda = lambda;
    }

    /**
     * Gets lambda which is used throughout the path.
     *
     * @return lambda which is used throughout the path
     */
    public int getLambda() {
        return lambda;
    }

    /**
     * Gets traffic filter for this flow.
     * <p>
     * This method only returns wildcard match, because the ingress transponder
     * port does not have filtering functionality.
     */
    @Override
    public PacketMatch getMatch() {
        return (new PacketMatchBuilder()).build();
    }

    @Override
    public List<MatchActionOperations> compile(Operator op,
            MatchActionIdGenerator maIdGenerator,
            MatchActionOperationsIdGenerator maoIdGenerator) {
        // TODO Auto-generated method stub
        return null;
    }
}
