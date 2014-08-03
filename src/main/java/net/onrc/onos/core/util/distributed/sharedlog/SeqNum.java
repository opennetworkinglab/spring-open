package net.onrc.onos.core.util.distributed.sharedlog;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.concurrent.Immutable;

import com.google.common.annotations.Beta;
import com.google.common.primitives.Longs;
import com.google.common.primitives.UnsignedLong;
import com.google.common.primitives.UnsignedLongs;

// TODO register to Kryo
/**
 * Sequence number used as log entry key.
 */
@Beta
@Immutable
public final class SeqNum extends Number implements Comparable<SeqNum> {

    /**
     * Long value reserved for {@link #INITIAL}.
     */
    public static final long ZERO = 0L;

    /**
     * Special sequence number which will never have LogValue.
     */
    public static final SeqNum INITIAL = new SeqNum(ZERO);


    private final long seq;

    /**
     * Constructor.
     *
     * @param number sequence number.
     */
    protected SeqNum(final long number) {
        this.seq = number;
    }

    /**
     * Gets an instance for specified number.
     *
     * @param number must not be {@link #ZERO}
     * @return SeqNum
     *
     * @throws IllegalArgumentException if given number was {@link #ZERO}
     */
    public static SeqNum valueOf(final long number) {
        checkArgument(number != ZERO);
        return new SeqNum(number);
    }

    // TODO give better name?
    /**
     * Gets an instance for specified number.
     *
     * @param number sequence number. Can be {@link #ZERO}
     * @return SeqNum
     */
    public static SeqNum anyValueOf(final long number) {
        return new SeqNum(number);
    }

    /**
     * Gets the next sequence number.
     * <p>
     * WARN: This is not a atomic sequencer,
     * this method just returns the next number in sequence.
     *
     * @return next sequence number
     */
    public SeqNum next() {
        return step(1);
    }

    /**
     * Gets the previous sequence number.
     * <p>
     * WARN: This is not a atomic sequencer,
     * this method just returns the previous number in sequence.
     *
     * @return prev sequence number
     */
    public SeqNum prev() {
        return step(-1);
    }

    /**
     * Gets the sequence number stepping forward/backward by {@code delta}.
     *
     * @param delta step
     * @return sequence number
     */
    public SeqNum step(long delta) {
        long next = seq + delta;
        if (next == SeqNum.ZERO) {
            if (delta >= 0) {
                return SeqNum.valueOf(next + 1);
            } else {
                // XXX Revisit this behavior
                return SeqNum.valueOf(next - 1);
            }
        }
        return SeqNum.valueOf(next);
    }

    // TODO comparator which treats long as ring?

    @Override
    public int intValue() {
        return UnsignedLong.valueOf(seq).intValue();
    }

    @Override
    public long longValue() {
        return seq;
    }

    @Override
    public float floatValue() {
        return UnsignedLong.valueOf(seq).floatValue();
    }

    @Override
    public double doubleValue() {
        return UnsignedLong.valueOf(seq).doubleValue();
    }

    @Override
    public int hashCode() {
        return Longs.hashCode(seq);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SeqNum other = (SeqNum) obj;
        return seq == other.seq;
    }

    @Override
    public String toString() {
        return UnsignedLongs.toString(seq);
    }

    @Override
    public int compareTo(final SeqNum o) {
        checkNotNull(o);

        return Long.signum(-this.distance(o));
    }

    /**
     * Returns the distance between other and this SeqNum.
     *
     * @param other other SeqNum
     * @return {@code other - this}
     *         or -Long.MAX_VALUE, Log.MAX_VALUE if too far apart.
     */
    public long distance(final SeqNum other) {
        checkNotNull(other);

        // distance from INITIAL is always measured clockwise on the ring
        if (this.equals(INITIAL)) {
            return (other.seq >= 0) ? other.seq : Long.MAX_VALUE;
        } else if (other.equals(INITIAL)) {
            return (this.seq >= 0) ? -this.seq : -Long.MAX_VALUE;
        }

        /// other cases measures using "shorter" half of the ring
        final long diff = other.seq - this.seq;
        if (diff == Long.MIN_VALUE) {
            // both arc is same distance
            // treat arc including INITIAL as shorter
            if (this.seq < 0) {
                // clock wise arc contain INITIAL
                return Long.MAX_VALUE;
            } else {
                // counter clock wise arc contain INITIAL
                return -Long.MAX_VALUE;
            }
        } else {
            return diff;
        }
    }
}
