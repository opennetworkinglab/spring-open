package net.onrc.onos.core.topology;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.topology.web.serializers.HostSerializer;

import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Interface of Host Object exposed to the "NB" read-only Topology.
 * <p/>
 * TODO What a Host Object represent is unclear at the moment.
 * <p/>
 * Everything returned by these interfaces must be either Unmodifiable view,
 * immutable object, or a copy of the original "SB" In-memory Topology.
 */
@JsonSerialize(using = HostSerializer.class)
public interface Host extends ITopologyElement {
    /**
     * Get the device MAC address.
     *
     * @return the device MAC address.
     */
    public MACAddress getMacAddress();

    /**
     * Get the device attachment points.
     * <p/>
     * TODO: There is only 1 attachment point right now.
     * TODO: Add requirement for Iteration order? Latest observed port first.
     *
     * @return the device attachment points.
     */
    public Iterable<Port> getAttachmentPoints();

    /**
     * Get the device last seen time.
     * <p/>
     *
     * @return the device last seen time. (UTC in ms)
     */
    public long getLastSeenTime();
}