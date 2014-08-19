package net.onrc.onos.api.flowmanager;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.onrc.onos.core.matchaction.MatchActionOperations;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.util.SwitchPort;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * A Flow object expressing the multipoints-to-point tree flow for the packet
 * layer.
 * <p>
 * NOTE: This class might generate the MatchActionPlan which includes the MAC
 * address modifications or other the label-switching-like schemes.
 */
public class SingleDstTreeFlow extends Flow {
    private final PacketMatch match;
    private final Set<SwitchPort> ingressPorts;
    private final Tree tree;
    private final List<Action> actions;

    /**
     * Creates new instance using Tree object.
     * <p>
     * For now, the actions parameter must be the list of a single
     * ModifyDstMacAction object and a single OutputAction object. But in the
     * future, the parameter should accept any type of the list of IAction
     * objects.
     *
     * @param id ID for this object
     * @param match the traffic filter for the tree
     * @param ingressPorts the set of ingress ports of the tree
     * @param tree the Tree object specifying tree topology for this object
     * @param actions the list of Action objects at the egress edge switch
     */
    public SingleDstTreeFlow(FlowId id, PacketMatch match,
            Collection<SwitchPort> ingressPorts, Tree tree, List<Action> actions) {
        super(id);
        this.match = checkNotNull(match);
        this.ingressPorts = ImmutableSet.copyOf(checkNotNull(ingressPorts));
        this.tree = checkNotNull(tree);
        this.actions = ImmutableList.copyOf(checkNotNull(actions));

        // TODO: check if the tree is a MP2P tree.
        // TODO: check consistency among ingressPorts, tree, and actions.
    }

    /**
     * Gets the ingress ports of the tree.
     *
     * @return the ingress ports of the tree
     */
    public Collection<SwitchPort> getIngressPorts() {
        return ingressPorts;
    }

    /**
     * Gets the tree.
     *
     * @return the tree object
     */
    public Tree getTree() {
        return tree;
    }

    /**
     * Gets the list of actions at the egress edge switch.
     *
     * @return the list of actions at the egress edge switch
     */
    public List<Action> getEgressActions() {
        return actions;
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
