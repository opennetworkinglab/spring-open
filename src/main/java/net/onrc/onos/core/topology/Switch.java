package net.onrc.onos.core.topology;

import java.util.Collection;

import net.onrc.onos.core.topology.web.serializers.SwitchSerializer;

import org.codehaus.jackson.map.annotate.JsonSerialize;

// TOOD Everything returned by these interfaces must be either Unmodifiable view,
// immutable object, or a copy of the original "SB" In-memory Topology.
/**
 * Interface of Switch object in the topology.
 */
@JsonSerialize(using = SwitchSerializer.class)
public interface Switch {

    /**
     * Gets the data path ID (dpid) of this switch.
     *
     * @return data path ID (dpid)
     */
    public Long getDpid();

    /**
     * Gets all the ports on this switch.
     *
     * @return Collection of {@link Port} on this switch.
     */
    public Collection<Port> getPorts();

    /**
     * Gets a port on switch by port number.
     *
     * @param number port number
     * @return {@link Port} with {@code number} on this switch, or {@code null}
     *         if this switch did not have a port for specified port number
     */
    public Port getPort(Long number);

    // Graph traversal API

    // XXX What is the Definition of neighbor? Link exist in both direction or
    // one-way is sufficient to be a neighbor, etc.
    /**
     * Gets all the neighbor switches.
     *
     * @return neighbor switches
     */
    public Iterable<Switch> getNeighbors();

    /**
     * Gets all the outgoing links.
     *
     * @return outgoing {@link Link}s from this switch.
     */
    public Iterable<Link> getOutgoingLinks();

    /**
     * Gets all the incoming links.
     *
     * @return outgoing {@link Link}s from this switch.
     */
    public Iterable<Link> getIncomingLinks();

    /**
     * Gets outgoing link to specified neighbor switch specified by dpid.
     *
     * @param dpid data path ID of neighbor switch.
     * @return {@link Link} to neighbor switch {@code dpid} or {@code null} if
     *         link does not exist.
     */
    public Link getLinkToNeighbor(Long dpid);

    // XXX Iterable or Collection?
    /**
     * Gets all the devices attached to this switch.
     *
     * @return {@link Device}s attached to this switch
     */
    public Collection<Device> getDevices();
}
