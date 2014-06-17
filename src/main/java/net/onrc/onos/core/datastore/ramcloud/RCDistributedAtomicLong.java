package net.onrc.onos.core.datastore.ramcloud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.onrc.onos.core.datastore.IKVTable;
import net.onrc.onos.core.datastore.IKVTableID;
import net.onrc.onos.core.datastore.ObjectDoesntExistException;
import net.onrc.onos.core.datastore.ObjectExistsException;
import net.onrc.onos.core.datastore.IKVTable.IKVEntry;
import net.onrc.onos.core.datastore.utils.ByteArrayUtil;
import net.onrc.onos.core.util.distributed.DistributedAtomicLong;

/**
 * RAMCloudImplementation of DistributedAtomicLong.
 */
public class RCDistributedAtomicLong implements DistributedAtomicLong {
    private static final String PREFIX = "DAL:";
    private static final byte[] KEY = {0};
    private static final byte[] ZERO = ByteArrayUtil.toLEBytes(0L);

    private static final Logger log = LoggerFactory.getLogger(RCDistributedAtomicLong.class);


    private final RCClient client;
    private final String name;
    private final IKVTableID tableID;


    /**
     * Creates or Gets the DistributedAtomicLong instance.
     *
     * @param client client to use.
     * @param name name of the DistributedAtomicLong instance.
     */
    public RCDistributedAtomicLong(final RCClient client, final String name) {

        this.client = client;
        this.name = name;
        IKVTable table = client.getTable(PREFIX + name);
        this.tableID = table.getTableId();

        try {
            table.create(KEY, ZERO);
        } catch (ObjectExistsException e) {
            log.trace("RCDistributedAtomicLong {} already exists", name);
        }
    }

    @Override
    public long get() {
        try {
            IKVEntry entry = client.read(tableID, KEY);
            return ByteArrayUtil.fromLEBytes(entry.getValue());
        } catch (ObjectDoesntExistException e) {
            log.error("RCDistributedAtomicLong {} does not exist", name);
            throw new IllegalStateException(name + " does not exist", e);
        }
    }

    @Override
    public long addAndGet(long delta) {
        return client.incrementCounter(tableID, KEY, delta);
    }

    @Override
    public void set(long newValue) {
        client.setCounter(tableID, KEY, newValue);
    }

    @Override
    public long incrementAndGet() {
        return addAndGet(1L);
    }

}
