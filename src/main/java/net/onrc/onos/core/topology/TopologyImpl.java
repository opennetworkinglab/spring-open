package net.onrc.onos.core.topology;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.concurrent.GuardedBy;

import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.LinkTuple;
import net.onrc.onos.core.util.OnosInstanceId;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;


// TODO add TopologyManager, etc. to populate Mastership information.
/**
 * Class to represent an instance of Topology Snapshot.
 */
public class TopologyImpl implements MutableTopology, MutableInternalTopology {

    private static final Logger log = LoggerFactory.getLogger(TopologyImpl.class);

    // TODO Revisit Map types after implementing CoW/lock-free

    // Mastership info
    // Dpid -> [ (InstanceID, Role) ]
    private final Map<Dpid, SortedSet<MastershipEvent>> mastership;

    // DPID -> Switch
    private final ConcurrentMap<Dpid, SwitchData> switches;
    private final ConcurrentMap<Dpid, ConcurrentMap<PortNumber, PortData>> ports;

    // Index from Port to Host
    private final Multimap<SwitchPort, HostData> hosts;
    private final ConcurrentMap<MACAddress, HostData> mac2Host;

    // SwitchPort -> (type -> Link)
    private final ConcurrentMap<SwitchPort, ConcurrentMap<String, LinkData>> outgoingLinks;
    private final ConcurrentMap<SwitchPort, ConcurrentMap<String, LinkData>> incomingLinks;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    // TODO use the write lock after refactor
    private final Lock writeLock = readWriteLock.writeLock();

    /**
     * Create an empty Topology.
     */
    public TopologyImpl() {
        mastership = new HashMap<>();
        // TODO: Does these object need to be stored in Concurrent Collection?
        switches = new ConcurrentHashMap<>();
        ports = new ConcurrentHashMap<>();
        hosts = Multimaps.synchronizedMultimap(
                HashMultimap.<SwitchPort, HostData>create());
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
            // shallow copy Set in Map
            this.mastership = new HashMap<>(original.mastership.size());
            for (Entry<Dpid, SortedSet<MastershipEvent>> e
                        : original.mastership.entrySet()) {
                this.mastership.put(e.getKey(), new TreeSet<>(e.getValue()));
            }

            this.switches = new ConcurrentHashMap<>(original.switches);

            // shallow copy Map in Map
            this.ports = new ConcurrentHashMap<>(original.ports.size());
            for (Entry<Dpid, ConcurrentMap<PortNumber, PortData>> entry
                    : original.ports.entrySet()) {
                this.ports.put(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
            }

            this.hosts = Multimaps.synchronizedMultimap(
                    HashMultimap.<SwitchPort, HostData>create(original.hosts));
            this.mac2Host = new ConcurrentHashMap<>(original.mac2Host);

            // shallow copy Map in Map
            this.outgoingLinks = new ConcurrentHashMap<>(original.outgoingLinks.size());
            for (Entry<SwitchPort, ConcurrentMap<String, LinkData>> entry
                    : original.outgoingLinks.entrySet()) {
                this.outgoingLinks.put(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
            }

            // shallow copy Map in Map
            this.incomingLinks = new ConcurrentHashMap<>(original.incomingLinks.size());
            for (Entry<SwitchPort, ConcurrentMap<String, LinkData>> entry
                    : original.incomingLinks.entrySet()) {
                this.incomingLinks.put(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
            }
        } finally {
            original.releaseReadLock();
        }
    }

    @Override
    public Switch getSwitch(Dpid dpid) {
        final SwitchData sw = switches.get(dpid);
        if (sw != null) {
            return new SwitchImpl(this, dpid);
        } else {
            return null;
        }
    }

    @Override
    public Iterable<Switch> getSwitches() {
        List<Switch> list = new ArrayList<>(switches.size());
        for (SwitchData elm : switches.values()) {
            list.add(new SwitchImpl(this, elm.getDpid()));
        }
        return list;
    }

    @Override
    public Port getPort(Dpid dpid, PortNumber number) {
        ConcurrentMap<PortNumber, PortData> portMap = ports.get(dpid);
        if (portMap != null) {
            final PortData port = portMap.get(number);
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
        ConcurrentMap<PortNumber, PortData> portMap = ports.get(dpid);
        if (portMap == null) {
            return Collections.emptyList();
        }
        List<Port> list = new ArrayList<>(portMap.size());
        for (PortData elm : portMap.values()) {
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
        Map<String, LinkData> links = outgoingLinks.get(port);
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
    private Link getPacketLinkIfExists(Map<String, LinkData> links) {

        if (links == null) {
            return null;
        }

        LinkData link = links.get(TopologyElement.TYPE_PACKET_LAYER);
        if (link != null) {
            // return packet link
            return new LinkImpl(this, link.getLinkTuple());
        } else {
            // return whatever found
            Iterator<LinkData> it = links.values().iterator();
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
        Map<String, LinkData> links = outgoingLinks.get(port);
        final LinkData link = links.get(type);
        if (link != null) {
            return new LinkImpl(this, link.getLinkTuple());
        }
        return null;
    }

    @Override
    public Collection<Link> getOutgoingLinks(SwitchPort port) {
        ConcurrentMap<String, LinkData> typeMap = outgoingLinks.get(port);
        if (typeMap == null) {
            return Collections.emptyList();
        }
        return toLinkImpls(typeMap.values());
    }

    /**
     * Converts collection of LinkData to collection of LinkImpls.
     *
     * @param links collection of LinkData
     * @return collection of LinkImpls
     */
    private Collection<Link> toLinkImpls(final Collection<LinkData> links) {
        if (links == null) {
            return Collections.emptyList();
        }
        List<Link> list = new ArrayList<>(links.size());
        for (LinkData elm : links) {
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
        Map<String, LinkData> links = incomingLinks.get(port);
        return getPacketLinkIfExists(links);
    }

    @Override
    public Link getIncomingLink(Dpid dpid, PortNumber number, String type) {
        return getIncomingLink(new SwitchPort(dpid, number), type);
    }

    @Override
    public Link getIncomingLink(SwitchPort port, String type) {
        Map<String, LinkData> links = incomingLinks.get(port);
        final LinkData link = links.get(type);
        if (link != null) {
            return new LinkImpl(this, link.getLinkTuple());
        }
        return null;
    }

    @Override
    public Collection<Link> getIncomingLinks(SwitchPort port) {
        ConcurrentMap<String, LinkData> typeMap = incomingLinks.get(port);
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

        for (Map<String, LinkData> portLinks : outgoingLinks.values()) {
            if (portLinks == null) {
                continue;
            }
            for (LinkData elm : portLinks.values()) {
                links.add(new LinkImpl(this, elm.getLinkTuple()));
            }
        }
        return links;
    }

    @Override
    public Host getHostByMac(MACAddress address) {
        HostData host = mac2Host.get(address);
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
     * Converts collection of HostData to collection of HostImpl.
     *
     * @param events collection of HostData
     * @return collection of HostImpl
     */
    private List<Host> toHostImpls(Collection<HostData> events) {
        if (events == null) {
            return Collections.emptyList();
        }
        List<Host> list = new ArrayList<>(events.size());
        for (HostData elm : events) {
            list.add(new HostImpl(this, elm.getMac()));
        }
        return list;
    }

    @Override
    public Collection<Host> getHosts(SwitchPort port) {
        return toHostImpls(hosts.get(port));
    }

    @Override
    public SwitchData getSwitchData(final Dpid dpid) {
        return this.switches.get(dpid);
    }

    @Override
    public Collection<SwitchData> getAllSwitchDataEntries() {
        return Collections.unmodifiableCollection(switches.values());
    }

    @Override
    public PortData getPortData(final SwitchPort port) {
        return getPortData(port.getDpid(), port.getPortNumber());
    }

    @Override
    public PortData getPortData(final Dpid dpid, PortNumber portNumber) {
        ConcurrentMap<PortNumber, PortData> portMap = this.ports.get(dpid);
        if (portMap != null) {
            return portMap.get(portNumber);
        }
        return null;
    }

    @Override
    public Collection<PortData> getPortDataEntries(final Dpid dpid) {
        ConcurrentMap<PortNumber, PortData> portList = ports.get(dpid);
        if (portList == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(portList.values());
    }

    @Override
    public Collection<PortData> getAllPortDataEntries() {
        List<PortData> events = new LinkedList<>();
        for (ConcurrentMap<PortNumber, PortData> cm : ports.values()) {
            events.addAll(cm.values());
        }
        return Collections.unmodifiableCollection(events);
    }

    @Override
    public LinkData getLinkData(final LinkTuple linkId) {
        ConcurrentMap<String, LinkData> links = this.outgoingLinks.get(linkId.getSrc());
        if (links == null) {
            return null;
        }

        // TODO Should we look for Packet link first?
        //  Not unless invariant is broken.

        for (LinkData link : links.values()) {
            if (link.getDst().equals(linkId.getDst())) {
                return link;
            }
        }
        return null;
    }

    @Override
    public LinkData getLinkData(final LinkTuple linkId, final String type) {
        ConcurrentMap<String, LinkData> links = this.outgoingLinks.get(linkId.getSrc());
        if (links == null) {
            return null;
        }
        LinkData link = links.get(type);
        if (link.getDst().equals(linkId.getDst())) {
            return link;
        }
        return null;
    }

    @Override
    public Collection<LinkData> getLinkDataEntriesFrom(SwitchPort srcPort) {
        ConcurrentMap<String, LinkData> links = this.outgoingLinks.get(srcPort);
        if (links == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(links.values());
    }

    @Override
    public Collection<LinkData> getLinkDataEntriesTo(SwitchPort dstPort) {
        ConcurrentMap<String, LinkData> links = this.incomingLinks.get(dstPort);
        if (links == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(links.values());
    }

    @Override
    public Collection<LinkData> getLinkDataEntries(final LinkTuple linkId) {
        ConcurrentMap<String, LinkData> links = this.outgoingLinks.get(linkId.getSrc());
        if (links == null) {
            return Collections.emptyList();
        }

        List<LinkData> linkDataEntries = new ArrayList<>();
        for (LinkData e : links.values()) {
            if (e.getDst().equals(linkId.getDst())) {
                linkDataEntries.add(e);
            }
        }

        // unless invariant is broken, this should contain at most 1 element.
        return linkDataEntries;
    }

    @Override
    public Collection<LinkData> getAllLinkDataEntries() {
        List<LinkData> events = new LinkedList<>();
        for (ConcurrentMap<String, LinkData> cm : outgoingLinks.values()) {
            events.addAll(cm.values());
        }
        return Collections.unmodifiableCollection(events);
    }

    @Override
    public HostData getHostData(final MACAddress mac) {
        return this.mac2Host.get(mac);
    }

    @Override
    public Collection<HostData> getHostDataEntries(SwitchPort port) {
        return Collections.unmodifiableCollection(hosts.get(port));
    }

    @Override
    public Collection<HostData> getAllHostDataEntries() {
        return Collections.unmodifiableCollection(mac2Host.values());
    }

    @Override
    public OnosInstanceId getSwitchMaster(Dpid dpid) {
        final SortedSet<MastershipEvent> candidates = mastership.get(dpid);
        if (candidates == null) {
            return null;
        }
        for (MastershipEvent candidate : candidates) {
            if (candidate.getRole() == Role.MASTER) {
                return candidate.getOnosInstanceId();
            }
        }
        return null;
    }

    /**
     * Puts a SwitchData.
     *
     * @param sw Switch to add. (Will be frozen if not already)
     */
    @GuardedBy("writeLock")
    protected void putSwitch(SwitchData sw) {
        // TODO isFrozen check once we implement CoW/lock-free
        switches.put(sw.getDpid(), sw.freeze());
        ports.putIfAbsent(sw.getDpid(), new ConcurrentHashMap<PortNumber, PortData>());
    }

    /**
     * Removes a SwitchData from this snapshot.
     * <p/>
     * Will also remove ports, if it has not been removed already.
     *
     * @param dpid Switch DPID
     */
    @GuardedBy("writeLock")
    protected void removeSwitch(Dpid dpid) {
        // TODO isFrozen check once we implement CoW/lock-free
        switches.remove(dpid);
        ConcurrentMap<PortNumber, PortData> removedPorts = ports.remove(dpid);
        if (removedPorts != null && !removedPorts.isEmpty()) {
            log.warn("Some ports were removed as side-effect of #removeSwitch({})", dpid);
        }
    }

    /**
     * Puts a PortData.
     *
     * @param port Port to add. (Will be frozen if not already)
     */
    @GuardedBy("writeLock")
    protected void putPort(PortData port) {

        ConcurrentMap<PortNumber, PortData> portMap = ports.get(port.getDpid());
        if (portMap == null) {
            portMap = new ConcurrentHashMap<>();
            ConcurrentMap<PortNumber, PortData> existing
                = ports.putIfAbsent(port.getDpid(), portMap);
            if (existing != null) {
                // port map was added concurrently, using theirs
                portMap = existing;
            }
        }
        portMap.put(port.getPortNumber(), port.freeze());
    }

    /**
     * Removes a PortData from this snapshot.
     *
     * @param port SwitchPort to remove
     */
    @GuardedBy("writeLock")
    protected void removePort(SwitchPort port) {
        removePort(port.getDpid(), port.getPortNumber());
    }

    /**
     * Removes a PortData from this snapshot.
     * <p/>
     * Will also remove ports, if it has not been removed already.
     *
     * @param dpid Switch DPID
     * @param number PortNumber
     */
    @GuardedBy("writeLock")
    protected void removePort(Dpid dpid, PortNumber number) {
        // TODO sanity check Host attachment point.
        ConcurrentMap<PortNumber, PortData> portMap = ports.get(dpid);
        if (portMap != null) {
            portMap.remove(number);
        }
    }

    /**
     * Puts a LinkData.
     *
     * @param link LinkData
     */
    @GuardedBy("writeLock")
    protected void putLink(LinkData link) {
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
    private void putLinkMap(ConcurrentMap<SwitchPort, ConcurrentMap<String, LinkData>> linkMap,
                            SwitchPort port, LinkData link) {

        ConcurrentMap<String, LinkData> linksOnPort = linkMap.get(port);
        if (linksOnPort == null) {
            linksOnPort = new ConcurrentHashMap<>(4);
            ConcurrentMap<String, LinkData> existing
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
     * Removes a LinkData from this snapshot.
     *
     * @param link Link to remove
     * @param type type of link to remove
     */
    @GuardedBy("writeLock")
    protected void removeLink(LinkTuple link, String type) {
        ConcurrentMap<String, LinkData> portLinks
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
     * Removes a LinkData from this snapshot.
     *
     * @param link Link to remove
     */
    @GuardedBy("writeLock")
    protected void removeLink(LinkTuple link) {
        Collection<LinkData> links = getLinkDataEntries(link);
        for (LinkData l : links) {
            removeLink(link, l.getType());
        }
    }

    /**
     * Puts a HostData.
     * <p/>
     * Removes attachment points for previous HostData and update
     * them with new HostData
     *
     * @param host HostData
     */
    @GuardedBy("writeLock")
    protected void putHost(HostData host) {
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
     * Removes a HostData from this snapshot.
     *
     * @param mac MACAddress of the Host to remove
     */
    @GuardedBy("writeLock")
    protected void removeHost(MACAddress mac) {
        HostData host = mac2Host.remove(mac);
        if (host != null) {
            for (SwitchPort port : host.getAttachmentPoints()) {
                hosts.remove(port, host);
            }
        }
    }

    /**
     * Puts a mastership change event.
     *
     * @param master MastershipEvent
     */
    @GuardedBy("writeLock")
    protected void putSwitchMastershipEvent(MastershipEvent master) {
        checkNotNull(master);

        SortedSet<MastershipEvent> candidates
            = mastership.get(master.getDpid());
        if (candidates == null) {
            // SortedSet, customized so that MASTER MastershipEvent appear
            // earlier during iteration.
            candidates = new TreeSet<>(new MastershipEvent.MasterFirstComparator());
        }

        // always replace
        candidates.remove(master);
        candidates.add(master);
    }

    /**
     * Removes a mastership change event.
     * <p>
     * Note: Only Dpid and OnosInstanceId will be used to identify the
     * {@link MastershipEvent} to remove.
     *
     * @param master {@link MastershipEvent} to remove. (Role is ignored)
     */
    @GuardedBy("writeLock")
    protected void removeSwitchMastershipEvent(MastershipEvent master) {
        checkNotNull(master);

        SortedSet<MastershipEvent> candidates
            = mastership.get(master.getDpid());
        if (candidates == null) {
            // nothing to do
            return;
        }
        candidates.remove(master);
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
