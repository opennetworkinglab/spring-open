package net.onrc.onos.core.newintent;

import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.newintent.Intent;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.core.util.IdGenerator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A base class of {@link net.onrc.onos.api.newintent.IntentCompiler},
 * which generates Flow objects.
 * @param <T>
 */
public abstract class AbstractFlowGeneratingIntentCompiler<T extends Intent>
        extends AbstractIntentCompiler<T> {

    private final IdGenerator<FlowId> flowIdGenerator;

    /**
     * Constructs an object with the specified {@link IdGenerator IdGenerators} of intent ID
     * and flow ID.
     *
     * @param intentIdGenerator
     * @param flowIdGenerator
     */
    protected AbstractFlowGeneratingIntentCompiler(IdGenerator<IntentId> intentIdGenerator,
                                                   IdGenerator<FlowId> flowIdGenerator) {
        super(intentIdGenerator);
        this.flowIdGenerator = checkNotNull(flowIdGenerator);
    }

    /**
     * Returns the next {@link FlowId}.
     *
     * @return the next {@link FlowId}
     */
    protected FlowId getNextFlowId() {
        return flowIdGenerator.getNewId();
    }
}
