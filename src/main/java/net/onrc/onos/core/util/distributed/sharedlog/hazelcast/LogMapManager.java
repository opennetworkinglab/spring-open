package net.onrc.onos.core.util.distributed.sharedlog.hazelcast;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.onrc.onos.core.util.distributed.sharedlog.LogEventListener;
import net.onrc.onos.core.util.distributed.sharedlog.SeqNum;
import net.onrc.onos.core.util.distributed.sharedlog.SharedLogObjectID;
import net.onrc.onos.core.util.distributed.sharedlog.internal.LogValue;

import com.google.common.annotations.Beta;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IMap;

/**
 * LogMapManager.
 *
 * - Listener to register to Hazelcast IMap.
 * - Caches LogValue notification.
 * - Dispatches LogValue added event to listeners
 * - TODO health check latest log to detect dropped notification
 */
@Beta
@ThreadSafe
public class LogMapManager implements EntryListener<SeqNum, LogValue> {

    private static final Logger log = LoggerFactory
            .getLogger(LogMapManager.class);

    // TODO revisit appropriate size. current guess MAX 5k/s * 2
    private static final int CACHE_SIZE = 10000;

    private final SharedLogObjectID oid;
    private final IMap<SeqNum, LogValue> logMap;

    // TODO is there a library to do listener management?
    private CopyOnWriteArrayList<LogEventListener> listeners;

    private Cache<SeqNum, Future<LogValue>> cache;

    // latest log notified to clients
    private SeqNum lastLog;

    // It might be that only oid is sufficient.
    /**
     * Constructor.
     *
     * @param oid ID of SharedLogObject this instance is attached
     * @param logMap Log Map this SharedLogObject use
     */
    public LogMapManager(SharedLogObjectID oid,
                                  IMap<SeqNum, LogValue> logMap) {
        this.oid = checkNotNull(oid);
        this.logMap = checkNotNull(logMap);
        this.listeners = new CopyOnWriteArrayList<>();
        this.cache = CacheBuilder.newBuilder()
                .weakValues()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .concurrencyLevel(1)
                .maximumSize(CACHE_SIZE)
                .build();
        this.lastLog = SeqNum.INITIAL;
    }

    // called from Hazelcast thread
    @Override
    public void entryAdded(EntryEvent<SeqNum, LogValue> event) {
        // Cache maintenance
        cache.put(event.getKey(),
                  ConcurrentUtils.constantFuture(event.getValue()));

        // TODO will need suppress mechanism once we have health check

        if (lastLog.compareTo(event.getKey()) < 0) {
            lastLog = event.getKey();
        }
        for (LogEventListener lsnr : listeners) {
            lsnr.logAdded(event.getKey());
        }
    }

    // called from Hazelcast thread
    @Override
    public void entryRemoved(EntryEvent<SeqNum, LogValue> event) {
        // Cache maintenance
        cache.invalidate(event.getKey());

        // only add will be notified to listeners
    }

    // called from Hazelcast thread
    @Override
    public void entryUpdated(EntryEvent<SeqNum, LogValue> event) {
        // Cache maintenance
        cache.put(event.getKey(),
                  ConcurrentUtils.constantFuture(event.getValue()));

        // only add will be notified to listeners
    }

    // called from Hazelcast thread
    @Override
    public void entryEvicted(EntryEvent<SeqNum, LogValue> event) {
        // Cache maintenance
        cache.invalidate(event.getKey());

        // only add will be notified to listeners
    }

    /**
     * Adds the listener for specified shared object log event.
     *
     * @param listener to add
     */
    public void addListener(LogEventListener listener) {
        listeners.addIfAbsent(listener);
    }

    /**
     * Removes the listener for specified shared object log event.
     *
     * @param listener to add
     */
    public void removeListener(LogEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Reads LogValue from LogMap.
     * <p>
     * It will use the cached value if it exist, if not it will
     * get the value from distributed store.
     *
     * @param key log sequence number
     * @return Future containing log value
     */
    public Future<LogValue> getLogValue(final SeqNum key) {
        try {
            return cache.get(key, new Callable<Future<LogValue>>() {

                @Override
                public Future<LogValue> call() throws Exception {
                    return logMap.getAsync(key);
                }
            });
        } catch (ExecutionException e) {
            log.error("Reading from Log Map failed.", e);
            // should never happen?
            return ConcurrentUtils.constantFuture(null);
        }
    }
}
