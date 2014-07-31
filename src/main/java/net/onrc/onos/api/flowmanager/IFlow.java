package net.onrc.onos.api.flowmanager;

import net.onrc.onos.api.batchoperation.IBatchOperationTarget;
import net.onrc.onos.core.matchaction.MatchActionPlan;
import net.onrc.onos.core.matchaction.match.IMatch;

/**
 * An interface class to define flow object which is managed by
 * FlowManagerModule.
 * <p>
 * The flow objects (eg. path, tree, disjoint-paths, etc.) must implement this
 * interface.
 */
public interface IFlow extends IBatchOperationTarget {
    /**
     * Gets ID for this flow object.
     *
     * @return ID for this object.
     */
    public FlowId getId();

    /**
     * Gets traffic filter for this flow object.
     *
     * @return a traffic filter for this flow object.
     */
    public IMatch getMatch();

    /**
     * Compiles this object to MatchAction plan.
     * <p>
     * This method is called by FlowManagerModule to create MatchAction plans.
     *
     * @return a MatchAction plan of this flow object.
     */
    public MatchActionPlan compile();
}
