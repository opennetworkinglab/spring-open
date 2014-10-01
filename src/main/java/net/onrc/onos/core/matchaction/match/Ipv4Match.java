package net.onrc.onos.core.matchaction.match;

import net.onrc.onos.core.util.IPv4;
import net.onrc.onos.core.util.IPv4Net;

public class Ipv4Match implements Match {

    IPv4Net dstIp;

    public Ipv4Match(String ipAddressSlash) {
        this.dstIp = new IPv4Net(ipAddressSlash);

        IPv4 ip = dstIp.address();
        short prefLen = dstIp.prefixLen();
        int mask = ~((1 << (32 - prefLen)) - 1);;
        int newIpInt = ip.value() & mask;
        IPv4 newIp = new IPv4(newIpInt);

        this.dstIp = new IPv4Net(newIp, prefLen);
    }

    public IPv4Net getDestination() {
        return dstIp;
    }

}
