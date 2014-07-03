package net.onrc.onos.core.topology;

import java.nio.ByteBuffer;
import java.util.Objects;

import net.onrc.onos.core.topology.web.serializers.LinkEventSerializer;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.apache.commons.lang.Validate;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Self-contained Link event Object.
 * <p/>
 * TODO: Rename to match what it is. (Switch/Port/Link/Device)Snapshot?
 * FIXME: Current implementation directly use this object as
 *        Replication message, but should be sending update operation info.
 */

@JsonSerialize(using = LinkEventSerializer.class)
public class LinkEvent extends TopologyElement<LinkEvent> {

    private final SwitchPort src;
    private final SwitchPort dst;
    // TODO add LastSeenTime, Capacity if appropriate

    /**
     * Default constructor for Serializer to use.
     */
    @Deprecated
    protected LinkEvent() {
        src = null;
        dst = null;
    }

    /**
     * Creates the Link object.
     *
     * @param src source SwitchPort
     * @param dst destination SwitchPort
     */
    public LinkEvent(SwitchPort src, SwitchPort dst) {
        Validate.notNull(src);
        Validate.notNull(dst);

        this.src = src;
        this.dst = dst;
    }

    /**
     * Create an unfrozen copy of given Object.
     *
     * @param original to make copy of.
     */
    public LinkEvent(LinkEvent original) {
        super(original);
        this.src = original.src;
        this.dst = original.dst;
    }

    // TODO probably want to remove this
    public LinkEvent(Link link) {
        src = new SwitchPort(link.getSrcSwitch().getDpid(),
                link.getSrcPort().getNumber());
        dst = new SwitchPort(link.getDstSwitch().getDpid(),
                link.getDstPort().getNumber());
        // FIXME losing attributes here
    }

    /**
     * Creates the Link object.
     *
     * @param srcDpid source switch DPID
     * @param srcPortNo source port number
     * @param dstDpid destination switch DPID
     * @param dstPortNo destination port number
     */
    public LinkEvent(Dpid srcDpid, PortNumber srcPortNo,
                     Dpid dstDpid, PortNumber dstPortNo) {
        src = new SwitchPort(srcDpid, srcPortNo);
        dst = new SwitchPort(dstDpid, dstPortNo);
    }

    /**
     * Gets the source SwitchPort.
     *
     * @return source SwitchPort.
     */
    public SwitchPort getSrc() {
        return src;
    }

    /**
     * Gets the destination SwitchPort.
     *
     * @return destination SwitchPort.
     */
    public SwitchPort getDst() {
        return dst;
    }

    @Override
    public String toString() {
        return "[LinkEvent " + src + "->" + dst + "]";
    }

    public static final int LINKID_BYTES = 2 + PortEvent.PORTID_BYTES * 2;

    public static ByteBuffer getLinkID(Dpid srcDpid, PortNumber srcPortNo,
                                       Dpid dstDpid, PortNumber dstPortNo) {
        return getLinkID(srcDpid.value(), (long) srcPortNo.value(),
                         dstDpid.value(), (long) dstPortNo.value());
    }

    public static ByteBuffer getLinkID(Long srcDpid, Long srcPortNo,
                                       Long dstDpid, Long dstPortNo) {
        return (ByteBuffer) ByteBuffer.allocate(LinkEvent.LINKID_BYTES)
                .putChar('L')
                .put(PortEvent.getPortID(srcDpid, srcPortNo))
                .put(PortEvent.getPortID(dstDpid, dstPortNo)).flip();
    }

    public byte[] getID() {
        return getIDasByteBuffer().array();
    }

    public ByteBuffer getIDasByteBuffer() {
        return getLinkID(src.getDpid(), src.getPortNumber(),
                dst.getDpid(), dst.getPortNumber());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((dst == null) ? 0 : dst.hashCode());
        result = prime * result + ((src == null) ? 0 : src.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }
        LinkEvent other = (LinkEvent) obj;

        // compare attributes
        if (!super.equals(obj)) {
            return false;
        }

        return Objects.equals(this.src, other.src) &&
                Objects.equals(this.dst, other.dst);
    }
}
