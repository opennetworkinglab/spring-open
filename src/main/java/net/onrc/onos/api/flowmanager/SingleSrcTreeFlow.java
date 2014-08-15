package net.onrc.onos.api.flowmanager;

import java.util.Set;

import net.onrc.onos.core.matchaction.MatchActionOperations;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.Pair;
import net.onrc.onos.core.util.SwitchPort;

/**
 * An IFlow object expressing the point-to-multipoints tree flow for the packet
 * layer.
 */
public class SingleSrcTreeFlow implements Flow {
    protected final FlowId id;
    protected PacketMatch match;
    protected SwitchPort ingressPort;
    protected Tree tree;
    protected Set<Pair<Dpid, OutputAction>> outputActions;

    /**
     * Creates new instance using Tree object.
     *
     * @param id ID for this object.
     * @param match Traffic filter for the tree.
     * @param ingressPort A ingress port of the tree.
     * @param tree Tree object specifying tree topology for this object.
     * @param outputActions The set of the pairs of the switch DPID and
     *        OutputAction object at the egress edge switchs.
     */
    public SingleSrcTreeFlow(String id, PacketMatch match,
            SwitchPort ingressPort, Tree tree, Set<Pair<Dpid, OutputAction>> outputActions) {
        this.id = new FlowId(id);
        this.match = match;
        this.ingressPort = ingressPort;
        this.tree = tree;
        this.outputActions = outputActions;

        // TODO: check if the tree is a P2MP tree.
        // TODO: check consistency among rootPort, tree, and actions.
    }

    @Override
    public FlowId getId() {
        return id;
    }

    @Override
    public PacketMatch getMatch() {
        return match;
    }

    @Override
    public MatchActionOperations compile() {
        // TODO Auto-generated method stub
        return null;
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
}
