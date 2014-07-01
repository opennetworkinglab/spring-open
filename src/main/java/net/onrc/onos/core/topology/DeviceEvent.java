package net.onrc.onos.core.topology;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.topology.web.serializers.DeviceEventSerializer;
import net.onrc.onos.core.util.SwitchPort;

import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Self-contained Device event(s) Object
 * <p/>
 * Device event differ from other events.
 * Device Event represent add/remove of attachmentPoint.
 * Not add/remove of the DeviceObject itself.
 * <p/>
 * Multiple attachmentPoints can be specified to batch events into 1 object.
 * Each should be treated as independent events.
 * <p/>
 * TODO: Rename to match what it is. (Switch/Port/Link/Device)Snapshot?
 * FIXME: Current implementation directly use this object as
 *        Replication message, but should be sending update operation info.
 */
@JsonSerialize(using = DeviceEventSerializer.class)
public class DeviceEvent extends TopologyElement<DeviceEvent> {

    private final MACAddress mac;
    private List<SwitchPort> attachmentPoints;
    private long lastSeenTime;

    /**
     * Default constructor for Serializer to use.
     */
    @Deprecated
    protected DeviceEvent() {
        mac = null;
    }

    public DeviceEvent(MACAddress mac) {
        if (mac == null) {
            throw new IllegalArgumentException("Device mac cannot be null");
        }
        this.mac = mac;
        this.attachmentPoints = new LinkedList<>();
    }

    /**
     * Create an unfrozen copy of given Object.
     *
     * @param original to make copy of.
     */
    public DeviceEvent(DeviceEvent original) {
        super(original);
        this.mac = original.mac;
        this.attachmentPoints = new ArrayList<>(original.attachmentPoints);
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

    @Override
    public String toString() {
        return "[DeviceEvent " + mac + " attachmentPoints:" + attachmentPoints + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((attachmentPoints == null) ? 0 : attachmentPoints.hashCode());
        result = prime * result + ((mac == null) ? 0 : mac.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!super.equals(obj)) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }
        DeviceEvent other = (DeviceEvent) obj;

        if (!super.equals(obj)) {
            return false;
        }

        // XXX lastSeenTime excluded from Equality condition, is it OK?
        return Objects.equals(mac, other.mac) &&
                Objects.equals(this.attachmentPoints, other.attachmentPoints);
    }

    // Assuming mac is unique cluster-wide
    public static ByteBuffer getDeviceID(final byte[] mac) {
        return (ByteBuffer) ByteBuffer.allocate(2 + mac.length)
                .putChar('D').put(mac).flip();
    }

    public byte[] getID() {
        return getDeviceID(mac.toBytes()).array();
    }

    public ByteBuffer getIDasByteBuffer() {
        return getDeviceID(mac.toBytes());
    }
}
