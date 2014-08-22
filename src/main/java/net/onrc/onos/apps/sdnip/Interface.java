package net.onrc.onos.apps.sdnip;

import java.net.InetAddress;

import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.projectfloodlight.openflow.util.HexString;

import com.google.common.net.InetAddresses;

/**
 * Represents an interface, which is an external-facing switch port that
 * connects to another network.
 *
 * SDN-IP treats external-facing ports similarly to router ports. Logically, it
 * assigns an IP address to these ports which is used for communication with
 * the BGP peers, for example, the BGP peering session. The other peer will be
 * configured to peer with the IP address (logically) assigned to the
 * interface. The logical {@code Interface} construct maps on to a physical port in the
 * data plane, which of course has no notion of IP addresses.
 *
 * Each interface has a name, which is a unique identifying String that is used
 * to reference this interface in the configuration (for example, to map
 * {@link BgpPeer}s to {@code Interfaces}.
 */
public class Interface {
    private final String name;
    private final long dpid;
    private final short port;
    private final InetAddress ipAddress;
    private final int prefixLength;

    /**
     * Class constructor used by the JSON library to create an object.
     *
     * @param name the name of the interface
     * @param dpid the dpid of the switch
     * @param port the port on the switch
     * @param ipAddress the IP address logically assigned to the interface
     * @param prefixLength the length of the network prefix of the IP address
     */
    @JsonCreator
    public Interface(@JsonProperty("name") String name,
                     @JsonProperty("dpid") String dpid,
                     @JsonProperty("port") short port,
                     @JsonProperty("ipAddress") String ipAddress,
                     @JsonProperty("prefixLength") int prefixLength) {
        this.name = name;
        this.dpid = HexString.toLong(dpid);
        this.port = port;
        this.ipAddress = InetAddresses.forString(ipAddress);
        this.prefixLength = prefixLength;
    }

    /**
     * Gets the name of the interface.
     *
     * @return the name of the interface
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the {@link SwitchPort} that this interface maps to.
     *
     * @return the switch port
     */
    public SwitchPort getSwitchPort() {
        //TODO SwitchPort, Dpid and Port are mutable, but they could probably
        //be made immutable which would prevent the need to copy
        return new SwitchPort(new Dpid(dpid), PortNumber.uint16(port));
    }

    /**
     * Gets the DPID of the switch.
     *
     * @return the DPID of the switch
     */
    public long getDpid() {
        return dpid;
    }

    /**
     * Gets the port number this interface maps to.
     *
     * @return the port number
     */
    public short getPort() {
        return port;
    }

    /**
     * Gets the IP address which is logically assigned to the switch port.
     *
     * @return the IP address
     */
    public InetAddress getIpAddress() {
        return ipAddress;
    }

    /**
     * Gets the prefix length of the interface's IP address.
     *
     * @return the prefix length
     */
    public int getPrefixLength() {
        return prefixLength;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Interface)) {
            return false;
        }

        Interface otherInterface = (Interface) other;

        //Don't check switchPort as it's comprised of dpid and port
        return (name.equals(otherInterface.name)) &&
                (dpid == otherInterface.dpid) &&
                (port == otherInterface.port) &&
                (ipAddress.equals(otherInterface.ipAddress)) &&
                (prefixLength == otherInterface.prefixLength);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + name.hashCode();
        hash = 31 * hash + (int) (dpid ^ dpid >>> 32);
        hash = 31 * hash + (int) port;
        hash = 31 * hash + ipAddress.hashCode();
        hash = 31 * hash + prefixLength;
        return hash;
    }
}
