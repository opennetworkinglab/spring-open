package net.onrc.onos.api.intent;

import com.google.common.base.Objects;
import net.onrc.onos.api.batchoperation.IBatchOperationTarget;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The base class of an intent type.

 * This class is sub-classed to provide other intent types like shortest path
 * connectivity and bandwidth constrained shortest path connectivity.

 * The reasoning behind "intent" is that the applications can provide some
 * abstract representation of how traffic should flow be handled by the
 * networking, allowing the network OS to compile, reserve and optimize the
 * data-plane to satisfy those constraints.
 *
 * It is assumed that any kinds of intents are immutable.
 * Developers that will define a new intent type should ensure its immutability.
 */
public abstract class Intent implements IBatchOperationTarget {
    private final IntentId id;

    /**
     * Constructs an intent, which is activated permanently until it is removed explicitly.
     *
     * @param id ID for this intent object.
     */
    protected Intent(IntentId id) {
        this.id = checkNotNull(id);
    }

    /**
     * Returns ID for this Intent object.
     *
     * @return ID for this Intent object.
     */
    @Override
    public IntentId getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof Intent)) {
            return false;
        }

        Intent that = (Intent) obj;
        return Objects.equal(this.id, that.id);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", id)
                .toString();
    }
}
