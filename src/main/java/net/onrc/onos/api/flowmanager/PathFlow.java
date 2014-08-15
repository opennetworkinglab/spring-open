package net.onrc.onos.api.flowmanager;

import java.util.List;

import net.onrc.onos.core.matchaction.MatchActionOperations;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.util.PortNumber;

/**
 * A path flow.
 * <p>
 * TODO: Think this: Should this class be an abstract class? Is it enough to
 * have only the PacketPathFlow and OpticalPathFlow classes?
 */
public class PathFlow implements Flow {
    protected final FlowId id;
    protected Match match;
    protected PortNumber ingressPort;
    protected Path path;
    protected List<Action> egressActions;

    /**
     * Constructor.
     *
     * @param id ID for this new PathFlow object.
     * @param match Match object at the ingress node of the path.
     * @param ingressPort The ingress port number at the ingress node of the
     *        path.
     * @param path Path between ingress and egress edge node.
     * @param egressActions The list of Action objects at the egress edge node.
     */
    public PathFlow(String id,
            Match match, PortNumber ingressPort, Path path, List<Action> egressActions) {
        this.id = new FlowId(id);
        this.match = match;
        this.ingressPort = ingressPort;
        this.path = path;
        this.egressActions = egressActions;
    }

    @Override
    public FlowId getId() {
        return id;
    }

    @Override
    public Match getMatch() {
        return match;
    }

    @Override
    public MatchActionOperations compile() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the ingress port number at the ingress node of the path.
     *
     * @return The ingress port number at the ingress node of the path.
     */
    public PortNumber getIngressPortNumber() {
        return ingressPort;
    }

    /**
     * Gets the path from ingress to egress edge node.
     *
     * @return The path object from ingress to egress edge node.
     */
    public Path getPath() {
        return path;
    }

    /**
     * Gets the list of Action objects at the egress edge node.
     *
     * @return The list of Action objects at the egress edge node.
     */
    public List<Action> getEgressActions() {
        return egressActions;
    }
}
