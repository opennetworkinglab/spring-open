package net.onrc.onos.core.topology;

import java.util.Collection;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

// TODO move to appropriate package. under api??
/**
 * BaseTopology interface common to both {@link ImmutableTopology} and {@link MutableTopology}.
 */
public interface BaseTopology extends BaseMastership {

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
     * @param portNumber the switch port number.
     * @return the switch port if found, otherwise null.
     */
    public Port getPort(Dpid dpid, PortNumber portNumber);

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
     * @param portNumber the switch port number.
     * @return the outgoing link if found, otherwise null.
     */
    public Link getOutgoingLink(Dpid dpid, PortNumber portNumber);

    /**
     * Gets the outgoing link from a switch port.
     *
     * @param dpid   the switch DPID.
     * @param portNumber the switch port number.
     * @param type   type of the link
     * @return the outgoing link if found, otherwise null.
     */
    public Link getOutgoingLink(Dpid dpid, PortNumber portNumber, String type);

    /**
     * Gets the outgoing link from a switch port.
     *
     * @param port port identifier
     * @return the outgoing link if found, otherwise null.
     */
    public Link getOutgoingLink(SwitchPort port);

    /**
     * Gets the outgoing link from a switch port.
     *
     * @param port port identifier
     * @param type type of the link
     * @return the outgoing link if found, otherwise null.
     */
    public Link getOutgoingLink(SwitchPort port, String type);

    /**
     * Gets all the outgoing link from a switch port.
     *
     * @param port port identifier
     * @return outgoing links
     */
    public Collection<Link> getOutgoingLinks(SwitchPort port);

    /**
     * Gets the incoming link to a switch port.
     *
     * @param dpid   the switch DPID.
     * @param portNumber the switch port number.
     * @return the incoming link if found, otherwise null.
     */
    public Link getIncomingLink(Dpid dpid, PortNumber portNumber);

    /**
     * Gets the incoming link to a switch port.
     *
     * @param dpid   the switch DPID.
     * @param portNumber the switch port number.
     * @param type type of the link
     * @return the incoming link if found, otherwise null.
     */
    public Link getIncomingLink(Dpid dpid, PortNumber portNumber, String type);

    /**
     * Gets the incoming link to a switch port.
     *
     * @param port port identifier
     * @return the incoming link if found, otherwise null.
     */
    public Link getIncomingLink(SwitchPort port);

    /**
     * Gets the incoming link to a switch port.
     *
     * @param port port identifier
     * @param type type of the link
     * @return the incoming link if found, otherwise null.
     */
    public Link getIncomingLink(SwitchPort port, String type);

    /**
     * Gets all the incoming link from a switch port.
     *
     * @param port port identifier
     * @return incoming links
     */
    public Collection<Link> getIncomingLinks(SwitchPort port);

    /**
     * Gets the outgoing link from a switch and a port to another switch and
     * a port.
     *
     * @param srcDpid   the source switch DPID.
     * @param srcPortNumber the source switch port number.
     * @param dstDpid   the destination switch DPID.
     * @param dstPortNumber the destination switch port number.
     * @return the outgoing link if found, otherwise null.
     */
    public Link getLink(Dpid srcDpid, PortNumber srcPortNumber,
                        Dpid dstDpid, PortNumber dstPortNumber);

    /**
     * Gets the outgoing link from a switch and a port to another switch and
     * a port.
     *
     * @param srcDpid   the source switch DPID.
     * @param srcPortNumber the source switch port number.
     * @param dstDpid   the destination switch DPID.
     * @param dstPortNumber the destination switch port number.
     * @param type      type of the link
     * @return the outgoing link if found, otherwise null.
     */
    public Link getLink(Dpid srcDpid, PortNumber srcPortNumber,
                        Dpid dstDpid, PortNumber dstPortNumber,
                        String type);

    /**
     * Gets all links in the network.
     * <p/>
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
    public Host getHostByMac(MACAddress address);

    /**
     * Gets all devices in the network.
     *
     * @return all devices in the network
     */
    public Iterable<Host> getHosts();

    /**
     * Gets all devices on specified port.
     *
     * @param port port which the device is attached
     * @return all devices attached to the port.
     */
    public Collection<Host> getHosts(SwitchPort port);
}
