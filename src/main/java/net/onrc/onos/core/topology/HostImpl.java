package net.onrc.onos.core.topology;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.SwitchPort;

/**
 * Handler to Host object stored in In-memory Topology snapshot.
 */
public class HostImpl extends TopologyObject implements Host {

    private final MACAddress id;
    private final int ipAddress;


    /**
     * Creates a Host handler object.
     *
     * @param topology Topology instance this object belongs to
     * @param mac MAC address of the host
     */
    HostImpl(BaseInternalTopology topology, MACAddress mac) {
        super(topology);
        this.id = checkNotNull(mac);
        this.ipAddress = 0;
    }

    /**
     * Creates a Host handler object.
     *
     * @param topology Topology instance this object belongs to
     * @param mac MAC address of the host
     * @param ipv4Address IP address of ths host
     */
    HostImpl(BaseInternalTopology topology, MACAddress mac, int ipv4Address) {
        super(topology);
        this.id = checkNotNull(mac);
        this.ipAddress = ipv4Address;
    }

    @Override
    public MACAddress getMacAddress() {
        return id;
    }

    @Override
    public Iterable<Port> getAttachmentPoints() {
        List<Port> ports = new ArrayList<>();
        final BaseTopologyAdaptor topo = new BaseTopologyAdaptor(topology);
        for (SwitchPort swp : getHostData().getAttachmentPoints()) {
            Port port = topo.getPort(swp);
            if (port != null) {
                ports.add(port);
            }
        }
        return ports;
    }

    @Override
    public long getLastSeenTime() {
        return this.topology.getHostData(id).getLastSeenTime();
    }

    @Override
    public String toString() {
        return getMacAddress().toString();
    }

    /**
     * Gets the current HostData.
     *
     * @return HostData
     */
    private HostData getHostData() {
        return this.topology.getHostData(id);
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


    /**
     *  Returns the IP address of the Host
     */
    @Override
    public int getIpAddress() {
        // TODO Auto-generated method stub
        return ipAddress;
    }
}
