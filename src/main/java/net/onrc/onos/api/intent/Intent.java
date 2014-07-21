package net.onrc.onos.api.intent;

import java.util.List;

import net.onrc.onos.api.batchoperation.BatchOperation;
import net.onrc.onos.api.batchoperation.IBatchOperationTarget;
import net.onrc.onos.api.flowmanager.IFlow;

/**
 * The base class for the connectivity abstraction. It allows applications to
 * specify end hosts, apply some basic filtering to traffic, and constrain
 * traffic.
 * <p>
 * This class is sub-classed to provide other intent types like shortest path
 * connectivity and bandwidth constrained shortest path connectivity.
 * <p>
 * The reasoning behind "intent" is that the applications can provide some
 * abstract representation of how traffic should flow be handled by the
 * networking, allowing the network OS to compile, reserve and optimize the
 * data-plane to satisfy those constraints.
 */
public abstract class Intent implements IBatchOperationTarget {
    protected final IntentId id;

    /**
     * Constructor.
     *
     * @param id ID for this Intent object.
     */
    public Intent(String id) {
        this.id = new IntentId(id);
    }

    /**
     * Gets ID for this Intent object.
     *
     * @return ID for this Intent object.
     */
    @Override
    public IntentId getId() {
        return id;
    }

    /**
     * Compiles this Intent object to the list of FlowOperations.
     * <p>
     * All Intent object must implement this method and IntentRuntimeModule use
     * this method to process this Intent.
     *
     * @return The list of FlowOperations of this Intent.
     */
    public abstract List<BatchOperation<IFlow>> compile();
}
