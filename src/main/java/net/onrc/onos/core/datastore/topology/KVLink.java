package net.onrc.onos.core.datastore.topology;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.onrc.onos.core.datastore.DataStoreClient;
import net.onrc.onos.core.datastore.IKVTable.IKVEntry;
import net.onrc.onos.core.datastore.serializers.Topology.LinkProperty;
import net.onrc.onos.core.datastore.utils.KVObject;
import net.onrc.onos.core.topology.LinkEvent;
import net.onrc.onos.core.topology.PortEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Link object in data store.
 */
public class KVLink extends KVObject {
    private static final Logger log = LoggerFactory.getLogger(KVLink.class);

    private static final ThreadLocal<Kryo> LINK_KRYO = new ThreadLocal<Kryo>() {
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

    /**
     * Internal data structure to represent a port on a switch.
     */
    public static class SwitchPort {
        public final Long dpid;
        public final Long number;

        /**
         * Constructor.
         *
         * @param dpid datapath ID of this switch port
         * @param number port number of this port on switch({@code dpid})
         */
        public SwitchPort(final Long dpid, final Long number) {
            this.dpid = dpid;
            this.number = number;
        }

        /**
         * Gets the PortID of a port this object represent.
         *
         * @return PortID
         */
        public byte[] getPortID() {
            return KVPort.getPortID(dpid, number);
        }

        /**
         * Gets the SwitchID of a switch this object represent.
         *
         * @return SwitchID
         */
        public byte[] getSwitchID() {
            return KVSwitch.getSwitchID(dpid);
        }

        @Override
        public String toString() {
            return "(" + Long.toHexString(dpid) + "@" + number + ")";
        }

    }

    static final String LINK_TABLE_SUFFIX = ":Link";

    // must not re-order enum members, ordinal will be sent over wire
    /**
     * Status.
     */
    public enum STATUS {
        INACTIVE, ACTIVE;
    }

    private final SwitchPort src;
    private final SwitchPort dst;
    private STATUS status;

    /**
     * Generate a LinkID from Link 4-tuples.
     *
     * @param srcDpid source DPID
     * @param srcPortNo source port number
     * @param dstDpid destination DPID
     * @param dstPortNo destination port number
     * @return LinkID
     */
    public static byte[] getLinkID(final Long srcDpid, final Long srcPortNo,
                                   final Long dstDpid, final Long dstPortNo) {
        return LinkEvent.getLinkID(srcDpid, srcPortNo, dstDpid,
                dstPortNo).array();
    }

    /**
     * Gets Link 4-tuples from LinkID.
     *
     * @param key LinkID
     * @return Link 4-tuple: [src DPID, src PortNo, dst DPID, dst PortNo]
     */
    public static long[] getLinkTupleFromKey(final byte[] key) {
        return getLinkTupleFromKey(ByteBuffer.wrap(key));
    }

    /**
     * Gets Link 4-tuples from LinkID.
     *
     * @param keyBuf LinkID
     * @return Link 4-tuple: [src DPID, src PortNo, dst DPID, dst PortNo]
     */
    public static long[] getLinkTupleFromKey(final ByteBuffer keyBuf) {
        if (keyBuf.getChar() != 'L') {
            throw new IllegalArgumentException("Invalid Link key");
        }
        final long[] srcPortPair = KVPort.getPortPairFromKey(keyBuf.slice());
        keyBuf.position(2 + PortEvent.PORTID_BYTES);
        final long[] dstPortPair = KVPort.getPortPairFromKey(keyBuf.slice());

        long[] tuple = new long[4];
        tuple[0] = srcPortPair[0];
        tuple[1] = srcPortPair[1];
        tuple[2] = dstPortPair[0];
        tuple[3] = dstPortPair[1];

        return tuple;
    }


    /**
     * KVLink constructor for default namespace.
     *
     * @param srcDpid source DPID
     * @param srcPortNo source port number
     * @param dstDpid destination DPID
     * @param dstPortNo destination port number
     */
    public KVLink(final Long srcDpid, final Long srcPortNo,
                  final Long dstDpid, final Long dstPortNo) {
        this(srcDpid, srcPortNo, dstDpid, dstPortNo, DEFAULT_NAMESPACE);
    }

    /**
     * KVLink constructor for specified namespace.
     *
     * @param srcDpid source DPID
     * @param srcPortNo source port number
     * @param dstDpid destination DPID
     * @param dstPortNo destination port number
     * @param namespace namespace to create this object
     */
    public KVLink(final Long srcDpid, final Long srcPortNo,
                  final Long dstDpid, final Long dstPortNo,
                  final String namespace) {
        super(DataStoreClient.getClient()
                .getTable(namespace + LINK_TABLE_SUFFIX),
                getLinkID(srcDpid, srcPortNo, dstDpid, dstPortNo),
                namespace);

        src = new SwitchPort(srcDpid, srcPortNo);
        dst = new SwitchPort(dstDpid, dstPortNo);
        status = STATUS.INACTIVE;
    }

    /**
     * Gets an instance from LinkID in default namespace.
     * <p/>
     * Note: You need to call `read()` to get the DB content.
     *
     * @param key LinkID
     * @return KVLink instance
     */
    public static KVLink createFromKey(final byte[] key) {
        return createFromKey(key, DEFAULT_NAMESPACE);
    }

    /**
     * Gets an instance from LinkID in specified namespace.
     * <p/>
     * Note: You need to call `read()` to get the DB content.
     *
     * @param key LinkID
     * @param namespace namespace to create this object in
     * @return KVLink instance
     */
    public static KVLink createFromKey(final byte[] key, final String namespace) {
        long[] linkTuple = getLinkTupleFromKey(key);
        return new KVLink(linkTuple[0], linkTuple[1],
                          linkTuple[2], linkTuple[3],
                          namespace);
    }

    /**
     * Gets all the Links in default namespace.
     *
     * @return Links
     */
    public static Iterable<KVLink> getAllLinks() {
        return getAllLinks(DEFAULT_NAMESPACE);
    }

    /**
     * Gets all the Links in specified namespace.
     *
     * @param namespace namespace to iterate over
     * @return Links
     */
    public static Iterable<KVLink> getAllLinks(final String namespace) {
        return new LinkEnumerator(namespace);
    }

    /**
     * Utility class to provide Iterable interface.
     */
    public static class LinkEnumerator implements Iterable<KVLink> {

        private final String namespace;

        /**
         * Constructor to iterate Links in specified namespace.
         *
         * @param namespace namespace to iterate through
         */
        public LinkEnumerator(final String namespace) {
            this.namespace = namespace;
        }

        @Override
        public Iterator<KVLink> iterator() {
            return new LinkIterator(namespace);
        }
    }

    /**
     * Utility class to provide Iterator over all the Link objects.
     */
    public static class LinkIterator extends AbstractObjectIterator<KVLink> {

        /**
         * Constructor to create an iterator to iterate all the Links
         * in specified namespace.
         *
         * @param namespace namespace to iterate through
         */
        public LinkIterator(final String namespace) {
            super(DataStoreClient.getClient()
                    .getTable(namespace + LINK_TABLE_SUFFIX),
                    namespace);
        }

        @Override
        public KVLink next() {
            IKVEntry o = enumerator.next();
            KVLink e = KVLink.createFromKey(o.getKey(), namespace);
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
     * Gets the source SwitchPort object.
     *
     * @return source SwitchPort object
     */
    public SwitchPort getSrc() {
        return src;
    }

    /**
     * Gets the destination SwitchPort object.
     *
     * @return destination SwitchPort object
     */
    public SwitchPort getDst() {
        return dst;
    }

    /**
     * Gets the LinkID of this object.
     *
     * @return LinkID
     */
    public byte[] getId() {
        return getKey();
    }

    @Override
    public byte[] serialize() {
        Map<Object, Object> map = getPropertyMap();

        LinkProperty.Builder link = LinkProperty.newBuilder();
        link.setSrcSwId(ByteString.copyFrom(src.getSwitchID()));
        link.setSrcPortId(ByteString.copyFrom(src.getPortID()));
        link.setDstSwId(ByteString.copyFrom(dst.getSwitchID()));
        link.setDstPortId(ByteString.copyFrom(dst.getPortID()));
        link.setStatus(status.ordinal());

        if (!map.isEmpty()) {
            byte[] propMaps = serializePropertyMap(LINK_KRYO.get(), map);
            link.setValue(ByteString.copyFrom(propMaps));
        }

        return link.build().toByteArray();
    }

    @Override
    protected boolean deserialize(final byte[] bytes) {
        try {
            boolean success = true;

            LinkProperty link = LinkProperty.parseFrom(bytes);
            byte[] props = link.getValue().toByteArray();
            success &= deserializePropertyMap(LINK_KRYO.get(), props);
            this.status = STATUS.values()[link.getStatus()];

            return success;
        } catch (InvalidProtocolBufferException e) {
            log.error("Deserializing Link: " + this + " failed.", e);
            return false;
        }
    }

    @Override
    public String toString() {
        // TODO output all properties?
        return "[" + this.getClass().getSimpleName()
                + " " + src + "->" + dst + " STATUS:" + status + "]";
    }
}
