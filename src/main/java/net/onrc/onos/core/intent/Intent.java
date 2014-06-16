package net.onrc.onos.core.intent;

import java.util.LinkedList;

import com.esotericsoftware.kryo.serializers.FieldSerializer.Optional;

/**
 * This is the base class for the connectivity abstraction. It allows applications
 * to specify end hosts, apply some basic filtering to traffic, and constrain traffic.
 * <p>
 * This class is subclassed to provide other "high level" intent types like shortest
 * path connectivity and bandwidth constrained shortest path, as well as "low level"
 * intent types like full specified/reserved path connectivity.
 * <p>
 * The reasoning behind "intent" is that the applications can provide some abstract
 * representation of how traffic should flow be handled by the networking, allowing
 * the network OS to compile, reserve and optimize the dataplane to satisfy those
 * constraints.
 */
public class Intent {
    public enum IntentState {
        /**
         * Intent has been created.
         */
        CREATED,

        /**
         * Installation of this intent has been requested.
         */
        INST_REQ,

        /**
         * Intent was not installed.
         */
        INST_NACK,

        /**
         * Intent has been successfully installed in the dataplane.
         */
        INST_ACK,

        /**
         * A delete/removal of the intent from this dataplane has been requested.
         */
        DEL_REQ,

        /**
         * The framework is in the process of removing this intent from the dataplane.
         */
        DEL_PENDING,

        /**
         * Intent has been successfully removed from the dataplane.
         */
        DEL_ACK,

        /**
         * Intent is pending reroute due to a network event.
         */
        REROUTE_REQ,
    }

    private String id;
    private IntentState state = IntentState.CREATED;
    private boolean pathFrozen = false;

    @Optional(value = "logs")
    private final LinkedList<String> logs = new LinkedList<>();

    /**
     * Default constructor for Kryo deserialization.
     */
    protected Intent() {
        logs.add(String.format("created, time:%d", System.nanoTime())); // for measurement
    }

    /**
     * Constructor.
     *
     * @param id Intent ID
     */
    public Intent(String id) {
        logs.add(String.format("created, time:%d", System.nanoTime())); // for measurement
        this.id = id;
    }

    /**
     * Constructor.
     *
     * @param id Intent ID
     * @param state current state of the new Intent
     */
    public Intent(String id, IntentState state) {
        logs.add(String.format("created, time:%d", System.nanoTime())); // for measurement
        setState(state);
        this.id = id;
    }

    /**
     * Gets the Intent ID.
     *
     * @return Intent ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the current state of this Intent.
     *
     * @return current state of this Intent
     */
    public IntentState getState() {
        return state;
    }

    /**
     * Sets the new state for this Intent, and stores the new state in
     * this Intent's log.
     *
     * @param newState new state for this Intent
     * @return the old state for this Intent
     */
    public IntentState setState(IntentState newState) {
        logs.add(String.format("setState, oldState:%s, newState:%s, time:%d",
                state, newState, System.nanoTime())); // for measurement
        if (logs.size() > 20) { // TODO this size should be configurable
            logs.removeFirst();
        }
        IntentState oldState = state;
        state = newState;
        return oldState;
    }

    /**
     * Checks to see if this Intent's path is frozen.
     * <p>
     * Frozen paths will not be rerouted when the network topology changes.
     *
     * @return true if frozen, false otherwise
     */
    public boolean isPathFrozen() {
        return pathFrozen;
    }

    /**
     * Sets the new frozen state for this Intent.
     *
     * @param isFrozen the new frozen state for this Intent
     */
    public void setPathFrozen(boolean isFrozen) {
        pathFrozen = isFrozen;
    }

    /**
     * Retrieves the logs for this intent which includes creation and state changes.
     *
     * @return a list of String log entries
     */
    public LinkedList<String> getLogs() {
        return logs;
    }

    /**
     * Generates a hash code using the Intent ID.
     *
     * @return hashcode
     */
    @Override
    public int hashCode() {
        return (id == null) ? 0 : id.hashCode();
    }

    /**
     * Compares two intent object by type (class) and Intent ID.
     *
     * @param obj other Intent
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        Intent other = (Intent) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    /**
     * Returns a String representation of this Intent.
     *
     * @return "Intent ID, State"
     */
    @Override
    public String toString() {
        return id.toString() + ", " + state.toString();
    }
}
