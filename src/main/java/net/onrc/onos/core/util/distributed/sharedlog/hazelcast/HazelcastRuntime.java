package net.onrc.onos.core.util.distributed.sharedlog.hazelcast;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.onrc.onos.core.util.distributed.sharedlog.ByteValue;
import net.onrc.onos.core.util.distributed.sharedlog.LogEventListener;
import net.onrc.onos.core.util.distributed.sharedlog.SeqNum;
import net.onrc.onos.core.util.distributed.sharedlog.Sequencer;
import net.onrc.onos.core.util.distributed.sharedlog.SharedLogObject;
import net.onrc.onos.core.util.distributed.sharedlog.SharedLogObjectID;
import net.onrc.onos.core.util.distributed.sharedlog.exception.LogNotApplicable;
import net.onrc.onos.core.util.distributed.sharedlog.exception.LogWriteTimedOut;
import net.onrc.onos.core.util.distributed.sharedlog.internal.LogValue;
import net.onrc.onos.core.util.distributed.sharedlog.internal.SnapShotValue;
import net.onrc.onos.core.util.distributed.sharedlog.internal.StringID;
import net.onrc.onos.core.util.distributed.sharedlog.internal.NoOp;
import net.onrc.onos.core.util.distributed.sharedlog.runtime.LogBasedRuntime;
import net.onrc.onos.core.util.distributed.sharedlog.runtime.SequencerRuntime;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.IFunction;
import com.hazelcast.core.IMap;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;


/**
 * LogBasedRuntime using Hazelcast.
 */
@Beta
public class HazelcastRuntime implements LogBasedRuntime {

    private static final Logger log = LoggerFactory
            .getLogger(HazelcastRuntime.class);

    // Hazelcast distributed object name prefixes
    private static final String LATEST_SNAPSHOT_PREFIX = "latestSS://";
    private static final String SNAPSHOTMAP_PREFIX = "ssmap://";
    private static final String LOGMAP_PREFIX = "logmap://";

    // FIXME set appropriate timeout & make it configurable
    private static final long LOG_READ_TIMEOUT_MS = 1000L;

    // FIXME make these configurable from file, etc.
    /**
     * Number of times to retry, if write to LogMap failed due to
     * other node invalidated the sequencer due to timeout.
     */
    private static final int UPDATE_RETRIES = 5;

    /**
     * Interval to check if we should create SnapShot.
     * <p>
     * If the sequence number of the log just written is multiple of this value,
     * SnapShot check and log cleanup will be triggered.
     */
    private static final int SNAPSHOT_CHECK_INTERVAL = 50;

    /**
     * Interval SnapShot should be created.
     * <p>
     * SnapShot will be created if latest SnapShot was more than
     * this interval apart from last written.
     */
    public static final int SNAPSHOT_INTERVAL = 500;

    /**
     * Maximum number of Snapshots to store in distributed Map.
     */
    public static final int MAX_SNAPSHOTS = 10;

    // TODO create ThreadFactory to assign thread name and lower priority
    // executor for SnapShot builder
    private static final ExecutorService EXECUTOR =
            new ThreadPoolExecutor(1, 5, 3L, TimeUnit.MINUTES,
                    new ArrayBlockingQueue<Runnable>(10),
                    new ThreadPoolExecutor.DiscardOldestPolicy());

    private final HazelcastInstance instance;
    private final SequencerRuntime sequencerRuntime;

    private final ConcurrentMap<SharedLogObjectID, LogMapManager>
                    listenerMap;

    /**
     * Initializes Hazelcast based LogRuntime.
     *
     * @param instance Hazelcast instance to use
     * @param sequencerRuntime {@link SequencerRuntime} to use
     */
    public HazelcastRuntime(HazelcastInstance instance,
                            SequencerRuntime sequencerRuntime) {

        this.instance = checkNotNull(instance);
        this.sequencerRuntime = checkNotNull(sequencerRuntime);
        this.listenerMap = new ConcurrentHashMap<>();
    }

    @Override
    public SharedLogObjectID getOid(String sharedObjectName) {

        // TODO Create specialized SharedLogObject, if getting IMap from
        //      HZ instance each time becomes a significant overhead.

        return new StringID(sharedObjectName);
    }

    @Override
    public SeqNum updateHelper(SharedLogObject sobj, ByteValue logValue,
                                boolean queryBeforeUpdate)
                                   throws LogNotApplicable, LogWriteTimedOut {

        return updateHelper(sobj, logValue, queryBeforeUpdate, UPDATE_RETRIES);
    }

    /**
     * Apply given logValue to SharedLogObject.
     *
     * @param sobj SharedLogObject to manipulate.
     * @param logValue LogValue to apply
     * @param queryBeforeUpdate true if log should be replayed to latest before updating
     * @param retries number of retries
     * @return sequence number of the new log entry
     *
     * @throws LogNotApplicable Thrown when {@code logValue} was not applicable to
     *          SharedLogObject.
     * @throws LogWriteTimedOut Thrown when write failed due to time out.
     */
    private SeqNum updateHelper(final SharedLogObject sobj, ByteValue logValue,
                                boolean queryBeforeUpdate, int retries)
                                   throws LogNotApplicable, LogWriteTimedOut {

        checkNotNull(sobj);
        checkNotNull(logValue);

        log.trace("updating {}@{} with {}", sobj.getObjectID(), sobj.getSeqNum(),
                    logValue);

        final Sequencer sequencer = sequencerRuntime.getSequencer(sobj.getObjectID());
        final IMap<SeqNum, LogValue> logMap = getLogMap(sobj);

        // allocate seq #
        final SeqNum allocated = sequencer.next();
        log.trace("allocated {}", allocated);

        // replay
        if (queryBeforeUpdate) {
            // FIXME How to handle initial case?
            queryHelper(sobj, allocated.prev());
        }

        // test if applicable
        sobj.acquireWriteLock();
        try {
            final boolean isApplicable = sobj.isApplicable(allocated, logValue);
            if (!isApplicable) {
                log.trace("log not applicable abondoning {}", allocated);
                logMap.putIfAbsent(allocated, NoOp.VALUE);
                throw new LogNotApplicable("Rejected by " + sobj.getObjectID());
            }

            // write to shared log
            LogValue existing = logMap.putIfAbsent(allocated, logValue);
            if (NoOp.VALUE.equals(existing)) {
                if (retries > 0) {
                    log.trace("write failed due to time out retrying {}", retries);
                    return updateHelper(sobj, logValue, queryBeforeUpdate, retries - 1);
                }
                throw new LogWriteTimedOut("Was timed out by other node by " + sobj.getObjectID());
            }

            // apply to local object
            sobj.apply(allocated, logValue);

            // Success.

            // FIXME Current design is that any SharedObject instance could
            // become SnapShot writer/Log cleaner.
            // We may need provide a way for a SharedObject instance
            // to declare that it does not want to become
            // a SnapShot writer (Don't wont unexpected ReadLock, etc.)

            // give hint to snapshot builder
            if (allocated.longValue() % SNAPSHOT_CHECK_INTERVAL == 0) {
                // check and create snapshot in background
                EXECUTOR.execute(new Runnable() {

                    @Override
                    public void run() {
                        createSnapShot(sobj);
                    }
                });
            }
            return allocated;

        } finally {
            sobj.releaseWriteLock();
        }
    }

    /**
     * Updates latest snapshot pointer.
     * <p>
     * IFunction to apply to IAtomicLong used as latest snapshot pointer.
     */
    public static final class UpdateLatestSnapshot implements
                                IFunction<Long, Long>, DataSerializable {

        private SeqNum ssCreated;

        /**
         * Updates latest snapshot pointer to {@code ssCreated}.
         *
         * @param ssCreated value to update to
         */
        public UpdateLatestSnapshot(SeqNum ssCreated) {
            this.ssCreated = ssCreated;
        }

        @Override
        public Long apply(Long input) {
            final SeqNum in = SeqNum.anyValueOf(input);

            if (ssCreated.compareTo(in) > 0) {
                // update if snapshot written is >= existing latest SS.
                return ssCreated.longValue();
            } else {
                return input;
            }
        }

        @Override
        public void writeData(ObjectDataOutput out) throws IOException {
            out.writeLong(ssCreated.longValue());
        }

        @Override
        public void readData(ObjectDataInput in) throws IOException {
            this.ssCreated = SeqNum.valueOf(in.readLong());
        }

        /**
         * Default constructor for deserialization.
         */
        public UpdateLatestSnapshot() {
            this.ssCreated = SeqNum.INITIAL;
        }
    }

    /**
     * Creates a snapshot of {@link SharedLogObject} if
     * latest snapshot is old.
     *
     * @param sobj {@link SharedLogObject} to create snapshot.
     */
    private void createSnapShot(SharedLogObject sobj) {
        if (sobj == null) {
            log.warn("Tried to create snapshot of null object. Ignoring.");
            return;
        }
        log.trace("Checking if {} needs a new snapshot.", sobj.getObjectID());

        // XXX lock can be more fine grained if necessary
        final IAtomicLong rawLatest = getLatestSnapshotStore(sobj.getObjectID());
        sobj.acquireReadLock();
        try {
            final SeqNum current = sobj.getSeqNum();
            final SeqNum latest = SeqNum.anyValueOf(rawLatest.get());

            // if distance(current - latest) > SNAPSHOT_INTERVAL
            if (latest.distance(current) < SNAPSHOT_INTERVAL) {
                // not the time to create snap shot yet
                log.trace("Skip creating snapshot. current:{}, latest:{}", current, latest);
                return;
            }
            // create SnapShot Bytes
            final ImmutablePair<SeqNum, ? extends SnapShotValue> snapshot
                = sobj.createSnapshot();

            // write to SnapShot Map
            IMap<SeqNum, SnapShotValue> ssMap = getSnapshotMap(sobj);
            final SeqNum ssCreated = snapshot.getKey();
            // XXX is there a way to do setIfAbsent
            ssMap.set(ssCreated, snapshot.getValue());
            log.info("Created snapshot. {}@{}", sobj.getObjectID(), ssCreated);

            // update latest snapshot pointer
            rawLatest.alter(new UpdateLatestSnapshot(ssCreated));

        } finally {
            sobj.releaseReadLock();
        }

        // XXX Should we be triggering log clean up in same context?
        IMap<SeqNum, SnapShotValue> ssMap = getSnapshotMap(sobj);

        // note: snapshots is a copy of keySet unlike usual Map
        Set<SeqNum> snapshots = ssMap.keySet();
        if (snapshots.size() > MAX_SNAPSHOTS) {
            // FIXME we should be able to avoid prev() walking if we can
            // always safely compare orders of SeqNums.

            SeqNum latest = SeqNum.valueOf(rawLatest.get());
            snapshots.remove(latest);
            SeqNum remove = latest.prev();
            while (snapshots.size() > MAX_SNAPSHOTS) {
                snapshots.remove(remove);
                remove = remove.prev();
            }

            // what's still left in `snapshots` is subject to removal.
            // delete snapshots and find log SeqNum to start removing
            SeqNum deleteBeforeThis = null;
            for (SeqNum ss : snapshots) {
                log.debug("Removed snapshot {}@{}", sobj.getObjectID(), ss);
                // XXX If we decide to persist snapshots for reference,
                //     evict() ing here is another option.
                ssMap.delete(ss);
                if (deleteBeforeThis == null) {
                    deleteBeforeThis = ss;
                } else {
                    if (deleteBeforeThis.compareTo(ss) > 0) {
                        deleteBeforeThis = ss;
                    }
                }
            }
            if (deleteBeforeThis == null) {
                // nothing to do
                return;
            }

            // start removing log
            IMap<SeqNum, LogValue> logMap = getLogMap(sobj);
            // naive log cleaning
            log.debug("Trimming log before {}@{}...", sobj.getObjectID(), deleteBeforeThis);
            while (logMap.remove(deleteBeforeThis) != null) {
                deleteBeforeThis = deleteBeforeThis.prev();
            }
            log.debug("Trimming log up to this{}@{}...", sobj.getObjectID(), deleteBeforeThis);
        }
    }

    /**
     * Gets the Log Map for specified shared object.
     *
     * @param sobj {@link SharedLogObject}
     * @return Log Map
     */
    private IMap<SeqNum, LogValue> getLogMap(SharedLogObject sobj) {
        return getLogMap(sobj.getObjectID());
    }

    /**
     * Gets the Log Map for specified shared object ID.
     *
     * @param oid {@link SharedLogObjectID}
     * @return Log Map
     */
    private IMap<SeqNum, LogValue> getLogMap(SharedLogObjectID oid) {
        return instance.getMap(LOGMAP_PREFIX + oid.getObjectName());
    }

    /**
     * Gets the Snapshot Map for specified shared object.
     *
     * @param sobj {@link SharedLogObject}
     * @return Snapshot Map
     */
    private IMap<SeqNum, SnapShotValue> getSnapshotMap(SharedLogObject sobj) {
        return instance.getMap(SNAPSHOTMAP_PREFIX + sobj.getObjectID().getObjectName());
    }

    /**
     * Gets the store holding latest snapshot version.
     *
     * @param oid {@link SharedLogObjectID}
     * @return IAtomicLong
     */
    private IAtomicLong getLatestSnapshotStore(SharedLogObjectID oid) {
        return instance.getAtomicLong(LATEST_SNAPSHOT_PREFIX + oid.getObjectName());
    }

    @Override
    public void queryHelper(SharedLogObject sobj) {
        queryHelper(checkNotNull(sobj), sequencerRuntime.getSequencer(sobj.getObjectID()).get());
    }

    @Override
    public void queryHelper(SharedLogObject sobj, SeqNum replayTo) {

        checkNotNull(sobj);
        checkNotNull(replayTo);
        // TODO check if sobj.getSeqNum() <= replayTo

        if (sobj.getSeqNum().equals(replayTo)) {
            // nothing to do.
            return;
        }

        log.trace("querying {}@{} to {}", sobj.getObjectID(), sobj.getSeqNum(),
                                            replayTo);

        IMap<SeqNum, LogValue> logMap = getLogMap(sobj);
        final LogMapManager logCache = getLogMapManager(sobj.getObjectID());

        // TODO Consider more fine grained lock if we ever need
        //   SharedLogObject to be exposed to heavy concurrent read/write.

        sobj.acquireWriteLock();
        try {
            final SeqNum current = sobj.getSeqNum();

            List<SeqNum> range = getSeqNumRange(current, replayTo);
            Map<SeqNum, Future<LogValue>> values = new HashMap<>(range.size());

            // FIXME use notification based cache

            // pre-request all range first to pre-populate near cache
            log.trace("Pre reading range: {}", range);
            for (SeqNum key : range) {
                Future<LogValue> value = logCache.getLogValue(key);
                values.put(key, value);
            }

            // walk and apply range 1 by 1
            for (SeqNum key : range) {
                Future<LogValue> future = values.get(key);
                LogValue value = null;

                // FIXME handle Recycled Snapshot log entry scenario
                // when the get result is null it could be waiting for writer
                // or log has been trimmed.

                final long deadline = System.currentTimeMillis()
                                        + LOG_READ_TIMEOUT_MS;
                if (log.isTraceEnabled()) {
                    log.trace("Try reading {} until {}", key, new DateTime(deadline));
                }
                while (System.currentTimeMillis() < deadline) {
                    try {
                        value = future.get(LOG_READ_TIMEOUT_MS,
                                                    TimeUnit.MILLISECONDS);
                        if (value == null) {
                            log.trace("{} was not there yet retrying.", key);
                            // value not set yet, retry;
                            future = logCache.getLogValue(key);
                            continue;
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("queryError retrying", e);
                        future = logCache.getLogValue(key);
                        continue;
                    } catch (TimeoutException e) {
                        log.warn("query timed out", e);
                    }
                }
                if (value == null) {
                    log.trace("{} was not found, writer failed?", key);
                    // writer failure scenario handling
                    //  mark sequence number null
                    LogValue newVal = logMap.put(key, NoOp.VALUE);
                    if (newVal == null) {
                        // skip this sequence number.
                        continue;
                    }
                    log.trace("{} appeared while invalidating.", newVal);
                    value = newVal;
                }

                log.trace("applying log {} {}", key, value);
                // apply log value
                sobj.apply(key, value);
            }

        } finally {
            sobj.releaseWriteLock();
            log.trace("query done");
        }
    }

    /**
     * Creates a List containing (afterThis, uptoThis].
     *
     * @param afterThis beginning of range not including this sequence number
     * @param upToThis end of range including this sequence number
     * @return List of SeqNum in order
     */
    private List<SeqNum> getSeqNumRange(SeqNum afterThis, SeqNum upToThis) {
        checkArgument(!upToThis.equals(SeqNum.INITIAL),
                        "upToThis must not be INITIAL");

        // TODO Wasting heap, etc.
        // If iterable SeqNum is the only thing required,
        // implement SeqNum range class, etc.
        List<SeqNum> range = new ArrayList<>();
        for (SeqNum i = afterThis.next(); !i.equals(upToThis); i = i.next()) {
            range.add(i);
        }
        range.add(upToThis);
        return range;
    }

    @Override
    public void addListener(SharedLogObjectID oid, LogEventListener listener) {
        LogMapManager hzListener = getLogMapManager(oid);
        hzListener.addListener(listener);

        getLogMap(oid).addEntryListener(hzListener, true);
    }

    @Override
    public void removeListener(SharedLogObjectID oid, LogEventListener listener) {
        LogMapManager hzListener = getLogMapManager(oid);
        hzListener.removeListener(listener);
    }

    /**
     * Gets the LogMapManager for given {@link SharedLogObjectID}.
     * <p/>
     * If listener was not registered, it will create and register a listener.
     *
     * @param oid {@link SharedLogObjectID}
     * @return {@link LogMapManager}
     */
    private LogMapManager getLogMapManager(final SharedLogObjectID oid) {
        LogMapManager listener
            = ConcurrentUtils.createIfAbsentUnchecked(listenerMap, oid,
                new ConcurrentInitializer<LogMapManager>() {
                    @Override
                    public LogMapManager get() throws ConcurrentException {
                        IMap<SeqNum, LogValue> logMap = getLogMap(oid);
                        return new LogMapManager(oid, logMap);
                    }
                });
        return listener;
    }

    @Override
    public SeqNum getLatestSnapShotId(SharedLogObjectID oid) {
        IAtomicLong latest = getLatestSnapshotStore(oid);
        final long ssId = latest.get();
        return SeqNum.anyValueOf(ssId);
    }

    @Override
    public boolean resetToSnapShot(SharedLogObject sobj, SeqNum ssId) {
        checkNotNull(ssId);

        IMap<SeqNum, SnapShotValue> ssMap = getSnapshotMap(sobj);
        SnapShotValue snapshot = ssMap.get(ssId);
        if (snapshot == null) {
            if (ssId.equals(SeqNum.INITIAL)) {
                snapshot = NoOp.VALUE;
            } else {
                log.error("Invalid Snapshot version {}@{} specified",
                                    sobj.getObjectID(), ssId);
                return false;
            }
        }

        sobj.reset(ssId, snapshot);

        return true;
    }

    /**
     * Gets the LogValues in range ({@code after}, {@code upToThis}].
     *
     * @param oid ID of SharedLogObject
     * @param after sequence number before (typically current sequence number)
     * @param upToThis last sequence number you want to retrieve
     * @return List of {@link LogValue}s in specified range
     */
    @Override
    public List<LogValue> getLogRange(final SharedLogObjectID oid,
                                    final SeqNum after, final SeqNum upToThis) {

        List<LogValue> logs = new ArrayList<>();
        final SeqNum oneAfterLast = upToThis.next();
        LogMapManager logCache = getLogMapManager(oid);
        // may want to do async pre-fetching if RPC is triggered often
        for (SeqNum s = after.next(); !s.equals(oneAfterLast); s = s.next()) {
            try {
                logs.add(logCache.getLogValue(s).get());
                // FIXME Need to somehow detect log has been recycled case and fail
            } catch (InterruptedException | ExecutionException e) {
                log.error("Failed getting log " + oid + "@" + s, e);
                // Should we retry/[ignore]/throw?
            }
        }
        return logs;
    }
}
