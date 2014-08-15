package net.onrc.onos.api.flowmanager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.onrc.onos.core.matchaction.MatchActionOperations;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.util.SwitchPort;

/**
 * A Flow object expressing the multipoints-to-point tree flow for the packet
 * layer.
 * <p>
 * NOTE: This class might generate the MatchActionPlan which includes the MAC
 * address modifications or other the label-switching-like schemes.
 */
public class SingleDstTreeFlow implements Flow {
    protected final FlowId id;
    protected PacketMatch match;
    protected Set<SwitchPort> ingressPorts;
    protected Tree tree;
    protected OutputAction outputAction;

    /**
     * Creates new instance using Tree object.
     *
     * @param id ID for this object.
     * @param match Traffic filter for the tree.
     * @param ingressPorts A set of ingress ports of the tree.
     * @param tree Tree object specifying tree topology for this object.
     * @param outputAction OutputAction object at the egress edge switch.
     */
    public SingleDstTreeFlow(String id, PacketMatch match,
            Collection<SwitchPort> ingressPorts, Tree tree, OutputAction outputAction) {
        this.id = new FlowId(id);
        this.match = match;
        this.ingressPorts = new HashSet<SwitchPort>(ingressPorts);
        this.tree = tree;
        this.outputAction = outputAction;

        // TODO: check if the tree is a MP2P tree.
        // TODO: check consistency among inPorts, tree, and action.
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
     * Gets the ingress ports of the tree.
     *
     * @return The ingress ports of the tree.
     */
    public Collection<SwitchPort> getIngressPorts() {
        return Collections.unmodifiableCollection(ingressPorts);
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
     * Gets the output action for the tree.
     *
     * @return The OutputAction object.
     */
    public OutputAction getOutputAction() {
        return outputAction;
    }
}
