package net.onrc.onos.core.topology;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.onrc.onos.core.util.SwitchPort;

/**
 * Port Object stored in In-memory Topology.
 * <p/>
 * TODO REMOVE following design memo: This object itself may hold the DBObject,
 * but this Object itself will not issue any read/write to the DataStore.
 */
public class PortImpl extends TopologyObject implements Port {

    private Switch sw;

    private Long number;
    private String description;

    private final SwitchPort switchPort;

    // These needs to be ConcurrentCollecton if allowing the topology to be
    // accessed concurrently
    protected Set<Device> devices;

    public PortImpl(Topology topology, Switch parentSwitch, Long number) {
        super(topology);
        this.sw = parentSwitch;
        this.number = number;
        this.devices = new HashSet<>();

        switchPort = new SwitchPort(parentSwitch.getDpid(), number.shortValue());
    }

    @Override
    public Long getDpid() {
        return sw.getDpid();
    }

    @Override
    public Long getNumber() {
        return number;
    }

    @Override
    public SwitchPort asSwitchPort() {
        return switchPort;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Long getHardwareAddress() {
        // TODO Auto-generated method stub
        return 0L;
    }

    @Override
    public Switch getSwitch() {
        return sw;
    }

    @Override
    public Link getOutgoingLink() {
        return topology.getOutgoingLink(switchPort.dpid().value(),
                (long) switchPort.port().value());
    }

    @Override
    public Link getIncomingLink() {
        return topology.getIncomingLink(switchPort.dpid().value(),
                (long) switchPort.port().value());
    }

    @Override
    public Iterable<Device> getDevices() {
        return Collections.unmodifiableSet(this.devices);
    }

    /**
     * @param d
     * @return true if successfully added
     */
    public boolean addDevice(Device d) {
        return this.devices.add(d);
    }

    /**
     * @param d
     * @return true if device existed and was removed
     */
    public boolean removeDevice(Device d) {
        return this.devices.remove(d);
    }

    public void removeAllDevice() {
        this.devices.clear();
    }

    @Override
    public String toString() {
        return String.format("%d:%d",
                getSwitch().getDpid(),
                getNumber());
    }
}
