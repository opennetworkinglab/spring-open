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
public interface BaseInternalTopology extends BaseMastership {

    /**
     * Gets a SwitchData.
     *
     * @param dpid Switch DPID
     * @return the SwitchData for the Switch DPID if found, otherwise null
     */
    public SwitchData getSwitchData(Dpid dpid);

    /**
     * Gets all SwitchData entries.
     *
     * @return all SwitchData entries.
     */
    public Collection<SwitchData> getAllSwitchDataEntries();

    /**
     * Gets a PortData.
     *
     * @param port Port identifier
     * @return the PortData for the Port identifier if found, otherwise null
     */
    public PortData getPortData(SwitchPort port);

    /**
     * Gets a PortData.
     *
     * @param dpid Switch DPID
     * @param portNumber Port number
     * @return the PortData for the (Dpid, PortNumber) if found, otherwise null
     */
    public PortData getPortData(Dpid dpid, PortNumber portNumber);

    /**
     * Gets all PortData entries on a switch.
     *
     * @param dpid Switch DPID
     * @return all PortData entries on a switch.
     */
    public Collection<PortData> getPortDataEntries(Dpid dpid);

    /**
     * Gets all PortData entries.
     *
     * @return all PortData entries.
     */
    public Collection<PortData> getAllPortDataEntries();

    /**
     * Gets a LinkData.
     *
     * @param linkId Link identifier
     * @return the LinkData for the Link identifier if found, otherwise null
     */
    public LinkData getLinkData(LinkTuple linkId);

    /**
     * Gets a LinkData.
     *
     * @param linkId Link identifier
     * @param type type
     * @return the LinkData for the Link identifier and type if found,
     * otherwise null
     */
    public LinkData getLinkData(final LinkTuple linkId, final String type);

    /**
     * Gets all LinkData entries departing from a specified port.
     *
     * @param srcPort source port identifier
     * @return Collection of LinkData entries
     */
    public Collection<LinkData> getLinkDataEntriesFrom(final SwitchPort srcPort);

    /**
     * Gets all LinkData entries pointing toward a specified port.
     *
     * @param dstPort destination port identifier
     * @return Collection of LinkData entries
     */
    public Collection<LinkData> getLinkDataEntriesTo(final SwitchPort dstPort);

    /**
     * Gets a collection of LinkData entries.
     *
     * @param linkId Link identifier
     * @return Collection of LinkData entries.
     */
    public Collection<LinkData> getLinkDataEntries(LinkTuple linkId);

    /**
     * Gets all LinkData entries.
     *
     * @return all LinkData entries.
     */
    public Collection<LinkData> getAllLinkDataEntries();

    /**
     * Gets a HostData.
     *
     * @param mac MACAddress of the host
     * @return the HostData for the MACAddress if found, otherwise null
     */
    public HostData getHostData(MACAddress mac);

    /**
     * Gets all HostData entries attached to a specified port.
     *
     * @param port attachment point identifier
     * @return Collection of HostData entries.
     */
    public Collection<HostData> getHostDataEntries(SwitchPort port);

    /**
     * Gets all HostData entries.
     *
     * @return all HostData entries.
     */
    public Collection<HostData> getAllHostDataEntries();
}
