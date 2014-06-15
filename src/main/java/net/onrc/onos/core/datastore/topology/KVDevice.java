package net.onrc.onos.core.datastore.topology;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.onrc.onos.core.datastore.DataStoreClient;
import net.onrc.onos.core.datastore.IKVTable.IKVEntry;
import net.onrc.onos.core.datastore.serializers.Device.DeviceProperty;
import net.onrc.onos.core.datastore.topology.KVLink.STATUS;
import net.onrc.onos.core.datastore.utils.ByteArrayComparator;
import net.onrc.onos.core.datastore.utils.ByteArrayUtil;
import net.onrc.onos.core.datastore.utils.KVObject;
import net.onrc.onos.core.topology.DeviceEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Device object.
 * <p/>
 */
public class KVDevice extends KVObject {
    private static final Logger log = LoggerFactory.getLogger(KVDevice.class);

    private static final ThreadLocal<Kryo> DEVICE_KRYO = new ThreadLocal<Kryo>() {
        @Override
        protected Kryo initialValue() {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(true);
            kryo.setReferences(false);
            kryo.register(byte[].class);
            kryo.register(byte[][].class);
            kryo.register(HashMap.class);
            // TODO check if we should explicitly specify EnumSerializer
            kryo.register(STATUS.class);
            return kryo;
        }
    };

    static final String DEVICE_TABLE_SUFFIX = ":Device";

    private final byte[] mac;
    private TreeSet<byte[]> portIds;

    /**
     * Generate a DeviceID from MAC address.
     * <p/>
     * We're assuming MAC address can be an unique identifier for Device.
     *
     * @param mac MAC address
     * @return DeviceID
     */
    public static byte[] getDeviceID(final byte[] mac) {
        return DeviceEvent.getDeviceID(mac).array();
    }

    /**
     * Gets the MAC address from DeviceID.
     *
     * @param key DeviceID
     * @return MAC address
     */
    public static byte[] getMacFromKey(final byte[] key) {
        ByteBuffer keyBuf = ByteBuffer.wrap(key);
        if (keyBuf.getChar() != 'D') {
            throw new IllegalArgumentException("Invalid Device key");
        }
        byte[] mac = new byte[keyBuf.remaining()];
        keyBuf.get(mac);
        return mac;
    }

    /**
     * KVDevice constructor for default namespace.
     *
     * @param mac MAC address
     */
    public KVDevice(final byte[] mac) {
        this(mac, DEFAULT_NAMESPACE);
    }

    /**
     * KVDevice constructor for specified namespace.
     *
     * @param mac MAC address
     * @param namespace namespace to create this object
     */
    public KVDevice(final byte[] mac, final String namespace) {
        super(DataStoreClient.getClient()
                .getTable(namespace + DEVICE_TABLE_SUFFIX),
                getDeviceID(mac), namespace);

        this.mac = mac.clone();
        this.portIds = new TreeSet<>(ByteArrayComparator.BYTEARRAY_COMPARATOR);
    }

    /**
     * Gets an instance from DeviceID in default namespace.
     * <p/>
     * Note: You need to call `read()` to get the DB content.
     *
     * @param key DeviceID
     * @return KVDevice instance
     */
    public static KVDevice createFromKey(final byte[] key) {
        return createFromKey(key, DEFAULT_NAMESPACE);
    }

    /**
     * Gets an instance from DeviceID in specified namespace.
     * <p/>
     * Note: You need to call `read()` to get the DB content.
     *
     * @param key DeviceID
     * @param namespace namespace to create this object in
     * @return KVDevice instance
     */
    public static KVDevice createFromKey(final byte[] key, final String namespace) {
        return new KVDevice(getMacFromKey(key), namespace);
    }

    /**
     * Gets all the Devices in default namespace.
     *
     * @return Devices
     */
    public static Iterable<KVDevice> getAllDevices() {
        return new DeviceEnumerator(DEFAULT_NAMESPACE);
    }

    /**
     * Gets all the Devices in specified namespace.
     *
     * @param namespace namespace to iterate over
     * @return Devices
     */
    public static Iterable<KVDevice> getAllDevices(final String namespace) {
        return new DeviceEnumerator(namespace);
    }

    /**
     * Utility class to provide Iterable interface.
     */
    public static class DeviceEnumerator implements Iterable<KVDevice> {

        private final String namespace;

        /**
         * Constructor to iterate Links in specified namespace.
         *
         * @param namespace namespace to iterate through
         */
        public DeviceEnumerator(final String namespace) {
            this.namespace = namespace;
        }

        @Override
        public Iterator<KVDevice> iterator() {
            return new DeviceIterator(namespace);
        }
    }

    /**
     * Utility class to provide Iterator over all the Device objects.
     */
    public static class DeviceIterator extends AbstractObjectIterator<KVDevice> {

        /**
         * Constructor to create an iterator to iterate all the Devices
         * in specified namespace.
         *
         * @param namespace namespace to iterate through
         */
        public DeviceIterator(final String namespace) {
            super(DataStoreClient.getClient()
                    .getTable(namespace + DEVICE_TABLE_SUFFIX), namespace);
        }

        @Override
        public KVDevice next() {
            IKVEntry o = enumerator.next();
            KVDevice e = KVDevice.createFromKey(o.getKey(), namespace);
            e.deserialize(o.getValue(), o.getVersion());
            return e;
        }
    }

    /**
     * Gets the MAC address.
     *
     * @return MAC address
     */
    public byte[] getMac() {
        return mac.clone();
    }

    /**
     * Gets the DeviceID.
     *
     * @return DeviceID
     */
    public byte[] getId() {
        return getKey();
    }

    /**
     * Add a port to this Device's attachment points.
     *
     * @param portId PortID of the port which this Device is attached
     */
    public void addPortId(final byte[] portId) {
        portIds.add(portId.clone());
    }

    /**
     * Remove a port from this Device's attachment points.
     *
     * @param portId PortID to remove
     */
    public void removePortId(final byte[] portId) {
        portIds.remove(portId);
    }

    /**
     * Empty this Device's attachment points.
     */
    public void emptyPortIds() {
        portIds.clear();
    }

    /**
     * Add ports to this Device's attachment points.
     *
     * @param newPortIds PortIDs which this Device is attached
     */
    public void addAllToPortIds(final Collection<byte[]> newPortIds) {
        // TODO: Should we copy each portId, or reference is OK.
        portIds.addAll(newPortIds);
    }

    /**
     * Gets all the PortIDs which this Device is attached.
     *
     * @return Unmodifiable Set view of all the PortIds;
     */
    public Set<byte[]> getAllPortIds() {
        return Collections.unmodifiableSet(portIds);
    }

    @Override
    public byte[] serialize() {
        Map<Object, Object> map = getPropertyMap();

        DeviceProperty.Builder dev = DeviceProperty.newBuilder();

        dev.setMac(ByteString.copyFrom(mac));
        for (byte[] port : portIds) {
            dev.addPortIds(ByteString.copyFrom(port));
        }

        if (!map.isEmpty()) {
            byte[] propMaps = serializePropertyMap(DEVICE_KRYO.get(), map);
            dev.setValue(ByteString.copyFrom(propMaps));
        }

        return dev.build().toByteArray();
    }

    @Override
    protected boolean deserialize(final byte[] bytes) {

        try {
            boolean success = true;

            DeviceProperty dev = DeviceProperty.parseFrom(bytes);
            for (ByteString portId : dev.getPortIdsList()) {
                this.addPortId(portId.toByteArray());
            }
            byte[] props = dev.getValue().toByteArray();
            success &= deserializePropertyMap(DEVICE_KRYO.get(), props);
            return success;
        } catch (InvalidProtocolBufferException e) {
            log.error("Deserializing Device: " + this + " failed.", e);
            return false;
        }
    }

    @Override
    public String toString() {
        // TODO output all properties?
        return "[" + this.getClass().getSimpleName()
                + " " + ByteArrayUtil.toHexStringBuilder(mac, ":") + "]";
    }
}
