package net.onrc.onos.core.intent;

/**
 * The ConstrainedShortestPathIntent is a "high-level" intent that allows
 * applications to reserve bandwidth along the shortest available path between
 * specified endpoints.
 */
public class ConstrainedShortestPathIntent extends ShortestPathIntent {
    protected double bandwidth;

    /**
     * Default constructor for Kryo deserialization.
     */
    protected ConstrainedShortestPathIntent() {
    }

    /**
     * Constructor.
     *
     * @param id          the ID for this Intent
     * @param srcSwitch   Source Switch DPID
     * @param srcPort     Source Port
     * @param srcMac      Source Host MAC Address
     * @param dstSwitch   Destination Switch DPID
     * @param dstPort     Destination Port
     * @param dstMac      Destination Host MAC Address
     * @param bandwidth   bandwidth which should be allocated for the path.
     *                    If 0, no intent for bandwidth allocation (best effort).
     */
    public ConstrainedShortestPathIntent(String id,
                                         long srcSwitch, long srcPort, long srcMac,
                                         long dstSwitch, long dstPort, long dstMac,
                                         double bandwidth) {
        super(id, srcSwitch, srcPort, srcMac, dstSwitch, dstPort, dstMac);
        this.bandwidth = bandwidth;
    }

    /**
     * Get the bandwidth specified for this Intent.
     * TODO: specify unit
     *
     * @return this Intent's bandwidth
     */
    public double getBandwidth() {
        return bandwidth;
    }

    /**
     * Generates a hash code using the Intent ID.
     *
     * @return hashcode
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Compares two intent object by type (class) and Intent ID.
     *
     * @param obj other Intent
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
