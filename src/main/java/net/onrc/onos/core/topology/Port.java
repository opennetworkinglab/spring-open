package net.onrc.onos.core.topology;

import java.util.Collection;

import net.onrc.onos.core.topology.web.serializers.PortSerializer;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.codehaus.jackson.map.annotate.JsonSerialize;

//TODO Everything returned by these interfaces must be either Unmodifiable view,
//immutable object, or a copy of the original "SB" In-memory Topology.

/**
 * Interface of Port object in the topology.
 */
@JsonSerialize(using = PortSerializer.class)
public interface Port extends ITopologyElement, StringAttributes {

    /**
     * Gets the data path ID (dpid) of the switch, which this port is on.
     *
     * @return data path ID (dpid)
     */
    public Dpid getDpid();

    /**
     * Gets the port number of this port.
     *
     * @return port number
     */
    public PortNumber getNumber();

    /**
     * Gets the port number of this port.
     *
     * @return port number
     */
    public PortNumber getPortNumber();

    /**
     * Gets a {@link SwitchPort} that represents this Port's dpid and port
     * number.
     *
     * @return a SwitchPort representing the Port
     */
    public SwitchPort getSwitchPort();

    /**
     * Gets the hardware address of this port.
     * <p/>
     * TODO Not implemented yet.
     * TODO Should return value type be Long?
     *
     * @return hardware address
     */
    public Long getHardwareAddress();

    /**
     * Description of this port.
     * <p/>
     * TODO Not implemented yet.
     *
     * @return description of this port
     */
    public String getDescription();

    /**
     * Gets the parent switch.
     *
     * @return {@link Switch} instance
     */
    public Switch getSwitch();

    /**
     * Gets the outgoing link from this port.
     * <p/>
     * FIXME As a temporary workaround, it will look for type "packet" and
     * returns it if found, else return whichever link is found first.
     *
     * @return {@link Link} if there exist a outgoing link from this,
     *         {@code null} otherwise.
     */
    public Link getOutgoingLink();

    /**
     * Gets the outgoing link from this port.
     *
     * @param type type of the link
     * @return {@link Link} if there exist a outgoing link from this,
     *         {@code null} otherwise.
     */
    public Link getOutgoingLink(String type);

    /**
     * Gets all the outgoing links from this port.
     *
     * @return Collection of {@link Link}s
     */
    public Collection<Link> getOutgoingLinks();

    /**
     * Gets the incoming link to this port.
     * <p/>
     * FIXME As a temporary workaround, it will look for type "packet" and
     * returns it if found, else return whichever link is found first.
     *
     * @return {@link Link} if there exist a incoming link to this, {@code null}
     *         otherwise.
     */
    public Link getIncomingLink();

    /**
     * Gets the incoming link to this port.
     *
     * @param type type of the link
     * @return {@link Link} if there exist a incoming link to this, {@code null}
     *         otherwise.
     */
    public Link getIncomingLink(String type);

    /**
     * Gets all the incoming links to this port.
     *
     * @return Collection of {@link Link}s
     */
    public Collection<Link> getIncomingLinks();

    /**
     * Gets all the devices attached to this port.
     *
     * @return {@link Host}s attached to this port
     */
    public Collection<Host> getHosts();


    /**
     * Returns the port type of this port.
     *
     * @return {@link PortType}
     */
    public PortType getPortType();
}
