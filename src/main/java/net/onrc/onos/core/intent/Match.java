package net.onrc.onos.core.intent;


import java.util.Objects;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.packet.Ethernet;
//import net.onrc.onos.core.topology.Port;
//import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.util.FlowEntryMatch;
import net.onrc.onos.core.util.IPv4;
import net.onrc.onos.core.util.IPv4Net;

/**
 * @author Brian O'Connor <bocon@onlab.us>
 */

public class Match {
    private static final short IPV4_PREFIX_LEN = 32;
    protected long sw;
    protected MACAddress srcMac;
    protected MACAddress dstMac;
    protected int srcIp;
    protected int dstIp;
    protected long srcPort;

    public Match(long sw, long srcPort,
                 MACAddress srcMac, MACAddress dstMac) {
        this(sw, srcPort, srcMac, dstMac, ShortestPathIntent.EMPTYIPADDRESS, ShortestPathIntent.EMPTYIPADDRESS);
    }

    public Match(long sw, long srcPort, MACAddress srcMac, MACAddress dstMac,
                 int srcIp, int dstIp) {

        this.sw = sw;
        this.srcPort = srcPort;
        this.srcMac = srcMac;
        this.dstMac = dstMac;
        this.srcIp = srcIp;
        this.dstIp = dstIp;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Match) {
            Match other = (Match) obj;
            if (this.sw != other.sw) {
                return false;
            }
            if (!Objects.equals(srcMac, other.srcMac)) {
                return false;
            }
            if (!Objects.equals(dstMac, other.dstMac)) {
                return false;
            }
            if (srcIp != other.srcIp) {
                return false;
            }

            if (dstIp != other.dstIp) {
                return false;
            }
            if (this.srcPort != other.srcPort) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public FlowEntryMatch getFlowEntryMatch() {
        FlowEntryMatch match = new FlowEntryMatch();
        if (srcMac != null) {
            match.enableSrcMac(srcMac);
        }
        if (dstMac != null) {
            match.enableDstMac(dstMac);
        }
        if (srcIp != ShortestPathIntent.EMPTYIPADDRESS) {
            match.enableEthernetFrameType(Ethernet.TYPE_IPV4);
            IPv4 srcIPv4 = new IPv4(srcIp);
            IPv4Net srcIP = new IPv4Net(srcIPv4, IPV4_PREFIX_LEN);
            match.enableSrcIPv4Net(srcIP);
        }
        if (dstIp != ShortestPathIntent.EMPTYIPADDRESS) {
            match.enableEthernetFrameType(Ethernet.TYPE_IPV4);
            IPv4 dstIPv4 = new IPv4(dstIp);
            IPv4Net dstIP = new IPv4Net(dstIPv4, IPV4_PREFIX_LEN);
            match.enableDstIPv4Net(dstIP);
        }
        match.enableInPort(new net.onrc.onos.core.util.Port((short) srcPort));
        return match;
    }

    @Override
    public String toString() {
        return "Sw:" + sw + " (" + srcPort + "," + srcMac + "," + dstMac + "," + srcIp + "," + dstIp + ")";
    }

    @Override
    public int hashCode() {
        return  Objects.hash(sw, srcPort, srcMac, dstMac, srcIp, dstIp);
    }
}
