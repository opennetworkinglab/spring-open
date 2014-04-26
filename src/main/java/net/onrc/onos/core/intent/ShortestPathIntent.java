package net.onrc.onos.core.intent;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.Dpid;

/**
 * @author Toshio Koide (t-koide@onlab.us)
 */
public class ShortestPathIntent extends Intent {
    protected long srcSwitchDpid;
    protected long srcPortNumber;
    protected long srcMacAddress;
    protected long dstSwitchDpid;
    protected long dstPortNumber;
    protected long dstMacAddress;
    protected String pathIntentId = null;

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
        super(id);
        srcSwitchDpid = srcSwitch;
        srcPortNumber = srcPort;
        srcMacAddress = srcMac;
        dstSwitchDpid = dstSwitch;
        dstPortNumber = dstPort;
        dstMacAddress = dstMac;
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
        return String.format("id:%s, state:%s, srcDpid:%s, srcPort:%d, srcMac:%s, dstDpid:%s, dstPort:%d, dstMac:%s",
                getId(), getState(),
                new Dpid(srcSwitchDpid), srcPortNumber, MACAddress.valueOf(srcMacAddress),
                new Dpid(dstSwitchDpid), dstPortNumber, MACAddress.valueOf(dstMacAddress));
    }
}
