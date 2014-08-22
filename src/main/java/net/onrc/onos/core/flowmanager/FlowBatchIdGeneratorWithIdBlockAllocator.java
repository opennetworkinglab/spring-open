package net.onrc.onos.core.flowmanager;

import static com.google.common.base.Preconditions.checkNotNull;
import net.onrc.onos.api.flowmanager.FlowBatchId;
import net.onrc.onos.core.util.IdBlock;
import net.onrc.onos.core.util.IdBlockAllocator;
import net.onrc.onos.core.util.UnavailableIdException;

/**
 * Generates a global unique FlowBatchId using
 * {@link IdBlockAllocator#allocateUniqueIdBlock()}.
 */
public class FlowBatchIdGeneratorWithIdBlockAllocator {
    private final IdBlockAllocator allocator;
    private IdBlock idBlock;

    /**
     * Creates a FlowBatchId generator instance using specified ID block allocator.
     *
     * @param allocator the ID block allocator to be used
     */
    public FlowBatchIdGeneratorWithIdBlockAllocator(IdBlockAllocator allocator) {
        this.allocator = checkNotNull(allocator);
        this.idBlock = allocator.allocateUniqueIdBlock();
    }

    public synchronized FlowBatchId getNewId() {
        try {
            return new FlowBatchId(idBlock.getNextId());
        } catch (UnavailableIdException e) {
            idBlock = allocator.allocateUniqueIdBlock();
            return new FlowBatchId(idBlock.getNextId());
        }
    }
}
