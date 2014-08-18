package net.onrc.onos.core.newintent;

import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.api.newintent.IntentIdGenerator;
import net.onrc.onos.core.util.IdBlock;
import net.onrc.onos.core.util.UnavailableIdException;
import net.onrc.onos.core.util.IdBlockAllocator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of {@link IntentIdGenerator},
 * which uses {@link IdBlockAllocator#allocateUniqueIdBlock()}.
 */
public class IdBlockAllocatorBasedIntentIdGenerator implements IntentIdGenerator {

    private final IdBlockAllocator allocator;
    private IdBlock idBlock;

    /**
     * Constructs an intent ID generator, which uses the specified ID block allocator
     * to generate a global unique intent ID.
     *
     * @param allocator the ID block allocator to use for generating intent IDs
     */
    public IdBlockAllocatorBasedIntentIdGenerator(IdBlockAllocator allocator) {
        this.allocator = checkNotNull(allocator);
        this.idBlock = allocator.allocateUniqueIdBlock();
    }

    @Override
    public synchronized IntentId getNewId() {
        try {
            return new IntentId(idBlock.getNextId());
        } catch (UnavailableIdException e) {
            idBlock = allocator.allocateUniqueIdBlock();
            return new IntentId(idBlock.getNextId());
        }
    }
}