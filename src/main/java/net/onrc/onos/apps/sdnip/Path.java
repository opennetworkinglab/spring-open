package net.onrc.onos.apps.sdnip;

import java.net.InetAddress;

/**
 * A {@link Path} represents paths within a network that forward traffic from
 * ingress port to egress port. For every {@code next_hop} received in route
 * updates from BGPd, we need to push forwarding paths from every other
 * possible ingress port to the egress port connected to the {@code next_hop}.
 * <p/>
 * The {@link Path} object doesn't contain lists of hops along the path.
 * Rather, it contains details about the egress {@link Interface} and
 * {@code next_hop} IP address. Implicitly, it represents paths from every
 * other ingress port to the {@code Interface}.
 * <p/>
 * Once flow mods are pushed to realize the path in the network, the
 * {@link Path} object will contain a list of pushed flow mods. These are used
 * if the path ever needs to be deleted.
 * <p/>
 * On startup, paths are pushed to all configured BGP peers, on the assumption
 * that they're likely to advertise routes to us. These paths are permanent
 * because the list of peers can't currently change at runtime. If we receive
 * a route for a {@code next_hop} which is not a peer, a temporary path will
 * be installed. These paths are temporary because they are removed if all
 * routes that use them are removed.
 * <p/>
 * Finally, the {@link Path} object counts references of prefixes that make use
 * of the path. If the reference count drops to zero as prefixes are deleted,
 * the path is no longer useful and will be removed from the network.
 */
public class Path {
    private final Interface dstInterface;
    private final InetAddress dstIpAddress;
    private int numUsers; // initialized to 0

    // XXX PushedFlowMod has been removed
    //private List<PushedFlowMod> flowMods; // initialized to null
    private boolean permanent; // initialized to false

    /**
     * Class constructor, taking the destination {@link Interface} and
     * destination IP address for the path.
     *
     * @param dstInterface the destination interface
     * @param dstIpAddress the destination IP address
     */
    public Path(Interface dstInterface, InetAddress dstIpAddress) {
        this.dstInterface = dstInterface;
        this.dstIpAddress = dstIpAddress;
    }

    /**
     * Gets the destination {@link Interface} of the path.
     *
     * @return the destination interface
     */
    public Interface getDstInterface() {
        return dstInterface;
    }

    /**
     * Gets the destination IP address.
     *
     * @return the destination IP address
     */
    public InetAddress getDstIpAddress() {
        return dstIpAddress;
    }

    /**
     * Increments the count of prefixes that use this path.
     */
    public void incrementUsers() {
        numUsers++;
    }

    /**
     * Decrements the count of prefixes that use this path.
     */
    public void decrementUsers() {
        numUsers--;
    }

    /**
     * Gets the count of prefixes that use this path.
     *
     * @return the number of prefixes currently using the path
     */
    public int getUsers() {
        return numUsers;
    }

    /**
     * Gets the list of flow mods that were installed to realize this path in
     * the network.
     *
     * @return the list of {@link PushedFlowMod}s
     */
    // XXX PushedFlowMod has been removed
    /*public List<PushedFlowMod> getFlowMods() {
        return Collections.unmodifiableList(flowMods);
    }*/

    /**
     * Sets the list of flow mods that were installed to realize this path in
     * the network.
     *
     * @param flowMods the list of {@link PushedFlowMod}s
     */
    // XXX PushedFlowMod has been removed
    /*public void setFlowMods(List<PushedFlowMod> flowMods) {
        this.flowMods = flowMods;
    }*/

    /**
     * Signifies whether the path is permanent and shouldn't be deleted when
     * the number of users drops to zero.
     *
     * @return true if the path is permanent, false if not
     */
    public boolean isPermanent() {
        return permanent;
    }

    /**
     * Set the permanent status of the path to true. Paths are not permanent
     * by default when constructed, and this method can be used to designate
     * them as permanent.
     */
    public void setPermanent() {
        permanent = true;
    }
}
