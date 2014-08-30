package net.onrc.onos.core.topology;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

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
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

/**
 * Immutable Topology snapshot.
 */
@Immutable
public final class ImmutableTopologySnapshot
                implements ImmutableTopology, ImmutableInternalTopology {

    private static final Logger log = LoggerFactory
                .getLogger(ImmutableTopologySnapshot.class);

    /**
     * Empty Topology.
     */
    public static final ImmutableTopologySnapshot EMPTY = new ImmutableTopologySnapshot();

    // interface adaptor
    private final BaseTopologyAdaptor adaptor;


    // Mastership info
    // Dpid -> [ (InstanceID, Role) ]
    private final Map<Dpid, SortedSet<MastershipEvent>> mastership;

    // DPID -> Switch
    private final Map<Dpid, SwitchEvent> switches;
    private final Map<Dpid, Map<PortNumber, PortEvent>> ports;

    // Index from Port to Host
    private final Multimap<SwitchPort, HostEvent> hosts;
    private final Map<MACAddress, HostEvent> mac2Host;

    // SwitchPort -> (type -> Link)
    private final Map<SwitchPort, Map<String, LinkEvent>> outgoingLinks;
    private final Map<SwitchPort, Map<String, LinkEvent>> incomingLinks;


    // TODO Slice out Topology Builder interface.
    //      May need to change put*, remove* return values.
    /**
     * Immutable Topology Builder.
     */
    @NotThreadSafe
    public static final class Builder {

        private final ImmutableTopologySnapshot current;

        /**
         * Builder to start from empty topology.
         */
        private Builder() {
            this.current = new ImmutableTopologySnapshot(EMPTY);
        }

        /**
         * Builder to start building from existing topology.
         *
         * @param original topology to start building from
         */
        private Builder(final ImmutableTopologySnapshot original) {
            // original must be internally mutable instance
            this.current = original;
        }


        /**
         * Gets the current InternalTopology being built.
         *
         * @return InternalTopology
         */
        BaseInternalTopology getCurrentInternal() {
            return this.current;
        }

        /**
         * Gets the current Topology being built.
         *
         * @return Topology
         */
        BaseTopology getCurrent() {
            return this.current;
        }

        /**
         * Builds the {@link ImmutableTopologySnapshot}.
         *
         * @return ImmutableTopologySnapshot
         */
        public ImmutableTopologySnapshot build() {
            return new ImmutableTopologySnapshot(this);
        }

        // TODO Define error conditions for topology mutation
        //  - Element was already gone:
        //          Treat as error or silently ignore?
        //  - Removing element, where child element still exist:
        //          Treat as error or silently remove?

        /**
         * Puts a SwitchEvent.
         *
         * @param sw Switch to add. (Will be frozen if not already)
         * @return Builder
         */
        public Builder putSwitch(SwitchEvent sw) {
            checkNotNull(sw);

            current.switches.put(sw.getDpid(), sw.freeze());
            if (current.ports.get(sw.getDpid()) == null) {
                current.ports.put(sw.getDpid(), new HashMap<PortNumber, PortEvent>());
            }
            return this;
        }

        /**
         * Removes a SwitchEvent from this snapshot.
         * <p>
         * Will also remove ports, if it has not been removed already.
         *
         * @param dpid Switch DPID
         * @return Builder
         */
        public Builder removeSwitch(Dpid dpid) {
            checkNotNull(dpid);

            current.switches.remove(dpid);
            Map<PortNumber, PortEvent> removedPorts = current.ports.remove(dpid);
            if (removedPorts != null && !removedPorts.isEmpty()) {
                log.warn("Some ports were removed as side-effect of #removeSwitch({})", dpid);
            }
            return this;
        }

        /**
         * Puts a PortEvent.
         *
         * @param port Port to add. (Will be frozen if not already)
         * @return Builder
         */
        public Builder putPort(PortEvent port) {
            checkNotNull(port);

            // TODO check parent port and throw TopologyMutationFailed

            Map<PortNumber, PortEvent> portMap = current.ports.get(port.getDpid());
            if (portMap == null) {
                // shouldn't happen but just to be sure
                portMap = new HashMap<>();
                current.ports.put(port.getDpid(), portMap);
            }
            portMap.put(port.getPortNumber(), port.freeze());
            return this;
        }

        /**
         * Removes a PortEvent from this snapshot.
         *
         * @param port SwitchPort to remove
         * @return Builder
         */
        public Builder removePort(SwitchPort port) {
            checkNotNull(port);

            removePort(port.getDpid(), port.getPortNumber());
            return this;
        }

        /**
         * Removes a PortEvent from this snapshot.
         * <p>
         * Will also remove ports, if it has not been removed already.
         *
         * @param dpid Switch DPID
         * @param number PortNumber
         * @return Builder
         */
        public Builder removePort(Dpid dpid, PortNumber number) {
            checkNotNull(dpid);
            checkNotNull(number);

            // TODO sanity check:
            // - Links should be removed
            // - Host attachment point should be updated.
            Map<PortNumber, PortEvent> portMap = current.ports.get(dpid);
            if (portMap != null) {
                portMap.remove(number);
            }
            return this;
        }

        /**
         * Puts a LinkEvent.
         *
         * @param link LinkEvent
         * @return Builder
         */
        public Builder putLink(LinkEvent link) {
            checkNotNull(link);

            // TODO check ports and throw TopologyMutationFailed

            // TODO remove host or ignore?

            // TODO Add sanity check?
            // - There cannot be 2 links in same direction between a port pair.
            putLinkMap(current.outgoingLinks, link.getSrc(), link);
            putLinkMap(current.incomingLinks, link.getDst(), link);
            return this;
        }

        /**
         * Helper method to update outgoingLinks, incomingLinks.
         *
         * @param linkMap outgoingLinks or incomingLinks to update
         * @param port {@code linkMap} key to update
         * @param link Link to add
         */
        private void putLinkMap(Map<SwitchPort, Map<String, LinkEvent>> linkMap,
                                SwitchPort port, LinkEvent link) {

            Map<String, LinkEvent> linksOnPort = linkMap.get(port);
            if (linksOnPort == null) {
                linksOnPort = new HashMap<String, LinkEvent>();
                linkMap.put(port, linksOnPort);
            }
            linksOnPort.put(link.getType(), link);
        }

        /**
         * Removes a LinkEvent from this snapshot.
         *
         * @param link Link to remove
         * @param type type of link to remove
         * @return Builder
         */
        public Builder removeLink(LinkTuple link, String type) {
            checkNotNull(link);

            Map<String, LinkEvent> portLinks
                = current.outgoingLinks.get(link.getSrc());
            if (portLinks != null) {
                // no conditional update here
                portLinks.remove(type);
            }
            portLinks
                = current.incomingLinks.get(link.getDst());
            if (portLinks != null) {
                // no conditional update here
                portLinks.remove(type);
            }
            return this;
        }

        /**
         * Removes a LinkEvent from this snapshot.
         *
         * @param link Link to remove
         * @return Builder
         */
        public Builder removeLink(LinkTuple link) {
            checkNotNull(link);

            Map<String, LinkEvent> links = current.outgoingLinks.get(link.getSrc());
            if (links == null) {
                // nothing to do
                return this;
            }

            for (LinkEvent linkEvt : links.values()) {
                removeLink(linkEvt.getLinkTuple(), linkEvt.getType());
            }
            return this;
        }

        /**
         * Puts a HostEvent.
         * <p>
         * Removes attachment points for previous HostEvent and update
         * them with new HostEvent
         *
         * @param host HostEvent
         * @return Builder
         */
        public Builder putHost(HostEvent host) {
            checkNotNull(host);

            // TODO check Link does not exist on port and throw TopologyMutationFailed

            // Host cannot be simply put() to replace instance
            // since we need to track attachment point update.
            // remove -> put to replace all attachment points, etc. for now.

            // remove old attachment points
            removeHost(host.getMac());

            // add new attachment points
            for (SwitchPort port : host.getAttachmentPoints()) {
                current.hosts.put(port, host);
            }
            current.mac2Host.put(host.getMac(), host);
            return this;
        }

        /**
         * Removes a HostEvent from this snapshot.
         *
         * @param mac MACAddress of the Host to remove
         * @return Builder
         */
        public Builder removeHost(MACAddress mac) {
            checkNotNull(mac);

            HostEvent host = current.mac2Host.remove(mac);
            if (host != null) {
                for (SwitchPort port : host.getAttachmentPoints()) {
                    current.hosts.remove(port, host);
                }
            }
            return this;
        }

        /**
         * Puts a mastership change event.
         *
         * @param master MastershipEvent
         * @return Builder
         */
        public Builder putSwitchMastershipEvent(MastershipEvent master) {
            checkNotNull(master);

            SortedSet<MastershipEvent> candidates
                = current.mastership.get(master.getDpid());
            if (candidates == null) {
                // SortedSet, customized so that MASTER MastershipEvent appear
                // earlier during iteration.
                candidates = new TreeSet<>(new MastershipEvent.MasterFirstComparator());
                current.mastership.put(master.getDpid(), candidates);
            }

            // always replace
            candidates.remove(master);
            candidates.add(master);
            return this;
        }

        /**
         * Removes a mastership change event.
         * <p>
         * Note: Only Dpid and OnosInstanceId will be used to identify the
         * {@link MastershipEvent} to remove.
         *
         * @param master {@link MastershipEvent} to remove. (Role is ignored)
         * @return Builder
         */
        public Builder removeSwitchMastershipEvent(MastershipEvent master) {
            checkNotNull(master);

            SortedSet<MastershipEvent> candidates
                = current.mastership.get(master.getDpid());
            if (candidates == null) {
                // nothing to do
                return this;
            }
            candidates.remove(master);

            return this;
        }
    }

    /**
     * Create an empty Topology.
     */
    private ImmutableTopologySnapshot() {
        mastership = Collections.emptyMap();
        switches = Collections.emptyMap();
        ports = Collections.emptyMap();
        hosts = ImmutableMultimap.of();
        mac2Host = Collections.emptyMap();
        outgoingLinks = Collections.emptyMap();
        incomingLinks = Collections.emptyMap();
        this.adaptor = new BaseTopologyAdaptor(this);
    }

    /**
     * Constructor to create instance from Builder.
     *
     * @param builder Builder
     */
    private ImmutableTopologySnapshot(final Builder builder) {

        // TODO Change to move semantics to avoid shallow copying or
        // Shallow copies should be created using
        // Immutable variant or wrapped by Unmodifiable.
        //
        // If we switched to Immutable* Collections,
        // wrapping by Collections.unmodifiableCollection() can be removed.

        // shallow copy Set in Map
        this.mastership = new HashMap<>(builder.current.mastership.size());
        for (Entry<Dpid, SortedSet<MastershipEvent>> e
                    : builder.current.mastership.entrySet()) {
            this.mastership.put(e.getKey(), new TreeSet<>(e.getValue()));
        }

        this.switches = new HashMap<>(builder.current.switches);

        // shallow copy Map in Map
        this.ports = new HashMap<>(builder.current.ports.size());
        for (Entry<Dpid, Map<PortNumber, PortEvent>> entry
                    : builder.current.ports.entrySet()) {
            this.ports.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }

        this.hosts =
                HashMultimap.<SwitchPort, HostEvent>create(builder.current.hosts);
        this.mac2Host = new HashMap<>(builder.current.mac2Host);

        // shallow copy Map in Map
        this.outgoingLinks = new HashMap<>(builder.current.outgoingLinks.size());
        for (Entry<SwitchPort, Map<String, LinkEvent>> entry
                : builder.current.outgoingLinks.entrySet()) {
            this.outgoingLinks.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }

        // shallow copy Map in Map
        this.incomingLinks = new HashMap<>(builder.current.incomingLinks.size());
        for (Entry<SwitchPort, Map<String, LinkEvent>> entry
                : builder.current.incomingLinks.entrySet()) {
            this.incomingLinks.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }

        this.adaptor = new BaseTopologyAdaptor(this);
    }

    /**
     * Create internally mutable shallow copy of given instance.
     * <p>
     * Note: only expected to be used by Builder.
     *
     * @param original instance to copy from
     */
    private ImmutableTopologySnapshot(ImmutableTopologySnapshot original) {

        // shallow copy Set in Map
        this.mastership = new HashMap<>(original.mastership.size());
        for (Entry<Dpid, SortedSet<MastershipEvent>> e
                        : original.mastership.entrySet()) {
            this.mastership.put(e.getKey(), new TreeSet<>(e.getValue()));
        }

        this.switches = new HashMap<>(original.switches);

        // shallow copy Map in Map
        this.ports = new HashMap<>(original.ports.size());
        for (Entry<Dpid, Map<PortNumber, PortEvent>> entry
                : original.ports.entrySet()) {
            this.ports.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }

        this.hosts =
                HashMultimap.<SwitchPort, HostEvent>create(original.hosts);
        this.mac2Host = new HashMap<>(original.mac2Host);

        // shallow copy Map in Map
        this.outgoingLinks = new HashMap<>(original.outgoingLinks.size());
        for (Entry<SwitchPort, Map<String, LinkEvent>> entry
                : original.outgoingLinks.entrySet()) {
            this.outgoingLinks.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }

        // shallow copy Map in Map
        this.incomingLinks = new HashMap<>(original.incomingLinks.size());
        for (Entry<SwitchPort, Map<String, LinkEvent>> entry
                : original.incomingLinks.entrySet()) {
            this.incomingLinks.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }

        this.adaptor = new BaseTopologyAdaptor(this);
    }


    /**
     * Gets the builder starting from empty topology.
     *
     * @return Builder
     */
    public static Builder initialBuilder() {
        return new Builder();
    }

    /**
     * Gets the builder starting from this topology.
     *
     * @return Builder
     */
    public Builder builder() {
        return new Builder(new ImmutableTopologySnapshot(this));
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
        Map<PortNumber, PortEvent> portMap = this.ports.get(dpid);
        if (portMap != null) {
            return portMap.get(portNumber);
        }
        return null;
    }

    @Override
    public Collection<PortEvent> getPortEvents(final Dpid dpid) {
        Map<PortNumber, PortEvent> portList = ports.get(dpid);
        if (portList == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(portList.values());
    }

    @Override
    public Collection<PortEvent> getAllPortEvents() {
        List<PortEvent> events = new LinkedList<>();
        for (Map<PortNumber, PortEvent> cm : ports.values()) {
            events.addAll(cm.values());
        }
        return Collections.unmodifiableCollection(events);
    }

    @Override
    public LinkEvent getLinkEvent(final LinkTuple linkId) {
        Map<String, LinkEvent> links = this.outgoingLinks.get(linkId.getSrc());
        if (links == null) {
            return null;
        }

        // Should we look for Packet link first?
        //  => Not needed unless invariant is broken.

        for (LinkEvent link : links.values()) {
            if (link.getDst().equals(linkId.getDst())) {
                return link;
            }
        }
        return null;
    }

    @Override
    public LinkEvent getLinkEvent(final LinkTuple linkId, final String type) {
        Map<String, LinkEvent> links = this.outgoingLinks.get(linkId.getSrc());
        if (links == null) {
            return null;
        }
        LinkEvent link = links.get(type);
        if (link.getDst().equals(linkId.getDst())) {
            return link;
        }
        return null;
    }

    @Override
    public Collection<LinkEvent> getLinkEventsFrom(SwitchPort srcPort) {
        Map<String, LinkEvent> links = this.outgoingLinks.get(srcPort);
        if (links == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableCollection(links.values());
    }

    @Override
    public Collection<LinkEvent> getLinkEventsTo(SwitchPort dstPort) {
        Map<String, LinkEvent> links = this.incomingLinks.get(dstPort);
        if (links == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableCollection(links.values());
    }

    @Override
    public Collection<LinkEvent> getLinkEvents(final LinkTuple linkId) {
        Map<String, LinkEvent> links = this.outgoingLinks.get(linkId.getSrc());
        if (links == null) {
            return Collections.emptyList();
        }

        List<LinkEvent> linkEvents = new ArrayList<>();
        for (LinkEvent e : links.values()) {
            if (e.getDst().equals(linkId.getDst())) {
                linkEvents.add(e);
            }
        }

        // unless invariant is broken, this should contain at most 1 element.
        return linkEvents;
    }

    @Override
    public Collection<LinkEvent> getAllLinkEvents() {
        List<LinkEvent> events = new LinkedList<>();
        for (Map<String, LinkEvent> cm : outgoingLinks.values()) {
            events.addAll(cm.values());
        }
        return Collections.unmodifiableCollection(events);
    }

    @Override
    public HostEvent getHostEvent(final MACAddress mac) {
        return this.mac2Host.get(mac);
    }

    @Override
    public Collection<HostEvent> getHostEvents(SwitchPort port) {
        return Collections.unmodifiableCollection(this.hosts.get(port));
    }

    @Override
    public Collection<HostEvent> getAllHostEvents() {
        return Collections.unmodifiableCollection(mac2Host.values());
    }

    /**
     * Gets the master instance ID for a switch.
     *
     * @param dpid switch dpid
     * @return master instance ID or null if there is no master
     */
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


    // TODO find better way to delegate following to interface adaptor

    @Override
    public Switch getSwitch(Dpid dpid) {
        return adaptor.getSwitch(dpid);
    }

    @Override
    public Iterable<Switch> getSwitches() {
        return adaptor.getSwitches();
    }

    @Override
    public Port getPort(Dpid dpid, PortNumber number) {
        return adaptor.getPort(dpid, number);
    }

    @Override
    public Port getPort(SwitchPort port) {
        return adaptor.getPort(port);
    }

    @Override
    public Collection<Port> getPorts(Dpid dpid) {
        return adaptor.getPorts(dpid);
    }

    @Override
    public Link getOutgoingLink(Dpid dpid, PortNumber number) {
        return adaptor.getOutgoingLink(dpid, number);
    }

    @Override
    public Link getOutgoingLink(SwitchPort port) {
        return adaptor.getOutgoingLink(port);
    }

    @Override
    public Link getOutgoingLink(Dpid dpid, PortNumber number, String type) {
        return adaptor.getOutgoingLink(dpid, number, type);
    }

    @Override
    public Link getOutgoingLink(SwitchPort port, String type) {
        return adaptor.getOutgoingLink(port, type);
    }

    @Override
    public Collection<Link> getOutgoingLinks(SwitchPort port) {
        return adaptor.getOutgoingLinks(port);
    }

    @Override
    public Link getIncomingLink(Dpid dpid, PortNumber number) {
        return adaptor.getIncomingLink(dpid, number);
    }

    @Override
    public Link getIncomingLink(SwitchPort port) {
        return adaptor.getIncomingLink(port);
    }

    @Override
    public Link getIncomingLink(Dpid dpid, PortNumber number, String type) {
        return adaptor.getIncomingLink(dpid, number, type);
    }

    @Override
    public Link getIncomingLink(SwitchPort port, String type) {
        return adaptor.getIncomingLink(port, type);
    }

    @Override
    public Collection<Link> getIncomingLinks(SwitchPort port) {
        return adaptor.getIncomingLinks(port);
    }

    @Override
    public Link getLink(Dpid srcDpid, PortNumber srcNumber,
                        Dpid dstDpid, PortNumber dstNumber) {

        return adaptor.getLink(srcDpid, srcNumber, dstDpid, dstNumber);
    }

    @Override
    public Link getLink(Dpid srcDpid, PortNumber srcNumber,
                        Dpid dstDpid, PortNumber dstNumber,
                        String type) {

        return adaptor.getLink(srcDpid, srcNumber, dstDpid, dstNumber, type);
    }

    @Override
    public Iterable<Link> getLinks() {
        return adaptor.getLinks();
    }

    @Override
    public Host getHostByMac(MACAddress address) {
        return adaptor.getHostByMac(address);
    }

    @Override
    public Iterable<Host> getHosts() {
        return adaptor.getHosts();
    }

    @Override
    public Collection<Host> getHosts(SwitchPort port) {
        return adaptor.getHosts(port);
    }
}
