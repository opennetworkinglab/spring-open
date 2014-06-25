package net.onrc.onos.core.datastore.topology;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.onrc.onos.core.datastore.DataStoreClient;
import net.onrc.onos.core.datastore.IKVTable.IKVEntry;
import net.onrc.onos.core.datastore.serializers.Topology.SwitchProperty;
import net.onrc.onos.core.datastore.utils.KVObject;
import net.onrc.onos.core.topology.SwitchEvent;
import net.onrc.onos.core.util.Dpid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Switch object in data store.
 * <p/>
 * Note: This class will not maintain invariants.
 * e.g., It will NOT automatically remove Ports on Switch,
 * when deleting a Switch.
 */
public class KVSwitch extends KVObject {
    private static final Logger log = LoggerFactory.getLogger(KVSwitch.class);

    private static final ThreadLocal<Kryo> SWITCH_KRYO = new ThreadLocal<Kryo>() {
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

    static final String SWITCH_TABLE_SUFFIX = ":Switch";

    // must not re-order enum members, ordinal will be sent over wire
    /**
     * Status.
     */
    public enum STATUS {
        INACTIVE, ACTIVE;
    }

    private final Long dpid;
    private STATUS status;

    /**
     * Generate a SwitchID from dpid.
     *
     * @param dpid dpid of the switch
     * @return SwitchID
     */
    public static byte[] getSwitchID(final Long dpid) {
        return SwitchEvent.getSwitchID(dpid).array();
    }

    /**
     * Gets the DPID from SwitchID.
     *
     * @param key SwitchID
     * @return dpid
     */
    public static long getDpidFromKey(final byte[] key) {
        return getDpidFromKey(ByteBuffer.wrap(key));
    }

    /**
     * Gets the DPID from SwitchID.
     *
     * @param keyBuf SwitchID
     * @return dpid
     */
    public static long getDpidFromKey(final ByteBuffer keyBuf) {
        if (keyBuf.getChar() != 'S') {
            throw new IllegalArgumentException("Invalid Switch key");
        }
        return keyBuf.getLong();
    }

    // FIXME Should the parameter be DPID here, or should caller specify the key?
    // Should layer above have the control of the ID computation/generation?
    /**
     * KVSwitch constructor for default namespace.
     *
     * @param dpid dpid of this switch
     */
    public KVSwitch(final Long dpid) {
        this(dpid, KVObject.DEFAULT_NAMESPACE);
    }

    /**
     * KVSwitch constructor for specified namespace.
     *
     * @param dpid dpid of this switch
     * @param namespace namespace to create this object
     */
    public KVSwitch(final Long dpid, final String namespace) {
        super(DataStoreClient.getClient()
                .getTable(namespace + SWITCH_TABLE_SUFFIX),
                getSwitchID(dpid), namespace);

        this.dpid = dpid;
        this.status = STATUS.INACTIVE;
        // may need to store namespace here or at KVObject.
    }

    /**
     * KVSwitch constructor for default namespace.
     *
     * @param dpid dpid of this switch
     */
    public KVSwitch(final Dpid dpid) {
        this(dpid, KVObject.DEFAULT_NAMESPACE);
    }

    /**
     * KVSwitch constructor for specified namespace.
     *
     * @param dpid dpid of this switch
     * @param namespace namespace to create this object
     */
    public KVSwitch(final Dpid dpid, final String namespace) {
        this(dpid.value(), namespace);
    }

    /**
     * Gets an instance from SwitchID in default namespace.
     * <p/>
     * Note: You need to call `read()` to get the DB content.
     *
     * @param key SwitchID
     * @return {@link KVSwitch} instance
     */
    public static KVSwitch createFromKey(final byte[] key) {
        return new KVSwitch(getDpidFromKey(key));
    }

    /**
     * Gets an instance from SwitchID in specified namespace.
     * <p/>
     * Note: You need to call `read()` to get the DB content.
     *
     * @param key SwitchID
     * @param namespace namespace to create this object
     * @return {@link KVSwitch} instance
     */
    public static KVSwitch createFromKey(final byte[] key, final String namespace) {
        return new KVSwitch(getDpidFromKey(key), namespace);
    }

    /**
     * Gets all the switches in default namespace.
     *
     * @return All the {@link KVSwitch}
     */
    public static Iterable<KVSwitch> getAllSwitches() {
        return getAllSwitches(DEFAULT_NAMESPACE);
    }

    /**
     * Gets all the switches in specified namespace.
     *
     * @param namespace Namespace to get all switches.
     * @return All the {@link KVSwitch}
     */
    public static Iterable<KVSwitch> getAllSwitches(final String namespace) {
        return new SwitchEnumerator(namespace);
    }

    /**
     * Utility class to provide Iterable interface.
     */
    public static class SwitchEnumerator implements Iterable<KVSwitch> {

        private final String namespace;

        /**
         * Constructor to iterate Ports in specified namespace.
         *
         * @param namespace namespace to iterate through
         */
        public SwitchEnumerator(final String namespace) {
            this.namespace = namespace;
        }

        @Override
        public Iterator<KVSwitch> iterator() {
            return new SwitchIterator(namespace);
        }
    }

    /**
     * Utility class to provide Iterator over all the Switch objects.
     */
    public static class SwitchIterator extends AbstractObjectIterator<KVSwitch> {

        /**
         * Constructor to create an iterator to iterate all the Switches
         * in specified namespace.
         *
         * @param namespace namespace to iterate through
         */
        public SwitchIterator(final String namespace) {
            super(DataStoreClient.getClient()
                    .getTable(namespace + SWITCH_TABLE_SUFFIX), namespace);
        }

        @Override
        public KVSwitch next() {
            IKVEntry o = enumerator.next();
            KVSwitch e = KVSwitch.createFromKey(o.getKey(), namespace);
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
     * Gets the SwitchID.
     *
     * @return SwitchID
     */
    public byte[] getId() {
        return getKey();
    }

    @Override
    public byte[] serialize() {
        Map<Object, Object> map = getPropertyMap();

        SwitchProperty.Builder sw = SwitchProperty.newBuilder();
        sw.setDpid(dpid);
        sw.setStatus(status.ordinal());

        if (!map.isEmpty()) {
            byte[] propMaps = serializePropertyMap(SWITCH_KRYO.get(), map);
            sw.setValue(ByteString.copyFrom(propMaps));
        }

        return sw.build().toByteArray();
    }

    @Override
    protected boolean deserialize(final byte[] bytes) {
        try {
            boolean success = true;

            SwitchProperty sw = SwitchProperty.parseFrom(bytes);
            byte[] props = sw.getValue().toByteArray();
            success &= deserializePropertyMap(SWITCH_KRYO.get(), props);
            this.status = STATUS.values()[sw.getStatus()];

            return success;
        } catch (InvalidProtocolBufferException e) {
            log.error("Deserializing Switch: " + this + " failed.", e);
            return false;
        }
    }

    @Override
    public String toString() {
        // TODO output all properties?
        return "[" + this.getClass().getSimpleName()
                + " 0x" + Long.toHexString(dpid) + " STATUS:" + status + "]";
    }

}
