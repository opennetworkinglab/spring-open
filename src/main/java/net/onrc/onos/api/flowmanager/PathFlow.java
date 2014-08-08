package net.onrc.onos.api.flowmanager;

import java.util.List;

import net.onrc.onos.core.matchaction.MatchActionPlan;
import net.onrc.onos.core.matchaction.action.IAction;
import net.onrc.onos.core.matchaction.match.IMatch;
import net.onrc.onos.core.util.PortNumber;

/**
 * A path flow.
 * <p>
 * TODO: Think this: Should this class be an abstract class? Is it enough to
 * have only the PacketPathFlow and OpticalPathFlow classes?
 */
public class PathFlow implements Flow {
    protected final FlowId id;
    protected IMatch match;
    protected PortNumber ingressPort;
    protected Path path;
    protected List<IAction> egressActions;

    /**
     * Constructor.
     *
     * @param id ID for this new PathFlow object.
     * @param match Match object at the ingress node of the path.
     * @param ingressPort The ingress port number at the ingress node of the
     *        path.
     * @param path Path between ingress and egress edge node.
     * @param egressActions The list of IAction objects at the egress edge node.
     */
    public PathFlow(String id,
            IMatch match, PortNumber ingressPort, Path path, List<IAction> egressActions) {
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
    public IMatch getMatch() {
        return match;
    }

    @Override
    public MatchActionPlan compile() {
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
     * Gets the list of IAction objects at the egress edge node.
     *
     * @return The list of IAction objects at the egress edge node.
     */
    public List<IAction> getEgressActions() {
        return egressActions;
    }
}
