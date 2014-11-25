/**
 *    Copyright 2011, Big Switch Networks, Inc.
 *    Originally created by David Erickson, Stanford University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package net.onrc.onos.core.linkdiscovery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitch.PortChangeType;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.IUpdate;
import net.floodlightcontroller.core.annotations.LogMessageCategory;
import net.floodlightcontroller.core.annotations.LogMessageDoc;
import net.floodlightcontroller.core.annotations.LogMessageDocs;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.SingletonTask;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.onrc.onos.core.linkdiscovery.web.LinkDiscoveryWebRoutable;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.packet.LLDP;
import net.onrc.onos.core.packet.OnosLldp;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.util.SwitchPort;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortReason;
import org.projectfloodlight.openflow.protocol.OFPortState;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers links between OpenFlow switches.
 * <p>
 * Discovery is performed by sending probes (LLDP packets) over the links in the
 * data plane. The LinkDiscoveryManager sends probes periodically on all ports
 * on all connected switches. The probes contain the sending switch's DPID and
 * outgoing port number. LLDP packets that are received (via an OpenFlow
 * packet-in) indicate there is a link between the receiving port and the
 * sending port, which was encoded in the LLDP. When the LinkDiscoveryManager
 * observes a new link, a Link object is created and an event is fired for any
 * event listeners.
 * </p>
 * Links are removed for one of three reasons:
 * <ul>
 * <li>A probe has not been received on the link for an interval (the timeout
 * interval)</li>
 * <li>The port went down or was disabled (as observed by OpenFlow port-status
 * messages) or the disconnection of the switch</li>
 * <li>Link discovery was explicitly disabled on a port with the
 * {@link #disableDiscoveryOnPort(long, short)} method</li>
 * </ul>
 * When the LinkDiscoveryManager removes a link it also fires an event for the
 * listeners.
 */
@LogMessageCategory("Network Topology")
public class LinkDiscoveryManager implements IOFMessageListener, IOFSwitchListener,
        ILinkDiscoveryService, IFloodlightModule {

    private static final Logger log =
            LoggerFactory.getLogger(LinkDiscoveryManager.class);

    private IFloodlightProviderService controller;

    private IFloodlightProviderService floodlightProvider;
    private IThreadPoolService threadPool;
    private IRestApiService restApi;
    private IControllerRegistryService registryService;

    // LLDP fields
    static final byte[] LLDP_STANDARD_DST_MAC_STRING =
            HexString.fromHexString("01:80:c2:00:00:0e");
    private static final long LINK_LOCAL_MASK = 0xfffffffffff0L;
    private static final long LINK_LOCAL_VALUE = 0x0180c2000000L;

    // Link discovery task details.
    private SingletonTask discoveryTask;
    private static final int DISCOVERY_TASK_INTERVAL = 1;
    private static final int LINK_TIMEOUT = 35; // original 35 secs, aggressive
                                                // 5 secs
    private static final int LLDP_TO_ALL_INTERVAL = 15; // original 15 seconds,
                                                        // aggressive 2 secs.
    private long lldpClock = 0;
    // This value is intentionally kept higher than LLDP_TO_ALL_INTERVAL.
    // If we want to identify link failures faster, we could decrease this
    // value to a small number, say 1 or 2 sec.
    private static final int LLDP_TO_KNOWN_INTERVAL = 20; // LLDP frequency for
                                                          // known links

    private ReentrantReadWriteLock lock;

    /**
     * Map from link to the most recent time it was verified functioning.
     */
    protected Map<Link, LinkInfo> links;

    /**
     * Map from switch id to a set of all links with it as an endpoint.
     */
    protected Map<Long, Set<Link>> switchLinks;

    /**
     * Map from a id:port to the set of links containing it as an endpoint.
     */
    protected Map<NodePortTuple, Set<Link>> portLinks;

    /**
     * Listeners are called in the order they were added to the the list.
     */
    private final List<ILinkDiscoveryListener> linkDiscoveryListeners =
            new CopyOnWriteArrayList<>();

    /**
     * List of ports through which LLDPs are not sent.
     */
    private Set<NodePortTuple> suppressLinkDiscovery;

    private enum UpdateType {
        LINK_ADDED,
        LINK_REMOVED
    }

    private class LinkUpdate implements IUpdate {
        private final Link link;
        private final UpdateType operation;

        public LinkUpdate(Link link, UpdateType operation) {
            this.link = link;
            this.operation = operation;
        }

        @Override
        public void dispatch() {
            if (log.isTraceEnabled()) {
                log.trace("Dispatching link discovery update {} for {}",
                        operation, link);
            }
            for (ILinkDiscoveryListener listener : linkDiscoveryListeners) {
                switch (operation) {
                case LINK_ADDED:
                    listener.linkAdded(link);
                    break;
                case LINK_REMOVED:
                    listener.linkRemoved(link);
                    break;
                default:
                    log.warn("Unknown link update operation {}", operation);
                    break;
                }
            }
        }
    }

    /**
     * Gets the LLDP sending period in seconds.
     *
     * @return LLDP sending period in seconds.
     */
    public int getLldpFrequency() {
        return LLDP_TO_KNOWN_INTERVAL;
    }

    /**
     * Gets the LLDP timeout value in seconds.
     *
     * @return LLDP timeout value in seconds
     */
    public int getLldpTimeout() {
        return LINK_TIMEOUT;
    }

    @Override
    public Set<NodePortTuple> getDiscoveryDisabledPorts() {
        return suppressLinkDiscovery;
    }

    @Override
    public void disableDiscoveryOnPort(long sw, short port) {
        NodePortTuple npt = new NodePortTuple(sw, port);
        this.suppressLinkDiscovery.add(npt);
        deleteLinksOnPort(npt);
    }

    @Override
    public void enableDiscoveryOnPort(long sw, short port) {
        NodePortTuple npt = new NodePortTuple(sw, port);
        this.suppressLinkDiscovery.remove(npt);
        discover(npt);
    }

    private boolean isLinkDiscoverySuppressed(long sw, short p) {
        return this.suppressLinkDiscovery.contains(new NodePortTuple(sw, p));
    }

    private void discoverLinks() {

        // time out known links.
        timeOutLinks();

        // increment LLDP clock
        lldpClock = (lldpClock + 1) % LLDP_TO_ALL_INTERVAL;

        if (lldpClock == 0) {
            log.debug("Sending LLDP out on all ports.");
            discoverOnAllPorts();
        }
    }

    /**
     * Send LLDP on known ports.
     */
    protected void discoverOnKnownLinkPorts() {
        // Copy the port set.
        Set<NodePortTuple> nptSet = new HashSet<NodePortTuple>();
        nptSet.addAll(portLinks.keySet());

        // Send LLDP from each of them.
        for (NodePortTuple npt : nptSet) {
            discover(npt);
        }
    }

    private void discover(NodePortTuple npt) {
        discover(npt.getNodeId(), npt.getPortId());
    }

    private void discover(long sw, short port) {
        sendDiscoveryMessage(sw, port, false);
    }

    /**
     * Send link discovery message out of a given switch port. The discovery
     * message is a standard LLDP containing ONOS-specific TLVs.
     *
     * @param sw the switch to send on
     * @param port the port to send out
     * @param isReverse indicates whether the LLDP was sent as a response
     */
    @LogMessageDoc(level = "ERROR",
            message = "Failure sending LLDP out port {port} on switch {switch}",
            explanation = "An I/O error occured while sending LLDP message " +
                    "to the switch.",
            recommendation = LogMessageDoc.CHECK_SWITCH)
    protected void sendDiscoveryMessage(long sw, short port,
            boolean isReverse) {

        IOFSwitch iofSwitch = floodlightProvider.getSwitches().get(sw);
        if (iofSwitch == null) {
            return;
        }

        if (port == OFPort.LOCAL.getShortPortNumber()) {
            return;
        }

        OFPortDesc ofpPort = iofSwitch.getPort(port);

        if (ofpPort == null) {
            if (log.isTraceEnabled()) {
                log.trace("Null physical port. sw={}, port={}", sw, port);
            }
            return;
        }

        if (isLinkDiscoverySuppressed(sw, port)) {
            // Don't send LLDPs out of this port as suppressLLDPs set
            return;
        }

        if (log.isTraceEnabled()) {
            log.trace("Sending LLDP packet out of swich: {}, port: {}",
                    sw, port);
        }

        OFFactory factory = iofSwitch.getFactory();
        OFPacketOut po = createLLDPPacketOut(sw, ofpPort, isReverse, factory);

        try {
            iofSwitch.write(po, null);
            iofSwitch.flush();
        } catch (IOException e) {
            log.error("Failure sending LLDP out port " + port + " on switch "
                    + iofSwitch.getStringId(), e);
        }

    }

    /**
     * Creates packet_out LLDP for specified output port.
     *
     * @param dpid the dpid of the outgoing switch
     * @param port the outgoing port
     * @param isReverse whether this is a reverse LLDP or not
     * @param factory the factory to use to create the message
     * @return Packet_out message with LLDP data
     */
    private OFPacketOut createLLDPPacketOut(long dpid,
            final OFPortDesc port, boolean isReverse, OFFactory factory) {
        // Set up packets
        // TODO optimize by not creating new packets each time
        OnosLldp lldpPacket = new OnosLldp();

        Ethernet ethPacket = new Ethernet();
        ethPacket.setEtherType(Ethernet.TYPE_LLDP);
        ethPacket.setDestinationMACAddress(LLDP_STANDARD_DST_MAC_STRING);
        ethPacket.setPayload(lldpPacket);
        ethPacket.setPad(true);

        lldpPacket.setSwitch(dpid);
        lldpPacket.setPort(port.getPortNo().getShortPortNumber());
        lldpPacket.setReverse(isReverse);
        ethPacket.setSourceMACAddress(port.getHwAddr().getBytes());
        final byte[] lldp = ethPacket.serialize();

        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(factory.actions()
                .buildOutput()
                .setPort(OFPort.ofShort(port.getPortNo().getShortPortNumber()))
                .build());
        OFPacketOut po = factory.buildPacketOut()
                .setData(lldp)
                .setBufferId(OFBufferId.NO_BUFFER)
                .setInPort(OFPort.CONTROLLER)
                .setActions(actions)
                .build();

        return po;
    }

    /**
     * Send LLDPs to all switch-ports.
     */
    protected void discoverOnAllPorts() {
        if (log.isTraceEnabled()) {
            log.trace("Sending LLDP packets out of all the enabled ports on switch");
        }

        for (IOFSwitch sw : floodlightProvider.getSwitches().values()) {
            if (sw.getEnabledPorts() == null) {
                continue;
            }
            for (OFPortDesc ofp : sw.getEnabledPorts()) {
                if (isLinkDiscoverySuppressed(sw.getId(),
                        ofp.getPortNo().getShortPortNumber())) {
                    continue;
                }

                sendDiscoveryMessage(sw.getId(),
                        ofp.getPortNo().getShortPortNumber(), false);
            }
        }
    }

    @Override
    public String getName() {
        return "linkdiscovery";
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        switch (msg.getType()) {
        case PACKET_IN:
            if (msg instanceof OFPacketIn) {
                return this.handlePacketIn(sw.getId(), (OFPacketIn) msg,
                        cntx);
            }
            break;
        case PORT_STATUS:
            if (msg instanceof OFPortStatus) {
                return this.handlePortStatus(sw, (OFPortStatus) msg);
            }
            break;
        default:
            break;
        }
        return Command.CONTINUE;
    }

    protected Command handleLldp(LLDP lldp, long sw, OFPacketIn pi, short inport) {
        // If LLDP is suppressed on this port, ignore received packet as well
        IOFSwitch iofSwitch = floodlightProvider.getSwitch(sw);
        if (iofSwitch == null) {
            return Command.STOP;
        }

        if (isLinkDiscoverySuppressed(sw, inport)) {
            return Command.STOP;
        }

        // If this is a malformed LLDP, or not from us, exit
        if (lldp.getPortId() == null || lldp.getPortId().getLength() != 3) {
            return Command.CONTINUE;
        }

        // Verify this LLDP packet matches what we're looking for
        byte[] packetData = pi.getData();
        if (!OnosLldp.isOnosLldp(packetData)) {
            log.trace("Dropping LLDP that wasn't sent by ONOS");
            return Command.STOP;
        }

        SwitchPort switchPort = OnosLldp.extractSwitchPort(packetData);
        long remoteDpid = switchPort.getDpid().value();
        short remotePort = switchPort.getPortNumber().shortValue();
        IOFSwitch remoteSwitch = floodlightProvider.getSwitches().get(
                switchPort.getDpid().value());

        OFPortDesc physicalPort = null;
        if (remoteSwitch != null) {
            physicalPort = remoteSwitch.getPort(remotePort);
            if (!remoteSwitch.portEnabled(remotePort)) {
                if (log.isTraceEnabled()) {
                    log.trace("Ignoring link with disabled source port: " +
                            "switch {} port {}", remoteSwitch, remotePort);
                }
                return Command.STOP;
            }
            if (suppressLinkDiscovery.contains(
                    new NodePortTuple(remoteSwitch.getId(), remotePort))) {
                if (log.isTraceEnabled()) {
                    log.trace("Ignoring link with suppressed src port: " +
                            "switch {} port {}", remoteSwitch, remotePort);
                }
                return Command.STOP;
            }
        }
        if (!iofSwitch.portEnabled(inport)) {
            if (log.isTraceEnabled()) {
                log.trace("Ignoring link with disabled dest port: " +
                        "switch {} port {}", sw, inport);
            }
            return Command.STOP;
        }

        // TODO It probably should be empty Set instead of null. Confirm and fix.
        Set<OFPortState> srcPortState = (physicalPort != null)
                ? physicalPort.getState() : null;
        physicalPort = iofSwitch.getPort(inport);
        Set<OFPortState> dstPortState = (physicalPort != null)
                ? physicalPort.getState() : null;

        // Store the time of update to this link, and push it out to
        // routingEngine
        Link lt = new Link(remoteDpid, remotePort, iofSwitch.getId(), inport);

        LinkInfo linkInfo = new LinkInfo(System.currentTimeMillis(),
                System.currentTimeMillis(), srcPortState, dstPortState);

        addOrUpdateLink(lt, linkInfo);

        // Check if reverse link exists.
        // If it doesn't exist and if the forward link was seen
        // first seen within a small interval, send probe on the
        // reverse link.
        boolean isReverse = OnosLldp.isReverse(lldp);

        LinkInfo newLinkInfo = links.get(lt);
        if (newLinkInfo != null && !isReverse) {
            Link reverseLink = new Link(lt.getDst(), lt.getDstPort(),
                    lt.getSrc(), lt.getSrcPort());
            LinkInfo reverseInfo = links.get(reverseLink);
            if (reverseInfo == null) {
                // the reverse link does not exist.
                if (newLinkInfo.getFirstSeenTime() > System.currentTimeMillis()
                        - LINK_TIMEOUT) {
                    this.sendDiscoveryMessage(lt.getDst(), lt.getDstPort(), true);
                }
            }
        }

        // Consume this message
        return Command.STOP;
    }

    protected Command handlePacketIn(long sw, OFPacketIn pi,
            FloodlightContext cntx) {
        Ethernet eth =
                IFloodlightProviderService.bcStore.get(cntx,
                        IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        short inport = (short) cntx.getStorage()
                .get(IFloodlightProviderService.CONTEXT_PI_INPORT);

        if (eth.getEtherType() == Ethernet.TYPE_LLDP) {
            return handleLldp((LLDP) eth.getPayload(), sw, pi, inport);
        } else if (eth.getEtherType() < 1500) {
            long destMac = eth.getDestinationMAC().toLong();
            if ((destMac & LINK_LOCAL_MASK) == LINK_LOCAL_VALUE) {
                if (log.isTraceEnabled()) {
                    log.trace("Ignoring packet addressed to 802.1D/Q " +
                            "reserved address.");
                }
                return Command.STOP;
            }
        }

        return Command.CONTINUE;
    }

    protected void addOrUpdateLink(Link lt, LinkInfo detectedLinkInfo) {
        lock.writeLock().lock();
        try {
            LinkInfo existingInfo = links.get(lt);

            LinkInfo newLinkInfo = new LinkInfo(
                    ((existingInfo == null) ? detectedLinkInfo.getFirstSeenTime()
                            : existingInfo.getFirstSeenTime()),
                    detectedLinkInfo.getLastProbeReceivedTime(),
                    detectedLinkInfo.getSrcPortState(),
                    detectedLinkInfo.getDstPortState());

            // Add new LinkInfo or update old LinkInfo
            links.put(lt, newLinkInfo);

            if (log.isTraceEnabled()) {
                log.trace("addOrUpdateLink: {}", lt);
            }

            NodePortTuple srcNpt = new NodePortTuple(lt.getSrc(), lt.getSrcPort());
            NodePortTuple dstNpt = new NodePortTuple(lt.getDst(), lt.getDstPort());

            // If this is the first time we've seen the link, add the Link
            // object to the data structures/indexes as well
            if (existingInfo == null) {
                log.trace("Creating new Link: {}", lt);
                // index it by switch source
                if (!switchLinks.containsKey(lt.getSrc())) {
                    switchLinks.put(lt.getSrc(), new HashSet<Link>());
                }
                switchLinks.get(lt.getSrc()).add(lt);

                // index it by switch dest
                if (!switchLinks.containsKey(lt.getDst())) {
                    switchLinks.put(lt.getDst(), new HashSet<Link>());
                }
                switchLinks.get(lt.getDst()).add(lt);

                // index both ends by switch:port
                if (!portLinks.containsKey(srcNpt)) {
                    portLinks.put(srcNpt, new HashSet<Link>());
                }
                portLinks.get(srcNpt).add(lt);

                if (!portLinks.containsKey(dstNpt)) {
                    portLinks.put(dstNpt, new HashSet<Link>());
                }
                portLinks.get(dstNpt).add(lt);

                // Publish LINK_ADDED event
                controller.publishUpdate(new LinkUpdate(lt, UpdateType.LINK_ADDED));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes links from the data structures.
     *
     * @param linksToDelete the list of links to delete
     */
    protected void deleteLinks(List<Link> linksToDelete) {
        lock.writeLock().lock();
        try {
            for (Link lt : linksToDelete) {
                NodePortTuple srcNpt = new NodePortTuple(lt.getSrc(), lt.getSrcPort());
                NodePortTuple dstNpt = new NodePortTuple(lt.getDst(), lt.getDstPort());

                switchLinks.get(lt.getSrc()).remove(lt);
                switchLinks.get(lt.getDst()).remove(lt);
                if (switchLinks.containsKey(lt.getSrc()) &&
                        switchLinks.get(lt.getSrc()).isEmpty()) {
                    this.switchLinks.remove(lt.getSrc());
                }
                if (this.switchLinks.containsKey(lt.getDst()) &&
                        this.switchLinks.get(lt.getDst()).isEmpty()) {
                    this.switchLinks.remove(lt.getDst());
                }

                if (this.portLinks.get(srcNpt) != null) {
                    this.portLinks.get(srcNpt).remove(lt);
                    if (this.portLinks.get(srcNpt).isEmpty()) {
                        this.portLinks.remove(srcNpt);
                    }
                }
                if (this.portLinks.get(dstNpt) != null) {
                    this.portLinks.get(dstNpt).remove(lt);
                    if (this.portLinks.get(dstNpt).isEmpty()) {
                        this.portLinks.remove(dstNpt);
                    }
                }

                this.links.remove(lt);

                controller.publishUpdate(new LinkUpdate(lt,
                        UpdateType.LINK_REMOVED));

                if (log.isTraceEnabled()) {
                    log.trace("Deleted link {}", lt);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Handles an OFPortStatus message from a switch. We will add or delete
     * LinkTupes as well re-compute the topology if needed.
     *
     * @param sw The dpid of the switch that sent the port status message
     * @param ps The OFPortStatus message
     * @return The Command to continue or stop after we process this message
     */
    protected Command handlePortStatus(IOFSwitch sw, OFPortStatus ps) {

        // If we do not control this switch, then we should not process its
        // port status messages
        if (!registryService.hasControl(sw.getId())) {
            return Command.CONTINUE;
        }

        if (log.isTraceEnabled()) {
            log.trace("handlePortStatus: Switch {} port #{} reason {}; " +
                    "config is {} state is {}",
                    new Object[] {sw.getStringId(),
                            ps.getDesc().getPortNo(),
                            ps.getReason(),
                            ps.getDesc().getConfig(),
                            ps.getDesc().getState()});
        }

        short port = ps.getDesc().getPortNo().getShortPortNumber();
        NodePortTuple npt = new NodePortTuple(sw.getId(), port);
        boolean linkDeleted = false;
        boolean linkInfoChanged = false;

        lock.writeLock().lock();
        try {
            // if ps is a delete, or a modify where the port is down or
            // configured down
            if (OFPortReason.DELETE == ps.getReason() ||
                    (OFPortReason.MODIFY == ps.getReason() &&
                    !portEnabled(ps.getDesc()))) {
                deleteLinksOnPort(npt);
                linkDeleted = true;
            } else if (ps.getReason() == OFPortReason.MODIFY) {
                // If ps is a port modification and the port state has changed
                // that affects links in the topology

                if (this.portLinks.containsKey(npt)) {
                    for (Link lt : this.portLinks.get(npt)) {
                        LinkInfo linkInfo = links.get(lt);
                        assert (linkInfo != null);
                        LinkInfo newLinkInfo = null;

                        if (lt.isSrcPort(npt) &&
                                !linkInfo.getSrcPortState().equals(
                                        ps.getDesc().getState())) {
                            // If this port status is for the src port and the
                            // port state has changed, create a new link info
                            // with the new state

                            newLinkInfo = new LinkInfo(linkInfo.getFirstSeenTime(),
                                    linkInfo.getLastProbeReceivedTime(),
                                    ps.getDesc().getState(),
                                    linkInfo.getDstPortState());
                        } else if (lt.isDstPort(npt) &&
                                !linkInfo.getDstPortState().equals(
                                        ps.getDesc().getState())) {
                            // If this port status is for the dst port and the
                            // port state has changed, create a new link info
                            // with the new state

                            newLinkInfo = new LinkInfo(linkInfo.getFirstSeenTime(),
                                    linkInfo.getLastProbeReceivedTime(),
                                    linkInfo.getSrcPortState(),
                                    ps.getDesc().getState());
                        }

                        if (newLinkInfo != null) {
                            linkInfoChanged = true;
                            links.put(lt, newLinkInfo);
                        }
                    }
                }
            }

            if (!linkDeleted && !linkInfoChanged) {
                if (log.isTraceEnabled()) {
                    log.trace("handlePortStatus: Switch {} port #{} reason {};" +
                            " no links to update/remove",
                            new Object[] {HexString.toHexString(sw.getId()),
                                    ps.getDesc().getPortNo(),
                                    ps.getReason()});
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

        if (!linkDeleted) {
            // Send LLDP right away when port state is changed for faster
            // cluster-merge. If it is a link delete then there is not need
            // to send the LLDPs right away and instead we wait for the LLDPs
            // to be sent on the timer as it is normally done
            // do it outside the write-lock
            // sendLLDPTask.reschedule(1000, TimeUnit.MILLISECONDS);
            processNewPort(npt.getNodeId(), npt.getPortId());
        }
        return Command.CONTINUE;
    }

    /**
     * Process a new port. If link discovery is disabled on the port, then do
     * nothing. Otherwise, send LLDP message.
     *
     * @param sw the dpid of the switch the port is on
     * @param p the number of the port
     */
    private void processNewPort(long sw, int p) {
        if (isLinkDiscoverySuppressed(sw, (short) p)) {
            // Do nothing as link discovery is suppressed.
            return;
        } else {
            discover(sw, (short) p);
        }
    }

    /**
     * We send out LLDP messages when a switch is added to discover the
     * topology.
     *
     * @param swId the datapath Id of the new switch
     */
    @Override
    public void switchActivatedMaster(long swId) {
        IOFSwitch sw = floodlightProvider.getSwitch(swId);
        if (sw == null) {
            log.warn("Added switch not available {} ", swId);
            return;
        }
        if (sw.getEnabledPorts() != null) {
            for (Integer p : sw.getEnabledPortNumbers()) {
                processNewPort(swId, p);
            }
        }
    }

    /**
     * When a switch disconnects we remove any links from our map and notify.
     *
     * @param swId the datapath Id of the switch that was removed
     */
    @Override
    public void switchDisconnected(long swId) {
        // Cleanup link state
        List<Link> eraseList = new ArrayList<Link>();
        lock.writeLock().lock();
        try {
            if (switchLinks.containsKey(swId)) {
                if (log.isTraceEnabled()) {
                    log.trace("Handle switchRemoved. Switch {}; removing links {}",
                            HexString.toHexString(swId), switchLinks.get(swId));
                }
                // add all tuples with an endpoint on this switch to erase list
                eraseList.addAll(switchLinks.get(swId));
                deleteLinks(eraseList);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void switchActivatedSlave(long swId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void switchMasterToSlave(long swId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void switchSlaveToMaster(long swId) {
        // for now treat as switchActivatedMaster
        switchActivatedMaster(swId);
    }

    /*
     * We don't react to port changed notifications here. we listen for
     * OFPortStatus messages directly. Might consider using this notifier
     * instead
     */
    @Override
    public void switchPortChanged(long swId, OFPortDesc port,
            PortChangeType changeType) {
        // TODO Auto-generated method stub

    }

    /**
     * Delete links incident on a given switch port.
     *
     * @param npt the port to delete links on
     */
    protected void deleteLinksOnPort(NodePortTuple npt) {
        List<Link> eraseList = new ArrayList<Link>();
        if (this.portLinks.containsKey(npt)) {
            if (log.isTraceEnabled()) {
                log.trace("handlePortStatus: Switch {} port #{} " +
                        "removing links {}",
                        new Object[] {HexString.toHexString(npt.getNodeId()),
                                npt.getPortId(),
                                this.portLinks.get(npt)});
            }
            eraseList.addAll(this.portLinks.get(npt));
            deleteLinks(eraseList);
        }
    }

    /**
     * Iterates through the list of links and deletes if the last discovery
     * message reception time exceeds timeout values.
     */
    protected void timeOutLinks() {
        List<Link> eraseList = new ArrayList<Link>();
        Long curTime = System.currentTimeMillis();

        // reentrant required here because deleteLink also write locks
        lock.writeLock().lock();
        try {
            Iterator<Entry<Link, LinkInfo>> it =
                    this.links.entrySet().iterator();
            while (it.hasNext()) {
                Entry<Link, LinkInfo> entry = it.next();
                LinkInfo info = entry.getValue();

                if ((info.getLastProbeReceivedTime() + (1000L * LINK_TIMEOUT)
                < curTime)) {
                    eraseList.add(entry.getKey());
                }
            }

            deleteLinks(eraseList);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private boolean portEnabled(OFPortDesc port) {
        if (port == null) {
            return false;
        }
        if (port.getConfig().contains(OFPortConfig.PORT_DOWN)) {
            return false;
        }
        if (port.getState().contains(OFPortState.LINK_DOWN)) {
            return false;
        }
        return true;
    }

    @Override
    public Map<Link, LinkInfo> getLinks() {
        lock.readLock().lock();
        Map<Link, LinkInfo> result;
        try {
            result = new HashMap<Link, LinkInfo>(links);
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    @Override
    public void addListener(ILinkDiscoveryListener listener) {
        linkDiscoveryListeners.add(listener);
    }

    @Override
    public void removeListener(ILinkDiscoveryListener listener) {
        linkDiscoveryListeners.remove(listener);
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        return false;
    }

    // IFloodlightModule classes

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(ILinkDiscoveryService.class);
        return l;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService>
            getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> m =
                new HashMap<>();
        m.put(ILinkDiscoveryService.class, this);
        return m;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<>();
        l.add(IFloodlightProviderService.class);
        l.add(IThreadPoolService.class);
        l.add(IRestApiService.class);
        l.add(IControllerRegistryService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context)
            throws FloodlightModuleException {
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        threadPool = context.getServiceImpl(IThreadPoolService.class);
        restApi = context.getServiceImpl(IRestApiService.class);
        registryService = context.getServiceImpl(IControllerRegistryService.class);

        this.lock = new ReentrantReadWriteLock();
        this.links = new HashMap<Link, LinkInfo>();
        this.portLinks = new HashMap<NodePortTuple, Set<Link>>();
        this.suppressLinkDiscovery =
                Collections.synchronizedSet(new HashSet<NodePortTuple>());
        this.switchLinks = new HashMap<Long, Set<Link>>();
    }

    @Override
    @LogMessageDocs({
            @LogMessageDoc(level = "ERROR",
                    message = "No storage source found.",
                    explanation = "Storage source was not initialized; cannot initialize "
                            +
                            "link discovery.",
                    recommendation = LogMessageDoc.REPORT_CONTROLLER_BUG),
            @LogMessageDoc(level = "ERROR",
                    message = "Error in installing listener for " +
                            "switch config table {table}",
                    explanation = "Failed to install storage notification for the " +
                            "switch config table",
                    recommendation = LogMessageDoc.REPORT_CONTROLLER_BUG),
            @LogMessageDoc(level = "ERROR",
                    message = "No storage source found.",
                    explanation = "Storage source was not initialized; cannot initialize "
                            +
                            "link discovery.",
                    recommendation = LogMessageDoc.REPORT_CONTROLLER_BUG),
            @LogMessageDoc(level = "ERROR",
                    message = "Exception in LLDP send timer.",
                    explanation = "An unknown error occured while sending LLDP " +
                            "messages to switches.",
                    recommendation = LogMessageDoc.CHECK_SWITCH)
    })
    public void startUp(FloodlightModuleContext context) {
        ScheduledExecutorService ses = threadPool.getScheduledExecutor();
        controller = context.getServiceImpl(IFloodlightProviderService.class);

        discoveryTask = new SingletonTask(ses, new Runnable() {
            @Override
            public void run() {
                try {
                    discoverLinks();
                } catch (Exception e) {
                    log.error("Exception in LLDP send timer.", e);
                } finally {
                    log.trace("Rescheduling discovery task");
                    discoveryTask.reschedule(DISCOVERY_TASK_INTERVAL,
                            TimeUnit.SECONDS);
                }
            }
        });

        discoveryTask.reschedule(DISCOVERY_TASK_INTERVAL, TimeUnit.SECONDS);

        // Register for the OpenFlow messages we want to receive
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
        floodlightProvider.addOFMessageListener(OFType.PORT_STATUS, this);
        // Register for switch updates
        floodlightProvider.addOFSwitchListener(this);
        restApi.addRestletRoutable(new LinkDiscoveryWebRoutable());
    }
}
