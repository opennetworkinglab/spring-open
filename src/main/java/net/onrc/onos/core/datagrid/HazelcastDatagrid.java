package net.onrc.onos.core.datagrid;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.onrc.onos.core.datagrid.web.DatagridWebRoutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

/**
 * A datagrid service that uses Hazelcast as a datagrid.
 * The relevant data is stored in the Hazelcast datagrid and shared as
 * appropriate in a multi-node cluster.
 */
public class HazelcastDatagrid implements IFloodlightModule, IDatagridService,
        ISharedCollectionsService {
    static final Logger log = LoggerFactory.getLogger(HazelcastDatagrid.class);

    /**
     * The name of Hazelcast instance in this JVM.
     */
    public static final String ONOS_HAZELCAST_INSTANCE = "ONOS_HazelcastInstance";

    private IRestApiService restApi;

    static final String HAZELCAST_CONFIG_FILE = "datagridConfig";
    public static final String HAZELCAST_DEFAULT_XML = "conf/hazelcast.default.xml";
    private HazelcastInstance hazelcastInstance;
    private Config hazelcastConfig;

    //
    // NOTE: eventChannels is kept thread safe by using explicit "synchronized"
    // blocks below. Those are needed to protect the integrity of each entry
    // instance, and avoid preemption during channel creation/startup.
    //
    private final Map<String, IEventChannel<?, ?>> eventChannels = new HashMap<>();

    /**
     * Load the Hazelcast Datagrid configuration file.
     *
     * @param configFilename the configuration filename.
     * @return Hazelcast configuration
     */
    public static Config loadHazelcastConfig(String configFilename) {

        Config hzConfig = null;
        /*
        System.setProperty("hazelcast.socket.receive.buffer.size", "32");
        System.setProperty("hazelcast.socket.send.buffer.size", "32");
        */
        // System.setProperty("hazelcast.heartbeat.interval.seconds", "100");

        // Init from configuration file
        try {
            hzConfig = new FileSystemXmlConfig(configFilename);
        } catch (FileNotFoundException e) {
            log.error("Error opening Hazelcast XML configuration. File not found: " + configFilename, e);

            // Fallback mechanism to support running unit test without setup.
            log.error("Falling back to default Hazelcast XML {}", HAZELCAST_DEFAULT_XML);
            try {
                hzConfig = new FileSystemXmlConfig(HAZELCAST_DEFAULT_XML);
            } catch (FileNotFoundException e2) {
                log.error("Error opening fall back Hazelcast XML configuration. "
                        + "File not found: " + HAZELCAST_DEFAULT_XML, e2);
                // XXX probably should throw some exception to kill ONOS instead.
                hzConfig = new Config();
            }
        }

        // set the name of Hazelcast instance in this JVM.
        hzConfig.setInstanceName(ONOS_HAZELCAST_INSTANCE);

        /*
        hazelcastConfig.setProperty(GroupProperties.PROP_IO_THREAD_COUNT, "1");
        hazelcastConfig.setProperty(GroupProperties.PROP_OPERATION_THREAD_COUNT, "1");
        hazelcastConfig.setProperty(GroupProperties.PROP_EVENT_THREAD_COUNT, "1");
        */

        return hzConfig;
    }

    /**
     * Shutdown the Hazelcast Datagrid operation.
     */
    @Override
    protected void finalize() {
        close();
    }

    /**
     * Shutdown the Hazelcast Datagrid operation.
     */
    public void close() {
        Hazelcast.shutdownAll();
    }

    /**
     * Get the collection of offered module services.
     *
     * @return the collection of offered module services.
     */
    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IDatagridService.class);
        l.add(ISharedCollectionsService.class);
        return l;
    }

    /**
     * Get the collection of implemented services.
     *
     * @return the collection of implemented services.
     */
    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService>
    getServiceImpls() {
        Map<Class<? extends IFloodlightService>,
                IFloodlightService> m =
                new HashMap<Class<? extends IFloodlightService>,
                        IFloodlightService>();
        m.put(IDatagridService.class, this);
        m.put(ISharedCollectionsService.class, this);
        return m;
    }

    /**
     * Get the collection of modules this module depends on.
     *
     * @return the collection of modules this module depends on.
     */
    @Override
    public Collection<Class<? extends IFloodlightService>>
    getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        l.add(IRestApiService.class);
        return l;
    }

    /**
     * Initialize the module.
     *
     * @param context the module context to use for the initialization.
     * @throws FloodlightModuleException on error
     */
    @Override
    public void init(FloodlightModuleContext context)
            throws FloodlightModuleException {
        restApi = context.getServiceImpl(IRestApiService.class);

        // Get the configuration file name and configure the Datagrid
        Map<String, String> configMap = context.getConfigParams(this);
        String configFilename = configMap.get(HAZELCAST_CONFIG_FILE);
        hazelcastConfig = loadHazelcastConfig(configFilename);
    }

    /**
     * Startup module operation.
     *
     * @param context the module context to use for the startup.
     */
    @Override
    public void startUp(FloodlightModuleContext context) {
        hazelcastInstance = Hazelcast.getOrCreateHazelcastInstance(hazelcastConfig);

        restApi.addRestletRoutable(new DatagridWebRoutable());
    }

    /**
     * Create an event channel.
     * <p/>
     * If the channel already exists, just return it.
     * NOTE: The channel is started automatically.
     *
     * @param channelName the event channel name.
     * @param <K>         the type of the Key in the Key-Value store.
     * @param <V>         the type of the Value in the Key-Value store.
     * @param typeK       the type of the Key in the Key-Value store.
     * @param typeV       the type of the Value in the Key-Value store.
     * @return the event channel for the channel name.
     */
    @Override
    public <K, V> IEventChannel<K, V> createChannel(String channelName,
                                                    Class<K> typeK, Class<V> typeV) {
        synchronized (eventChannels) {
            IEventChannel<K, V> eventChannel =
                    createChannelImpl(channelName, typeK, typeV);
            eventChannel.startup();
            return eventChannel;
        }
    }

    /**
     * Create an event channel implementation.
     * <p/>
     * If the channel already exists, just return it.
     * NOTE: The caller must call IEventChannel.startup() to startup the
     * channel operation.
     * NOTE: The caller must own the lock on "eventChannels".
     *
     * @param channelName the event channel name.
     * @param <K>         the type of the Key in the Key-Value store.
     * @param <V>         the type of the Value in the Key-Value store.
     * @param typeK       the type of the Key in the Key-Value store.
     * @param typeV       the type of the Value in the Key-Value store.
     * @return the event channel for the channel name.
     */
    private <K, V> IEventChannel<K, V> createChannelImpl(
            String channelName,
            Class<K> typeK, Class<V> typeV) {
        IEventChannel<?, ?> genericEventChannel =
                eventChannels.get(channelName);

        // Add the channel if the first listener
        if (genericEventChannel == null) {
            IEventChannel<K, V> castedEventChannel =
                    new HazelcastEventChannel<K, V>(hazelcastInstance,
                            channelName, typeK, typeV);
            eventChannels.put(channelName, castedEventChannel);
            return castedEventChannel;
        }

        //
        // TODO: Find if we can use Java internal support to check for
        // type mismatch.
        //
        if (!genericEventChannel.verifyKeyValueTypes(typeK, typeV)) {
            throw new ClassCastException("Key-value type mismatch for event channel " + channelName);
        }
        @SuppressWarnings("unchecked")
        IEventChannel<K, V> castedEventChannel =
                (IEventChannel<K, V>) genericEventChannel;
        return castedEventChannel;
    }

    /**
     * Add event channel listener.
     * <p/>
     * NOTE: The channel is started automatically right after the listener
     * is added.
     *
     * @param channelName the event channel name.
     * @param listener    the listener to add.
     * @param <K>         the type of the Key in the Key-Value store.
     * @param <V>         the type of the Value in the Key-Value store.
     * @param typeK       the type of the Key in the Key-Value store.
     * @param typeV       the type of the Value in the Key-Value store.
     * @return the event channel for the channel name.
     */
    @Override
    public <K, V> IEventChannel<K, V> addListener(String channelName,
                                                  IEventChannelListener<K, V> listener,
                                                  Class<K> typeK, Class<V> typeV) {
        synchronized (eventChannels) {
            IEventChannel<K, V> eventChannel =
                    createChannelImpl(channelName, typeK, typeV);
            eventChannel.addListener(listener);
            eventChannel.startup();

            return eventChannel;
        }
    }

    /**
     * Remove event channel listener.
     *
     * @param <K>         the type of the Key in the Key-Value store.
     * @param <V>         the type of the Value in the Key-Value store.
     * @param channelName the event channel name.
     * @param listener    the listener to remove.
     */
    @Override
    public <K, V> void removeListener(String channelName,
                                      IEventChannelListener<K, V> listener) {
        synchronized (eventChannels) {
            IEventChannel<?, ?> genericEventChannel =
                    eventChannels.get(channelName);

            if (genericEventChannel != null) {
                //
                // TODO: Find if we can use Java internal support to check for
                // type mismatch.
                // NOTE: Using "ClassCastException" exception below doesn't
                // work.
                //
                @SuppressWarnings("unchecked")
                IEventChannel<K, V> castedEventChannel =
                        (IEventChannel<K, V>) genericEventChannel;
                castedEventChannel.removeListener(listener);
            }
        }
    }

    /**
     * Create an shared, concurrent map.
     *
     * @param mapName the shared map name.
     * @param typeK the type of the Key in the map.
     * @param typeV the type of the Value in the map.
     * @return the shared map for the channel name.
     */
    @Override
    public <K, V> IMap<K, V> getConcurrentMap(String mapName, Class<K> typeK,
            Class<V> typeV) {
        return hazelcastInstance.getMap(mapName);
    }

    /**
     * Create an shared, blocking queue.
     *
     * @param queueName the shared queue name.
     * @param typeT the type of the queue.
     * @return the shared queue for the queue name.
     */
    @Override
    public <T> BlockingQueue<T> getBlockingQueue(String queueName, Class<T> typeT) {
        return hazelcastInstance.getQueue(queueName);
    }

}
