package net.onrc.onos.core.newintent;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IMap;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.core.datagrid.ISharedCollectionsService;
import net.onrc.onos.core.util.serializers.KryoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A class representing map storing an intent related value associated with
 * intent ID as key.
 * <p>
 * Implementation-Specific: The backend of this data structure is Hazelcast IMap.
 * </p>
 * FIXME: refactor this class to aggregate logic for distributed listenable map.
 * Intent Service, Flow Manager, and Match-Action Service would want to have similar
 * logic to store and load the data from a distributed data structure, but these logic
 * is scattered in each package now.
 *
 * @param <V> the type of value
 */
class IntentMap<V> {
    private static final Logger log = LoggerFactory.getLogger(IntentMap.class);

    private final Class<V> valueType;
    private final IMap<String, byte[]> map;

    /**
     * Constructs a map which stores intent related information with the specified arguments.
     *
     * @param name name of the map
     * @param valueType type of value
     * @param collectionsService service for creating Hazelcast IMap
     */
    public IntentMap(String name, Class<V> valueType, ISharedCollectionsService collectionsService) {
        this.valueType = checkNotNull(valueType);

        this.map = checkNotNull(collectionsService.getConcurrentMap(name, String.class, byte[].class));
    }

    /**
     * Stores the specified value associated with the intent ID.
     *
     * @param id intent ID
     * @param value value
     */
    public void put(IntentId id, V value) {
        checkNotNull(id);
        checkNotNull(value);

        map.set(id.toString(), KryoFactory.serialize(value));
    }

    /**
     * Returns the value associated with the specified intent ID.
     *
     * @param id intent ID
     * @return the value associated with the key
     */
    public V get(IntentId id) {
        checkNotNull(id);

        byte[] bytes = map.get(id.toString());
        if (bytes == null) {
            return null;
        }

        return KryoFactory.deserialize(bytes);
    }

    /**
     * Removes the value associated with the specified intent ID.
     *
     * @param id intent ID
     */
    public void remove(IntentId id) {
        checkNotNull(id);

        map.remove(id.toString());
    }

    /**
     * Returns all values stored in the instance.
     *
     * @return all values stored in the sintance.
     */
    public Collection<V> values() {
        Collection<V> values = new ArrayList<>();
        for (byte[] bytes : map.values()) {
            V value = KryoFactory.deserialize(bytes);
            if (value == null) {
                continue;
            }

            values.add(value);
        }

        return values;
    }

    /**
     * Adds an entry listener for this map. Listener will get notified for all events.
     *
     * @param listener entry listener
     */
    public void addListener(final EntryListener<IntentId, V> listener) {
        checkNotNull(listener);

        EntryListener<String, byte[]> internalListener = new EntryListener<String, byte[]>() {
            @Override
            public void entryAdded(EntryEvent<String, byte[]> event) {
                listener.entryAdded(convertEntryEvent(event));
            }

            @Override
            public void entryRemoved(EntryEvent<String, byte[]> event) {
                listener.entryRemoved(convertEntryEvent(event));
            }

            @Override
            public void entryUpdated(EntryEvent<String, byte[]> event) {
                listener.entryUpdated(convertEntryEvent(event));
            }

            @Override
            public void entryEvicted(EntryEvent<String, byte[]> event) {
                listener.entryEvicted(convertEntryEvent(event));
            }

            /**
             * Converts an entry event used internally to another entry event exposed externally.
             *
             * @param internalEvent entry event used internally used
             * @return entry event exposed externally
             */
            private EntryEvent<IntentId, V> convertEntryEvent(EntryEvent<String, byte[]> internalEvent) {
                EntryEvent<IntentId, V> converted =
                        new EntryEvent<>(
                                internalEvent.getSource(),
                                internalEvent.getMember(),
                                internalEvent.getEventType().getType(),
                                IntentId.valueOf(internalEvent.getKey()),
                                KryoFactory.<V>deserialize(internalEvent.getValue())
                        );
                return converted;
            }
        };

        map.addEntryListener(internalListener, true);
    }

    /**
     * Destroys the backend Hazelcast IMap. This method is only for testing purpose.
     */
    void destroy() {
        map.destroy();
    }
}
