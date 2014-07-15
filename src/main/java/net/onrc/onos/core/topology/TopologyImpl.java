package net.onrc.onos.core.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.concurrent.GuardedBy;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class TopologyImpl implements Topology {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(TopologyImpl.class);

    // DPID -> Switch
    private final ConcurrentMap<Dpid, Switch> switches;
    // XXX may need to be careful when shallow copying.
    private final ConcurrentMap<Dpid, ConcurrentMap<PortNumber, Port>> ports;

    // Index from Port to Host
    private final Multimap<SwitchPort, Host> hosts;
    private final ConcurrentMap<MACAddress, Host> mac2Host;

    // SwitchPort -> (type -> Link)
    private final ConcurrentMap<SwitchPort, ConcurrentMap<String, Link>> outgoingLinks;
    private final ConcurrentMap<SwitchPort, ConcurrentMap<String, Link>> incomingLinks;

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private Lock readLock = readWriteLock.readLock();
    // TODO use the write lock after refactor
    private Lock writeLock = readWriteLock.writeLock();

    public TopologyImpl() {
        // TODO: Does these object need to be stored in Concurrent Collection?
        switches = new ConcurrentHashMap<>();
        ports = new ConcurrentHashMap<>();
        hosts = Multimaps.synchronizedMultimap(
                HashMultimap.<SwitchPort, Host>create());
        mac2Host = new ConcurrentHashMap<>();
        outgoingLinks = new ConcurrentHashMap<>();
        incomingLinks = new ConcurrentHashMap<>();
    }

    @Override
    public Switch getSwitch(Dpid dpid) {
        // TODO Check if it is safe to directly return this Object.
        return switches.get(dpid);
    }

    // Only add switch.
    protected void putSwitch(Switch sw) {
        switches.put(sw.getDpid(), sw);
        ports.putIfAbsent(sw.getDpid(), new ConcurrentHashMap<PortNumber, Port>());
    }

    // XXX Will remove ports in snapshot as side-effect.
    protected void removeSwitch(Dpid dpid) {
        switches.remove(dpid);
        ports.remove(dpid);
    }

    // This method is expected to be serialized by writeLock.
    protected void putPort(Port port) {
        ConcurrentMap<PortNumber, Port> portMap = ports.get(port.getDpid());
        if (portMap == null) {
            portMap = new ConcurrentHashMap<>();
            ConcurrentMap<PortNumber, Port> existing =
                    ports.putIfAbsent(port.getDpid(), portMap);
            if (existing != null) {
                // port map was added concurrently, using theirs
                portMap = existing;
            }
        }
        portMap.put(port.getNumber(), port);
    }

    protected void removePort(Port port) {
        ConcurrentMap<PortNumber, Port> portMap = ports.get(port.getDpid());
        if (portMap != null) {
            portMap.remove(port.getNumber());
        }
    }

    @Override
    public Iterable<Switch> getSwitches() {
        // TODO Check if it is safe to directly return this Object.
        return Collections.unmodifiableCollection(switches.values());
    }

    @Override
    public Port getPort(Dpid dpid, PortNumber number) {
        ConcurrentMap<PortNumber, Port> portMap = ports.get(dpid);
        if (portMap != null) {
            return portMap.get(number);
        }
        return null;
    }

    @Override
    public Port getPort(SwitchPort port) {
        return getPort(port.dpid(), port.port());
    }

    @Override
    public Collection<Port> getPorts(Dpid dpid) {
        ConcurrentMap<PortNumber, Port> portMap = ports.get(dpid);
        if (portMap == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(portMap.values());
    }

    @Override
    public Link getOutgoingLink(Dpid dpid, PortNumber number) {
        return getOutgoingLink(new SwitchPort(dpid, number));
    }

    @Override
    public Link getOutgoingLink(SwitchPort port) {
        Map<String, Link> links = outgoingLinks.get(port);
        return getPacketLinkIfExists(links);
    }

    // TODO remove when we no longer need packet fall back behavior
    /**
     * Gets the "packet" link if such exists, if not return whatever found.
     *
     * @param links Collection of links to search from
     * @return Link instance found or null if no link exists
     */
    private Link getPacketLinkIfExists(Map<String, Link> links) {

        if (links == null) {
            return null;
        }

        Link link = links.get(TopologyElement.TYPE_PACKET_LAYER);
        if (link != null) {
            // return packet link
            return link;
        } else {
            // return whatever found
            Iterator<Link> it = links.values().iterator();
            if (it.hasNext()) {
                return it.next();
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
        Map<String, Link> links = outgoingLinks.get(port);
        return links.get(type);
    }

    @Override
    public Collection<Link> getOutgoingLinks(SwitchPort port) {
        return Collections.unmodifiableCollection(outgoingLinks.get(port).values());
    }

    @Override
    public Link getIncomingLink(Dpid dpid, PortNumber number) {
        return getIncomingLink(new SwitchPort(dpid, number));
    }

    @Override
    public Link getIncomingLink(SwitchPort port) {
        Map<String, Link> links = incomingLinks.get(port);
        return getPacketLinkIfExists(links);
    }

    @Override
    public Link getIncomingLink(Dpid dpid, PortNumber number, String type) {
        return getIncomingLink(new SwitchPort(dpid, number), type);
    }

    @Override
    public Link getIncomingLink(SwitchPort port, String type) {
        Map<String, Link> links = incomingLinks.get(port);
        return links.get(type);
    }

    @Override
    public Collection<Link> getIncomingLinks(SwitchPort port) {
        return Collections.unmodifiableCollection(incomingLinks.get(port).values());
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
            if (link.getDstPort().asSwitchPort().equals(dstSwitchPort)) {
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

        for (Map<String, Link> portLinks : outgoingLinks.values()) {
            links.addAll(portLinks.values());
        }
        return links;
    }

    @GuardedBy("topology.writeLock")
    protected void putLink(Link link) {
        putLinkMap(outgoingLinks, link.getSrcPort().asSwitchPort(), link);
        putLinkMap(incomingLinks, link.getDstPort().asSwitchPort(), link);
    }

    /**
     * Helper method to update outgoingLinks, incomingLinks.
     *
     * @param linkMap outgoingLinks or incomingLinks
     * @param port Map key
     * @param link Link to add
     */
    @GuardedBy("topology.writeLock")
    private void putLinkMap(ConcurrentMap<SwitchPort, ConcurrentMap<String, Link>> linkMap,
                            SwitchPort port, Link link) {
        ConcurrentMap<String, Link> portLinks = new ConcurrentHashMap<String, Link>(3);
        portLinks.put(link.getType(), link);
        Map<String, Link> existing = linkMap.putIfAbsent(
                    port,
                    portLinks);
        if (existing != null) {
            // no conditional update here
            existing.put(link.getType(), link);
        }
    }

    @GuardedBy("topology.writeLock")
    protected void removeLink(Link link) {
        ConcurrentMap<String, Link> portLinks = outgoingLinks.get(link.getSrcPort().asSwitchPort());
        if (portLinks != null) {
            // no conditional update here
            portLinks.remove(link.getType());
        }
        portLinks = incomingLinks.get(link.getDstPort().asSwitchPort());
        if (portLinks != null) {
            // no conditional update here
            portLinks.remove(link.getType());
        }
    }

    @Override
    public Host getHostByMac(MACAddress address) {
        return mac2Host.get(address);
    }

    @Override
    public Iterable<Host> getHosts() {
        return Collections.unmodifiableCollection(mac2Host.values());
    }

    @Override
    public Collection<Host> getHosts(SwitchPort port) {
        return Collections.unmodifiableCollection(hosts.get(port));
    }

    // This method is expected to be serialized by writeLock.
    // XXX new or updated device
    protected void putHost(Host host) {
        // assuming Host is immutable
        Host oldHost = mac2Host.get(host.getMacAddress());
        if (oldHost != null) {
            // remove old attachment point
            removeHost(oldHost);
        }
        // add new attachment points
        for (Port port : host.getAttachmentPoints()) {
            // TODO Won't need remove() if we define Host equality to reflect
            //      all of it's fields.
            hosts.remove(port.asSwitchPort(), host);
            hosts.put(port.asSwitchPort(), host);
        }
        mac2Host.put(host.getMacAddress(), host);
    }

    protected void removeHost(Host host) {
        for (Port port : host.getAttachmentPoints()) {
            hosts.remove(port.asSwitchPort(), host);
        }
        mac2Host.remove(host.getMacAddress());
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
