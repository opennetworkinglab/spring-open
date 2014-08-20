package net.onrc.onos.core.flowmanager;

import static com.google.common.base.Preconditions.checkNotNull;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowIdGenerator;
import net.onrc.onos.core.util.IdBlock;
import net.onrc.onos.core.util.IdBlockAllocator;
import net.onrc.onos.core.util.UnavailableIdException;

/**
 * Generates a global unique FlowId using
 * {@link IdBlockAllocator#allocateUniqueIdBlock()}.
 */
public class FlowIdGeneratorWithIdBlockAllocator implements FlowIdGenerator {

    private final IdBlockAllocator allocator;
    private IdBlock idBlock;

    /**
     * Creates a FlowId generator instance using specified ID block allocator.
     *
     * @param allocator the ID block allocator to be used
     */
    public FlowIdGeneratorWithIdBlockAllocator(IdBlockAllocator allocator) {
        this.allocator = checkNotNull(allocator);
        this.idBlock = allocator.allocateUniqueIdBlock();
    }

    @Override
    public synchronized FlowId getNewId() {
        try {
            return new FlowId(idBlock.getNextId());
        } catch (UnavailableIdException e) {
            idBlock = allocator.allocateUniqueIdBlock();
            return new FlowId(idBlock.getNextId());
        }
    }
}
