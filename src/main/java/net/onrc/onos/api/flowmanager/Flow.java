package net.onrc.onos.api.flowmanager;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import net.onrc.onos.api.batchoperation.BatchOperationTarget;
import net.onrc.onos.core.matchaction.MatchActionIdGenerator;
import net.onrc.onos.core.matchaction.MatchActionOperations;
import net.onrc.onos.core.matchaction.MatchActionOperationsIdGenerator;
import net.onrc.onos.core.matchaction.match.Match;

/**
 * An abstract class to define flow object which is managed by
 * FlowManagerModule.
 */
public abstract class Flow implements BatchOperationTarget {
    private final FlowId id;

    /**
     * Default constructor for Kryo deserialization.
     */
    @Deprecated
    protected Flow() {
        id = null;
    }

    /**
     * Creates Flow object using specified ID.
     *
     * @param id the ID to be assigned
     */
    public Flow(FlowId id) {
        this.id = checkNotNull(id);
    }

    /**
     * Gets ID for this flow object.
     *
     * @return ID for this object
     */
    public FlowId getId() {
        return id;
    }

    /**
     * Gets traffic filter for this flow object.
     *
     * @return a traffic filter for this flow object
     */
    public abstract Match getMatch();

    /**
     * Compiles this object to MatchAction operations.
     * <p>
     * This method is called by FlowManagerModule to create MatchAction
     * operations.
     *
     * @param op {@link FlowBatchOperation}'s operator to be used for compiling
     *        this object
     * @param maIdGenerator ID generator to be used for generating MatchAction
     *        objects.
     * @param maoIdGenerator ID generator to be used for generating
     *        {@link MatchActionOperations} objects.
     * @return a list of {@link MatchActionOperations} objects to realize this
     *         flow
     */
    public abstract List<MatchActionOperations> compile(
            FlowBatchOperation.Operator op,
            MatchActionIdGenerator maIdGenerator,
            MatchActionOperationsIdGenerator maoIdGenerator);

    /**
     * Generates a hash code using the FlowId.
     *
     * @return hashcode
     */
    @Override
    public int hashCode() {
        return (id == null) ? 0 : id.hashCode();
    }

    /**
     * Compares two flow objects by type (class) and FlowId.
     *
     * @param obj other Flow object
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Flow)) {
            return false;
        }
        Flow other = (Flow) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }
}
