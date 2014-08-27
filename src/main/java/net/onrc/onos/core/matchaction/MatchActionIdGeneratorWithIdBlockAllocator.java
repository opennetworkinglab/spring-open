package net.onrc.onos.core.matchaction;

import net.onrc.onos.core.util.AbstractBlockAllocatorBasedIdGenerator;
import net.onrc.onos.core.util.IdBlockAllocator;

/**
 * Generates a global unique MatchActionId.
 */
public class MatchActionIdGeneratorWithIdBlockAllocator
        extends AbstractBlockAllocatorBasedIdGenerator<MatchActionId> {

    /**
     * Creates a FlowId generator instance using specified ID block allocator.
     *
     * @param allocator the ID block allocator to be used
     */
    public MatchActionIdGeneratorWithIdBlockAllocator(IdBlockAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MatchActionId convertFrom(long value) {
        return new MatchActionId(value);
    }
}
