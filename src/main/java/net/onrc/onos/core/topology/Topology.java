package net.onrc.onos.core.topology;

import java.util.Collection;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.topology.web.serializers.TopologySerializer;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * The northbound interface to the topology. This interface
 * is presented to the rest of ONOS. It is currently read-only, as we want
 * only the discovery modules to be allowed to modify the topology.
 */
@JsonSerialize(using = TopologySerializer.class)
public interface Topology {

    /**
     * Gets the switch for a given switch DPID.
     *
     * @param dpid the switch dpid.
     * @return the switch if found, otherwise null.
     */
    public Switch getSwitch(Dpid dpid);

    /**
     * Gets all switches in the network.
     *
     * @return all switches in the network.
     */
    public Iterable<Switch> getSwitches();

    /**
     * Gets the port on a switch.
     *
     * @param dpid   the switch DPID.
     * @param number the switch port number.
     * @return the switch port if found, otherwise null.
     */
    public Port getPort(Dpid dpid, PortNumber number);

    /**
     * Gets the port on a switch.
     *
     * @param port port identifier
     * @return the switch port if found, otherwise null.
     */
    public Port getPort(SwitchPort port);

    /**
     * Gets all ports on a switch specified.
     *
     * @param dpid Switch dpid
     * @return ports.
     */
    public Collection<Port> getPorts(Dpid dpid);

    /**
     * Gets the outgoing link from a switch port.
     *
     * @param dpid   the switch DPID.
     * @param number the switch port number.
     * @return the outgoing link if found, otherwise null.
     */
    public Link getOutgoingLink(Dpid dpid, PortNumber number);

    /**
     * Gets the outgoing link from a switch port.
     *
     * @param port port identifier
     * @return the switch port if found, otherwise null.
     */
    public Link getOutgoingLink(SwitchPort port);

    /**
     * Gets the incoming link to a switch port.
     *
     * @param dpid   the switch DPID.
     * @param number the switch port number.
     * @return the incoming link if found, otherwise null.
     */
    public Link getIncomingLink(Dpid dpid, PortNumber number);

    /**
     * Gets the incoming link to a switch port.
     *
     * @param port port identifier
     * @return the switch port if found, otherwise null.
     */
    public Link getIncomingLink(SwitchPort port);

    /**
     * Gets the outgoing link from a switch and a port to another switch and
     * a port.
     *
     * @param srcDpid   the source switch DPID.
     * @param srcNumber the source switch port number.
     * @param dstDpid   the destination switch DPID.
     * @param dstNumber the destination switch port number.
     * @return the outgoing link if found, otherwise null.
     */
    public Link getLink(Dpid srcDpid, PortNumber srcNumber,
                        Dpid dstDpid, PortNumber dstNumber);

    /**
     * Gets all links in the network.
     * <p/>
     * TODO: Not clear if this method is needed. Remove if not used.
     *
     * @return all links in the network.
     */
    public Iterable<Link> getLinks();

    /**
     * Gets the network device for a given MAC address.
     *
     * @param address the MAC address to use.
     * @return the network device for the MAC address if found, otherwise null.
     */
    public Device getDeviceByMac(MACAddress address);

    /**
     * Gets all devices in the network.
     *
     * @return all devices in the network
     */
    public Iterable<Device> getDevices();

    /**
     * Gets all devices on specified port.
     *
     * @param port port which the device is attached
     * @return all devices attached to the port.
     */
    public Collection<Device> getDevices(SwitchPort port);

    /**
     * Acquire a read lock on the entire topology. The topology will not
     * change while readers have the lock. Must be released using
     * {@link #releaseReadLock()}. This method will block until a read lock is
     * available.
     */
    public void acquireReadLock();

    /**
     * Release the read lock on the topology.
     */
    public void releaseReadLock();
}
