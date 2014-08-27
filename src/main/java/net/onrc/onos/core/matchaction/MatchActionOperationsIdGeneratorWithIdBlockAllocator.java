package net.onrc.onos.core.matchaction;

import net.onrc.onos.core.util.AbstractBlockAllocatorBasedIdGenerator;
import net.onrc.onos.core.util.IdBlockAllocator;


/**
 * Generates a global unique MatchActionIdId.
 */
public class MatchActionOperationsIdGeneratorWithIdBlockAllocator
        extends AbstractBlockAllocatorBasedIdGenerator<MatchActionOperationsId> {

    /**
     * Creates a FlowId generator instance using specified ID block allocator.
     *
     * @param allocator the ID block allocator to be used
     */
    public MatchActionOperationsIdGeneratorWithIdBlockAllocator(IdBlockAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MatchActionOperationsId convertFrom(long value) {
        return new MatchActionOperationsId(value);
    }
}
