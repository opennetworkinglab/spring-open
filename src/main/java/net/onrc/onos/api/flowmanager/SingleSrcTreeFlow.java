package net.onrc.onos.api.flowmanager;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Set;

import net.onrc.onos.core.matchaction.MatchActionOperations;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.Pair;
import net.onrc.onos.core.util.SwitchPort;

/**
 * A Flow object expressing the point-to-multipoints tree flow for the packet
 * layer.
 * <p>
 * NOTE: This class is not fully supported for the August release.
 */
public class SingleSrcTreeFlow extends Flow {
    protected PacketMatch match;
    protected SwitchPort ingressPort;
    protected Tree tree;
    protected Set<Pair<Dpid, OutputAction>> outputActions;

    /**
     * Creates new instance using Tree object.
     *
     * @param id ID for this object
     * @param match the traffic filter for the tree
     * @param ingressPort an ingress port of the tree
     * @param tree the tree object specifying tree topology for this object
     * @param outputActions the set of the pairs of the switch DPID and
     *        OutputAction object at the egress edge switchs
     */
    public SingleSrcTreeFlow(FlowId id, PacketMatch match,
            SwitchPort ingressPort, Tree tree, Set<Pair<Dpid, OutputAction>> outputActions) {
        super(id);
        this.match = checkNotNull(match);
        this.ingressPort = checkNotNull(ingressPort);
        this.tree = checkNotNull(tree);
        this.outputActions = checkNotNull(outputActions);

        // TODO: check if the tree is a P2MP tree.
        // TODO: check consistency among rootPort, tree, and actions.
    }

    /**
     * Gets the ingress port (the root) of the tree.
     *
     * @return The ingress port of the tree.
     */
    public SwitchPort getIngressPort() {
        return ingressPort;
    }

    /**
     * Gets the tree.
     *
     * @return The tree object.
     */
    public Tree getTree() {
        return tree;
    }

    /**
     * Gets the output actions for the tree.
     *
     * @return The set of the pairs of Dpid and OutputAction object.
     */
    public Set<Pair<Dpid, OutputAction>> getOutputActions() {
        return outputActions;
    }

    @Override
    public PacketMatch getMatch() {
        return match;
    }

    @Override
    public List<MatchActionOperations> compile(FlowBatchOperation.Operator op) {
        // TODO Auto-generated method stub
        return null;
    }
}
