package net.onrc.onos.api.flowmanager;

import java.util.List;

import net.onrc.onos.core.matchaction.MatchActionPlan;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.util.PortNumber;

/**
 * IFlow object representing an optical path.
 * <p>
 * TODO: Think this: How do we deal the optical path flow going through the
 * regenerators? Can we express it with multiple OpticalPathFlow objects?
 */
public class OpticalPathFlow extends PathFlow {
    protected int lambda;

    /**
     * Constructor.
     *
     * @param id ID for this new IFlow object.
     * @param inPort Ingress port number at the ingress edge node.
     * @param path Path between ingress and egress edge node.
     * @param actions The list of Action objects at the egress edge node.
     * @param lambda The lambda to be used throughout the path.
     */
    public OpticalPathFlow(String id,
            PortNumber inPort, Path path, List<Action> actions, int lambda) {
        super(id, null, inPort, path, actions);
        this.lambda = lambda;
        // TODO Auto-generated constructor stub
    }

    /**
     * Gets lambda which is used throughout the path.
     *
     * @return lambda which is used throughout the path.
     */
    public int getLambda() {
        return lambda;
    }

    @Override
    public MatchActionPlan compile() {
        // TODO Auto-generated method stub
        return super.compile();
    }
}
