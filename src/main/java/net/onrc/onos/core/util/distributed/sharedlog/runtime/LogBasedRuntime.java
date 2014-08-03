package net.onrc.onos.core.util.distributed.sharedlog.runtime;

import java.util.List;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.annotations.Beta;

import net.onrc.onos.core.util.distributed.sharedlog.ByteValue;
import net.onrc.onos.core.util.distributed.sharedlog.LogEventListener;
import net.onrc.onos.core.util.distributed.sharedlog.SeqNum;
import net.onrc.onos.core.util.distributed.sharedlog.SharedLogObject;
import net.onrc.onos.core.util.distributed.sharedlog.SharedLogObjectID;
import net.onrc.onos.core.util.distributed.sharedlog.exception.LogNotApplicable;
import net.onrc.onos.core.util.distributed.sharedlog.exception.LogWriteTimedOut;
import net.onrc.onos.core.util.distributed.sharedlog.internal.LogValue;


/**
 * Runtime to read/write shared object using shared log.
 */
@Beta
@ThreadSafe
public interface LogBasedRuntime {

    // FIXME better method name
    /**
     * Assign or get already assigned SharedLogObject ID.
     *
     * @param sharedObjectName The name of the shared log object
     * @return SharedLogObjectID
     */
    public SharedLogObjectID getOid(final String sharedObjectName);

    /**
     * Apply given logValue to SharedLogObject.
     *
     * @param sobj SharedLogObject to manipulate.
     * @param logValue LogValue to apply
     * @param queryBeforeUpdate true if log should be replayed to latest before updating
     * @return sequence number of the new log entry
     *
     * @throws LogNotApplicable Thrown when {@code logValue} was not applicable to
     *          SharedLogObject.
     * @throws LogWriteTimedOut Thrown when write failed due to time out.
     */
    public SeqNum updateHelper(final SharedLogObject sobj, final ByteValue logValue,
                             final boolean queryBeforeUpdate)
                                throws LogNotApplicable, LogWriteTimedOut;

    /**
     * Reads from the shared log and
     * updates the {@link SharedLogObject} to latest state,
     * by calling {@link SharedLogObject#apply(SeqNum, LogValue)}.
     *
     * @param sobj SharedLogObject to update.
     * @throws LogNotApplicable
     */
    public void queryHelper(final SharedLogObject sobj);


    /**
     * Reads from the shared log and
     * updates the {@link SharedLogObject} to replayTo,
     * by calling {@link SharedLogObject#apply(SeqNum, LogValue)}.
     *
     * @param sobj SharedLogObject to update.
     * @param replayTo Sequence number to replay to. (must be <= latest)
     */
    public void queryHelper(final SharedLogObject sobj, final SeqNum replayTo);

    // TODO we may need to provide a way to do manual update (getSeqNum, update)


    // Snapshot related interfaces

    // TODO should there be a public interface to create snapshot?
    // If we expose to public, how to determine "latest" may have issue

    /**
     * Gets the latest snapshot ID.
     *
     * @param oid {@link SharedLogObjectID}
     * @return latest snapshot ID
     */
    public SeqNum getLatestSnapShotId(final SharedLogObjectID oid);

    // TODO give me better name for LogBaseRuntime#resetToSnapShot
    /**
     * Updates the {@link SharedLogObject} to specified snapshot.
     *
     * @param sobj {@link SharedLogObject} to update
     * @param ssId snap shot ID
     * @return true if success, false otherwise.
     */
    public boolean resetToSnapShot(final SharedLogObject sobj, final SeqNum ssId);


    // TODO should LogRuntime be responsible for polling, to detect dropped notification.
    /**
     * Adds the listener for specified shared object log event.
     *
     * @param oid {@link SharedLogObjectID}
     * @param listener to add
     */
    public void addListener(SharedLogObjectID oid, LogEventListener listener);

    /**
     * Removes the listener for specified shared object log event.
     *
     * @param oid {@link SharedLogObjectID}
     * @param listener to add
     */
    public void removeListener(SharedLogObjectID oid, LogEventListener listener);

    // TODO might not need this any more
    /**
     * Gets the LogValues in range ({@code after}, {@code upToThis}].
     *
     * @param oid ID of SharedLogObject
     * @param after sequence number before (typically current sequence number)
     * @param upToThis last sequence number you want to retrieve
     * @return List of {@link LogValue}s in specified range
     */
    public List<LogValue> getLogRange(final SharedLogObjectID oid,
                                    final SeqNum after, final SeqNum upToThis);
}
