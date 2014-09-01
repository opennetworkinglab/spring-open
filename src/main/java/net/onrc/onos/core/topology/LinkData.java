package net.onrc.onos.core.topology;

import java.nio.ByteBuffer;
import java.util.Objects;

import net.onrc.onos.core.topology.web.serializers.LinkDataSerializer;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.LinkTuple;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import static com.google.common.base.Preconditions.checkNotNull;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Self-contained Link object.
 */

@JsonSerialize(using = LinkDataSerializer.class)
public class LinkData extends TopologyElement<LinkData> {
    public static final int LINKID_BYTES = 2 + PortData.PORTID_BYTES * 2;

    private final LinkTuple id;
    // TODO add LastSeenTime, Capacity if appropriate
    protected static final Double DEFAULT_CAPACITY = Double.POSITIVE_INFINITY;
    private Double capacity = DEFAULT_CAPACITY;

    /**
     * Default constructor for Serializer to use.
     */
    @Deprecated
    protected LinkData() {
        id = null;
    }

    /**
     * Constructor for given LinkTuple.
     *
     * @param id the link tuple to identify the link
     */
    public LinkData(LinkTuple id) {
        this.id = checkNotNull(id);
    }

    /**
     * Constructor for given source and destination switch ports.
     *
     * @param src the source SwitchPort to use
     * @param dst the destination SwitchPort to use
     */
    public LinkData(SwitchPort src, SwitchPort dst) {
        this(new LinkTuple(src, dst));
    }

    /**
     * Constructor for a given Link object.
     * <p>
     * TODO: This constructor should probably be removed.
     *
     * @param link the Link object to use.
     */
    public LinkData(Link link) {
        this(link.getLinkTuple());
        // FIXME losing attributes here
    }

    /**
     * Copy constructor.
     * <p>
     * Creates an unfrozen copy of the given LinkData object.
     *
     * @param original the object ot make copy of
     */
    public LinkData(LinkData original) {
        super(original);
        this.id = original.id;
    }

    /**
     * Gets the LinkTuple that identifies this link.
     *
     * @return the LinkTuple identifying this link
     */
    public LinkTuple getLinkTuple() {
        return id;
    }

    /**
     * Gets the link source SwitchPort.
     *
     * @return the link source SwitchPort
     */
    public SwitchPort getSrc() {
        return getLinkTuple().getSrc();
    }

    /**
     * Gets the link destination SwitchPort.
     *
     * @return the link destination SwitchPort
     */
    public SwitchPort getDst() {
        return getLinkTuple().getDst();
    }

    /**
     * Gets the link capacity.
     * <p>
     * TODO: What is the unit?
     *
     * @return the link capacity
     */
    public Double getCapacity() {
        return capacity;
    }

    /**
     * Sets the link capacity.
     * <p>
     * TODO: What is the unit?
     *
     * @param capacity the link capacity to set
     */
    void setCapacity(Double capacity) {
        if (isFrozen()) {
            throw new IllegalStateException("Tried to modify frozen instance: " + this);
        }

        this.capacity = capacity;
    }

    /**
     * Computes the link ID for given source and destination switch DPID and
     * port numbers.
     *
     * @param srcDpid the source switch DPID to use
     * @param srcPortNo the source port number to use
     * @param dstDpid the destination switch DPID to use
     * @param dstPortNo the destination port number to use
     * @return the link ID as a ByteBuffer
     */
    public static ByteBuffer getLinkID(Dpid srcDpid, PortNumber srcPortNo,
                                       Dpid dstDpid, PortNumber dstPortNo) {
        return getLinkID(srcDpid.value(), srcPortNo.value(),
                         dstDpid.value(), dstPortNo.value());
    }

    /**
     * Computes the link ID for given source and destination switch DPID and
     * port numbers.
     * <p>
     * TODO: This method should be removed and replaced with the corresponding
     * getLinkID(Dpid, PortNumber, Dpid, PortNumber) method.
     *
     * @param srcDpid the source switch DPID to use
     * @param srcPortNo the source port number to use
     * @param dstDpid the destination switch DPID to use
     * @param dstPortNo the destination port number to use
     * @return the link ID as a ByteBuffer
     */
    public static ByteBuffer getLinkID(Long srcDpid, Long srcPortNo,
                                       Long dstDpid, Long dstPortNo) {
        return (ByteBuffer) ByteBuffer.allocate(LinkData.LINKID_BYTES)
                .putChar('L')
                .put(PortData.getPortID(srcDpid, srcPortNo))
                .put(PortData.getPortID(dstDpid, dstPortNo)).flip();
    }

    @Override
    public Dpid getOriginDpid() {
        return this.id.getDst().getDpid();
    }

    @Override
    public ByteBuffer getIDasByteBuffer() {
        return getLinkID(getSrc().getDpid(), getSrc().getPortNumber(),
                getDst().getDpid(), getDst().getPortNumber());
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        // Compare attributes
        if (!super.equals(o)) {
            return false;
        }

        LinkData other = (LinkData) o;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return "[LinkData " + getSrc() + "->" + getDst() + "]";
    }
}
