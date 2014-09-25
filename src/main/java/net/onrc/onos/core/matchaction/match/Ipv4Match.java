package net.onrc.onos.core.matchaction.match;

import net.onrc.onos.core.util.IPv4Net;

public class Ipv4Match implements Match {

    IPv4Net dstIp;

    public Ipv4Match(String ipAddressSlash) {
        this.dstIp = new IPv4Net(ipAddressSlash);
    }

    public IPv4Net getDestination() {
        return dstIp;
    }

}
