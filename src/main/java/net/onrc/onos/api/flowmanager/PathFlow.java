package net.onrc.onos.api.flowmanager;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.LinkedList;
import java.util.List;

import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.util.PortNumber;

/**
 * An abstract class expressing a path flow.
 */
public abstract class PathFlow extends Flow {
    private final PortNumber ingressPort;
    private final Path path;
    private final List<Action> egressActions;

    /**
     * Default constructor for Kryo deserialization.
     */
    @Deprecated
    protected PathFlow() {
        ingressPort = null;
        path = null;
        egressActions = null;
    }

    /**
     * Creates the new flow instance.
     *
     * @param id ID for this new PathFlow object
     * @param ingressPort the ingress port number at the ingress node of the
     *        path
     * @param path the Path between ingress and egress edge node
     * @param egressActions the list of Action objects at the egress edge node
     */
    public PathFlow(FlowId id,
            PortNumber ingressPort, Path path, List<Action> egressActions) {
        super(id);
        this.ingressPort = checkNotNull(ingressPort);
        this.path = new Path(checkNotNull(path));
        this.egressActions = new LinkedList<>(checkNotNull(egressActions));
    }

    /**
     * Gets the ingress port number at the ingress node of the path.
     *
     * @return the ingress port number at the ingress node of the path
     */
    public PortNumber getIngressPortNumber() {
        return ingressPort;
    }

    /**
     * Gets the path from ingress to egress edge node.
     *
     * @return the path object from ingress to egress edge node
     */
    public Path getPath() {
        return path;
    }

    /**
     * Gets the list of Action objects at the egress edge node.
     *
     * @return the list of Action objects at the egress edge node
     */
    public List<Action> getEgressActions() {
        return egressActions;
    }
}
