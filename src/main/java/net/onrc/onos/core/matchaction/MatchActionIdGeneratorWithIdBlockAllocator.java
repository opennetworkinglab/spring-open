package net.onrc.onos.core.matchaction;

import net.onrc.onos.core.util.IdBlock;
import net.onrc.onos.core.util.IdBlockAllocator;
import net.onrc.onos.core.util.UnavailableIdException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Generates a global unique MatchActionIdId.
 */
public class MatchActionIdGeneratorWithIdBlockAllocator
        implements MatchActionIdGenerator {

    private final IdBlockAllocator allocator;
    private IdBlock idBlock;

    /**
     * Creates a FlowId generator instance using specified ID block allocator.
     *
     * @param allocator the ID block allocator to be used
     */
    public MatchActionIdGeneratorWithIdBlockAllocator(IdBlockAllocator allocator) {
        this.allocator = checkNotNull(allocator);
        this.idBlock = allocator.allocateUniqueIdBlock();
    }

    @Override
    public synchronized MatchActionId getNewId() {
        try {
            return new MatchActionId(idBlock.getNextId());
        } catch (UnavailableIdException e) {
            idBlock = allocator.allocateUniqueIdBlock();
            return new MatchActionId(idBlock.getNextId());
        }
    }
}
