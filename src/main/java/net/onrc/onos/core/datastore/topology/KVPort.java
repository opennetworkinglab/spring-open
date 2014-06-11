package net.onrc.onos.core.datastore.topology;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.onrc.onos.core.datastore.DataStoreClient;
import net.onrc.onos.core.datastore.IKVTable.IKVEntry;
import net.onrc.onos.core.datastore.serializers.Topology.PortProperty;
import net.onrc.onos.core.datastore.utils.ByteArrayUtil;
import net.onrc.onos.core.datastore.utils.KVObject;
import net.onrc.onos.core.topology.PortEvent;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Port object in data store.
 * <p/>
 * Note: This class will not maintain invariants.
 * e.g., It will NOT automatically remove Links or Devices on Port,
 * when deleting a Port.
 */
public class KVPort extends KVObject {
    private static final Logger log = LoggerFactory.getLogger(KVPort.class);

    private static final ThreadLocal<Kryo> PORT_KRYO = new ThreadLocal<Kryo>() {
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

    static final String PORT_TABLE_SUFFIX = ":Port";

    // must not re-order enum members, ordinal will be sent over wire
    /**
     * Status.
     */
    public enum STATUS {
        INACTIVE, ACTIVE;
    }

    private final Long dpid;
    private final Long number;

    private STATUS status;

    /**
     * Generate a PortID from port pair (dpid, number).
     *
     * @param dpid DPID of a switch it reside on.
     * @param number port number of this
     * @return PortID
     */
    public static byte[] getPortID(final Dpid dpid, final PortNumber number) {
        return PortEvent.getPortID(dpid, number).array();
    }

    /**
     * Generate a PortID from port pair (dpid, number).
     *
     * @param dpid DPID of a switch it reside on.
     * @param number port number of this
     * @return PortID
     */
    public static byte[] getPortID(final Long dpid, final Long number) {
        return PortEvent.getPortID(dpid, number).array();
    }

    /**
     * Gets the port pair from PortID.
     *
     * @param key PortID
     * @return port pair (dpid, number)
     */
    public static long[] getPortPairFromKey(final byte[] key) {
        return getPortPairFromKey(ByteBuffer.wrap(key));
    }

    /**
     * Gets the port pair from PortID.
     *
     * @param keyBuf PortID
     * @return port pair (dpid, number)
     */
    public static long[] getPortPairFromKey(final ByteBuffer keyBuf) {
        if (keyBuf.getChar() != 'S') {
            throw new IllegalArgumentException("Invalid Port key:" + keyBuf
                    + " "
                    + ByteArrayUtil.toHexStringBuilder(keyBuf.array(), ":"));
        }
        long[] pair = new long[2];
        pair[0] = keyBuf.getLong();
        if (keyBuf.getChar() != 'P') {
            throw new IllegalArgumentException("Invalid Port key:" + keyBuf
                    + " "
                    + ByteArrayUtil.toHexStringBuilder(keyBuf.array(), ":"));
        }
        pair[1] = keyBuf.getLong();
        return pair;

    }

    /**
     * Gets the port number from PortID.
     *
     * @param key PortID
     * @return port number
     */
    public static long getDpidFromKey(final byte[] key) {
        return getPortPairFromKey(key)[0];
    }

    /**
     * Gets the dpid of an switch from PortID.
     *
     * @param key PortID
     * @return dpid
     */
    public static long getNumberFromKey(final byte[] key) {
        return getPortPairFromKey(key)[1];
    }

    /**
     * KVPort constructor for default namespace.
     *
     * @param dpid DPID of the switch this port is on
     * @param number port number of this port
     */
    public KVPort(final Long dpid, final Long number) {
        this(dpid, number, DEFAULT_NAMESPACE);
    }

    /**
     * KVPort constructor for specified namespace.
     *
     * @param dpid DPID of the switch this port is on
     * @param number port number of this port
     * @param namespace namespace to create this object
     */
    public KVPort(final Long dpid, final Long number, final String namespace) {
        super(DataStoreClient.getClient()
                .getTable(namespace + PORT_TABLE_SUFFIX),
                getPortID(dpid, number), namespace);

        this.dpid = dpid;
        this.number = number;
        this.status = STATUS.INACTIVE;
    }

    /**
     * KVPort constructor for default namespace.
     *
     * @param dpid DPID of the switch this port is on
     * @param number port number of this port
     */
    public KVPort(final Dpid dpid, final PortNumber number) {
        this(dpid, number, DEFAULT_NAMESPACE);
    }

    /**
     * KVPort constructor for specified namespace.
     *
     * @param dpid DPID of the switch this port is on
     * @param number port number of this port
     * @param namespace namespace to create this object
     */
    public KVPort(final Dpid dpid, final PortNumber number, final String namespace) {
        this(dpid.value(), number.value(), namespace);
    }

    /**
     * Gets an instance from PortID in default namespace.
     * <p/>
     * Note: You need to call `read()` to get the DB content.
     *
     * @param key PortID
     * @return {@link KVPort} instance
     */
    public static KVPort createFromKey(final byte[] key) {
        return createFromKey(key, DEFAULT_NAMESPACE);
    }

    /**
     * Gets an instance from PortID in specified namespace.
     * <p/>
     * Note: You need to call `read()` to get the DB content.
     *
     * @param key PortID
     * @param namespace namespace to create this object.
     * @return {@link KVPort} instance
     */
    public static KVPort createFromKey(final byte[] key, final String namespace) {
        long[] pair = getPortPairFromKey(key);
        return new KVPort(pair[0], pair[1], namespace);
    }

    /**
     * Gets all the Ports in default namespace.
     *
     * @return Ports
     */
    public static Iterable<KVPort> getAllPorts() {
        return getAllPorts(DEFAULT_NAMESPACE);
    }

    /**
     * Gets all the Ports in specified namespace.
     *
     * @param namespace namespace to iterate over
     * @return Ports
     */
    public static Iterable<KVPort> getAllPorts(final String namespace) {
        return new PortEnumerator(namespace);
    }

    /**
     * Utility class to provide Iterable interface.
     */
    public static class PortEnumerator implements Iterable<KVPort> {

        private final String namespace;

        /**
         * Constructor to iterate Ports in specified namespace.
         *
         * @param namespace namespace to iterate through
         */
        public PortEnumerator(final String namespace) {
            this.namespace = namespace;
        }

        @Override
        public Iterator<KVPort> iterator() {
            return new PortIterator(namespace);
        }
    }

    /**
     * Utility class to provide Iterator over all the Port objects.
     */
    public static class PortIterator extends AbstractObjectIterator<KVPort> {

        /**
         * Constructor to create an iterator to iterate all the Ports
         * in specified namespace.
         *
         * @param namespace namespace to iterate through
         */
        public PortIterator(final String namespace) {
            super(DataStoreClient.getClient()
                    .getTable(namespace + PORT_TABLE_SUFFIX), namespace);
        }

        @Override
        public KVPort next() {
            IKVEntry o = enumerator.next();
            KVPort e = KVPort.createFromKey(o.getKey(), namespace);
            e.deserialize(o.getValue(), o.getVersion());
            return e;
        }
    }

    /**
     * Gets the status.
     *
     * @return status
     */
    public STATUS getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status new status
     */
    public void setStatus(final STATUS status) {
        this.status = status;
    }

    /**
     * Gets the DPID of the switch this port is on.
     *
     * @return DPID of the switch this port is on
     */
    public Long getDpid() {
        return dpid;
    }

    /**
     * Gets the port number of this port.
     *
     * @return port number of this port
     */
    public Long getNumber() {
        return number;
    }

    /**
     * Gets the PortID.
     *
     * @return PortID
     */
    public byte[] getId() {
        return getKey();
    }

    @Override
    public byte[] serialize() {
        Map<Object, Object> map = getPropertyMap();

        PortProperty.Builder port = PortProperty.newBuilder();
        port.setDpid(dpid);
        port.setNumber(number);
        port.setStatus(status.ordinal());

        if (!map.isEmpty()) {
            byte[] propMaps = serializePropertyMap(PORT_KRYO.get(), map);
            port.setValue(ByteString.copyFrom(propMaps));
        }

        return port.build().toByteArray();
    }

    @Override
    protected boolean deserialize(final byte[] bytes) {
        try {
            boolean success = true;

            PortProperty port = PortProperty.parseFrom(bytes);
            byte[] props = port.getValue().toByteArray();
            success &= deserializePropertyMap(PORT_KRYO.get(), props);
            this.status = STATUS.values()[port.getStatus()];

            return success;
        } catch (InvalidProtocolBufferException e) {
            log.error("Deserializing Port: " + this + " failed.", e);
            return false;
        }
    }

    @Override
    public String toString() {
        // TODO output all properties?
        return "[" + this.getClass().getSimpleName()
                + " 0x" + Long.toHexString(dpid) + "@" + number
                + " STATUS:" + status + "]";
    }
}
