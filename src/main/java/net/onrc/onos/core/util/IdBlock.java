package net.onrc.onos.core.util;

import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A class representing an ID space.
 */
@ThreadSafe
public final class IdBlock {
    private final long start;
    private final long size;

    private AtomicLong currentId;

    /**
     * Constructs a new ID block with the specified size and initial value.
     *
     * @param start initial value of the block
     * @param size size of the block
     * @throws IllegalArgumentException if the size is less than or equal to 0
     */
    public IdBlock(long start, long size) {
        checkArgument(size > 0, "size should be more than 0, but %s", size);

        this.start = start;
        this.size = size;

        this.currentId = new AtomicLong(start);
    }

    // TODO: consider if this method is needed or not
    /**
     * Returns the initial value.
     *
     * @return initial value
     */
    public long getStart() {
        return start;
    }

    // TODO: consider if this method is needed or not
    /**
     * Returns the last value.
     *
     * @return last value
     */
    public long getEnd() {
        return start + size - 1;
    }

    /**
     * Returns the block size.
     *
     * @return block size
     */
    public long getSize() {
        return size;
    }

    /**
     * Returns the next ID in the block.
     *
     * @return next ID
     * @throws UnavailableIdException if there is no available ID in the block.
     */
    public long getNextId() {
        final long id = currentId.getAndIncrement();
        if (id > getEnd()) {
            throw new UnavailableIdException(String.format(
                    "used all IDs in allocated space (size: %d, end: %d, current: %d)",
                    size, getEnd(), id
            ));
        }

        return id;
    }

    // TODO: Do we really need equals and hashCode? Should it contain currentId?
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IdBlock that = (IdBlock) o;
        return Objects.equal(this.start, that.start)
                && Objects.equal(this.size, that.size)
                && Objects.equal(this.currentId.get(), that.currentId.get());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(start, size, currentId);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("start", start)
                .add("size", size)
                .add("currentId", currentId)
                .toString();
    }
}
