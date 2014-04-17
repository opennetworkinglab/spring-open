package net.onrc.onos.apps.bgproute;

import java.net.InetAddress;

import com.google.common.net.InetAddresses;

/**
 * Represents an entry in the Routing Information Base (RIB) of a router.
 * <p/>
 * A route update from the BGP daemon contains a prefix and a RibEntry
 * containing the next hop for the route. The RibEntry also contains
 * information related to the synchronization mechanism between BGPd and
 * SDN-IP, such as sequence numbers.
 */
public class RibEntry {
    private final InetAddress routerId;
    private final InetAddress nextHop;

    /*
     * Store the sequence number information provided on the update here for
     * now. I think this *should* really be in the RibUpdate, and we should
     * store RibUpdates in the Ptree. But, that's a bigger change to change
     * what the Ptree stores.
     */
    private final long sysUpTime;
    private final long sequenceNum;

    /*
     * Marker for RibEntries where we don't have sequence number info.
     * The user of this class should make sure they don't check this data
     * if they don't provide it.
     */
    private static final long NULL_TIME = -1;

    /**
     * Class constructor, taking the router ID and next hop IP address as
     * {@link InetAddress} objects.
     *
     * @param routerId the router ID which identifies the route table in BGPd
     * that this update came from
     * @param nextHop next hop IP address for this route entry
     */
    public RibEntry(InetAddress routerId, InetAddress nextHop) {
        this.routerId = routerId;
        this.nextHop = nextHop;
        sequenceNum = NULL_TIME;
        sysUpTime = NULL_TIME;
    }

    /**
     * Class constructor, taking the router ID and next hop IP address as
     * Strings. The addresses must be in dot-notation form
     * (e.g. {@code 0.0.0.0}).
     *
     * @param routerId the router ID which identifies the route table in BGPd
     * that this update came from
     * @param nextHop next hop IP address for this route entry
     */
    public RibEntry(String routerId, String nextHop) {
        this.routerId = InetAddresses.forString(routerId);
        this.nextHop = InetAddresses.forString(nextHop);
        sequenceNum = NULL_TIME;
        sysUpTime = NULL_TIME;
    }

    /**
     * Class constructor, taking the router ID and next hop IP address as
     * Strings, as well as the sequence numbers of the updates. Sequence
     * numbers are used to establish ordering of updates from BGPd. The
     * addresses must be in dot-notation form (e.g. {@code 0.0.0.0}).
     *
     * @param routerId the router ID which identifies the route table in BGPd
     * that this update came from
     * @param nextHop next hop IP address for this route entry
     * @param sysUpTime the sysuptime parameter on the update from BGPd
     * @param sequenceNum the sequencenum parameter on the update from BGPd
     */
    public RibEntry(String routerId, String nextHop, long sysUpTime,
            long sequenceNum) {
        this.routerId = InetAddresses.forString(routerId);
        this.nextHop = InetAddresses.forString(nextHop);
        this.sequenceNum = sequenceNum;
        this.sysUpTime = sysUpTime;
    }

    /**
     * Gets the next hop IP address of this route entry.
     *
     * @return the next hop IP address
     */
    public InetAddress getNextHop() {
        return nextHop;
    }

    /**
     * Gets the sysuptime parameter sent with the update from BGPd.
     *
     * @return the sysuptime parameter
     */
    public long getSysUpTime() {
        return sysUpTime;
    }

    /**
     * Gets the sequencenum parameter sent with the update from BGPd.
     *
     * @return the sequencenum parameter
     */
    public long getSequenceNum() {
        return sequenceNum;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof RibEntry)) {
            return false;
        }

        RibEntry otherRibEntry = (RibEntry) other;

        return this.routerId.equals(otherRibEntry.routerId)
                && this.nextHop.equals(otherRibEntry.nextHop);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + routerId.hashCode();
        hash = 31 * hash + nextHop.hashCode();
        return hash;
    }
}
