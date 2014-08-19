package net.onrc.onos.api.flowmanager;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.onrc.onos.api.flowmanager.FlowBatchOperation.Operator;
import net.onrc.onos.core.matchaction.MatchAction;
import net.onrc.onos.core.matchaction.MatchActionIdGenerator;
import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.matchaction.MatchActionOperations;
import net.onrc.onos.core.matchaction.MatchActionOperationsIdGenerator;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

/**
 * Flow object representing a packet path.
 * <p>
 * TODO: Think this: Do we need a bandwidth constraint?
 */
public class PacketPathFlow extends PathFlow {
    private final PacketMatch match;
    private final int hardTimeout;
    private final int idleTimeout;

    /**
     * Constructor.
     *
     * @param id ID for this new Flow object
     * @param match the Match object at the source node of the path
     * @param ingressPort the Ingress port number at the ingress edge node
     * @param path the Path between ingress and egress edge node
     * @param egressActions the list of Action objects at the egress edge node
     * @param hardTimeout the hard-timeout value in seconds, or 0 for no timeout
     * @param idleTimeout the idle-timeout value in seconds, or 0 for no timeout
     */
    public PacketPathFlow(FlowId id,
            PacketMatch match, PortNumber ingressPort, Path path,
            List<Action> egressActions,
            int hardTimeout, int idleTimeout) {
        super(id, ingressPort, path, egressActions);
        this.match = checkNotNull(match);
        this.hardTimeout = hardTimeout;
        this.idleTimeout = idleTimeout;
    }

    /**
     * Gets idle-timeout value.
     *
     * @return Idle-timeout value (seconds)
     */
    public int getIdleTimeout() {
        return idleTimeout;
    }

    /**
     * Gets hard-timeout value.
     *
     * @return Hard-timeout value (seconds)
     */
    public int getHardTimeout() {
        return hardTimeout;
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
            return compileRemoveOperation(maIdGenerator, maoIdGenerator);
        default:
            throw new UnsupportedOperationException("Unknown operation.");
        }
    }

    /**
     * Creates the next {@link MatchAction} object using iterators.
     *
     * @param portIterator the iterator for {@link SwitchPort} objects
     * @param actionsIterator the iterator for the lists of {@link Action}
     * @param maIdGenerator the ID generator of {@link MatchAction}
     * @return {@link MatchAction} object based on the specified iterators
     */
    private MatchAction createNextMatchAction(Iterator<SwitchPort> portIterator,
            Iterator<List<Action>> actionsIterator,
            MatchActionIdGenerator maIdGenerator) {
        if (portIterator == null || actionsIterator == null ||
                !portIterator.hasNext() || !actionsIterator.hasNext()) {
            return null;
        }

        // TODO: Update this after merging the new MatchAction API.
        return new MatchAction(
                maIdGenerator.getNewId(),
                portIterator.next(),
                getMatch(), actionsIterator.next());
    }

    /**
     * Generates the list of {@link MatchActionOperations} objects with
     * add-operation.
     *
     * @return the list of {@link MatchActionOperations} objects
     */
    private List<MatchActionOperations> compileAddOperation(
            MatchActionIdGenerator maIdGenerator,
            MatchActionOperationsIdGenerator maoIdGenerator) {
        Path path = checkNotNull(getPath());
        checkState(path.size() > 0, "Path object has no link.");

        // Preparing each actions and ingress port for each switch
        List<List<Action>> actionsList = new LinkedList<>();
        List<SwitchPort> portList = new LinkedList<>();
        for (FlowLink link : path) {
            portList.add(link.getDstSwitchPort());
            actionsList.add(Arrays.asList(
                    (Action) new OutputAction(link.getSrcPortNumber())));
        }

        // The head switch's ingress port
        portList.add(0, new SwitchPort(path.getSrcDpid(), getIngressPortNumber()));

        // The tail switch's action
        actionsList.add(getEgressActions());

        Iterator<SwitchPort> portIterator = portList.iterator();
        Iterator<List<Action>> actionsIterator = actionsList.iterator();

        // Creates the second phase operation
        // using the head switch's match action
        MatchAction headMatchAction = createNextMatchAction(portIterator,
                actionsIterator, maIdGenerator);
        if (headMatchAction == null) {
            return null;
        }
        MatchActionOperations secondOp = new MatchActionOperations(
                maoIdGenerator.getNewId());
        secondOp.addOperation(new MatchActionOperationEntry(
                MatchActionOperations.Operator.ADD, headMatchAction));

        // Creates the first phase operation
        // using the remaining switches' match actions
        MatchActionOperations firstOp = new MatchActionOperations(
                maoIdGenerator.getNewId());
        MatchAction ma;
        while ((ma = createNextMatchAction(portIterator, actionsIterator, maIdGenerator)) != null) {
            firstOp.addOperation(new MatchActionOperationEntry(
                    MatchActionOperations.Operator.ADD, ma));
        }

        return Arrays.asList(firstOp, secondOp);
    }

    /**
     * Generates the list of {@link MatchActionOperations} objects with
     * remote-operation.
     *
     * @return the list of {@link MatchActionOperations} objects
     */
    private List<MatchActionOperations> compileRemoveOperation(
            MatchActionIdGenerator maIdGenerator,
            MatchActionOperationsIdGenerator maoIdGenerator) {
        // TODO implement it
        throw new UnsupportedOperationException(
                "REMOVE operation is not implemented yet.");
    }
}
