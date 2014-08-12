package net.onrc.onos.api.newintent;

/**
 * This interface is for generator of IntentId.
 *
 * <p>
 * {@link #getNewId()} generates a globally unique {@link IntentId} instance
 * on each invocation. Application developers should not generate IntentId
 * by themselves. Instead use an implementation of this interface.
 * </p>
 */
public interface IntentIdGenerator {
    /**
     * Generates a globally unique {@link IntentId} instance.
     *
     * @return a globally unique {@link IntentId} instance.
     */
    public IntentId getNewId();
}
