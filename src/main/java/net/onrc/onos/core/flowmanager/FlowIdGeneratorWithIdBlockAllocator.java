package net.onrc.onos.core.flowmanager;

import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.core.util.AbstractBlockAllocatorBasedIdGenerator;
import net.onrc.onos.core.util.IdBlockAllocator;

/**
 * Generates a global unique FlowId using
 * {@link IdBlockAllocator#allocateUniqueIdBlock()}.
 */
public class FlowIdGeneratorWithIdBlockAllocator
        extends AbstractBlockAllocatorBasedIdGenerator<FlowId> {

    /**
     * Creates a FlowId generator instance using specified ID block allocator.
     *
     * @param allocator the ID block allocator to be used
     */
    public FlowIdGeneratorWithIdBlockAllocator(IdBlockAllocator allocator) {
        super(allocator);
    }

    @Override
    protected FlowId convertFrom(long value) {
        return new FlowId(value);
    }
}
