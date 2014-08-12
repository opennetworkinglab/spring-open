package net.onrc.onos.api.newintent;

import net.onrc.onos.api.batchoperation.BatchOperationTarget;

/**
 * Abstraction of an application level intent.
 *
 * Make sure that an Intent should be immutable when a new type is defined.
 */
public interface Intent extends BatchOperationTarget {
    /**
     * Returns the intent identifier.
     *
     * @return intent identifier
     */
    IntentId getId();
}
