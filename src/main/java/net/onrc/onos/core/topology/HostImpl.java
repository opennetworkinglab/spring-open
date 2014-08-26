package net.onrc.onos.core.topology;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.SwitchPort;

/**
 * Handler to Host object stored in In-memory Topology snapshot.
 */
public class HostImpl extends TopologyObject implements Host {

    private final MACAddress id;


    /**
     * Creates a Host handler object.
     *
     * @param topology Topology instance this object belongs to
     * @param mac MAC address of the host
     */
    HostImpl(BaseInternalTopology topology, MACAddress mac) {
        super(topology);
        this.id = checkNotNull(mac);
    }

    @Override
    public MACAddress getMacAddress() {
        return id;
    }

    @Override
    public Iterable<Port> getAttachmentPoints() {
        List<Port> ports = new ArrayList<>();
        final BaseTopologyAdaptor topo = new BaseTopologyAdaptor(topology);
        for (SwitchPort swp : getHostEvent().getAttachmentPoints()) {
            Port port = topo.getPort(swp);
            if (port != null) {
                ports.add(port);
            }
        }
        return ports;
    }

    @Override
    public long getLastSeenTime() {
        return this.topology.getHostEvent(id).getLastSeenTime();
    }

    @Override
    public String toString() {
        return getMacAddress().toString();
    }

    /**
     * Gets the current HostEvent.
     *
     * @return HostEvent
     */
    private HostEvent getHostEvent() {
        return this.topology.getHostEvent(id);
    }

    /**
     * Returns the type of topology object.
     *
     * @return the type of the topology object
     */
    @Override
    public String getType() {
        // FIXME assuming Host is always in packet layer for now.
        return TopologyElement.TYPE_PACKET_LAYER;
    }

    /**
     * Returns the config state of topology element.
     *
     * @return ConfigState
     */
    @Override
    public ConfigState getConfigState() {
        return ConfigState.NOT_CONFIGURED;
    }

    /**
     * Returns the status of topology element.
     *
     * @return AdminStatus
     */
    @Override
    public AdminStatus getStatus() {
        return AdminStatus.ACTIVE;
    }
}
