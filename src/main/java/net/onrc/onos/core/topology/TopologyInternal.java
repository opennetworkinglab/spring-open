package net.onrc.onos.core.topology;

import java.util.Collection;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.LinkTuple;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

/**
 * Interface to reference internal self-contained elements.
 */
public interface TopologyInternal extends Topology {
    /**
     * Gets a SwitchEvent.
     *
     * @param dpid Switch DPID
     * @return the SwitchEvent for the Switch DPID if found, otherwise null
     */
    public SwitchEvent getSwitchEvent(Dpid dpid);

    /**
     * Gets all SwitchEvent entries.
     *
     * @return all SwitchEvent entries.
     */
    public Collection<SwitchEvent> getAllSwitchEvents();

    /**
     * Gets a PortEvent.
     *
     * @param port Port identifier
     * @return the PortEvent for the Port identifier if found, otherwise null
     */
    public PortEvent getPortEvent(SwitchPort port);

    /**
     * Gets a PortEvent.
     *
     * @param dpid Switch DPID
     * @param portNumber Port number
     * @return the PortEvent for the (Dpid, PortNumber) if found, otherwise null
     */
    public PortEvent getPortEvent(Dpid dpid, PortNumber portNumber);

    /**
     * Gets all the PortEvents on a switch.
     *
     * @param dpid Switch DPID
     * @return PortEvents
     */
    public Collection<PortEvent> getPortEvents(Dpid dpid);

    /**
     * Gets all PortEvent entries.
     *
     * @return all PortEvent entries.
     */
    public Collection<PortEvent> getAllPortEvents();

    /**
     * Gets a LinkEvent.
     *
     * @param linkId Link identifier
     * @return the LinkEvent for the Link identifier if found, otherwise null
     */
    public LinkEvent getLinkEvent(LinkTuple linkId);

    /**
     * Gets a LinkEvent.
     *
     * @param linkId Link identifier
     * @param type type
     * @return the LinkEvent for the Link identifier and type if found, otherwise null
     */
    public LinkEvent getLinkEvent(final LinkTuple linkId, final String type);

    /**
     * Gets a collection of LinkEvent entries.
     *
     * @param linkId Link identifier
     * @return Collection of LinkEvent entries.
     */
    public Collection<LinkEvent> getLinkEvents(LinkTuple linkId);

    /**
     * Gets all LinkEvent entries.
     *
     * @return all LinkEvent entries.
     */
    public Collection<LinkEvent> getAllLinkEvents();

    /**
     * Gets a HostEvent.
     *
     * @param mac MACAddress of the host
     * @return the HostEvent for the MACAddress if found, otherwise null
     */
    public HostEvent getHostEvent(MACAddress mac);

    /**
     * Gets all HostEvent entries.
     *
     * @return all HostEvent entries.
     */
    public Collection<HostEvent> getAllHostEvents();
}
