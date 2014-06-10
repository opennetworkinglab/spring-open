package net.onrc.onos.core.topology;

import java.util.Collections;
import java.util.LinkedList;

import net.floodlightcontroller.util.MACAddress;

/**
 * Device Object stored in In-memory Topology.
 */
public class DeviceImpl extends TopologyObject implements Device {

    private final MACAddress macAddr;
    protected LinkedList<Port> attachmentPoints;
    private long lastSeenTime;

    public DeviceImpl(Topology topology, MACAddress mac) {
        super(topology);
        this.macAddr = mac;
        this.attachmentPoints = new LinkedList<>();
    }

    @Override
    public MACAddress getMacAddress() {
        return this.macAddr;
    }

    @Override
    public Iterable<Port> getAttachmentPoints() {
        return Collections.unmodifiableList(this.attachmentPoints);
    }

    @Override
    public long getLastSeenTime() {
        return lastSeenTime;
    }

    @Override
    public String toString() {
        return macAddr.toString();
    }

    void setLastSeenTime(long lastSeenTime) {
        this.lastSeenTime = lastSeenTime;
    }

    /**
     * Only {@link TopologyManager} should use this method.
     *
     * @param port the port that the device is attached to
     */
    void addAttachmentPoint(Port port) {
        this.attachmentPoints.remove(port);
        this.attachmentPoints.addFirst(port);
    }

    /**
     * Only {@link TopologyManager} should use this method.
     *
     * @param port the port that the device is attached to
     */
    boolean removeAttachmentPoint(Port port) {
        return this.attachmentPoints.remove(port);
    }
}
