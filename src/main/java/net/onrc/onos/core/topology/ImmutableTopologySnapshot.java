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
    private final Map<Dpid, SwitchData> switches;
    private final Map<Dpid, Map<PortNumber, PortData>> ports;

    // Index from Port to Host
    private final Multimap<SwitchPort, HostData> hosts;
    private final Map<MACAddress, HostData> mac2Host;

    // SwitchPort -> (type -> Link)
    private final Map<SwitchPort, Map<String, LinkData>> outgoingLinks;
    private final Map<SwitchPort, Map<String, LinkData>> incomingLinks;


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
         * Puts a SwitchData.
         *
         * @param sw Switch to add. (Will be frozen if not already)
         * @return Builder
         */
        public Builder putSwitch(SwitchData sw) {
            checkNotNull(sw);

            current.switches.put(sw.getDpid(), sw.freeze());
            if (current.ports.get(sw.getDpid()) == null) {
                current.ports.put(sw.getDpid(), new HashMap<PortNumber, PortData>());
            }
            return this;
        }

        /**
         * Removes a SwitchData from this snapshot.
         * <p>
         * Will also remove ports, if it has not been removed already.
         *
         * @param dpid Switch DPID
         * @return Builder
         */
        public Builder removeSwitch(Dpid dpid) {
            checkNotNull(dpid);

            current.switches.remove(dpid);
            Map<PortNumber, PortData> removedPorts = current.ports.remove(dpid);
            if (removedPorts != null && !removedPorts.isEmpty()) {
                log.warn("Some ports were removed as side-effect of #removeSwitch({})", dpid);
            }
            return this;
        }

        /**
         * Puts a PortData.
         *
         * @param port Port to add. (Will be frozen if not already)
         * @return Builder
         */
        public Builder putPort(PortData port) {
            checkNotNull(port);

            // TODO check parent port and throw TopologyMutationFailed

            Map<PortNumber, PortData> portMap = current.ports.get(port.getDpid());
            if (portMap == null) {
                // shouldn't happen but just to be sure
                portMap = new HashMap<>();
                current.ports.put(port.getDpid(), portMap);
            }
            portMap.put(port.getPortNumber(), port.freeze());
            return this;
        }

        /**
         * Removes a PortData from this snapshot.
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
         * Removes a PortData from this snapshot.
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
            Map<PortNumber, PortData> portMap = current.ports.get(dpid);
            if (portMap != null) {
                portMap.remove(number);
            }
            return this;
        }

        /**
         * Puts a LinkData.
         *
         * @param link LinkData
         * @return Builder
         */
        public Builder putLink(LinkData link) {
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
        private void putLinkMap(Map<SwitchPort, Map<String, LinkData>> linkMap,
                                SwitchPort port, LinkData link) {

            Map<String, LinkData> linksOnPort = linkMap.get(port);
            if (linksOnPort == null) {
                linksOnPort = new HashMap<String, LinkData>();
                linkMap.put(port, linksOnPort);
            }
            linksOnPort.put(link.getType(), link);
        }

        /**
         * Removes a LinkData from this snapshot.
         *
         * @param link Link to remove
         * @param type type of link to remove
         * @return Builder
         */
        public Builder removeLink(LinkTuple link, String type) {
            checkNotNull(link);

            Map<String, LinkData> portLinks
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
         * Removes a LinkData from this snapshot.
         *
         * @param link Link to remove
         * @return Builder
         */
        public Builder removeLink(LinkTuple link) {
            checkNotNull(link);

            Map<String, LinkData> links = current.outgoingLinks.get(link.getSrc());
            if (links == null) {
                // nothing to do
                return this;
            }

            for (LinkData linkData : links.values()) {
                removeLink(linkData.getLinkTuple(), linkData.getType());
            }
            return this;
        }

        /**
         * Puts a HostData.
         * <p>
         * Removes attachment points for previous HostData and update
         * them with new HostData
         *
         * @param host HostData
         * @return Builder
         */
        public Builder putHost(HostData host) {
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
         * Removes a HostData from this snapshot.
         *
         * @param mac MACAddress of the Host to remove
         * @return Builder
         */
        public Builder removeHost(MACAddress mac) {
            checkNotNull(mac);

            HostData host = current.mac2Host.remove(mac);
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
        for (Entry<Dpid, Map<PortNumber, PortData>> entry
                    : builder.current.ports.entrySet()) {
            this.ports.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }

        this.hosts =
                HashMultimap.<SwitchPort, HostData>create(builder.current.hosts);
        this.mac2Host = new HashMap<>(builder.current.mac2Host);

        // shallow copy Map in Map
        this.outgoingLinks = new HashMap<>(builder.current.outgoingLinks.size());
        for (Entry<SwitchPort, Map<String, LinkData>> entry
                : builder.current.outgoingLinks.entrySet()) {
            this.outgoingLinks.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }

        // shallow copy Map in Map
        this.incomingLinks = new HashMap<>(builder.current.incomingLinks.size());
        for (Entry<SwitchPort, Map<String, LinkData>> entry
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
        for (Entry<Dpid, Map<PortNumber, PortData>> entry
                : original.ports.entrySet()) {
            this.ports.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }

        this.hosts =
                HashMultimap.<SwitchPort, HostData>create(original.hosts);
        this.mac2Host = new HashMap<>(original.mac2Host);

        // shallow copy Map in Map
        this.outgoingLinks = new HashMap<>(original.outgoingLinks.size());
        for (Entry<SwitchPort, Map<String, LinkData>> entry
                : original.outgoingLinks.entrySet()) {
            this.outgoingLinks.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }

        // shallow copy Map in Map
        this.incomingLinks = new HashMap<>(original.incomingLinks.size());
        for (Entry<SwitchPort, Map<String, LinkData>> entry
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
        Map<PortNumber, PortData> portMap = this.ports.get(dpid);
        if (portMap != null) {
            return portMap.get(portNumber);
        }
        return null;
    }

    @Override
    public Collection<PortData> getPortDataEntries(final Dpid dpid) {
        Map<PortNumber, PortData> portList = ports.get(dpid);
        if (portList == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(portList.values());
    }

    @Override
    public Collection<PortData> getAllPortDataEntries() {
        List<PortData> dataEntries = new LinkedList<>();
        for (Map<PortNumber, PortData> cm : ports.values()) {
            dataEntries.addAll(cm.values());
        }
        return Collections.unmodifiableCollection(dataEntries);
    }

    @Override
    public LinkData getLinkData(final LinkTuple linkId) {
        Map<String, LinkData> links = this.outgoingLinks.get(linkId.getSrc());
        if (links == null) {
            return null;
        }

        // Should we look for Packet link first?
        //  => Not needed unless invariant is broken.

        for (LinkData link : links.values()) {
            if (link.getDst().equals(linkId.getDst())) {
                return link;
            }
        }
        return null;
    }

    @Override
    public LinkData getLinkData(final LinkTuple linkId, final String type) {
        Map<String, LinkData> links = this.outgoingLinks.get(linkId.getSrc());
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
        Map<String, LinkData> links = this.outgoingLinks.get(srcPort);
        if (links == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableCollection(links.values());
    }

    @Override
    public Collection<LinkData> getLinkDataEntriesTo(SwitchPort dstPort) {
        Map<String, LinkData> links = this.incomingLinks.get(dstPort);
        if (links == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableCollection(links.values());
    }

    @Override
    public Collection<LinkData> getLinkDataEntries(final LinkTuple linkId) {
        Map<String, LinkData> links = this.outgoingLinks.get(linkId.getSrc());
        if (links == null) {
            return Collections.emptyList();
        }

        List<LinkData> linkDataEntries = new ArrayList<>();
        for (LinkData ld : links.values()) {
            if (ld.getDst().equals(linkId.getDst())) {
                linkDataEntries.add(ld);
            }
        }

        // unless invariant is broken, this should contain at most 1 element.
        return linkDataEntries;
    }

    @Override
    public Collection<LinkData> getAllLinkDataEntries() {
        List<LinkData> dataEntries = new LinkedList<>();
        for (Map<String, LinkData> cm : outgoingLinks.values()) {
            dataEntries.addAll(cm.values());
        }
        return Collections.unmodifiableCollection(dataEntries);
    }

    @Override
    public HostData getHostData(final MACAddress mac) {
        return this.mac2Host.get(mac);
    }

    @Override
    public Collection<HostData> getHostDataEntries(SwitchPort port) {
        return Collections.unmodifiableCollection(this.hosts.get(port));
    }

    @Override
    public Collection<HostData> getAllHostDataEntries() {
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
