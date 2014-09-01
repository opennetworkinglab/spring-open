package net.onrc.onos.core.topology;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.topology.web.serializers.HostDataSerializer;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.SwitchPort;

import static com.google.common.base.Preconditions.checkNotNull;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Self-contained Host object.
 */
@JsonSerialize(using = HostDataSerializer.class)
public class HostData extends TopologyElement<HostData> {

    private final MACAddress mac;
    private List<SwitchPort> attachmentPoints;
    private long lastSeenTime;

    /**
     * Default constructor for Serializer to use.
     */
    @Deprecated
    protected HostData() {
        mac = null;
    }

    /**
     * Constructor for a given host MAC address.
     *
     * @param mac the MAC address to identify the host
     */
    public HostData(MACAddress mac) {
        this.mac = checkNotNull(mac);
        this.attachmentPoints = new LinkedList<>();
    }

    /**
     * Copy constructor.
     * <p>
     * Creates an unfrozen copy of the given HostData object.
     *
     * @param original the object to make copy of
     */
    public HostData(HostData original) {
        super(original);
        this.mac = original.mac;
        this.attachmentPoints = new ArrayList<>(original.attachmentPoints);
        this.lastSeenTime = original.lastSeenTime;
    }

    /**
     * Gets the host MAC address.
     *
     * @return the MAC address.
     */
    public MACAddress getMac() {
        return mac;
    }

    /**
     * Gets the switch attachment points.
     *
     * @return the switch attachment points
     */
    public List<SwitchPort> getAttachmentPoints() {
        return Collections.unmodifiableList(attachmentPoints);
    }

    /**
     * Sets the switch attachment points.
     *
     * @param attachmentPoints the switch attachment points to set
     */
    public void setAttachmentPoints(List<SwitchPort> attachmentPoints) {
        if (isFrozen()) {
            throw new IllegalStateException("Tried to modify frozen instance: " + this);
        }
        this.attachmentPoints = attachmentPoints;
    }

    /**
     * Adds a switch attachment point.
     *
     * @param attachmentPoint the switch attachment point to add
     */
    public void addAttachmentPoint(SwitchPort attachmentPoint) {
        if (isFrozen()) {
            throw new IllegalStateException("Tried to modify frozen instance: " + this);
        }
        // may need to maintain uniqueness
        this.attachmentPoints.add(0, attachmentPoint);
    }

    /**
     * Removes a switch attachment point.
     *
     * @param attachmentPoint the switch attachment point to remove
     */
    public boolean removeAttachmentPoint(SwitchPort attachmentPoint) {
        if (isFrozen()) {
            throw new IllegalStateException("Tried to modify frozen instance: " + this);
        }
        return this.attachmentPoints.remove(attachmentPoint);
    }

    /**
     * Gets the host last seen time in milliseconds since the epoch.
     *
     * @return the host last seen time in milliseconds since the epoch
     */
    public long getLastSeenTime() {
        return lastSeenTime;
    }

    /**
     * Sets the host last seen time in milliseconds since the epoch.
     *
     * @param lastSeenTime the host last seen time in milliseconds since the
     * epoch
     */
    public void setLastSeenTime(long lastSeenTime) {
        if (isFrozen()) {
            throw new IllegalStateException("Tried to modify frozen instance: " + this);
        }
        this.lastSeenTime = lastSeenTime;
    }

    /**
     * Computes the host ID for a given host MAC address.
     * <p>
     * TODO: This method should be replaced with a method that uses
     * MACAddress as an argument instead of a byte array.
     *
     * @param mac the host MAC address to use
     * @return the host ID as a ByteBuffer
     */
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

        HostData other = (HostData) o;
        // XXX lastSeenTime excluded from Equality condition, is it OK?
        return Objects.equals(mac, other.mac) &&
                Objects.equals(this.attachmentPoints, other.attachmentPoints);
    }

    @Override
    public String toString() {
        return "[HostData " + mac + " attachmentPoints:" + attachmentPoints + "]";
    }
}
