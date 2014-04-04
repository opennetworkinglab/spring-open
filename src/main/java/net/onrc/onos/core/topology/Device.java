package net.onrc.onos.core.topology;

import java.net.InetAddress;
import java.util.Collection;

import net.floodlightcontroller.util.MACAddress;

/**
 * Interface of Device Object exposed to the "NB" read-only Topology.
 * <p/>
 * TODO What a Device Object represent is unclear at the moment.
 * <p/>
 * Everything returned by these interfaces must be either Unmodifiable view,
 * immutable object, or a copy of the original "SB" In-memory Topology.
 */
public interface Device {
    /**
     * Get the device MAC address.
     *
     * @return the device MAC address.
     */
    public MACAddress getMacAddress();

    /**
     * Get the device IP addresses.
     *
     * @return the device IP addresses.
     */
    public Collection<InetAddress> getIpAddress();

    /**
     * Get the device attachment points.
     * <p/>
     * Add requirement for Iteration order? Latest observed port first.
     *
     * @return the device attachment points.
     */
    public Iterable<Port> getAttachmentPoints();

    /**
     * Get the device last seen time.
     * <p/>
     * TODO: what is the time definition?
     *
     * @return the device last seen time.
     */
    public long getLastSeenTime();
}