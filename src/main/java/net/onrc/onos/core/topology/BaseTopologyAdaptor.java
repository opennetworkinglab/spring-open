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
        final SwitchEvent sw = internal.getSwitchEvent(dpid);
        if (sw != null) {
            return new SwitchImpl(internal, dpid);
        }
        return null;
    }

    @Override
    public Iterable<Switch> getSwitches() {
        final Collection<SwitchEvent> switches = internal.getAllSwitchEvents();
        List<Switch> list = new ArrayList<>(switches.size());
        for (SwitchEvent elm : switches) {
            list.add(new SwitchImpl(internal, elm.getDpid()));
        }
        return list;
    }

    @Override
    public Port getPort(Dpid dpid, PortNumber portNumber) {
        final PortEvent port = internal.getPortEvent(dpid, portNumber);
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
        final Collection<PortEvent> ports = internal.getPortEvents(dpid);
        List<Port> list = new ArrayList<>(ports.size());
        for (PortEvent elm : ports) {
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
        final Collection<LinkEvent> links = internal.getLinkEventsFrom(port);
        LinkEvent link = getPacketLinkEventIfExists(links);
        if (link != null) {
            return new LinkImpl(internal, link.getLinkTuple());
        }
        return null;
    }

    @Override
    public Link getIncomingLink(SwitchPort port) {
        final Collection<LinkEvent> links = internal.getLinkEventsTo(port);
        LinkEvent link = getPacketLinkEventIfExists(links);
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
    private static LinkEvent getPacketLinkEventIfExists(Collection<LinkEvent> links) {
        for (LinkEvent link : links) {
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
        final Collection<LinkEvent> links = internal.getLinkEventsFrom(port);
        for (LinkEvent link : links) {
            if (link.getType().equals(type)) {
                return new LinkImpl(internal, link.getLinkTuple());
            }
        }
        return null;
    }

    @Override
    public Link getIncomingLink(SwitchPort port, String type) {
        final Collection<LinkEvent> links = internal.getLinkEventsTo(port);
        for (LinkEvent link : links) {
            if (link.getType().equals(type)) {
                return new LinkImpl(internal, link.getLinkTuple());
            }
        }
        return null;
    }


    @Override
    public Collection<Link> getOutgoingLinks(SwitchPort port) {
        final Collection<LinkEvent> links = internal.getLinkEventsFrom(port);
        return toLinkImpls(internal, links);
    }

    @Override
    public Collection<Link> getIncomingLinks(SwitchPort port) {
        final Collection<LinkEvent> links = internal.getLinkEventsTo(port);
        return toLinkImpls(internal, links);
    }


    /**
     * Converts collection of LinkEvent to collection of LinkImpls.
     *
     * @param internalTopology topology {@code links} resides
     * @param links collection of LinkEvent
     * @return collection of {@link LinkImpl}s
     */
    private static Collection<Link> toLinkImpls(
                                final BaseInternalTopology internalTopology,
                                final Collection<LinkEvent> links) {

        if (links == null) {
            return Collections.emptyList();
        }
        List<Link> list = new ArrayList<>(links.size());
        for (LinkEvent elm : links) {
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
        final Collection<LinkEvent> links = internal.getAllLinkEvents();
        return toLinkImpls(internal, links);
    }

    @Override
    public Host getHostByMac(MACAddress address) {

        HostEvent host = internal.getHostEvent(address);
        if (host != null) {
            return new HostImpl(internal, address);
        }
        return null;
    }

    @Override
    public Iterable<Host> getHosts() {
        return toHostImpls(internal, internal.getAllHostEvents());
    }

    /**
     * Converts collection of HostEvent to collection of HostImpl.
     *
     * @param internalTopology topology {@code hosts} resides
     * @param hosts collection of HostEvent
     * @return collection of HostImpl
     */
    private static List<Host> toHostImpls(BaseInternalTopology internalTopology,
                                          Collection<HostEvent> hosts) {
        if (hosts == null) {
            return Collections.emptyList();
        }
        List<Host> list = new ArrayList<>(hosts.size());
        for (HostEvent elm : hosts) {
            list.add(new HostImpl(internalTopology, elm.getMac()));
        }
        return list;
    }

    @Override
    public Collection<Host> getHosts(SwitchPort port) {
        return toHostImpls(internal, internal.getHostEvents(port));
    }


    @Override
    public OnosInstanceId getSwitchMaster(Dpid dpid) {
        return internal.getSwitchMaster(dpid);
    }
}
