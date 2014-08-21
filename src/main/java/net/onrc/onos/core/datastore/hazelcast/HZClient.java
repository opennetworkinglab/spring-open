package net.onrc.onos.core.datastore.hazelcast;

import java.util.Collection;
import java.util.List;

import net.onrc.onos.core.datagrid.HazelcastDatagrid;
import net.onrc.onos.core.datastore.IKVClient;
import net.onrc.onos.core.datastore.IKVTable;
import net.onrc.onos.core.datastore.IKVTable.IKVEntry;
import net.onrc.onos.core.datastore.IKVTableID;
import net.onrc.onos.core.datastore.IMultiEntryOperation;
import net.onrc.onos.core.datastore.IMultiEntryOperation.OPERATION;
import net.onrc.onos.core.datastore.IMultiEntryOperation.STATUS;
import net.onrc.onos.core.datastore.ObjectDoesntExistException;
import net.onrc.onos.core.datastore.ObjectExistsException;
import net.onrc.onos.core.datastore.WrongVersionException;
import net.onrc.onos.core.datastore.hazelcast.HZTable.VersionedValue;
import net.onrc.onos.core.datastore.internal.IModifiableMultiEntryOperation;
import net.onrc.onos.core.datastore.utils.ByteArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.IMap;

/**
 * Hazelcast implementation of datastore IKVClient.
 */
public final class HZClient implements IKVClient {
    private static final Logger log = LoggerFactory.getLogger(HZClient.class);

    static final long VERSION_NONEXISTENT = 0L;

    private static final String MAP_PREFIX = "datastore://";

    private static final String BASE_CONFIG_FILENAME =
            System.getProperty("net.onrc.onos.core.datastore.hazelcast.baseConfig", "conf/hazelcast.xml");

    private final HazelcastInstance hazelcastInstance;

    private static final HZClient THE_INSTANCE = new HZClient();

    /**
     * Get DataStoreClient implemented on Hazelcast.
     *
     * @return HZClient
     */
    public static HZClient getClient() {
        return THE_INSTANCE;
    }

    /**
     * Default constructor.
     * <p/>
     * Get or create the Hazelcast Instance to use for datastore.
     */
    private HZClient() {
        Config config = HazelcastDatagrid.loadHazelcastConfig(BASE_CONFIG_FILENAME);
        // Try to get the existing HZ instance in JVM if possible.
        hazelcastInstance = Hazelcast.getOrCreateHazelcastInstance(config);
    }

    /**
     * Gets the HazelcastInstance object.
     *
     * @return HazelcastInstance
     */
    HazelcastInstance getHZInstance() {
        return hazelcastInstance;
    }

    @Override
    public IKVTable getTable(final String tableName) {
        IMap<byte[], VersionedValue> map = hazelcastInstance.getMap(MAP_PREFIX + tableName);

        return new HZTable(tableName, map);
    }

    @Override
    public void dropTable(final IKVTable table) {
        ((HZTable) table).getBackendMap().clear();
    }

    @Override
    public long create(final IKVTableID tableId, final byte[] key, final byte[] value)
            throws ObjectExistsException {
        IKVTable table = (IKVTable) tableId;
        return table.create(key, value);
    }

    @Override
    public long forceCreate(final IKVTableID tableId, final byte[] key, final byte[] value) {
        IKVTable table = (IKVTable) tableId;
        return table.forceCreate(key, value);
    }

    @Override
    public IKVEntry read(final IKVTableID tableId, final byte[] key)
            throws ObjectDoesntExistException {
        IKVTable table = (IKVTable) tableId;
        return table.read(key);
    }

    @Override
    public long update(final IKVTableID tableId, final byte[] key, final byte[] value,
                       final long version) throws ObjectDoesntExistException,
            WrongVersionException {
        IKVTable table = (IKVTable) tableId;
        return table.update(key, value, version);
    }

    @Override
    public long update(final IKVTableID tableId, final byte[] key, final byte[] value)
            throws ObjectDoesntExistException {
        IKVTable table = (IKVTable) tableId;
        return table.update(key, value);
    }

    @Override
    public long delete(final IKVTableID tableId, final byte[] key, final long version)
            throws ObjectDoesntExistException, WrongVersionException {
        IKVTable table = (IKVTable) tableId;
        return table.delete(key, version);
    }

    @Override
    public long forceDelete(final IKVTableID tableId, final byte[] key) {
        IKVTable table = (IKVTable) tableId;
        return table.forceDelete(key);
    }

    @Override
    public Iterable<IKVEntry> getAllEntries(final IKVTableID tableId) {
        IKVTable table = (IKVTable) tableId;
        return table.getAllEntries();
    }

    @Override
    public IMultiEntryOperation createOp(final IKVTableID tableId, final byte[] key,
                                         final byte[] value) {
        return new HZMultiEntryOperation((HZTable) tableId, key, value, HZClient.VERSION_NONEXISTENT, OPERATION.CREATE);
    }

    @Override
    public IMultiEntryOperation forceCreateOp(final IKVTableID tableId, final byte[] key,
                                              final byte[] value) {
        return new HZMultiEntryOperation((HZTable) tableId, key, value,
                HZClient.VERSION_NONEXISTENT, OPERATION.FORCE_CREATE);
    }

    @Override
    public IMultiEntryOperation readOp(final IKVTableID tableId, final byte[] key) {
        return new HZMultiEntryOperation((HZTable) tableId, key, OPERATION.READ);
    }

    @Override
    public IMultiEntryOperation updateOp(final IKVTableID tableId, final byte[] key,
                                         final byte[] value, final long version) {
        return new HZMultiEntryOperation((HZTable) tableId, key, value, version, OPERATION.UPDATE);
    }

    @Override
    public IMultiEntryOperation deleteOp(final IKVTableID tableId, final byte[] key,
                                         final byte[] value, final long version) {
        return new HZMultiEntryOperation((HZTable) tableId, key, value, version, OPERATION.DELETE);
    }

    @Override
    public IMultiEntryOperation forceDeleteOp(final IKVTableID tableId, final byte[] key) {
        return new HZMultiEntryOperation((HZTable) tableId, key, OPERATION.FORCE_DELETE);
    }

    @Override
    public boolean multiDelete(final Collection<IMultiEntryOperation> ops) {
        boolean failExists = false;
        for (IMultiEntryOperation op : ops) {
            HZMultiEntryOperation mop = (HZMultiEntryOperation) op;
            switch (mop.getOperation()) {
                case DELETE:
                    try {
                        final long version = delete(mop.getTableId(), mop.getKey(), mop.getVersion());
                        mop.setVersion(version);
                        mop.setStatus(STATUS.SUCCESS);
                    } catch (ObjectDoesntExistException | WrongVersionException e) {
                        log.error(mop + " failed.", e);
                        mop.setStatus(STATUS.FAILED);
                        failExists = true;
                    }
                    break;
                case FORCE_DELETE:
                    final long version = forceDelete(mop.getTableId(), mop.getKey());
                    mop.setVersion(version);
                    mop.setStatus(STATUS.SUCCESS);
                    break;
                default:
                    throw new UnsupportedOperationException(mop.toString());
            }
        }
        return failExists;
    }

    @Override
    public boolean multiWrite(final List<IMultiEntryOperation> ops) {
        // there may be room to batch to improve performance
        boolean failExists = false;
        for (IMultiEntryOperation op : ops) {
            IModifiableMultiEntryOperation mop = (IModifiableMultiEntryOperation) op;
            switch (mop.getOperation()) {
                case CREATE:
                    try {
                        long version = create(mop.getTableId(), mop.getKey(), mop.getValue());
                        mop.setVersion(version);
                        mop.setStatus(STATUS.SUCCESS);
                    } catch (ObjectExistsException e) {
                        log.error(mop + " failed.", e);
                        mop.setStatus(STATUS.FAILED);
                        failExists = true;
                    }
                    break;
                case FORCE_CREATE: {
                    final long version = forceCreate(mop.getTableId(), mop.getKey(), mop.getValue());
                    mop.setVersion(version);
                    mop.setStatus(STATUS.SUCCESS);
                    break;
                }
                case UPDATE:
                    try {
                        long version = update(mop.getTableId(), mop.getKey(), mop.getValue(), mop.getVersion());
                        mop.setVersion(version);
                        mop.setStatus(STATUS.SUCCESS);
                    } catch (ObjectDoesntExistException | WrongVersionException e) {
                        log.error(mop + " failed.", e);
                        mop.setStatus(STATUS.FAILED);
                        failExists = true;
                    }
                    break;
                default:
                    throw new UnsupportedOperationException(mop.toString());
            }
        }
        return failExists;
    }

    @Override
    public boolean multiRead(final Collection<IMultiEntryOperation> ops) {
        boolean failExists = false;
        for (IMultiEntryOperation op : ops) {
            IModifiableMultiEntryOperation mop = (IModifiableMultiEntryOperation) op;
            HZTable table = (HZTable) op.getTableId();
            ((HZMultiEntryOperation) mop.getActualOperation()).setFuture(table.getBackendMap().getAsync(op.getKey()));
        }
        for (IMultiEntryOperation op : ops) {
            IModifiableMultiEntryOperation mop = (IModifiableMultiEntryOperation) op;
            if (!mop.hasSucceeded()) {
                failExists = true;
            }
        }

        return failExists;
    }

    @Override
    public long getVersionNonexistant() {
        return VERSION_NONEXISTENT;
    }

    private String getCounterName(final IKVTableID tableId, final byte[] key) {
        StringBuilder buf = new StringBuilder(tableId.getTableName());
        buf.append('@');
        ByteArrayUtil.toHexStringBuilder(key, ":", buf);
        return buf.toString();
    }

    private IAtomicLong getAtomicLong(final IKVTableID tableId, final byte[] key) {
        // TODO we probably want to implement some sort of caching
        return hazelcastInstance.getAtomicLong(getCounterName(tableId, key));
    }

    /**
     * {@inheritDoc}
     * <p />
     * Warning: The counter is a different object from {@code key} entry on
     * IKVTable with {@code tableId}. You cannot use table API to read/write
     * counters.
     *
     * @param tableId Only getTableName() will be used.
     * @param key tableId + key will be used as Counter name
     */
    @Override
    public void createCounter(final IKVTableID tableId,
                              final byte[] key, final long initialValue)
                                      throws ObjectExistsException {

        IAtomicLong counter = getAtomicLong(tableId, key);
        // Assumption here is that AtomicLong is initialized to 0L
        final boolean success = counter.compareAndSet(0L, initialValue);
        if (!success) {
            throw new ObjectExistsException("Atomic counter "
                    + getCounterName(tableId, key)
                    + " already exist with value:" + counter.get());
        }
    }

    @Override
    public void setCounter(final IKVTableID tableId,
                           final byte[] key, final long value) {

        IAtomicLong counter = getAtomicLong(tableId, key);
        counter.set(value);
    }

    @Override
    public long incrementCounter(final IKVTableID tableId,
                                 final byte[] key, final long incrementValue) {

        IAtomicLong counter = getAtomicLong(tableId, key);
        return counter.addAndGet(incrementValue);
    }

    @Override
    public void destroyCounter(final IKVTableID tableId, final byte[] key) {
        IAtomicLong counter = getAtomicLong(tableId, key);
        counter.destroy();

    }

    @Override
    public long getCounter(final IKVTableID tableId, final byte[] key)
            throws ObjectDoesntExistException {

        IAtomicLong counter = getAtomicLong(tableId, key);
        return counter.get();
    }

}
