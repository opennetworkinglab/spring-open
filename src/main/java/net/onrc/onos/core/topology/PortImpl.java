package net.onrc.onos.core.topology;

import java.util.Map;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

/**
 * Port Object stored in In-memory Topology.
 * <p/>
 * TODO REMOVE following design memo: This object itself may hold the DBObject,
 * but this Object itself will not issue any read/write to the DataStore.
 */
public class PortImpl extends TopologyObject implements Port {

    private Switch sw;

    private PortNumber number;
    private String description;

    private final SwitchPort switchPort;

    public PortImpl(Topology topology, Switch parentSwitch, PortNumber number) {
        super(topology);
        this.sw = parentSwitch;
        this.number = number;

        switchPort = new SwitchPort(parentSwitch.getDpid(),
                                    number);
    }

    public PortImpl(Topology topology, Switch parentSwitch, Long number) {
        this(topology, parentSwitch, new PortNumber(number.shortValue()));
    }

    @Override
    public Dpid getDpid() {
        return sw.getDpid();
    }

    @Override
    public PortNumber getNumber() {
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
        return topology.getOutgoingLink(switchPort.dpid(),
                                        switchPort.port());
    }

    @Override
    public Link getIncomingLink() {
        return topology.getIncomingLink(switchPort.dpid(),
                                        switchPort.port());
    }

    @Override
    public Iterable<Device> getDevices() {
        return topology.getDevices(this.asSwitchPort());
    }

    @Override
    public String getStringAttribute(String attr) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
       justification = "getStringAttribute might return null once implemented")
    public String getStringAttribute(String attr, String def) {
        final String v = getStringAttribute(attr);
        if (v == null) {
            return def;
        } else {
            return v;
        }
    }

    @Override
    public Map<String, String> getAllStringAttributes() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String toString() {
        return String.format("%s:%s",
                getSwitch().getDpid(),
                getNumber());
    }
}
