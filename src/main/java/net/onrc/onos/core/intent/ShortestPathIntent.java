package net.onrc.onos.core.intent;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.topology.web.serializers.ShortestPathIntentSerializer;
import net.onrc.onos.core.util.Dpid;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * @author Toshio Koide (t-koide@onlab.us)
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
        //super(id);
        this(id, srcSwitch, srcPort, srcMac, EMPTYIPADDRESS, dstSwitch, dstPort, dstMac, EMPTYIPADDRESS);
    }

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

    public void setPathIntent(PathIntent pathIntent) {
        pathIntentId = pathIntent.getId();
    }

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

    @Override
    public int hashCode() {
        // TODO: Is this the intended behavior?
        return (super.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        // TODO: Is this the intended behavior?
        return (super.equals(obj));
    }

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

    public int getSrcIp() {
        return srcIpAddress;
    }

    public int getDstIp() {
        return dstIpAddress;
    }
}
