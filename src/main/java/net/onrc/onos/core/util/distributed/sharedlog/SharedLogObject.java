package net.onrc.onos.core.util.distributed.sharedlog;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.google.common.annotations.Beta;

import net.onrc.onos.core.util.distributed.sharedlog.internal.LogValue;
import net.onrc.onos.core.util.distributed.sharedlog.internal.SnapShotValue;

// TODO Should this be abstract class instead, to hide apply/isApplicable?
// MEMO: Something SharedObject implementor defines
/**
 * Shared object backed by shared log structure.
 *
 * Object state must be updated only by {@link #apply(SeqNum, LogValue)}
 */
@Beta
@NotThreadSafe // FIXME remove when we make these objects thread safe.
public interface SharedLogObject {

    /**
     * ID of this Shared Object.
     *
     * @return SharedLogObjectID
     */
//    @ThreadSafe // TODO find annotation for method or add javadoc
    public SharedLogObjectID getObjectID();

    /**
     * Gets the current log sequence number this instance is on.
     *
     * @return Log version of this object.
     */
//    @ThreadSafe
    public SeqNum getSeqNum();

    // FIXME Is there a good way to ensure this will be called only by Runtime
    /**
     * Apply changes to the shared object.
     * <p/>
     * Developer implementing shared object must implement this method to:
     * <ul>
     *  <li>update local instance based on given logValue if ByteValue.</li>
     *  <li>update SeqNum to given seq.</li>
     * </ul>
     * <p/>
     * Developer must also ensure that this method will never fail.
     * Any potential error check should be checked beforehand on
     * {@link #isApplicable(SeqNum, ByteValue)} call.
     * <p/>
     * Modification to this shared object instance should only happen
     * inside this method.
     * <p/>
     * This method should only be called by the runtime.
     *
     * This method will be called as a side-effect of calling
     * {@link net.onrc.onos.core.util.distributed.sharedlog.runtime
     *          .LogBasedRuntime#queryHelper(SharedLogObject)
     *        LogBasedRuntime#queryHelper(SharedLogObject)}.
     *
     * @param seq sequence number of the LogValue
     * @param logValue {@link ByteValue} to apply or NoOp
     */
    @GuardedBy("acquireWriteLock()")
    void apply(final SeqNum seq, final LogValue logValue);

    /**
     * Tests if given LogValue is applicable to this .
     * <p/>
     * This method will be called before {@link #apply(SeqNum, LogValue)} call.
     * This method should be implemented to be side-effect free.
     *
     * @param seq sequence number of the LogValue
     * @param logValue LogValue to test
     * @return true if {@code data} is applicable
     */
    @GuardedBy("acquireWriteLock()")
    public boolean isApplicable(final SeqNum seq, final ByteValue logValue);


    // TODO give me better name for SharedLogObject#reset
    /**
     * Resets the object to specified snapshot value.
     *
     * @param seq Log version of this snapshot
     * @param ssValue snapshot {@link ByteValue} to apply or NoOp representing initial state.
     */
    @GuardedBy("acquireWriteLock()")
    void reset(final SeqNum seq, final SnapShotValue ssValue);

    /**
     * Creates a snapshot value of current object.
     *
     * @return (current log version, snapshot value)
     */
    @GuardedBy("acquireReadLock()")
    ImmutablePair<SeqNum, ? extends SnapShotValue> createSnapshot();

    /**
     * Acquires read lock for this object.
     * <p/>
     * Note: Lock implementation must be reentrant.
     */
    public void acquireReadLock();

    /**
     * Releases read lock for this object.
     * <p/>
     * Note: Lock implementation must be reentrant.
     */
    public void releaseReadLock();

    /**
     * Acquires write lock for this object.
     * <p/>
     * Note: Lock implementation must be reentrant.
     */
    public void acquireWriteLock();

    /**
     * Releases write lock for this object.
     * <p/>
     * Note: Lock implementation must be reentrant.
     */
    public void releaseWriteLock();
}
