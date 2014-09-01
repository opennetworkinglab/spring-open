package net.onrc.onos.core.topology;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.OnosInstanceId;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

/**
 * Adaptor to access {@link BaseInternalTopology} as {@link BaseTopology}.
 */
public class BaseTopologyAdaptor implements BaseTopology {

    private final BaseInternalTopology internal;

    /**
     * Constructor.
     *
     * @param internal {@link BaseInternalTopology} to use internally
     */
    public BaseTopologyAdaptor(BaseInternalTopology internal) {
        this.internal = checkNotNull(internal);
    }


    @Override
    public Switch getSwitch(Dpid dpid) {
        final SwitchData sw = internal.getSwitchData(dpid);
        if (sw != null) {
            return new SwitchImpl(internal, dpid);
        }
        return null;
    }

    @Override
    public Iterable<Switch> getSwitches() {
        final Collection<SwitchData> switches = internal.getAllSwitchDataEntries();
        List<Switch> list = new ArrayList<>(switches.size());
        for (SwitchData elm : switches) {
            list.add(new SwitchImpl(internal, elm.getDpid()));
        }
        return list;
    }

    @Override
    public Port getPort(Dpid dpid, PortNumber portNumber) {
        final PortData port = internal.getPortData(dpid, portNumber);
        if (port != null) {
            return new PortImpl(internal, port.getSwitchPort());
        }
        return null;
    }

    @Override
    public Port getPort(SwitchPort port) {
        return getPort(port.getDpid(), port.getPortNumber());
    }

    @Override
    public Collection<Port> getPorts(Dpid dpid) {
        final Collection<PortData> ports = internal.getPortDataEntries(dpid);
        List<Port> list = new ArrayList<>(ports.size());
        for (PortData elm : ports) {
            list.add(new PortImpl(internal, elm.getSwitchPort()));
        }
        return list;
    }

    @Override
    public Link getOutgoingLink(Dpid dpid, PortNumber number) {
        return getOutgoingLink(new SwitchPort(dpid, number));
    }

    @Override
    public Link getIncomingLink(Dpid dpid, PortNumber number) {
        return getIncomingLink(new SwitchPort(dpid, number));
    }


    @Override
    public Link getOutgoingLink(SwitchPort port) {
        final Collection<LinkData> links = internal.getLinkDataEntriesFrom(port);
        LinkData link = getPacketLinkDataIfExists(links);
        if (link != null) {
            return new LinkImpl(internal, link.getLinkTuple());
        }
        return null;
    }

    @Override
    public Link getIncomingLink(SwitchPort port) {
        final Collection<LinkData> links = internal.getLinkDataEntriesTo(port);
        LinkData link = getPacketLinkDataIfExists(links);
        if (link != null) {
            return new LinkImpl(internal, link.getLinkTuple());
        }
        return null;
    }

    /**
     * Gets the "packet" link if such exists,
     * otherwise return whichever link is found first.
     *
     * @param links Collection of links to search from
     * @return Link instance found or null if no link exists
     */
    private static LinkData getPacketLinkDataIfExists(Collection<LinkData> links) {
        for (LinkData link : links) {
            if (TopologyElement.TYPE_PACKET_LAYER.equals(link.getType())) {
                return link;
            }
        }
        if (!links.isEmpty()) {
            return links.iterator().next();
        }
        return null;
    }

    @Override
    public Link getOutgoingLink(Dpid dpid, PortNumber number, String type) {
        return getOutgoingLink(new SwitchPort(dpid, number), type);
    }

    @Override
    public Link getIncomingLink(Dpid dpid, PortNumber number, String type) {
        return getIncomingLink(new SwitchPort(dpid, number), type);
    }


    @Override
    public Link getOutgoingLink(SwitchPort port, String type) {
        final Collection<LinkData> links = internal.getLinkDataEntriesFrom(port);
        for (LinkData link : links) {
            if (link.getType().equals(type)) {
                return new LinkImpl(internal, link.getLinkTuple());
            }
        }
        return null;
    }

    @Override
    public Link getIncomingLink(SwitchPort port, String type) {
        final Collection<LinkData> links = internal.getLinkDataEntriesTo(port);
        for (LinkData link : links) {
            if (link.getType().equals(type)) {
                return new LinkImpl(internal, link.getLinkTuple());
            }
        }
        return null;
    }


    @Override
    public Collection<Link> getOutgoingLinks(SwitchPort port) {
        final Collection<LinkData> links = internal.getLinkDataEntriesFrom(port);
        return toLinkImpls(internal, links);
    }

    @Override
    public Collection<Link> getIncomingLinks(SwitchPort port) {
        final Collection<LinkData> links = internal.getLinkDataEntriesTo(port);
        return toLinkImpls(internal, links);
    }


    /**
     * Converts collection of LinkData to collection of LinkImpls.
     *
     * @param internalTopology topology {@code links} resides
     * @param links collection of LinkData
     * @return collection of {@link LinkImpl}s
     */
    private static Collection<Link> toLinkImpls(
                                final BaseInternalTopology internalTopology,
                                final Collection<LinkData> links) {

        if (links == null) {
            return Collections.emptyList();
        }
        List<Link> list = new ArrayList<>(links.size());
        for (LinkData elm : links) {
            list.add(new LinkImpl(internalTopology, elm.getLinkTuple()));
        }
        return list;
    }

    @Override
    public Link getLink(Dpid srcDpid, PortNumber srcNumber,
                        Dpid dstDpid, PortNumber dstNumber) {

        final SwitchPort dstSwitchPort = new SwitchPort(dstDpid, dstNumber);
        Collection<Link> links = getOutgoingLinks(new SwitchPort(srcDpid, srcNumber));
        for (Link link : links) {
            if (link == null) {
                continue;
            }
            if (link.getDstPort().getSwitchPort().equals(dstSwitchPort)) {
                return link;
            }
        }
        return null;
    }

    @Override
    public Link getLink(Dpid srcDpid, PortNumber srcNumber,
                        Dpid dstDpid, PortNumber dstNumber,
                        String type) {

        Link link = getOutgoingLink(srcDpid, srcNumber, type);
        if (link == null) {
            return null;
        }
        if (!link.getDstSwitch().getDpid().equals(dstDpid)) {
            return null;
        }
        if (!link.getDstPort().getNumber().equals(dstNumber)) {
            return null;
        }
        return link;
    }

    @Override
    public Iterable<Link> getLinks() {
        final Collection<LinkData> links = internal.getAllLinkDataEntries();
        return toLinkImpls(internal, links);
    }

    @Override
    public Host getHostByMac(MACAddress address) {

        HostData host = internal.getHostData(address);
        if (host != null) {
            return new HostImpl(internal, address);
        }
        return null;
    }

    @Override
    public Iterable<Host> getHosts() {
        return toHostImpls(internal, internal.getAllHostDataEntries());
    }

    /**
     * Converts collection of HostData to collection of HostImpl.
     *
     * @param internalTopology topology {@code hosts} resides
     * @param hosts collection of HostData
     * @return collection of HostImpl
     */
    private static List<Host> toHostImpls(BaseInternalTopology internalTopology,
                                          Collection<HostData> hosts) {
        if (hosts == null) {
            return Collections.emptyList();
        }
        List<Host> list = new ArrayList<>(hosts.size());
        for (HostData elm : hosts) {
            list.add(new HostImpl(internalTopology, elm.getMac()));
        }
        return list;
    }

    @Override
    public Collection<Host> getHosts(SwitchPort port) {
        return toHostImpls(internal, internal.getHostDataEntries(port));
    }


    @Override
    public OnosInstanceId getSwitchMaster(Dpid dpid) {
        return internal.getSwitchMaster(dpid);
    }
}
