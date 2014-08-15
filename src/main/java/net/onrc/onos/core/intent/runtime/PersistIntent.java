package net.onrc.onos.core.intent.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import net.onrc.onos.core.datastore.DataStoreClient;
import net.onrc.onos.core.datastore.IKVTable;
import net.onrc.onos.core.datastore.ObjectExistsException;
import net.onrc.onos.core.intent.IntentOperationList;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.util.IdBlock;
import net.onrc.onos.core.util.serializers.KryoFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

/**
 * The module used by PathCalcRuntimeModule class.
 * <p>
 * It persists intent operations into persistent storage.
 */
public class PersistIntent {
    private static final Logger log = LoggerFactory.getLogger(PersistIntent.class);
    private long range = 10000L;
    private final IControllerRegistryService controllerRegistry;
    private static final String INTENT_JOURNAL = "G:IntentJournal";
    private static final int VALUE_STORE_LIMIT = 1024 * 1024;
    private IKVTable table;
    private Kryo kryo;
    private ByteArrayOutputStream stream;
    private Output output = null;
    private AtomicLong nextId = null;
    private long rangeEnd;
    private IdBlock idBlock = null;

    /**
     * Constructor.
     *
     * @param controllerRegistry the Registry Service to use.
     */
    public PersistIntent(final IControllerRegistryService controllerRegistry) {
        this.controllerRegistry = controllerRegistry;
        table = DataStoreClient.getClient().getTable(INTENT_JOURNAL);
        stream = new ByteArrayOutputStream(1024);
        output = new Output(stream);
        // FIXME Using KryoFactory only to register classes and not using the pool.
        kryo = new KryoFactory(1).newKryo();
    }

    private long getNextBlock() {
        // XXX This method is not thread safe, may lose allocated IdBlock
        idBlock = controllerRegistry.allocateUniqueIdBlock(range);
        nextId = new AtomicLong(idBlock.getStart());
        rangeEnd = idBlock.getEnd();
        return nextId.get();
    }

    /**
     * Provides the unique key for persisting.
     * <p>
     * This key is necessary for persistIfLeader() method.
     *
     * @return a key for persisting.
     */
    public long getKey() {
        long key;
        if (idBlock == null) {
            key = getNextBlock();
        } else {
            key = nextId.incrementAndGet();
            if (key >= rangeEnd) {
                key = getNextBlock();
            }
        }
        return key;
    }

    /**
     * Persist intent operations into persistent storage only if this instance was a leader.
     *
     * @param key a unique key
     * @param operations intent operations
     * @return true if succeeded, otherwise false.
     */
    public boolean persistIfLeader(long key, IntentOperationList operations) {
        boolean leader = true;
        boolean ret = false;
        long keyValue = key;
        // TODO call controllerRegistry.isClusterLeader()
        if (leader) {
            try {
                // reserve key 10 entries for multi-write if size over 1MB
                keyValue *= 10;
                kryo.writeObject(output, operations);
                output.close();
                ByteBuffer keyBytes = ByteBuffer.allocate(8).putLong(keyValue);
                byte[] buffer = stream.toByteArray();
                int total = buffer.length;
                if ((total >= VALUE_STORE_LIMIT)) {
                    int writeCount = total / VALUE_STORE_LIMIT;
                    int remainder = total % VALUE_STORE_LIMIT;
                    int upperIndex = 0;
                    for (int i = 0; i < writeCount; i++, keyValue++) {
                        keyBytes.clear();
                        keyBytes.putLong(keyValue);
                        keyBytes.flip();
                        upperIndex = (i * VALUE_STORE_LIMIT + VALUE_STORE_LIMIT) - 1;
                        log.debug("writing using indexes {}:{}", (i * VALUE_STORE_LIMIT), upperIndex);
                        table.create(keyBytes.array(), Arrays.copyOfRange(buffer, i * VALUE_STORE_LIMIT, upperIndex));
                    }
                    if (remainder > 0) {
                        keyBytes.clear();
                        keyBytes.putLong(keyValue);
                        keyBytes.flip();
                        log.debug("writing using indexes {}:{}", upperIndex, total);
                        table.create(keyBytes.array(), Arrays.copyOfRange(buffer, upperIndex + 1, total - 1));
                    }
                } else {
                    keyBytes.flip();
                    table.create(keyBytes.array(), buffer);
                }
                log.debug("key is {} value length is {}", keyValue, buffer.length);
                stream.reset();
                stream.close();
                log.debug("persist operations to ramcloud size of operations: {}", operations.size());
                ret = true;
            } catch (ObjectExistsException ex) {
                log.warn("Failed to store intent journal with key " + keyValue);
            } catch (IOException ex) {
                log.error("Failed to close the stream");
            }
        }
        return ret;
    }
}
