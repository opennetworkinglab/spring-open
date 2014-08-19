package net.onrc.onos.core.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.concurrent.GuardedBy;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.LinkTuple;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;


/**
 * Class to represent an instance of Topology Snapshot.
 */
public class TopologyImpl implements Topology, TopologyInternal {

    private static final Logger log = LoggerFactory.getLogger(TopologyImpl.class);

    // TODO Revisit Map types after implementing CoW/lock-free

    // DPID -> Switch
    private final ConcurrentMap<Dpid, SwitchEvent> switches;
    private final ConcurrentMap<Dpid, ConcurrentMap<PortNumber, PortEvent>> ports;

    // Index from Port to Host
    private final Multimap<SwitchPort, HostEvent> hosts;
    private final ConcurrentMap<MACAddress, HostEvent> mac2Host;

    // SwitchPort -> (type -> Link)
    private final ConcurrentMap<SwitchPort, ConcurrentMap<String, LinkEvent>> outgoingLinks;
    private final ConcurrentMap<SwitchPort, ConcurrentMap<String, LinkEvent>> incomingLinks;

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private Lock readLock = readWriteLock.readLock();
    // TODO use the write lock after refactor
    private Lock writeLock = readWriteLock.writeLock();

    /**
     * Create an empty Topology.
     */
    public TopologyImpl() {
        // TODO: Does these object need to be stored in Concurrent Collection?
        switches = new ConcurrentHashMap<>();
        ports = new ConcurrentHashMap<>();
        hosts = Multimaps.synchronizedMultimap(
                HashMultimap.<SwitchPort, HostEvent>create());
        mac2Host = new ConcurrentHashMap<>();
        outgoingLinks = new ConcurrentHashMap<>();
        incomingLinks = new ConcurrentHashMap<>();
    }

    /**
     * Create a shallow copy of given Topology.
     *
     * @param original Topology
     */
    public TopologyImpl(TopologyImpl original) {
        original.acquireReadLock();
        try {
            this.switches = new ConcurrentHashMap<>(original.switches);

            // shallow copy Map in Map
            this.ports = new ConcurrentHashMap<>(original.ports.size());
            for (Entry<Dpid, ConcurrentMap<PortNumber, PortEvent>> entry
                    : original.ports.entrySet()) {
                this.ports.put(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
            }

            this.hosts = Multimaps.synchronizedMultimap(
                    HashMultimap.<SwitchPort, HostEvent>create(original.hosts));
            this.mac2Host = new ConcurrentHashMap<>(original.mac2Host);

            // shallow copy Map in Map
            this.outgoingLinks = new ConcurrentHashMap<>(original.outgoingLinks.size());
            for (Entry<SwitchPort, ConcurrentMap<String, LinkEvent>> entry
                    : original.outgoingLinks.entrySet()) {
                this.outgoingLinks.put(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
            }

            // shallow copy Map in Map
            this.incomingLinks = new ConcurrentHashMap<>(original.incomingLinks.size());
            for (Entry<SwitchPort, ConcurrentMap<String, LinkEvent>> entry
                    : original.incomingLinks.entrySet()) {
                this.incomingLinks.put(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
            }
        } finally {
            original.releaseReadLock();
        }
    }

    @Override
    public Switch getSwitch(Dpid dpid) {
        final SwitchEvent sw = switches.get(dpid);
        if (sw != null) {
            return new SwitchImpl(this, dpid);
        } else {
            return null;
        }
    }

    @Override
    public Iterable<Switch> getSwitches() {
        List<Switch> list = new ArrayList<>(switches.size());
        for (SwitchEvent elm : switches.values()) {
            list.add(new SwitchImpl(this, elm.getDpid()));
        }
        return list;
    }

    @Override
    public Port getPort(Dpid dpid, PortNumber number) {
        ConcurrentMap<PortNumber, PortEvent> portMap = ports.get(dpid);
        if (portMap != null) {
            final PortEvent port = portMap.get(number);
            if (port != null) {
                return new PortImpl(this, port.getSwitchPort());
            }
        }
        return null;
    }

    @Override
    public Port getPort(SwitchPort port) {
        return getPort(port.getDpid(), port.getPortNumber());
    }

    @Override
    public Collection<Port> getPorts(Dpid dpid) {
        ConcurrentMap<PortNumber, PortEvent> portMap = ports.get(dpid);
        if (portMap == null) {
            return Collections.emptyList();
        }
        List<Port> list = new ArrayList<>(portMap.size());
        for (PortEvent elm : portMap.values()) {
            list.add(new PortImpl(this, elm.getSwitchPort()));
        }
        return list;
    }

    @Override
    public Link getOutgoingLink(Dpid dpid, PortNumber number) {
        return getOutgoingLink(new SwitchPort(dpid, number));
    }

    @Override
    public Link getOutgoingLink(SwitchPort port) {
        Map<String, LinkEvent> links = outgoingLinks.get(port);
        return getPacketLinkIfExists(links);
    }

    // TODO remove when we no longer need packet fall back behavior
    /**
     * Gets the "packet" link if such exists,
     * if not return whichever link is found first.
     *
     * @param links Collection of links to search from
     * @return Link instance found or null if no link exists
     */
    private Link getPacketLinkIfExists(Map<String, LinkEvent> links) {

        if (links == null) {
            return null;
        }

        LinkEvent link = links.get(TopologyElement.TYPE_PACKET_LAYER);
        if (link != null) {
            // return packet link
            return new LinkImpl(this, link.getLinkTuple());
        } else {
            // return whatever found
            Iterator<LinkEvent> it = links.values().iterator();
            if (it.hasNext()) {
                return new LinkImpl(this, it.next().getLinkTuple());
            }
        }
        return null;
    }

    @Override
    public Link getOutgoingLink(Dpid dpid, PortNumber number, String type) {
        return getOutgoingLink(new SwitchPort(dpid, number), type);
    }

    @Override
    public Link getOutgoingLink(SwitchPort port, String type) {
        Map<String, LinkEvent> links = outgoingLinks.get(port);
        final LinkEvent link = links.get(type);
        if (link != null) {
            return new LinkImpl(this, link.getLinkTuple());
        }
        return null;
    }

    @Override
    public Collection<Link> getOutgoingLinks(SwitchPort port) {
        ConcurrentMap<String, LinkEvent> typeMap = outgoingLinks.get(port);
        if (typeMap == null) {
            return Collections.emptyList();
        }
        return toLinkImpls(typeMap.values());
    }

    /**
     * Converts collection of LinkEvent to collection of LinkImpls.
     *
     * @param links collection of LinkEvent
     * @return collection of LinkImpls
     */
    private Collection<Link> toLinkImpls(final Collection<LinkEvent> links) {
        if (links == null) {
            return Collections.emptyList();
        }
        List<Link> list = new ArrayList<>(links.size());
        for (LinkEvent elm : links) {
            list.add(new LinkImpl(this, elm.getLinkTuple()));
        }
        return list;
    }

    @Override
    public Link getIncomingLink(Dpid dpid, PortNumber number) {
        return getIncomingLink(new SwitchPort(dpid, number));
    }

    @Override
    public Link getIncomingLink(SwitchPort port) {
        Map<String, LinkEvent> links = incomingLinks.get(port);
        return getPacketLinkIfExists(links);
    }

    @Override
    public Link getIncomingLink(Dpid dpid, PortNumber number, String type) {
        return getIncomingLink(new SwitchPort(dpid, number), type);
    }

    @Override
    public Link getIncomingLink(SwitchPort port, String type) {
        Map<String, LinkEvent> links = incomingLinks.get(port);
        final LinkEvent link = links.get(type);
        if (link != null) {
            return new LinkImpl(this, link.getLinkTuple());
        }
        return null;
    }

    @Override
    public Collection<Link> getIncomingLinks(SwitchPort port) {
        ConcurrentMap<String, LinkEvent> typeMap = incomingLinks.get(port);
        if (typeMap == null) {
            return Collections.emptyList();
        }
        return toLinkImpls(typeMap.values());
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
        List<Link> links = new ArrayList<>();

        for (Map<String, LinkEvent> portLinks : outgoingLinks.values()) {
            if (portLinks == null) {
                continue;
            }
            for (LinkEvent elm : portLinks.values()) {
                links.add(new LinkImpl(this, elm.getLinkTuple()));
            }
        }
        return links;
    }

    @Override
    public Host getHostByMac(MACAddress address) {
        HostEvent host = mac2Host.get(address);
        if (host != null) {
            return new HostImpl(this, address);
        }
        return null;
    }

    @Override
    public Iterable<Host> getHosts() {
        return toHostImpls(mac2Host.values());
    }

    /**
     * Converts collection of HostEvent to collection of HostImpl.
     *
     * @param events collection of HostEvent
     * @return collection of HostImpl
     */
    private List<Host> toHostImpls(Collection<HostEvent> events) {
        if (events == null) {
            return Collections.emptyList();
        }
        List<Host> list = new ArrayList<>(events.size());
        for (HostEvent elm : events) {
            list.add(new HostImpl(this, elm.getMac()));
        }
        return list;
    }

    @Override
    public Collection<Host> getHosts(SwitchPort port) {
        return toHostImpls(hosts.get(port));
    }

    @Override
    public SwitchEvent getSwitchEvent(final Dpid dpid) {
        return this.switches.get(dpid);
    }

    @Override
    public Collection<SwitchEvent> getAllSwitchEvents() {
        return Collections.unmodifiableCollection(switches.values());
    }

    @Override
    public PortEvent getPortEvent(final SwitchPort port) {
        return getPortEvent(port.getDpid(), port.getPortNumber());
    }

    @Override
    public PortEvent getPortEvent(final Dpid dpid, PortNumber portNumber) {
        ConcurrentMap<PortNumber, PortEvent> portMap = this.ports.get(dpid);
        if (portMap != null) {
            return portMap.get(portNumber);
        }
        return null;
    }

    @Override
    public Collection<PortEvent> getPortEvents(final Dpid dpid) {
        ConcurrentMap<PortNumber, PortEvent> portList = ports.get(dpid);
        if (portList == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(portList.values());
    }

    @Override
    public Collection<PortEvent> getAllPortEvents() {
        List<PortEvent> events = new LinkedList<>();
        for (ConcurrentMap<PortNumber, PortEvent> cm : ports.values()) {
            events.addAll(cm.values());
        }
        return Collections.unmodifiableCollection(events);
    }

    @Override
    public LinkEvent getLinkEvent(final LinkTuple linkId) {
        ConcurrentMap<String, LinkEvent> links = this.outgoingLinks.get(linkId.getSrc());
        if (links == null) {
            return null;
        }

        // TODO Should we look for Packet link first?
        //  Not unless invariant is broken.

        for (LinkEvent link : links.values()) {
            if (link.getDst().equals(linkId.getDst())) {
                return link;
            }
        }
        return null;
    }

    @Override
    public LinkEvent getLinkEvent(final LinkTuple linkId, final String type) {
        ConcurrentMap<String, LinkEvent> links = this.outgoingLinks.get(linkId.getSrc());
        if (links == null) {
            return null;
        }
        return links.get(type);
    }

    @Override
    public Collection<LinkEvent> getLinkEvents(final LinkTuple linkId) {
        ConcurrentMap<String, LinkEvent> links = this.outgoingLinks.get(linkId.getSrc());
        if (links == null) {
            return Collections.emptyList();
        }

        // unless invariant is broken, this should contain at most 1 element.
        return Collections.unmodifiableCollection(links.values());
    }

    @Override
    public Collection<LinkEvent> getAllLinkEvents() {
        List<LinkEvent> events = new LinkedList<>();
        for (ConcurrentMap<String, LinkEvent> cm : outgoingLinks.values()) {
            events.addAll(cm.values());
        }
        return Collections.unmodifiableCollection(events);
    }

    @Override
    public HostEvent getHostEvent(final MACAddress mac) {
        return this.mac2Host.get(mac);
    }

    @Override
    public Collection<HostEvent> getAllHostEvents() {
        return Collections.unmodifiableCollection(mac2Host.values());
    }

    /**
     * Puts a SwitchEvent.
     *
     * @param sw Switch to add. (Will be frozen if not already)
     */
    @GuardedBy("writeLock")
    protected void putSwitch(SwitchEvent sw) {
        // TODO isFrozen check once we implement CoW/lock-free
        switches.put(sw.getDpid(), sw.freeze());
        ports.putIfAbsent(sw.getDpid(), new ConcurrentHashMap<PortNumber, PortEvent>());
    }

    /**
     * Removes a SwitchEvent from this snapshot.
     * <p/>
     * Will also remove ports, if it has not been removed already.
     *
     * @param dpid Switch DPID
     */
    @GuardedBy("writeLock")
    protected void removeSwitch(Dpid dpid) {
        // TODO isFrozen check once we implement CoW/lock-free
        switches.remove(dpid);
        ConcurrentMap<PortNumber, PortEvent> removedPorts = ports.remove(dpid);
        if (removedPorts != null && !removedPorts.isEmpty()) {
            log.warn("Some ports were removed as side-effect of #removeSwitch({})", dpid);
        }
    }

    /**
     * Puts a PortEvent.
     *
     * @param port Port to add. (Will be frozen if not already)
     */
    @GuardedBy("writeLock")
    protected void putPort(PortEvent port) {

        ConcurrentMap<PortNumber, PortEvent> portMap = ports.get(port.getDpid());
        if (portMap == null) {
            portMap = new ConcurrentHashMap<>();
            ConcurrentMap<PortNumber, PortEvent> existing
                = ports.putIfAbsent(port.getDpid(), portMap);
            if (existing != null) {
                // port map was added concurrently, using theirs
                portMap = existing;
            }
        }
        portMap.put(port.getPortNumber(), port.freeze());
    }

    /**
     * Removes a PortEvent from this snapshot.
     *
     * @param port SwitchPort to remove
     */
    @GuardedBy("writeLock")
    protected void removePort(SwitchPort port) {
        removePort(port.getDpid(), port.getPortNumber());
    }

    /**
     * Removes a PortEvent from this snapshot.
     * <p/>
     * Will also remove ports, if it has not been removed already.
     *
     * @param dpid Switch DPID
     * @param number PortNumber
     */
    @GuardedBy("writeLock")
    protected void removePort(Dpid dpid, PortNumber number) {
        // TODO sanity check Host attachment point.
        ConcurrentMap<PortNumber, PortEvent> portMap = ports.get(dpid);
        if (portMap != null) {
            portMap.remove(number);
        }
    }

    /**
     * Puts a LinkEvent.
     *
     * @param link LinkEvent
     */
    @GuardedBy("writeLock")
    protected void putLink(LinkEvent link) {
        // TODO Do sanity check?
        //   - There cannot be 2 links in same direction between a port pair.
        putLinkMap(outgoingLinks, link.getSrc(), link);
        putLinkMap(incomingLinks, link.getDst(), link);
    }

    /**
     * Helper method to update outgoingLinks, incomingLinks.
     *
     * @param linkMap outgoingLinks or incomingLinks to update
     * @param port {@code linkMap} key to update
     * @param link Link to add
     */
    @GuardedBy("writeLock")
    private void putLinkMap(ConcurrentMap<SwitchPort, ConcurrentMap<String, LinkEvent>> linkMap,
                            SwitchPort port, LinkEvent link) {

        ConcurrentMap<String, LinkEvent> linksOnPort = linkMap.get(port);
        if (linksOnPort == null) {
            linksOnPort = new ConcurrentHashMap<>(4);
            ConcurrentMap<String, LinkEvent> existing
                = linkMap.putIfAbsent(
                    port,
                    linksOnPort);

            if (existing != null) {
                linksOnPort = existing;
            }
        }
        linksOnPort.put(link.getType(), link);
    }

    /**
     * Removes a LinkEvent from this snapshot.
     *
     * @param link Link to remove
     * @param type type of link to remove
     */
    @GuardedBy("writeLock")
    protected void removeLink(LinkTuple link, String type) {
        ConcurrentMap<String, LinkEvent> portLinks
            = outgoingLinks.get(link.getSrc());
        if (portLinks != null) {
            // no conditional update here
            portLinks.remove(type);
        }
        portLinks
            = incomingLinks.get(link.getDst());
        if (portLinks != null) {
            // no conditional update here
            portLinks.remove(type);
        }
    }

    /**
     * Removes a LinkEvent from this snapshot.
     *
     * @param link Link to remove
     */
    @GuardedBy("writeLock")
    protected void removeLink(LinkTuple link) {
        Collection<LinkEvent> links = getLinkEvents(link);
        for (LinkEvent l : links) {
            removeLink(link, l.getType());
        }
    }

    /**
     * Puts a HostEvent.
     * <p/>
     * Removes attachment points for previous HostEvent and update
     * them with new HostEvent
     *
     * @param host HostEvent
     */
    @GuardedBy("writeLock")
    protected void putHost(HostEvent host) {
        // Host cannot be simply put() to replace instance since it has mobility.
        // Simply remove -> put for now.

        // remove old attachment points
        removeHost(host.getMac());

        // add new attachment points
        for (SwitchPort port : host.getAttachmentPoints()) {
            hosts.put(port, host);
        }
        mac2Host.put(host.getMac(), host);
    }

    /**
     * Removes a HostEvent from this snapshot.
     *
     * @param mac MACAddress of the Host to remove
     */
    @GuardedBy("writeLock")
    protected void removeHost(MACAddress mac) {
        HostEvent host = mac2Host.remove(mac);
        if (host != null) {
            for (SwitchPort port : host.getAttachmentPoints()) {
                hosts.remove(port, host);
            }
        }
    }


    @Override
    public void acquireReadLock() {
        readLock.lock();
    }

    @Override
    public void releaseReadLock() {
        readLock.unlock();
    }

    protected void acquireWriteLock() {
        writeLock.lock();
    }

    protected void releaseWriteLock() {
        writeLock.unlock();
    }
}
