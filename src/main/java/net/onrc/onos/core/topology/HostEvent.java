package net.onrc.onos.core.topology;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.topology.web.serializers.HostEventSerializer;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.SwitchPort;

import static com.google.common.base.Preconditions.checkNotNull;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Self-contained Host event(s) Object
 * <p/>
 * Host event differ from other events.
 * Host Event represent add/remove of attachmentPoint.
 * Not add/remove of the Host Object itself.
 * <p/>
 * Multiple attachmentPoints can be specified to batch events into 1 object.
 * Each should be treated as independent events.
 * <p/>
 * TODO: Rename to match what it is. (Switch/Port/Link/Host)Snapshot?
 * FIXME: Current implementation directly use this object as
 *        Replication message, but should be sending update operation info.
 */
@JsonSerialize(using = HostEventSerializer.class)
public class HostEvent extends TopologyElement<HostEvent> {

    private final MACAddress mac;
    private List<SwitchPort> attachmentPoints;
    private long lastSeenTime;

    /**
     * Default constructor for Serializer to use.
     */
    @Deprecated
    protected HostEvent() {
        mac = null;
    }

    /**
     * Creates the Host object.
     *
     * @param mac the MAC address to identify the host
     */
    public HostEvent(MACAddress mac) {
        this.mac = checkNotNull(mac);
        this.attachmentPoints = new LinkedList<>();
    }

    /**
     * Creates an unfrozen copy of given Object.
     *
     * @param original to make copy of.
     */
    public HostEvent(HostEvent original) {
        super(original);
        this.mac = original.mac;
        this.attachmentPoints = new ArrayList<>(original.attachmentPoints);
        this.lastSeenTime = original.lastSeenTime;
    }


    public MACAddress getMac() {
        return mac;
    }

    public List<SwitchPort> getAttachmentPoints() {
        return Collections.unmodifiableList(attachmentPoints);
    }

    public void setAttachmentPoints(List<SwitchPort> attachmentPoints) {
        if (isFrozen()) {
            throw new IllegalStateException("Tried to modify frozen instance: " + this);
        }
        this.attachmentPoints = attachmentPoints;
    }

    public void addAttachmentPoint(SwitchPort attachmentPoint) {
        if (isFrozen()) {
            throw new IllegalStateException("Tried to modify frozen instance: " + this);
        }
        // may need to maintain uniqueness
        this.attachmentPoints.add(0, attachmentPoint);
    }

    public boolean removeAttachmentPoint(SwitchPort attachmentPoint) {
        if (isFrozen()) {
            throw new IllegalStateException("Tried to modify frozen instance: " + this);
        }
        return this.attachmentPoints.remove(attachmentPoint);
    }


    public long getLastSeenTime() {
        return lastSeenTime;
    }

    public void setLastSeenTime(long lastSeenTime) {
        if (isFrozen()) {
            throw new IllegalStateException("Tried to modify frozen instance: " + this);
        }
        this.lastSeenTime = lastSeenTime;
    }

    // Assuming mac is unique cluster-wide
    public static ByteBuffer getHostID(final byte[] mac) {
        return (ByteBuffer) ByteBuffer.allocate(2 + mac.length)
                .putChar('H').put(mac).flip();
    }

    @Override
    public Dpid getOriginDpid() {
        // TODO: Eventually, we should return a collection of Dpid values
        for (SwitchPort sp : attachmentPoints) {
            return sp.getDpid();
        }
        return null;
    }

    @Override
    public ByteBuffer getIDasByteBuffer() {
        return getHostID(mac.toBytes());
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(attachmentPoints, mac);
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

        HostEvent other = (HostEvent) o;
        // XXX lastSeenTime excluded from Equality condition, is it OK?
        return Objects.equals(mac, other.mac) &&
                Objects.equals(this.attachmentPoints, other.attachmentPoints);
    }

    @Override
    public String toString() {
        return "[HostEvent " + mac + " attachmentPoints:" + attachmentPoints + "]";
    }
}
