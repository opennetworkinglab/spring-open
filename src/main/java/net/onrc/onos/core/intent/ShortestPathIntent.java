package net.onrc.onos.core.intent;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.intent.runtime.web.serializers.ShortestPathIntentSerializer;
import net.onrc.onos.core.util.Dpid;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * The ShortestPathIntent is a simple, "high-level" intent that
 * provides shortest path connectivity between two end points in
 * the network.
 */
@JsonSerialize(using = ShortestPathIntentSerializer.class)
public class ShortestPathIntent extends Intent {
    public static final long EMPTYMACADDRESS = 0;
    public static final int EMPTYIPADDRESS = 0;
    protected long srcSwitchDpid;
    protected long srcPortNumber;
    protected long srcMacAddress;
    protected long dstSwitchDpid;
    protected long dstPortNumber;
    protected long dstMacAddress;
    protected int srcIpAddress;
    protected int dstIpAddress;
    protected String pathIntentId = null;
    protected int idleTimeout;
    protected int hardTimeout;
    protected int firstSwitchIdleTimeout;
    protected int firstSwitchHardTimeout;

    /**
     * Default constructor for Kryo deserialization.
     */
    protected ShortestPathIntent() {
    }

    /**
     * Constructor for ShortestPathIntent.
     *
     * @param id Intent ID
     * @param srcSwitch Source Switch DPID
     * @param srcPort Source Port
     * @param srcMac Source Host MAC Address
     * @param dstSwitch Destination Switch DPID
     * @param dstPort Destination Port
     * @param dstMac Destination Host MAC Address
     */
    public ShortestPathIntent(String id,
            long srcSwitch, long srcPort, long srcMac,
            long dstSwitch, long dstPort, long dstMac) {
        this(id, srcSwitch, srcPort, srcMac, EMPTYIPADDRESS, dstSwitch, dstPort, dstMac, EMPTYIPADDRESS);
    }

    /**
     * Constructor.
     *
     * @param id Intent ID
     * @param srcSwitch Source Switch DPID
     * @param srcPort Source Port
     * @param srcMac Source Host MAC Address
     * @param srcIp Source IP Address
     * @param dstSwitch Destination Switch DPID
     * @param dstPort Destination Port
     * @param dstMac Destination Host MAC Address
     * @param dstIp Destination IP Address
     */
    // CHECKSTYLE:OFF suppress the warning about too many parameters
    public ShortestPathIntent(String id,
            long srcSwitch, long srcPort, long srcMac, int srcIp,
            long dstSwitch, long dstPort, long dstMac, int dstIp ) {
        // CHECKSTYLE:ON
        super(id);
        this.srcSwitchDpid = srcSwitch;
        this.srcPortNumber = srcPort;
        this.srcMacAddress = srcMac;
        this.dstSwitchDpid = dstSwitch;
        this.dstPortNumber = dstPort;
        this.dstMacAddress = dstMac;
        this.srcIpAddress = srcIp;
        this.dstIpAddress = dstIp;
    }

    /**
     * Gets the source Switch DPID.
     *
     * @return Source Switch DPID
     */
    public long getSrcSwitchDpid() {
        return srcSwitchDpid;
    }

    /**
     * Gets the source Port.
     *
     * @return Source Port
     */
    public long getSrcPortNumber() {
        return srcPortNumber;
    }

    /**
     * Gets the source Host MAC Address.
     *
     * @return Source Host MAC Address
     */
    public long getSrcMac() {
        return srcMacAddress;
    }

    /**
     * Gets the destination Switch DPID.
     *
     * @return Destination Switch DPID
     */
    public long getDstSwitchDpid() {
        return dstSwitchDpid;
    }

    /**
     * Gets the destination Port.
     *
     * @return Destination Port
     */
    public long getDstPortNumber() {
        return dstPortNumber;
    }

    /**
     * Gets the destination Host MAC Address.
     *
     * @return Destination Host MAC Address
     */
    public long getDstMac() {
        return dstMacAddress;
    }

    /**
     * Get the source IP address.
     *
     * @return source IP address
     */
    public int getSrcIp() {
        return srcIpAddress;
    }

    /**
     * Get the destination IP address.
     *
     * @return destination IP address
     */
    public int getDstIp() {
        return dstIpAddress;
    }

    /**
     * Set the low-level PathIntent ID.
     *
     * @param pathIntent new PathIntent
     */
    public void setPathIntentId(PathIntent pathIntent) {
        pathIntentId = pathIntent.getId();
    }

    /**
     * Get the low-level PathIntent ID.
     *
     * @return the ID of the low-level PathIntent
     */
    public String getPathIntentId() {
        return pathIntentId;
    }

    // TODO - this is intended to be refactored and removed
    public int getIdleTimeout() {
        return idleTimeout;
    }

    // TODO - this is intended to be refactored and removed
    public int getHardTimeout() {
        return hardTimeout;
    }

    // TODO - this is intended to be refactored and removed
    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    // TODO - this is intended to be refactored and removed
    public void setHardTimeout(int hardTimeout) {
        this.hardTimeout = hardTimeout;
    }

    // TODO - this is intended to be refactored and removed
    public int getFirstSwitchIdleTimeout() {
        return firstSwitchIdleTimeout;
    }

    // TODO - this is intended to be refactored and removed
    public int getFirstSwitchHardTimeout() {
        return firstSwitchHardTimeout;
    }

    // TODO - this is intended to be refactored and removed
    public void setFirstSwitchIdleTimeout(int firstSwitchIdleTimeout) {
        this.firstSwitchIdleTimeout = firstSwitchIdleTimeout;
    }

    // TODO - this is intended to be refactored and removed
    public void setFirstSwitchHardTimeout(int firstSwitchHardTimeout) {
        this.firstSwitchHardTimeout = firstSwitchHardTimeout;
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

    /**
     * Returns a String representation of this Intent.
     *
     * @return comma separated list of Intent parameters
     */
    @Override
    public String toString() {
        return String.format("id:%s, state:%s, srcDpid:%s, srcPort:%d, " +
                "srcMac:%s, srcIP:%s, dstDpid:%s, dstPort:%d, dstMac:%s, dstIP:%s",
                getId(), getState(),
                new Dpid(srcSwitchDpid), srcPortNumber,
                MACAddress.valueOf(srcMacAddress), Integer.toString(srcIpAddress),
                new Dpid(dstSwitchDpid), dstPortNumber,
                MACAddress.valueOf(dstMacAddress), Integer.toString(dstIpAddress));
    }

}
