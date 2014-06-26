package net.onrc.onos.core.topology;

import net.onrc.onos.core.topology.web.serializers.PortSerializer;
import net.onrc.onos.core.util.SwitchPort;

import org.codehaus.jackson.map.annotate.JsonSerialize;

//TODO Everything returned by these interfaces must be either Unmodifiable view,
//immutable object, or a copy of the original "SB" In-memory Topology.

/**
 * Interface of Port object in the topology.
 */
@JsonSerialize(using = PortSerializer.class)
public interface Port extends StringAttributes {

    /**
     * Gets the data path ID (dpid) of the switch, which this port is on.
     *
     * @return data path ID (dpid)
     */
    public Long getDpid();

    /**
     * Gets the port number of this port.
     *
     * @return port number
     */
    public Long getNumber();

    /**
     * Gets a {@link SwitchPort} that represents this Port's dpid and port
     * number.
     *
     * @return a SwitchPort representing the Port
     */
    public SwitchPort asSwitchPort();

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
     *
     * @return {@link Link} if there exist a outgoing link from this,
     *         {@code null} otherwise.
     */
    public Link getOutgoingLink();

    /**
     * Gets the incoming link to this port.
     *
     * @return {@link Link} if there exist a incoming link to this, {@code null}
     *         otherwise.
     */
    public Link getIncomingLink();

    // XXX Iterable or Collection?
    /**
     * Gets all the devices attached to this port.
     *
     * @return {@link Device}s attached to this port
     */
    public Iterable<Device> getDevices();
}
