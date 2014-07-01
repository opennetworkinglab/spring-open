package net.onrc.onos.core.topology;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.SwitchPort;

/**
 * Device Object stored in In-memory Topology.
 */
public class DeviceImpl extends TopologyObject implements Device {

    //////////////////////////////////////////////////////
    /// Topology element attributes
    ///  - any changes made here needs to be replicated.
    //////////////////////////////////////////////////////
    private DeviceEvent deviceObj;

    ///////////////////
    /// In-memory index
    ///////////////////

    // none

    /**
     * Creates a Device object based on {@link DeviceEvent}.
     *
     * @param topology Topology instance this object belongs to
     * @param scHost self contained {@link DeviceEvent}
     */
    public DeviceImpl(Topology topology, DeviceEvent scHost) {
        super(topology);
        Validate.notNull(scHost);

        // TODO should we assume deviceObj is already frozen before this call
        //      or expect attribute update will happen after .
        if (scHost.isFrozen()) {
            this.deviceObj = scHost;
        } else {
            this.deviceObj = new DeviceEvent(scHost);
            this.deviceObj.freeze();
        }
    }

    /**
     * Creates a Device object with empty attributes.
     *
     * @param topology Topology instance this object belongs to
     * @param mac MAC address of the host
     */
    public DeviceImpl(Topology topology, MACAddress mac) {
        this(topology, new DeviceEvent(mac).freeze());
    }

    @Override
    public MACAddress getMacAddress() {
        return this.deviceObj.getMac();
    }

    @Override
    public Iterable<Port> getAttachmentPoints() {
        List<Port> ports = new ArrayList<>();
        topology.acquireReadLock();
        try {
            for (SwitchPort swp : this.deviceObj.getAttachmentPoints()) {
                Port p = this.topology.getPort(swp);
                if (p != null) {
                    ports.add(p);
                }
            }
        } finally {
            topology.releaseReadLock();
        }
        return ports;
    }

    @Override
    public long getLastSeenTime() {
        return deviceObj.getLastSeenTime();
    }

    @Override
    public String toString() {
        return getMacAddress().toString();
    }

    // TODO we may no longer need this. confirm and delete later.
    void setLastSeenTime(long lastSeenTime) {
        // XXX Following will make this instance thread unsafe. Need to use AtomicRef.
        DeviceEvent updated = new DeviceEvent(this.deviceObj);
        updated.setLastSeenTime(lastSeenTime);
        updated.freeze();
        this.deviceObj = updated;
    }

    /**
     * Only {@link TopologyManager} should use this method.
     *
     * @param port the port that the device is attached to
     */
    void addAttachmentPoint(Port port) {
        // XXX Following will make this instance thread unsafe. Need to use AtomicRef.
        DeviceEvent updated = new DeviceEvent(this.deviceObj);
        updated.removeAttachmentPoint(port.asSwitchPort());
        updated.addAttachmentPoint(port.asSwitchPort());
        updated.freeze();
        this.deviceObj = updated;
    }

    /**
     * Only {@link TopologyManager} should use this method.
     *
     * @param port the port that the device is attached to
     */
    boolean removeAttachmentPoint(Port port) {
        // XXX Following will make this instance thread unsafe. Need to use AtomicRef.
        DeviceEvent updated = new DeviceEvent(this.deviceObj);
        final boolean result = updated.removeAttachmentPoint(port.asSwitchPort());
        updated.freeze();
        this.deviceObj = updated;
        return result;
    }


    /**
     * Returns the type of topology object.
     *
     * @return the type of the topology object
     */
    @Override
    public String getType() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
