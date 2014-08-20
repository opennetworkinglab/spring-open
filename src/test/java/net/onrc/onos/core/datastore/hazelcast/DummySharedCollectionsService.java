package net.onrc.onos.core.datastore.hazelcast;

import java.util.concurrent.BlockingQueue;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import net.onrc.onos.core.datagrid.HazelcastDatagrid;
import net.onrc.onos.core.datagrid.ISharedCollectionsService;

/**
 * Dummy ISharedCollectionsService implementation to use for testing.
 */
public class DummySharedCollectionsService implements ISharedCollectionsService {

    private final HazelcastInstance instance;

    /**
     * Default constructor.
     */
    public DummySharedCollectionsService() {
        Config cfg = HazelcastDatagrid.loadHazelcastConfig(
                        HazelcastDatagrid.HAZELCAST_DEFAULT_XML);
        instance = Hazelcast.getOrCreateHazelcastInstance(cfg);
    }

    @Override
    public <K, V> IMap<K, V> getConcurrentMap(String mapName, Class<K> typeK, Class<V> typeV) {
        return instance.getMap(mapName);
    }

    @Override
    public <T> BlockingQueue<T> getBlockingQueue(String queueName, Class<T> typeT) {
        return instance.getQueue(queueName);
    }

}
