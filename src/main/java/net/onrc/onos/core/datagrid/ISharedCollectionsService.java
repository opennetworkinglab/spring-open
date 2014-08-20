package net.onrc.onos.core.datagrid;

import java.util.concurrent.BlockingQueue;
import com.hazelcast.core.IMap;

import net.floodlightcontroller.core.module.IFloodlightService;

/**
 * Interface for providing shared maps and queues to other modules.
 */
public interface ISharedCollectionsService extends IFloodlightService {

    // FIXME Refactor and change IMap to interface defined by us later.
    /**
     * Create an shared, concurrent map.
     *
     * @param mapName the shared map name.
     * @param typeK the type of the Key in the map.
     * @param typeV the type of the Value in the map.
     * @return the shared map for the channel name.
     */
    <K, V> IMap<K, V> getConcurrentMap(String mapName,
            Class<K> typeK, Class<V> typeV);

    /**
     * Create an shared, blocking queue.
     *
     * @param queueName the shared queue name.
     * @param typeT the type of the queue.
     * @return the shared queue for the queue name.
     */
    <T> BlockingQueue<T> getBlockingQueue(String queueName,
            Class<T> typeT);
}
