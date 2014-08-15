package net.onrc.onos.core.newintent;

import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowIdGenerator;
import net.onrc.onos.api.newintent.Intent;
import net.onrc.onos.api.newintent.IntentIdGenerator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A base class of {@link net.onrc.onos.api.newintent.IntentCompiler},
 * which generates Flow objects.
 * @param <T>
 */
public abstract class AbstractFlowGeneratingIntentCompiler<T extends Intent>
        extends AbstractIntentCompiler<T> {

    private final FlowIdGenerator flowIdGenerator;

    /**
     * Constructs an object with the specified {@link IntentIdGenerator}
     * and {@link FlowIdGenerator}.
     *
     * @param intentIdGenerator
     * @param flowIdGenerator
     */
    protected AbstractFlowGeneratingIntentCompiler(IntentIdGenerator intentIdGenerator,
                                                   FlowIdGenerator flowIdGenerator) {
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
