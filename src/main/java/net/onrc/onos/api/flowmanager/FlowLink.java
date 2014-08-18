package net.onrc.onos.api.flowmanager;

import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

/**
 * A link representation used by Flow objects.
 * <p>
 * TODO: Should lambda, bandwidth, tag, etc. be defined in this FlowLink, Path,
 * Tree or Flow? We have to define it.
 */
public class FlowLink {
    protected SwitchPort srcSwitchPort;
    protected SwitchPort dstSwitchPort;

    /**
     * Creates new FlowLink object using source/destination switch port pair.
     *
     * @param src The source switch port.
     * @param dst The destination switch port.
     */
    public FlowLink(SwitchPort src, SwitchPort dst) {
        this.srcSwitchPort = src;
        this.dstSwitchPort = dst;
    }

    /**
     * Creates new FlowLink object using DPID and port number pairs at
     * source/destination switches.
     *
     * @param srcDpid The source switch DPID.
     * @param srcPortNumber The source port number at the source switch.
     * @param dstDpid The destination switch DPID.
     * @param dstPortNumber The destination port number at the destination
     *        switch.
     */
    public FlowLink(Dpid srcDpid, PortNumber srcPortNumber,
            Dpid dstDpid, PortNumber dstPortNumber) {
        this.srcSwitchPort = new SwitchPort(srcDpid, srcPortNumber);
        this.dstSwitchPort = new SwitchPort(dstDpid, dstPortNumber);
    }

    /**
     * Gets the source switch port.
     *
     * @return The source switch port.
     */
    public SwitchPort getSrcSwitchPort() {
        return srcSwitchPort;
    }

    /**
     * Gets the source switch DPID.
     *
     * @return The source switch DPID.
     */
    public Dpid getSrcDpid() {
        return srcSwitchPort.getDpid();
    }

    /**
     * Gets the source port number at the source switch.
     *
     * @return The source port number at the source switch.
     */
    public PortNumber getSrcPortNumber() {
        return srcSwitchPort.getPortNumber();
    }

    /**
     * Gets the destination switch port.
     *
     * @return The destination switch port.
     */
    public SwitchPort getDstSwitchPort() {
        return dstSwitchPort;
    }

    /**
     * Gets the destination switch DPID.
     *
     * @return The destination switch DPID.
     */
    public Dpid getDstDpid() {
        return dstSwitchPort.getDpid();
    }

    /**
     * Gets the destination port number at the destination switch.
     *
     * @return The destination port number at the destination switch.
     */
    public PortNumber getDstPortNumber() {
        return dstSwitchPort.getPortNumber();
    }

    @Override
    public String toString() {
        return srcSwitchPort + "-->" + dstSwitchPort;
    }
}
