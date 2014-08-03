package net.onrc.onos.core.util.distributed.sharedlog.example;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.concurrent.GuardedBy;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.onrc.onos.core.datastore.utils.KryoSerializer;
import net.onrc.onos.core.datastore.utils.Serializer;
import net.onrc.onos.core.util.distributed.sharedlog.ByteValue;
import net.onrc.onos.core.util.distributed.sharedlog.SeqNum;
import net.onrc.onos.core.util.distributed.sharedlog.SharedLogObject;
import net.onrc.onos.core.util.distributed.sharedlog.SharedLogObjectID;
import net.onrc.onos.core.util.distributed.sharedlog.exception.LogNotApplicable;
import net.onrc.onos.core.util.distributed.sharedlog.exception.LogWriteTimedOut;
import net.onrc.onos.core.util.distributed.sharedlog.internal.LogValue;
import net.onrc.onos.core.util.distributed.sharedlog.internal.NoOp;
import net.onrc.onos.core.util.distributed.sharedlog.internal.SnapShotValue;
import net.onrc.onos.core.util.distributed.sharedlog.runtime.LogBasedRuntime;

/**
 * Example implementing CAS-able Long as {@link SharedLogObject}.
 * <p>
 * This is just an example implementation of SharedLogObject,
 * not intended for actual use.
 */
public class LogAtomicLong implements SharedLogObject {

    private static final Logger log = LoggerFactory
            .getLogger(LogAtomicLong.class);

    private static final Serializer SERIALIZER = new KryoSerializer(
            SetEvent.class,
            CompareAndSetEvent.class);


    private final LogBasedRuntime runtime;
    private final SharedLogObjectID oid;

    private volatile SeqNum current;
    private long value;

    // adding volatile to `value` is good-enough for this example in reality
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private Lock readLock = readWriteLock.readLock();
    private Lock writeLock = readWriteLock.writeLock();


    /**
     * Constructor to create instance synced to latest snapshot.
     *
     * @param runtime {@link LogBasedRuntime} to use
     * @param name name of this {@link SharedLogObject}
     */
    public LogAtomicLong(LogBasedRuntime runtime, String name) {
        this(runtime, checkNotNull(runtime).getOid(name));
    }

    /**
     * Constructor to create instance synced to latest snapshot.
     *
     * @param runtime {@link LogBasedRuntime} to use
     * @param name name of this {@link SharedLogObject}
     * @param ssId snap shot ID to initialize to
     */
    public LogAtomicLong(LogBasedRuntime runtime, String name, SeqNum ssId) {
        this(runtime, checkNotNull(runtime).getOid(name), ssId);
    }

    /**
     * Constructor to create instance synced to latest snapshot.
     *
     * @param runtime {@link LogBasedRuntime} to use
     * @param oid {@link SharedLogObjectID} of this object
     */
    protected LogAtomicLong(LogBasedRuntime runtime, SharedLogObjectID oid) {
        this(runtime, oid, checkNotNull(runtime).getLatestSnapShotId(oid));
    }

    /**
     * Constructor to initialize to specified snap shot.
     *
     * @param runtime {@link LogBasedRuntime} to use
     * @param oid {@link SharedLogObjectID} of this object
     * @param ssId snap shot ID to initialize to
     */
    protected LogAtomicLong(LogBasedRuntime runtime, SharedLogObjectID oid, SeqNum ssId) {
        this.runtime = checkNotNull(runtime);
        this.oid = checkNotNull(oid);
        boolean success = runtime.resetToSnapShot(this, ssId);
        if (!success) {
            throw new IllegalStateException(
                    "Failed to initialize to specified Snapshot " + ssId);
        }
    }


    /**
     * LogMessage for {@link LogAtomicLong#set(long)}.
     */
    private static final class SetEvent {

        private final long newValue;

        /**
         * Constructor.
         *
         * @param newValue value to set
         */
        public SetEvent(long newValue) {
            this.newValue = newValue;
        }

        /**
         * Gets the new value to set.
         *
         * @return new value
         */
        public long getNewValue() {
            return newValue;
        }

        /**
         * Default constructor for deserializer.
         */
        @SuppressWarnings("unused")
        @Deprecated
        public SetEvent() {
            this.newValue = 0L;
        }
    }

    /**
     * LogMessage for {@link LogAtomicLong#compareAndSet(long, long)}.
     */
    private static final class CompareAndSetEvent {

        private final long expect;
        private final long update;

        /**
         * Constructor.
         *
         * @param expect expected value
         * @param update new value
         */
        public CompareAndSetEvent(long expect, long update) {
            this.expect = expect;
            this.update = update;
        }

        /**
         * Gets the expected value.
         *
         * @return expected value
         */
        public long getExpect() {
            return expect;
        }

        /**
         * Gets the new value to update.
         *
         * @return new value to update
         */
        public long getUpdate() {
            return update;
        }

        /**
         * Default constructor for deserializer.
         */
        @SuppressWarnings("unused")
        @Deprecated
        public CompareAndSetEvent() {
            this.expect = 0L;
            this.update = 0L;
        }
    }


    /**
     * Sets the specified value regardless of current value.
     *
     * @param newValue new value to set
     */
    public void set(long newValue) {
        ByteValue newLog = serialize(new SetEvent(newValue));

        try {
            // no need to replay to latest log (3rd option)
            runtime.updateHelper(this, newLog, false);
        } catch (LogNotApplicable e) {
            log.error("Should never happen", e);
        } catch (LogWriteTimedOut e) {
            log.warn("Timeout, retrying", e);
            set(newValue);
        }
    }

    /**
     * Compare and set new value.
     *
     * @param expect value expected
     * @param update new value
     * @return true if successfully updated
     */
    public boolean compareAndSet(long expect, long update) {
        ByteValue newLog = serialize(new CompareAndSetEvent(expect, update));

        try {
            runtime.updateHelper(this, newLog, true);
            return true;
        } catch (LogNotApplicable e) {
            return false;
        } catch (LogWriteTimedOut e) {
            log.warn("Timeout, retrying", e);
            return compareAndSet(expect, update);
        }
    }

    /**
     * Serializes SetEvent to ByteValue.
     *
     * @param event {@link SetEvent}
     * @return {@link ByteValue}
     */
    private ByteValue serialize(SetEvent event) {
        return new ByteValue(SERIALIZER.serialize(event));
    }

    /**
     * Serializes CompareAndSetEvent to ByteValue.
     *
     * @param event {@link CompareAndSetEvent}
     * @return {@link ByteValue}
     */
    private ByteValue serialize(CompareAndSetEvent event) {
        return new ByteValue(SERIALIZER.serialize(event));
    }

    /**
     * Gets the current value.
     *
     * @return current value
     */
    public long get() {
        runtime.queryHelper(this);
        acquireReadLock();
        try {
            return value;
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public String toString() {
        return Long.toString(get());
    }

    @Override
    public SharedLogObjectID getObjectID() {
        return this.oid;
    }

    @Override
    public SeqNum getSeqNum() {
        return this.current;
    }

    @GuardedBy("writeLock")
    @Override
    public void apply(SeqNum seq, LogValue logValue) {
        // This should be the only method modifying this instance's field

        if (logValue instanceof ByteValue) {
            ByteValue byteValue = (ByteValue) logValue;
            final Object event = SERIALIZER.deserialize(byteValue.getBytes());
            if (event instanceof SetEvent) {
                applySetEvent(seq, (SetEvent) event);
            }
            if (event instanceof CompareAndSetEvent) {
                applyCompareAndSetEvent(seq, (CompareAndSetEvent) event);
            }

        } else if (logValue instanceof NoOp) {
            this.current = seq;
        }
    }

    /**
     * Applies {@link CompareAndSetEvent}.
     *
     * @param seq sequence number of event
     * @param casEvent {@link CompareAndSetEvent}
     */
    @GuardedBy("writeLock")
    private void applyCompareAndSetEvent(SeqNum seq, final CompareAndSetEvent casEvent) {
        assert (this.value == casEvent.expect);
        this.current = seq;
        this.value = casEvent.getUpdate();
    }

    /**
     * Applies {@link SetEvent}.
     *
     * @param seq sequence number of event
     * @param setEvent {@link SetEvent}
     */
    @GuardedBy("writeLock")
    private void applySetEvent(SeqNum seq, final SetEvent setEvent) {
        this.current = seq;
        this.value = setEvent.getNewValue();
    }

    @GuardedBy("writeLock")
    @Override
    public void reset(SeqNum seq, SnapShotValue ssValue) {
        if (ssValue instanceof ByteValue) {
            ByteValue byteValue = (ByteValue) ssValue;
            final Object event = SERIALIZER.deserialize(byteValue.getBytes());
            if (event instanceof SetEvent) {
                applySetEvent(seq, (SetEvent) event);
            } else {
                log.error("Unexpected SnapShot ByteValue encountered {}", event);
            }

        } else if (ssValue instanceof NoOp) {
            this.current = seq;
            this.value = 0L;
        } else {
            log.error("Unexpected SnapShotValue encountered {}", ssValue);
        }
    }

    @GuardedBy("readLock")
    @Override
    public ImmutablePair<SeqNum, ? extends SnapShotValue> createSnapshot() {
        return ImmutablePair.of(getSeqNum(), serialize(new SetEvent(get())));
    }

    @GuardedBy("readLock")
    @Override
    public boolean isApplicable(SeqNum seq, ByteValue logValue) {

        final Object event = SERIALIZER.deserialize(logValue.getBytes());
        if (event instanceof SetEvent) {
            return true;
        }
        if (event instanceof CompareAndSetEvent) {
            CompareAndSetEvent casEvent = (CompareAndSetEvent) event;
            return casEvent.getExpect() == value;
        }
        return false;
    }

    @Override
    public void acquireReadLock() {
        this.readLock.lock();
    }

    @Override
    public void releaseReadLock() {
        this.readLock.unlock();
    }

    @Override
    public void acquireWriteLock() {
        this.writeLock.lock();
    }

    @Override
    public void releaseWriteLock() {
        this.writeLock.unlock();
    }
}
