package net.onrc.onos.apps.sdnip;

/**
 * Represents a route update received from BGPd. An update has an operation
 * describing whether the update is adding a route or revoking a route. It also
 * contains the route prefix, and {@link RibEntry} containing next hop and
 * sequence number information for the update.
 */
public class RibUpdate {
    private final Operation operation;
    private final Prefix prefix;
    private final RibEntry ribEntry;

    /**
     * Updates can either add new routes or revoke old routes. The
     * {@link Operation} enum descibes which action is being taken.
     */
    public enum Operation {
        /**
         * Represents a route update. ONOS should update its route information
         * for this prefix to the new information provided in this
         * {@link RibUpdate}. This means either add a new prefix, or update
         * the information for an existing prefix.
         */
        UPDATE,
        /**
         * Represents a route delete. ONOS should remove this prefix and route
         * information from its route table.
         */
        DELETE
    }

    /**
     * Class constructor, taking the operation of the update, the route prefix
     * and the {@link RibEntry} describing the update.
     *
     * @param operation the operation of the update
     * @param prefix the route prefix
     * @param ribEntry the update entry
     */
    public RibUpdate(Operation operation, Prefix prefix, RibEntry ribEntry) {
        this.operation = operation;
        this.prefix = prefix;
        this.ribEntry = ribEntry;
    }

    /**
     * Gets the operation of the update.
     *
     * @return the operation
     */
    public Operation getOperation() {
        return operation;
    }

    /**
     * Gets the route prefix of the update.
     *
     * @return the prefix
     */
    public Prefix getPrefix() {
        return prefix;
    }

    /**
     * Gets the {@link RibEntry} of the update.
     *
     * @return the entry
     */
    public RibEntry getRibEntry() {
        return ribEntry;
    }
}
