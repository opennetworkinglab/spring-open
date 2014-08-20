package net.onrc.onos.api.flowmanager;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.onrc.onos.api.flowmanager.FlowBatchOperation.Operator;
import net.onrc.onos.core.matchaction.MatchAction;
import net.onrc.onos.core.matchaction.MatchActionIdGenerator;
import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.matchaction.MatchActionOperations;
import net.onrc.onos.core.matchaction.MatchActionOperationsIdGenerator;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * A {@link Flow} object expressing the multipoints-to-point tree flow for the
 * packet layer.
 * <p>
 * NOTE: This class might generate the {@link MatchAction} operations which
 * includes the MAC address modifications or other the label-switching-like
 * schemes.
 */
public class SingleDstTreeFlow extends Flow {
    private final PacketMatch match;
    private final Set<SwitchPort> ingressPorts;
    private final Tree tree;
    private final List<Action> egressActions;

    /**
     * Creates new instance using Tree object.
     *
     * @param id ID for this object
     * @param match the traffic filter for the tree
     * @param ingressPorts the set of ingress ports of the tree
     * @param tree the Tree object specifying tree topology for this object
     * @param egressActions the list of {@link Action} objects to be executed at
     *        the egress edge switch
     */
    public SingleDstTreeFlow(FlowId id, PacketMatch match,
            Collection<SwitchPort> ingressPorts, Tree tree, List<Action> egressActions) {
        super(id);
        this.match = checkNotNull(match);
        this.ingressPorts = ImmutableSet.copyOf(checkNotNull(ingressPorts));
        this.tree = checkNotNull(tree);
        this.egressActions = ImmutableList.copyOf(checkNotNull(egressActions));

        // TODO: check if the tree is a MP2P tree.
        // TODO: check consistency between ingressPorts and tree topology.
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
        return egressActions;
    }

    @Override
    public PacketMatch getMatch() {
        return match;
    }

    @Override
    public List<MatchActionOperations> compile(Operator op,
            MatchActionIdGenerator maIdGenerator,
            MatchActionOperationsIdGenerator maoIdGenerator) {
        switch (op) {
        case ADD:
            return compileAddOperation(maIdGenerator, maoIdGenerator);
        case REMOVE:
            return compileRemoveOperation();
        default:
            throw new UnsupportedOperationException("Unknown operation.");
        }
    }

    private MatchAction createMatchAction(SwitchPort port, List<Action> actions,
            MatchActionIdGenerator maIdGenerator) {
        checkNotNull(port);
        checkNotNull(actions);

        return new MatchAction(maIdGenerator.getNewId(), port, getMatch(), actions);
    }

    /**
     * Generates MatchAactionOperations at inner ports and at the egress switch.
     *
     * @param egressSwitch the egress switch of the tree
     * @param inPorts a map of a set of incoming ports on each switch in the
     *        tree
     * @param outPorts a map of outgoing port on each switch in the tree
     * @param maIdGenerator ID generator for MatchAction objects
     * @param maoIdGenerator ID generator for MatchActionOperations objects
     * @return the operations at inner ports and egress switch
     */
    private MatchActionOperations generateFirstAddOperations(
            Dpid egressSwitch,
            Map<Dpid, Set<PortNumber>> inPorts,
            Map<Dpid, PortNumber> outPorts,
            MatchActionIdGenerator maIdGenerator,
            MatchActionOperationsIdGenerator maoIdGenerator) {
        MatchActionOperations firstOps =
                new MatchActionOperations(maoIdGenerator.getNewId());
        for (Entry<Dpid, Set<PortNumber>> innerSw : inPorts.entrySet()) {
            for (PortNumber innerPortNumber : innerSw.getValue()) {
                SwitchPort innerPort = new SwitchPort(innerSw.getKey(), innerPortNumber);
                MatchAction ma;
                if (innerPort.getDpid().equals(egressSwitch)) {
                    ma = createMatchAction(innerPort, getEgressActions(), maIdGenerator);
                } else {
                    PortNumber outputPortNumber = checkNotNull(
                            outPorts.get(innerPort.getDpid()),
                            String.format("The tree has no output port at %s",
                                    innerPort.getDpid()));
                    ma = createMatchAction(innerPort,
                            Arrays.asList((Action) new OutputAction(outputPortNumber)),
                            maIdGenerator);
                }
                firstOps.addOperation(new MatchActionOperationEntry(
                        MatchActionOperations.Operator.ADD, ma));
            }
        }
        return firstOps;
    }

    /**
     * Generates MatchActionOperations for ingress switches in the tree.
     *
     * @param egressSwitch the egress switch of the tree
     * @param outPorts a map of outgoing port on each switch in the tree
     * @param maIdGenerator ID generator for MatchAction objects
     * @param maoIdGenerator ID generator for MatchActionOperations objects
     * @return operations at ingress switches in the tree
     */
    private MatchActionOperations generateSecondAddOperations(
            Dpid egressSwitch,
            Map<Dpid, PortNumber> outPorts,
            MatchActionIdGenerator maIdGenerator,
            MatchActionOperationsIdGenerator maoIdGenerator) {
        MatchActionOperations secondOps =
                new MatchActionOperations(maoIdGenerator.getNewId());
        for (SwitchPort port : getIngressPorts()) {
            PortNumber outputPort = outPorts.get(port.getDpid());
            if (outputPort == null) {
                if (port.getDpid().equals(egressSwitch)) {
                    MatchAction ma = createMatchAction(
                            port, getEgressActions(), maIdGenerator);
                    secondOps.addOperation(new MatchActionOperationEntry(
                            MatchActionOperations.Operator.ADD, ma));
                } else {
                    throw new IllegalStateException(String.format(
                            "The switch %s specified as one of ingress ports "
                                    + "does not have path to the egress switch.",
                            port.getDpid()));
                }
            } else {
                MatchAction ma = createMatchAction(port,
                        Arrays.asList((Action) new OutputAction(outputPort)),
                        maIdGenerator);
                secondOps.addOperation(new MatchActionOperationEntry(
                        MatchActionOperations.Operator.ADD, ma));
            }
        }
        return secondOps;
    }

    private List<MatchActionOperations> compileAddOperation(
            MatchActionIdGenerator maIdGenerator,
            MatchActionOperationsIdGenerator maoIdGenerator) {
        checkNotNull(tree);
        checkState(tree.size() > 0, "Tree object has no link.");

        // TODO: check consistency of the tree topology

        // collect input ports and output ports checking consistency
        Map<Dpid, PortNumber> outPorts = new HashMap<>();
        Map<Dpid, Set<PortNumber>> inPorts = new HashMap<>();
        for (FlowLink link : tree) {
            SwitchPort srcPort = link.getSrcSwitchPort();
            if (outPorts.containsKey(srcPort.getDpid())) {
                throw new IllegalStateException(
                        String.format("Dpid:%s has multiple output ports.",
                                srcPort.getDpid()));
            }
            outPorts.put(srcPort.getDpid(), srcPort.getPortNumber());

            SwitchPort dstPort = link.getDstSwitchPort();
            Set<PortNumber> inPortNumbers = inPorts.get(dstPort.getDpid());
            if (inPortNumbers == null) {
                inPortNumbers = new HashSet<>();
            }
            inPortNumbers.add(dstPort.getPortNumber());
            inPorts.put(dstPort.getDpid(), inPortNumbers);
        }

        // find the egress switch
        Set<Dpid> egressSwitches = new HashSet<>(inPorts.keySet());
        egressSwitches.removeAll(outPorts.keySet());
        checkState(egressSwitches.size() == 1,
                "The specified tree is not a single destination tree.");
        Dpid egressSwitch = egressSwitches.iterator().next();

        MatchActionOperations firstOps = generateFirstAddOperations(
                egressSwitch, inPorts, outPorts, maIdGenerator, maoIdGenerator);
        checkState(firstOps.size() > 0,
                "No operations found for the first set of operations.");

        MatchActionOperations secondOps = generateSecondAddOperations(
                egressSwitch, outPorts, maIdGenerator, maoIdGenerator);
        checkState(secondOps.size() > 0,
                "No operations found for the second set of operations.");

        return Arrays.asList(firstOps, secondOps);
    }

    private List<MatchActionOperations> compileRemoveOperation() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
                "REMOVE operation is not implemented yet.");
    }
}
