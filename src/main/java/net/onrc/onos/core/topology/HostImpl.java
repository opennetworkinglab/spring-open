package net.onrc.onos.core.topology;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.SwitchPort;

/**
 * Handler to Host object stored in In-memory Topology snapshot.
 * <p/>
 */
public class HostImpl extends TopologyObject implements Host {

    private final MACAddress id;


    /**
     * Creates a Host handler object.
     *
     * @param topology Topology instance this object belongs to
     * @param mac MAC address of the host
     */
    HostImpl(TopologyInternal topology, MACAddress mac) {
        super(topology);
        Validate.notNull(mac);
        this.id = mac;
    }

    @Override
    public MACAddress getMacAddress() {
        return id;
    }

    @Override
    public Iterable<Port> getAttachmentPoints() {
        List<Port> ports = new ArrayList<>();
        topology.acquireReadLock();
        try {
            for (SwitchPort swp : getHostEvent().getAttachmentPoints()) {
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
        // FIXME assuming device is always in packet layer for now.
        return TopologyElement.TYPE_PACKET_LAYER;
    }
}
