package net.onrc.onos.apps.segmentrouting;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOF13Switch;
import net.floodlightcontroller.core.IOF13Switch.NeighborSet;
import net.floodlightcontroller.core.internal.OFBarrierReplyFuture;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.SingletonTask;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.api.packet.IPacketListener;
import net.onrc.onos.api.packet.IPacketService;
import net.onrc.onos.apps.segmentrouting.SegmentRoutingPolicy.PolicyType;
import net.onrc.onos.apps.segmentrouting.web.SegmentRoutingWebRoutable;
import net.onrc.onos.core.datagrid.IDatagridService;
import net.onrc.onos.core.datagrid.IEventChannel;
import net.onrc.onos.core.datagrid.IEventChannelListener;
import net.onrc.onos.core.drivermanager.OFSwitchImplDellOSR;
import net.onrc.onos.core.flowprogrammer.IFlowPusherService;
import net.onrc.onos.core.intent.Path;
import net.onrc.onos.core.main.config.IConfigInfoService;
import net.onrc.onos.core.matchaction.MatchAction;
import net.onrc.onos.core.matchaction.MatchActionFloodlightService;
import net.onrc.onos.core.matchaction.MatchActionId;
import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.matchaction.MatchActionOperations;
import net.onrc.onos.core.matchaction.MatchActionOperations.Operator;
import net.onrc.onos.core.matchaction.MatchActionOperationsId;
import net.onrc.onos.core.matchaction.MatchActionService;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.CopyTtlInAction;
import net.onrc.onos.core.matchaction.action.CopyTtlOutAction;
import net.onrc.onos.core.matchaction.action.DecMplsTtlAction;
import net.onrc.onos.core.matchaction.action.DecNwTtlAction;
import net.onrc.onos.core.matchaction.action.GroupAction;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.action.PopMplsAction;
import net.onrc.onos.core.matchaction.action.PushMplsAction;
import net.onrc.onos.core.matchaction.action.SetDAAction;
import net.onrc.onos.core.matchaction.action.SetMplsIdAction;
import net.onrc.onos.core.matchaction.action.SetSAAction;
import net.onrc.onos.core.matchaction.match.Ipv4Match;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.matchaction.match.MplsMatch;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.packet.ARP;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.packet.IPv4;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.topology.ITopologyListener;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.Link;
import net.onrc.onos.core.topology.LinkData;
import net.onrc.onos.core.topology.MastershipData;
import net.onrc.onos.core.topology.MutableTopology;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.PortData;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.topology.SwitchData;
import net.onrc.onos.core.topology.TopologyEvents;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.IPv4Net;
import net.onrc.onos.core.util.IdGenerator;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.json.JSONArray;
import org.json.JSONException;
import org.projectfloodlight.openflow.protocol.OFBarrierReply;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SegmentRoutingManager implements IFloodlightModule,
        ITopologyListener, IPacketListener, ISegmentRoutingService {

    private static final Logger log = LoggerFactory
            .getLogger(SegmentRoutingManager.class);

    private ITopologyService topologyService;
    private IPacketService packetService;
    private IDatagridService datagridService;
    private IControllerRegistryService registryService;
    private IEventChannel<String, PolicyNotification> policyEventChannel;
    private IEventChannel<String, TunnelNotification> tunnelEventChannel;
    private MatchActionService matchActionService;
    private MutableTopology mutableTopology;
    private ConcurrentLinkedQueue<IPv4> ipPacketQueue;
    private IRestApiService restApi;
    private List<ArpEntry> arpEntries;
    private ArpHandler arpHandler;
    private GenericIpHandler ipHandler;
    private IcmpHandler icmpHandler;
    private IThreadPoolService threadPool;
    private SingletonTask discoveryTask;
    private SingletonTask linkAddTask;
    private SingletonTask testTask;
    private IFloodlightProviderService floodlightProvider;

    private HashMap<Switch, ECMPShortestPathGraph> graphs;
    private HashMap<String, LinkData> linksDown;
    private HashMap<String, LinkData> linksToAdd;
    private ConcurrentLinkedQueue<TopologyEvents> topologyEventQueue;
    private HashMap<String, SegmentRoutingPolicy> policyTable;
    private HashMap<String, SegmentRoutingTunnel> tunnelTable;
    private HashMap<Integer, HashMap<Integer, List<Integer>>> adjacencySidTable;
    private HashMap<String, HashMap<Integer, Integer>> adjcencyGroupIdTable;
    private PolicyEventHandler policyEventHandler;
    private TunnelEventHandler tunnelEventHandler;

    private int operationMode = 0;

    // Flag whether transit router supports ECMP or not
    // private boolean supportTransitECMP = true;

    private int testMode = 0;

    private int numOfEvents = 0;
    private int numOfEventProcess = 0;
    private int numOfPopulation = 0;
    // private long matchActionId = 0L;
    private IdGenerator<MatchActionId> maIdGenerator;
    private IdGenerator<MatchActionOperationsId> maoIdGenerator;
    private List<String> switchDpidListWithMastership;

    private static final String SR_POLICY_CHANNEL_NAME = "onos.sr_policy";
    private static final String SR_TUNNEL_CHANNEL_NAME = "onos.sr_tunnel";

    private final int DELAY_TO_ADD_LINK = 10;
    private final int MAX_NUM_LABELS = 3;

    private final int POLICY_ADD1 = 1;
    private final int POLICY_ADD2 = 2;
    private final int POLICY_REMOVE1 = 3;
    private final int POLICY_REMOVE2 = 4;
    private final int TUNNEL_REMOVE1 = 5;
    private final int TUNNEL_REMOVE2 = 6;


    // ************************************
    // IFloodlightModule implementation
    // ************************************

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<>();
        l.add(ISegmentRoutingService.class);
        return l;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<>();
        m.put(ISegmentRoutingService.class, this);
        return m;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();

        l.add(IFloodlightProviderService.class);
        l.add(IConfigInfoService.class);
        l.add(ITopologyService.class);
        l.add(IPacketService.class);
        l.add(IFlowPusherService.class);
        l.add(ITopologyService.class);
        l.add(IRestApiService.class);
        // l.add(MatchActionModule.class);
        // l.add(MatchActionComponent.class);
        l.add(MatchActionFloodlightService.class);

        return l;

    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        datagridService = context.getServiceImpl(IDatagridService.class);
        registryService = context.getServiceImpl(IControllerRegistryService.class);
        arpHandler = new ArpHandler(context, this);
        icmpHandler = new IcmpHandler(context, this);
        ipHandler = new GenericIpHandler(context, this);
        arpEntries = new ArrayList<ArpEntry>();
        topologyService = context.getServiceImpl(ITopologyService.class);
        threadPool = context.getServiceImpl(IThreadPoolService.class);
        mutableTopology = topologyService.getTopology();
        ipPacketQueue = new ConcurrentLinkedQueue<IPv4>();
        graphs = new HashMap<Switch, ECMPShortestPathGraph>();
        linksDown = new HashMap<String, LinkData>();
        linksToAdd = new HashMap<String, LinkData>();
        topologyEventQueue = new ConcurrentLinkedQueue<TopologyEvents>();
        packetService = context.getServiceImpl(IPacketService.class);
        matchActionService = context.getServiceImpl(MatchActionFloodlightService.class);
        restApi = context.getServiceImpl(IRestApiService.class);
        policyTable = new HashMap<String, SegmentRoutingPolicy>();
        tunnelTable = new HashMap<String, SegmentRoutingTunnel>();
        adjacencySidTable = new HashMap<Integer,HashMap<Integer, List<Integer>>>();
        adjcencyGroupIdTable = new HashMap<String, HashMap<Integer, Integer>>();
        switchDpidListWithMastership = new ArrayList<String>();
        policyEventHandler = new PolicyEventHandler(this);
        tunnelEventHandler = new TunnelEventHandler(this);

        packetService.registerPacketListener(this);
        topologyService.addListener(this, false);

    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        maIdGenerator = matchActionService.getMatchActionIdGenerator();
        maoIdGenerator = matchActionService.getMatchActionOperationsIdGenerator();

        ScheduledExecutorService ses = threadPool.getScheduledExecutor();
        restApi.addRestletRoutable(new SegmentRoutingWebRoutable());

        policyEventChannel = datagridService.addListener(SR_POLICY_CHANNEL_NAME,
                policyEventHandler,
                String.class,
                PolicyNotification.class);

        tunnelEventChannel = datagridService.addListener(SR_TUNNEL_CHANNEL_NAME,
                tunnelEventHandler,
                String.class,
                TunnelNotification.class);

        discoveryTask = new SingletonTask(ses, new Runnable() {
            @Override
            public void run() {
                handleTopologyChangeEvents();
            }
        });

        linkAddTask = new SingletonTask(ses, new Runnable() {
            @Override
            public void run() {
                delayedAddLink();
            }
        });

        testTask = new SingletonTask(ses, new Runnable() {
            @Override
            public void run() {
                runTest();
            }
        });

        testMode = POLICY_ADD1;
        //testTask.reschedule(20, TimeUnit.SECONDS);
    }

    @Override
    public void receive(Switch sw, Port inPort, Ethernet payload) {
        if (payload.getEtherType() == Ethernet.TYPE_ARP)
            arpHandler.processPacketIn(sw, inPort, payload);
        if (payload.getEtherType() == Ethernet.TYPE_IPV4) {
            addPacketToPacketBuffer((IPv4) payload.getPayload());
            if (((IPv4) payload.getPayload()).getProtocol() == IPv4.PROTOCOL_ICMP)
                icmpHandler.processPacketIn(sw, inPort, payload);
            else
                ipHandler.processPacketIn(sw, inPort, payload);
        }
        else {
            log.debug("{}", payload.toString());
        }
    }

    private class PolicyEventHandler implements
    IEventChannelListener<String, PolicyNotification> {

        SegmentRoutingManager srManager;

        public PolicyEventHandler(SegmentRoutingManager srm) {
            this.srManager = srm;
        }

        @Override
        public void entryAdded(PolicyNotification policyNotication) {
            log.debug("Policy entry {} was added", policyNotication);
            if (PolicyType.valueOf(policyNotication.getPolicyType()) ==
                    PolicyType.TUNNEL_FLOW) {
                SegmentRoutingPolicyTunnel srPolicy =
                        new SegmentRoutingPolicyTunnel(srManager, policyNotication);
                policyTable.put(srPolicy.getPolicyId(), srPolicy);
            }
        }

        @Override
        public void entryRemoved(PolicyNotification policyNotication) {
            log.debug("Policy entry {} was removed", policyNotication);
            policyTable.remove(policyNotication.getPolicyId());
        }

       @Override
        public void entryUpdated(PolicyNotification value) {
           log.debug("Policy entry {} was updatged", value);

        }

    }

    private class TunnelEventHandler implements
    IEventChannelListener<String, TunnelNotification> {
        SegmentRoutingManager srManager;

        public TunnelEventHandler(SegmentRoutingManager srm) {
            super();
            this.srManager = srm;
        }

        @Override
        public void entryAdded(TunnelNotification tunnelNotification) {
            SegmentRoutingTunnel srTunnel =
                    new SegmentRoutingTunnel(srManager, tunnelNotification);
            if (srTunnel.checkAndCreateTunnel()) {
                TunnelNotification tunnelNotificationBack =
                        new TunnelNotification(srTunnel);
                tunnelEventChannel.updateEntry(srTunnel.getTunnelId(),
                        tunnelNotificationBack);
            }
            tunnelTable.put(srTunnel.getTunnelId(), srTunnel);
        }

        @Override
        public void entryRemoved(TunnelNotification tunnelNotification) {
            if (tunnelTable.containsKey(tunnelNotification.getTunnelId())) {
                srManager.removeTunnel(tunnelNotification.getTunnelId());
            }
        }

        @Override
        public void entryUpdated(TunnelNotification tunnelNotification) {
            SegmentRoutingTunnel srTunnel =
                    new SegmentRoutingTunnel(srManager, tunnelNotification);
            tunnelTable.put(srTunnel.getTunnelId(), srTunnel);
        }

    }


    // ************************************
    // Topology event handlers
    // ************************************

    /**
     * Topology events that have been generated.
     *
     * @param topologyEvents the generated Topology Events
     * @see TopologyEvents
     */
    public void topologyEvents(TopologyEvents topologyEvents)
    {

        if (!topologyEvents.getAddedMastershipDataEntries().isEmpty()) {
            processMastershipAdded(topologyEvents.getAddedMastershipDataEntries());
        }

        if (operationMode == 0) {
            discoveryTask.reschedule(20, TimeUnit.SECONDS);
            operationMode = 1; // Wait until all switches are up ..
        }
        else if (operationMode == 1) { // waiting for all switches to be up
            // Do nothing
        }
        else if (operationMode == 2) { // all switches are up and we need to
            // handle events quickly.
            topologyEventQueue.add(topologyEvents);
            discoveryTask.reschedule(100, TimeUnit.MILLISECONDS);
        }

        // discoveryTask.reschedule(100, TimeUnit.MILLISECONDS);
        // log.debug("A task is scheduled to handle events {}",
        // topologyEvents);

    }

    private void processMastershipAdded(
            Collection<MastershipData> mastershipAdded) {
        for (MastershipData mastershipData : mastershipAdded) {
            String dpid = mastershipData.getDpid().toString();
            if (!switchDpidListWithMastership.contains(dpid))
                switchDpidListWithMastership.add(dpid);
        }
    }

    /**
     * Process the multiple topology events with some delay (100MS at most for now)
     *
     */
    private void handleTopologyChangeEvents() {
        operationMode = 2;  // all switches are up now..

        if (topologyEventQueue.isEmpty()) {
            populateEcmpRoutingRules(false);
            return;
        }

        numOfEventProcess ++;

        Collection<LinkData> linkEntriesAddedAll = new ArrayList<LinkData>();
        Collection<PortData> portEntriesAddedAll = new ArrayList<PortData>();
        Collection<PortData> portEntriesRemovedAll = new ArrayList<PortData>();
        Collection<LinkData> linkEntriesRemovedAll = new ArrayList<LinkData>();
        Collection<SwitchData> switchAddedAll = new ArrayList<SwitchData>();
        Collection<SwitchData> switchRemovedAll = new ArrayList<SwitchData>();
        Collection<MastershipData> mastershipRemovedAll = new ArrayList<MastershipData>();

        while (!topologyEventQueue.isEmpty()) {
            // We should handle the events in the order of when they happen
            // TODO: We need to simulate the final results of multiple events
            // and shoot only the final state.
            // Ex: link s1-s2 down, link s1-s2 up --> Do nothing
            // Ex: ink s1-s2 up, s1-p1,p2 down --> link s1-s2 down

            TopologyEvents topologyEvents = topologyEventQueue.poll();

            Collection<LinkData> linkEntriesAdded = topologyEvents.getAddedLinkDataEntries();
            Collection<PortData> portEntriesAdded = topologyEvents.getAddedPortDataEntries();
            Collection<PortData> portEntriesRemoved = topologyEvents.getRemovedPortDataEntries();
            Collection<LinkData> linkEntriesRemoved = topologyEvents.getRemovedLinkDataEntries();
            Collection<SwitchData> switchAdded = topologyEvents.getAddedSwitchDataEntries();
            Collection<SwitchData> switchRemoved = topologyEvents.getRemovedSwitchDataEntries();
            Collection<MastershipData> mastershipRemoved = topologyEvents.getRemovedMastershipDataEntries();

            linkEntriesAddedAll.addAll(linkEntriesAdded);
            portEntriesAddedAll.addAll(portEntriesAdded);
            portEntriesRemovedAll.addAll(portEntriesRemoved);
            linkEntriesRemovedAll.addAll(linkEntriesRemoved);
            switchAddedAll.addAll(switchAdded);
            switchRemovedAll.addAll(switchRemoved);
            mastershipRemovedAll.addAll(mastershipRemoved);
            numOfEvents++;

            if (!portEntriesRemoved.isEmpty()) {
                log.debug("handleTopologyChangeEvents: portEntriesRemoved are {}",portEntriesRemoved);
                processPortRemoval(portEntriesRemoved);
            }

            if (!linkEntriesRemoved.isEmpty()) {
                log.debug("handleTopologyChangeEvents: linkEntriesRemoved are {}",linkEntriesRemoved);
                processLinkRemoval(linkEntriesRemoved);
            }

            if (!switchRemoved.isEmpty()) {
                log.debug("handleTopologyChangeEvents: switchRemoved are {}",switchRemoved);
                processSwitchRemoved(switchRemoved);
            }

            if (!mastershipRemoved.isEmpty()) {
                log.debug("Mastership is removed. Check if ports are down also.");
            }

            if (!linkEntriesAdded.isEmpty()) {
                log.debug("handleTopologyChangeEvents: linkEntriesAdded are {}",linkEntriesAdded);
                processLinkAdd(linkEntriesAdded, false);
            }

            if (!portEntriesAdded.isEmpty()) {
                log.debug("handleTopologyChangeEvents: portEntriesAdded are {}",portEntriesAdded);
                processPortAdd(portEntriesAdded);
            }

            if (!switchAdded.isEmpty()) {
                log.debug("handleTopologyChangeEvents: switchAdded are {}",switchAdded);
                processSwitchAdd(switchAdded);
            }

        }

        // TODO: 100ms is enough to check both mastership removed events
        // and the port removed events? What if the PORT_STATUS packets comes late?
        if (!mastershipRemovedAll.isEmpty()) {
            if (portEntriesRemovedAll.isEmpty()) {
                log.debug("Just mastership is removed. Do not do anthing.");
            }
            else {
                HashMap<String, MastershipData> mastershipToRemove =
                        new HashMap<String, MastershipData>();
                for (MastershipData ms: mastershipRemovedAll) {
                    for (PortData port: portEntriesRemovedAll) {
                        // TODO: check ALL ports of the switch are dead ..
                        mastershipToRemove.put(ms.getDpid().toString(), ms);
                        log.debug("Swtich {} is really down.", ms.getDpid());
                        break;
                    }
                }
                processMastershipRemoved(mastershipToRemove.values());
            }
        }

        log.debug("num events {}, num of process {}, "
                + "num of Population {}", numOfEvents, numOfEventProcess,
                numOfPopulation);
    }

    /**
     * Process the SwitchAdded events from topologyMananger.
     * It does nothing. When a switch is added, then link will be added too.
     * LinkAdded event will handle process all re-computation.
     *
     * @param switchAdded
     */
    private void processSwitchAdd(Collection<SwitchData> switchAdded) {

    }

    /**
     * Remove all ports connected to the switch removed
     *
     * @param mastershipRemoved master switch info removed
     */
    private void processMastershipRemoved(Collection<MastershipData>
        mastershipRemoved) {
        for (MastershipData mastership: mastershipRemoved) {
            Switch sw = mutableTopology.getSwitch(mastership.getDpid());
            for (Link link: sw.getOutgoingLinks()) {
                Port dstPort = link.getDstPort();
                IOF13Switch dstSw = (IOF13Switch) floodlightProvider.getMasterSwitch(
                        dstPort.getDpid().value());
                if (dstSw != null) {
                    dstSw.removePortFromGroups(dstPort.getNumber());
                    log.debug("MasterSwitch {} is gone: remove port {}",
                            sw.getDpid(), dstPort);
                }
            }
            // Flush the groupId table for adjacencySid;
            log.debug("Flush the AdjacencyGroupId table for sw {}", sw);
            adjcencyGroupIdTable.remove(sw.getDpid().toString());
        }

        linksToAdd.clear();
        linksDown.clear();
    }

    /**
     * Remove all ports connected to the switch removed
     *
     * @param switchRemoved Switch removed
     */
    private void processSwitchRemoved(Collection<SwitchData> switchRemoved) {
        log.debug("SwitchRemoved event occurred !!!");
    }

    /**
     * Report ports added to driver
     *
     * @param portEntries
     */
    private void processPortAdd(Collection<PortData> portEntries) {
        // TODO: do we need to add ports with delay?
        for (PortData port : portEntries) {
            Dpid dpid = port.getDpid();

            IOF13Switch sw = (IOF13Switch) floodlightProvider.getMasterSwitch(
                    port.getDpid().value());
            if (sw != null) {
                sw.addPortToGroups(port.getPortNumber());
                //log.debug("Add port {} to switch {}", port, dpid);
            }
        }
    }

    /**
     * Reports ports of new links to driver and recalculate ECMP SPG
     * If the link to add was removed before, then we just schedule the add link
     * event and do not recompute the path now.
     *
     * @param linkEntries
     */
    private void processLinkAdd(Collection<LinkData> linkEntries, boolean delayed) {

        for (LinkData link : linkEntries) {

            SwitchPort srcPort = link.getSrc();
            SwitchPort dstPort = link.getDst();

            String key = srcPort.getDpid().toString() +
                    dstPort.getDpid().toString();
            if (!delayed) {
                if (linksDown.containsKey(key)) {
                    linksToAdd.put(key, link);
                    linksDown.remove(key);
                    linkAddTask.reschedule(DELAY_TO_ADD_LINK, TimeUnit.SECONDS);
                    log.debug("Add link {} with 5 sec delay", link);
                    // TODO: What if we have multiple events of add link:
                    // one is new link add, the other one is link up for
                    // broken link? ECMPSPG function cannot deal with it for now
                    return;
                }
            }
            else {
                if (linksDown.containsKey(key)) {
                    linksToAdd.remove(key);
                    log.debug("Do not add the link {}: it is down again!", link);
                    return;
                }
            }

            IOF13Switch srcSw = (IOF13Switch) floodlightProvider.getMasterSwitch(
                    srcPort.getDpid().value());
            IOF13Switch dstSw = (IOF13Switch) floodlightProvider.getMasterSwitch(
                    dstPort.getDpid().value());

            if ((srcSw == null) || (dstSw == null))
                continue;

            srcSw.addPortToGroups(srcPort.getPortNumber());
            dstSw.addPortToGroups(dstPort.getPortNumber());

            //log.debug("Add a link port {} to switch {} to add link {}", srcPort, srcSw,
            //        link);
            //log.debug("Add a link port {} to switch {} to add link {}", dstPort, dstSw,
            //        link);

        }
        populateEcmpRoutingRules(false);
    }

    /**
     * Check if all links are gone b/w the two switches. If all links are gone,
     * then we need to recalculate the path. Otherwise, just report link failure
     * to the driver. IF the switches do not support ECMP in transit routers and
     * the link removed is between transit routers, then just recompute the path
     * regardless of ECMP.
     *
     * @param linkEntries
     */
    private void processLinkRemoval(Collection<LinkData> linkEntries) {
        for (LinkData link : linkEntries) {
            SwitchPort srcPort = link.getSrc();
            SwitchPort dstPort = link.getDst();

            IOF13Switch srcSw = (IOF13Switch) floodlightProvider.getMasterSwitch(
                    srcPort.getDpid().value());
            IOF13Switch dstSw = (IOF13Switch) floodlightProvider.getMasterSwitch(
                    dstPort.getDpid().value());
            if ((srcSw == null) || (dstSw == null))
                /* If this link is not between two switches, ignore it */
                continue;

            srcSw.removePortFromGroups(srcPort.getPortNumber());
            dstSw.removePortFromGroups(dstPort.getPortNumber());
            log.debug("Remove port {} from switch {}", srcPort, srcSw);
            log.debug("Remove port {} from switch {}", dstPort, dstSw);

            if ((srcSw instanceof OFSwitchImplDellOSR &&
                    dstSw instanceof OFSwitchImplDellOSR) &&
               isTransitRouter(mutableTopology.getSwitch(srcPort.getDpid())) &&
               isTransitRouter(mutableTopology.getSwitch(dstPort.getDpid()))) {
                populateEcmpRoutingRules(false);
            }
            else {
                Switch srcSwitch = mutableTopology.getSwitch(srcPort.getDpid());
                if (srcSwitch.getLinkToNeighbor(dstPort.getDpid()) == null) {
                    log.debug("All links are gone b/w {} and {}", srcPort.getDpid(),
                            dstPort.getDpid());
                    log.debug("All paths will recomputed regardless of the rest "
                            + "of the event");
                    populateEcmpRoutingRules(false);
                }
            }

            String key = link.getSrc().getDpid().toString()+
                    link.getDst().getDpid().toString();
            if (!linksDown.containsKey(key)) {
                linksDown.put(key, link);
            }
        }

    }

    /**
     * report ports removed to the driver immediately
     *
     * @param portEntries
     */
    private void processPortRemoval(Collection<PortData> portEntries) {
        for (PortData port : portEntries) {
            Dpid dpid = port.getDpid();

            IOF13Switch sw = (IOF13Switch) floodlightProvider.getMasterSwitch(
                    port.getDpid().value());
            if (sw != null) {
                sw.removePortFromGroups(port.getPortNumber());
                log.debug("Remove port {} from switch {}", port, dpid);
            }
        }
    }

    /**
     * Add the link immediately
     * The function is scheduled when link add event happens and called
     * DELAY_TO_ADD_LINK seconds after the event to avoid link flip-flop.
     */
    private void delayedAddLink() {

        processLinkAdd(linksToAdd.values(), true);

    }


    // ************************************
    // ECMP shorted path routing functions
    // ************************************

    /**
     * Populate routing rules walking through the ECMP shortest paths
     *
     * @param modified if true, it "modifies" the rules
     */
    private void populateEcmpRoutingRules(boolean modified) {
        graphs.clear();
        Iterable<Switch> switches = mutableTopology.getSwitches();
        for (Switch sw : switches) {
            if (switchDpidListWithMastership.contains(sw.getDpid().toString())) {
                ECMPShortestPathGraph ecmpSPG = new ECMPShortestPathGraph(sw);
                graphs.put(sw, ecmpSPG);
                // log.debug("ECMPShortestPathGraph is computed for switch {}",
                // HexString.toHexString(sw.getDpid().value()));
                populateEcmpRoutingRulesForPath(sw, ecmpSPG, modified);

                // Set adjacency routing rule for all switches
                try {
                    populateAdjacencyncyRule(sw);
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        numOfPopulation++;
    }

    /**
     * populate the MPLS rules to handle Adjacency IDs
     *
     * @param sw  Switch
     * @throws JSONException
     */
    private void populateAdjacencyncyRule(Switch sw) throws JSONException {
        String adjInfo = sw.getStringAttribute("adjacencySids");
        String nodeSidStr = sw.getStringAttribute("nodeSid");
        String srcMac = sw.getStringAttribute("routerMac");
        String autoAdjInfo = sw.getStringAttribute("autogenAdjSids");

        if (autoAdjInfo == null || srcMac == null || nodeSidStr == null)
            return;

        // parse adjacency Id
        HashMap<Integer, List<Integer>> adjacencyInfo = null;
        if (adjInfo != null) {
            adjacencyInfo = parseAdjacencySidInfo(adjInfo);
        }
        // parse auto generated adjacency Id
        adjacencyInfo.putAll(parseAdjacencySidInfo(autoAdjInfo));

        adjacencySidTable.put(Integer.parseInt(nodeSidStr), adjacencyInfo);

        for (Integer adjId: adjacencyInfo.keySet()) {
            List<Integer> ports = adjacencyInfo.get(adjId);
            if (ports.size() == 1) {
                setAdjacencyRuleOfOutput(sw, adjId, srcMac, ports.get(0));
            }
            else {
                setAdjacencyRuleOfGroup(sw, adjId, ports);
            }
        }
    }

    /**
     * Set Adjacency Rule to MPLS table for adjacency Ids attached to multiple
     * ports
     *
     * @param sw Switch
     * @param adjId Adjacency ID
     * @param ports List of ports assigned to the Adjacency ID
     */
    private void setAdjacencyRuleOfGroup(Switch sw, Integer adjId,
            List<Integer> ports) {

        IOF13Switch sw13 = (IOF13Switch) floodlightProvider.getMasterSwitch(
                sw.getDpid().value());

        Integer groupId = -1;
        if (sw13 != null) {
            List<PortNumber> portList = new ArrayList<PortNumber>();
            int key = 0;
            for (Integer port: ports) {
                PortNumber portNumber = PortNumber.uint32(port);
                portList.add(portNumber);
                key += portNumber.hashCode();
            }
            key += sw.getDpid().hashCode();

            HashMap<Integer, Integer> adjGroupIdMap =
                    adjcencyGroupIdTable.get(sw.getDpid().toString());
            if (adjGroupIdMap != null) {
                groupId = adjGroupIdMap.get(key);
                if (groupId == null) {
                    List<Integer> groupIdList = sw13.createGroup(
                            new ArrayList<Integer>(), portList);
                    if (groupIdList == null || groupIdList.isEmpty()) {
                        log.debug("Failed to create a group at driver for "
                                + "adj ID {}", adjId);
                        return;
                    }
                    else {
                        groupId = groupIdList.get(0);
                        adjGroupIdMap.put(key, groupId);
                    }
                }
            }
            else {
                List<Integer> groupIdList = sw13.createGroup(
                        new ArrayList<Integer>(), portList);
                if (groupIdList == null || groupIdList.isEmpty()) {
                    log.debug("Failed to create a group at driver for adj ID {}",
                            adjId);
                    return;
                }
                else {
                    groupId = groupIdList.get(0);
                    adjGroupIdMap = new HashMap<Integer, Integer>();
                    adjGroupIdMap.put(key, groupId);
                    adjcencyGroupIdTable.put(sw.getDpid().toString(),
                            adjGroupIdMap);
                }
            }
        }

        pushAdjRule(sw, adjId, null, null, groupId, true);
        pushAdjRule(sw, adjId, null, null, groupId, false);
    }

    /**
     * Set Adjacency Rule to MPLS table for adjacency Ids attached to single port
     *
     * @param sw Switch
     * @param adjId Adjacency ID
     * @param ports List of ports assigned to the Adjacency ID
     */
    private void setAdjacencyRuleOfOutput(Switch sw, Integer adjId, String srcMac, Integer portNo) {

        Dpid dstDpid = null;
        for (Link link: sw.getOutgoingLinks()) {
            if (link.getSrcPort().getPortNumber().value() == portNo) {
                dstDpid = link.getDstPort().getDpid();
                break;
            }
        }
        if (dstDpid == null) {
            //log.debug("Cannot find the destination switch for the adjacency ID {}", adjId);
            return;
        }
        Switch dstSw = mutableTopology.getSwitch(dstDpid);
        String dstMac = null;
        if (dstSw == null) {
            log.debug("Cannot find SW {}", dstDpid.toString());
            return;
        }
        else {
            dstMac = dstSw.getStringAttribute("routerMac");
        }

        pushAdjRule(sw, adjId, srcMac, dstMac, portNo, true); // BoS = 1
        pushAdjRule(sw, adjId, srcMac, dstMac, portNo, false); // BoS = 0

    }

    public void executeMatchActionOpEntry(MatchActionOperationEntry maEntry) {
        MatchActionOperations mao = new MatchActionOperations(maoIdGenerator.getNewId());
        mao.addOperation(maEntry);
        matchActionService.executeOperations(mao);
    }

    /**
     * Push the MPLS rule for Adjacency ID
     *
     * @param sw  Switch to push the rule
     * @param id  Adjacency ID
     * @param srcMac  source MAC address
     * @param dstMac  destination MAC address
     * @param portNo  port number assigned to the ID
     * @param bos  BoS option
     */
    private void pushAdjRule(Switch sw, int id, String srcMac, String dstMac,
            int num, boolean bos) {

        MplsMatch mplsMatch = new MplsMatch(id, bos);
        List<Action> actions = new ArrayList<Action>();

        CopyTtlInAction copyTtlInAction = new CopyTtlInAction();
        actions.add(copyTtlInAction);
        if (bos) {
            PopMplsAction popAction = new PopMplsAction(EthType.IPv4);
            DecNwTtlAction decNwTtlAction = new DecNwTtlAction(1);
            actions.add(popAction);
            actions.add(decNwTtlAction);
        }
        else {
            PopMplsAction popAction = new PopMplsAction(EthType.MPLS_UNICAST);
            DecMplsTtlAction decMplsTtlAction = new DecMplsTtlAction(1);
            actions.add(popAction);
            actions.add(decMplsTtlAction);
        }

        // Output action
        if (srcMac != null && dstMac != null) {
            SetDAAction setDstAction = new SetDAAction(MacAddress.of(dstMac));
            SetSAAction setSrcAction = new SetSAAction(MacAddress.of(srcMac));
            OutputAction outportAction = new OutputAction(PortNumber.uint32(num));

            actions.add(setDstAction);
            actions.add(setSrcAction);
            actions.add(outportAction);
        }
        // Group Action
        else {
            GroupAction groupAction = new GroupAction();
            groupAction.setGroupId(num);
            actions.add(groupAction);
        }

        MatchAction matchAction = new MatchAction(maIdGenerator.getNewId(),
                new SwitchPort(sw.getDpid().value(), (short) 0), mplsMatch, actions);
        Operator operator = Operator.ADD;
        MatchActionOperationEntry maEntry =
                new MatchActionOperationEntry(operator, matchAction);
        executeMatchActionOpEntry(maEntry);

        /* IOF13Switch sw13 = (IOF13Switch) floodlightProvider.getMasterSwitch(
                 sw.getDpid().value());

         if (sw13 != null) {
             try {
                 //printMatchActionOperationEntry(sw, maEntry);
                 sw13.pushFlow(maEntry);
             } catch (IOException e) {
                 e.printStackTrace();
             }
         }*/
    }

    /**
     * populate routing rules to forward packets from the switch given to
     * all other switches.
     *
     * @param sw source switch
     * @param ecmpSPG shortest path from the the source switch to all others
     * @param modified modification flag
     */
    private void populateEcmpRoutingRulesForPath(Switch sw,
            ECMPShortestPathGraph ecmpSPG, boolean modified) {

        HashMap<Integer, HashMap<Switch, ArrayList<ArrayList<Dpid>>>> switchVia =
                ecmpSPG.getAllLearnedSwitchesAndVia();
        List<OFBarrierReplyFuture> replies = new ArrayList<OFBarrierReplyFuture>();
        for (Integer itrIdx : switchVia.keySet()) {
            //log.debug("ECMPShortestPathGraph:Switches learned in "
            //        + "Iteration{} from switch {}:",
            //        itrIdx,
            //        HexString.toHexString(sw.getDpid().value()));
            HashMap<Switch, ArrayList<ArrayList<Dpid>>> swViaMap =
                    switchVia.get(itrIdx);
            for (Switch targetSw : swViaMap.keySet()) {
                //log.debug("ECMPShortestPathGraph:****switch {} via:",
                //        HexString.toHexString(targetSw.getDpid().value()));
                String destSw = sw.getDpid().toString();
                List<String> fwdToSw = new ArrayList<String>();

                for (ArrayList<Dpid> via : swViaMap.get(targetSw)) {
                    //log.debug("ECMPShortestPathGraph:******{}) {}", ++i, via);
                    if (via.isEmpty()) {
                        fwdToSw.add(destSw);
                    }
                    else {
                        fwdToSw.add(via.get(0).toString());
                    }
                }
                setRoutingRule(targetSw, destSw, fwdToSw, modified);
                // Send Barrier Message and make sure all rules are set
                // before we set the rules to next routers
                OFBarrierReplyFuture replyFuture = sendBarrier(targetSw);
                if (replyFuture != null)
                    replies.add(replyFuture);
                else {
                    log.warn("Failed to send a Barrier message to sw {}",
                            targetSw);
                }
            }
            if (!checkBarrierReplies(replies)) {
                log.warn("Failed to get Barrier replis");
                return;
            }
        }
    }

    /**
     *
     * Set routing rules in targetSw {forward packets to fwdToSw switches in
     * order to send packets to destSw} - If the target switch is an edge router
     * and final destnation switch is also an edge router, then set IP
     * forwarding rules to subnets - If only the target switch is an edge
     * router, then set IP forwarding rule to the transit router loopback IP
     * address - If the target is a transit router, then just set the MPLS
     * forwarding rule
     *
     * @param targetSw Switch to set the rules
     * @param destSw Final destination switches
     * @param fwdToSw next hop switches
     */
    private void setRoutingRule(Switch targetSw, String destSw,
            List<String> fwdToSw, boolean modified) {

        if (fwdToSw.isEmpty()) {
            fwdToSw.add(destSw);
        }

        // if both target SW and dest SW are an edge router, then set IP table
        if (IsEdgeRouter(targetSw.getDpid().toString()) &&
                IsEdgeRouter(destSw)) {
            // We assume that there is at least one transit router b/w edge
            // routers
            Switch destSwitch = mutableTopology.getSwitch(new Dpid(destSw));
            String subnets = destSwitch.getStringAttribute("subnets");
            setIpTableRouterSubnet(targetSw, subnets, getMplsLabel(destSw)
                    , fwdToSw, modified);

            String routerIp = destSwitch.getStringAttribute("routerIp");
            setIpTableRouter(targetSw, routerIp, getMplsLabel(destSw), fwdToSw,
                    null, modified);
            // Edge router can be a transit router
            setMplsTable(targetSw, getMplsLabel(destSw), fwdToSw, modified);
        }
        // Only if the target switch is the edge router, then set the IP rules
        else if (IsEdgeRouter(targetSw.getDpid().toString())) {
            // We assume that there is at least one transit router b/w edge
            // routers
            Switch destSwitch = mutableTopology.getSwitch(new Dpid(destSw));
            String routerIp = destSwitch.getStringAttribute("routerIp");
            setIpTableRouter(targetSw, routerIp, getMplsLabel(destSw), fwdToSw,
                    null, modified);
            // Edge router can be a transit router
            setMplsTable(targetSw, getMplsLabel(destSw), fwdToSw, modified);
        }
        // if it is a transit router, then set rules in the MPLS table
        else {
            setMplsTable(targetSw, getMplsLabel(destSw), fwdToSw, modified);
        }

    }

    /**
     * Set IP forwarding rule to the gateway of each subnet of switches
     *
     * @param targetSw Switch to set rules
     * @param subnets  subnet information
     * @param mplsLabel destination MPLS label
     * @param fwdToSw  router to forward packets to
     */
    private void setIpTableRouterSubnet(Switch targetSw, String subnets,
            String mplsLabel, List<String> fwdToSw, boolean modified) {

        Collection<MatchActionOperationEntry> entries =
                new ArrayList<MatchActionOperationEntry>();

        try {
            JSONArray arry = new JSONArray(subnets);
            for (int i = 0; i < arry.length(); i++) {
                String subnetIp = (String) arry.getJSONObject(i).get("subnetIp");
                setIpTableRouter(targetSw, subnetIp, mplsLabel, fwdToSw, entries,
                        modified);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (!entries.isEmpty()) {
            MatchActionOperations mao = new MatchActionOperations(
                    maoIdGenerator.getNewId());
            for (MatchActionOperationEntry maEntry : entries)
                mao.addOperation(maEntry);
            matchActionService.executeOperations(mao);
        }

        /*if (!entries.isEmpty()) {
            IOF13Switch sw13 = (IOF13Switch) floodlightProvider.getMasterSwitch(
                    targetSw.getDpid().value());

            if (sw13 != null) {
                try {
                    sw13.pushFlows(entries);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }*/

    }

    /**
     * Set IP forwarding rule - If the destination is the next hop, then do not
     * push MPLS, just decrease the NW TTL - Otherwise, push MPLS label and set
     * the MPLS ID
     *
     * @param sw target switch to set rules
     * @param subnetIp Match IP address
     * @param mplsLabel MPLS label of final destination router
     * @param fwdToSws next hop routers
     * @param entries
     */
    private void setIpTableRouter(Switch sw, String subnetIp, String mplsLabel,
            List<String> fwdToSws, Collection<MatchActionOperationEntry> entries,
            boolean modified) {

        Ipv4Match ipMatch = new Ipv4Match(subnetIp);
        List<Action> actions = new ArrayList<>();
        GroupAction groupAction = new GroupAction();

        // If destination SW is the same as the fwd SW, then do not push MPLS
        // label
        if (fwdToSws.size() > 1) {
            PushMplsAction pushMplsAction = new PushMplsAction();
            SetMplsIdAction setIdAction = new SetMplsIdAction(Integer.parseInt(mplsLabel));
            CopyTtlOutAction copyTtlOutAction = new CopyTtlOutAction();
            DecMplsTtlAction decMplsTtlAction = new DecMplsTtlAction(1);

            //actions.add(pushMplsAction);
            //actions.add(copyTtlOutAction);
            //actions.add(decMplsTtlAction);
            // actions.add(setIdAction);
            groupAction.setEdgeLabel(Integer.parseInt(mplsLabel));
        }
        else {
            String fwdToSw = fwdToSws.get(0);
            if (getMplsLabel(fwdToSw).equals(mplsLabel)) {
                DecNwTtlAction decTtlAction = new DecNwTtlAction(1);
                actions.add(decTtlAction);
            }
            else {
                SetMplsIdAction setIdAction = new SetMplsIdAction(
                        Integer.parseInt(mplsLabel));
                CopyTtlOutAction copyTtlOutAction = new CopyTtlOutAction();
                DecMplsTtlAction decMplsTtlAction = new DecMplsTtlAction(1);

                //actions.add(pushMplsAction);
                //actions.add(copyTtlOutAction);
                //actions.add(decMplsTtlAction);
                // actions.add(setIdAction);
                groupAction.setEdgeLabel(Integer.parseInt(mplsLabel));
            }
        }

        for (String fwdSw : fwdToSws) {
            groupAction.addSwitch(new Dpid(fwdSw));
        }
        actions.add(groupAction);

        MatchAction matchAction = new MatchAction(maIdGenerator.getNewId(),
                new SwitchPort(sw.getDpid().value(), (short) 0), ipMatch, actions);

        Operator operator = null;
        if (modified)
            operator =  Operator.MODIFY;
        else
            operator = Operator.ADD;

        MatchActionOperationEntry maEntry =
                new MatchActionOperationEntry(operator, matchAction);

        //        getSwId(sw.getDpid().toString()));

        if (entries != null)
            entries.add(maEntry);
        else {
            executeMatchActionOpEntry(maEntry);
            /*IOF13Switch sw13 = (IOF13Switch)
                    floodlightProvider.getMasterSwitch(sw.getDpid().value());
            if (sw13 != null) {
                try {
                    sw13.pushFlow(maEntry);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }*/
        }
    }

    /**
     * Set MPLS forwarding rules to MPLS table
     * </p>
     * If the destination is the same as the next hop to forward packets then,
     * pop the MPLS label according to PHP rule. Here, if BoS is set, then
     * copy TTL In and decrement NW TTL. Otherwise, it just decrement the MPLS
     * TTL of the another MPLS header.
     * If the next hop is not the destination, just forward packets to next
     * hops using Group action.
     *
     * TODO: refactoring required
     *
     * @param sw Switch to set the rules
     * @param mplsLabel destination MPLS label
     * @param fwdSws next hop switches
     * */
    private void setMplsTable(Switch sw, String mplsLabel, List<String> fwdSws,
            boolean modified) {

        if (fwdSws.isEmpty())
            return;

        Collection<MatchActionOperationEntry> maEntries =
                new ArrayList<MatchActionOperationEntry>();
        String fwdSw1 = fwdSws.get(0);

        //If the next hop is the destination router, do PHP
        if (fwdSws.size() == 1 && mplsLabel.equals(getMplsLabel(fwdSw1))) {
            maEntries.add(buildMAEntry(sw, mplsLabel, fwdSws, true, true));
            maEntries.add(buildMAEntry(sw, mplsLabel, fwdSws, true, false));
        }
        else {
            maEntries.add(buildMAEntry(sw, mplsLabel, fwdSws, false, true));
            maEntries.add(buildMAEntry(sw, mplsLabel, fwdSws, false, false));
        }
        MatchActionOperations mao = new MatchActionOperations(
                maoIdGenerator.getNewId());
        for (MatchActionOperationEntry maEntry : maEntries)
            mao.addOperation(maEntry);
        matchActionService.executeOperations(mao);
        /*IOF13Switch sw13 = (IOF13Switch) floodlightProvider.getMasterSwitch(
                sw.getDpid().value());

        if (sw13 != null) {
            try {
                //printMatchActionOperationEntry(sw, maEntry);
                sw13.pushFlows(maEntries);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
    }


    // ************************************
    // Policy routing classes and functions
    // ************************************

    /**
     * Return the Tunnel table
     *
     * @return collection of TunnelInfo
     */
    public Collection<SegmentRoutingTunnel> getTunnelTable() {
        return this.tunnelTable.values();
    }

    /**
     * Return the Policy Table
     */
    public Collection<SegmentRoutingPolicy> getPoclicyTable() {
        return this.policyTable.values();
    }

    /**
     * Return router DPIDs for the tunnel
     *
     * @param tid tunnel ID
     * @return List of DPID
     */
    public SegmentRoutingTunnel getTunnelInfo(String tid) {
        return tunnelTable.get(tid);
    }

    /**
     * Get the first group ID for the tunnel for specific source router
     * If Segment Stitching was required to create the tunnel, there are
     * multiple source routers.
     *
     * @param tunnelId ID for the tunnel
     * @param dpid source router DPID
     * @return the first group ID of the tunnel
     */
    public int getTunnelGroupId(String tunnelId, String dpid) {
       SegmentRoutingTunnel tunnel = tunnelTable.get(tunnelId);
       for (TunnelRouteInfo routeInfo: tunnel.getRoutes()) {
           String tunnelSrcDpid = routeInfo.getSrcSwDpid();
           if (tunnelSrcDpid.equals(dpid))
               return routeInfo.getGroupId();
        }

        return -1;
    }

    /**
     * Create a tunnel for policy routing
     * It delivers the node IDs of tunnels to driver.
     * Split the node IDs if number of IDs exceeds the limit for stitching.
     *
     * @param tunnelId  Node IDs for the tunnel
     * @param Ids tunnel ID
     */
    public boolean createTunnel(String tunnelId, List<Integer> labelIds) {

        SegmentRoutingTunnel srTunnel =
                new SegmentRoutingTunnel(this, tunnelId, labelIds);
        if (srTunnel.createTunnel()) {
            tunnelTable.put(tunnelId, srTunnel);
            TunnelNotification tunnelNotification =
                    new TunnelNotification(srTunnel);
            tunnelEventChannel.addEntry(tunnelId, tunnelNotification);
            return true;
        }
        else {
            log.warn("Failed to create a tunnel");
            return false;
        }
    }

    /**
     *  Create a policy
     *  TODO: To be removed
     */
    @Override
    public boolean createPolicy(String pid, MACAddress srcMac, MACAddress dstMac,
            Short etherType, IPv4Net srcIp, IPv4Net dstIp, Byte ipProto,
            Short srcPort, Short dstPort, int priority, String tid) {

        return createPolicy(pid, srcMac, dstMac, etherType, srcIp, dstIp, ipProto,
                srcPort, dstPort, priority, tid, PolicyType.TUNNEL_FLOW);
    }

    /**
     * Create a policy
     *
     */
    @Override
    public boolean createPolicy(String pid, MACAddress srcMac, MACAddress dstMac,
            Short etherType, IPv4Net srcIp, IPv4Net dstIp, Byte ipProto,
            Short srcTcpPort, Short dstTcpPort, int priority, String tid,
            PolicyType type) {

        // Sanity check
        SegmentRoutingTunnel tunnelInfo = tunnelTable.get(tid);
        if (tunnelInfo == null) {
            log.warn("Tunnel {} is not defined", tid);
            return false;
        }

        PacketMatchBuilder packetBuilder = new PacketMatchBuilder();

        if (srcMac != null)
            packetBuilder.setSrcMac(srcMac);
        if (dstMac != null)
            packetBuilder.setDstMac(dstMac);
        if (etherType == null) // Cqpd requires the type of IPV4
            packetBuilder.setEtherType(Ethernet.TYPE_IPV4);
        else
            packetBuilder.setEtherType(etherType);
        if (srcIp != null)
            packetBuilder.setSrcIp(srcIp.address(), srcIp.prefixLen());
        if (dstIp != null)
            packetBuilder.setDstIp(dstIp.address(), dstIp.prefixLen());
        if (ipProto != null)
            packetBuilder.setIpProto(ipProto);
        if (srcTcpPort > 0)
            packetBuilder.setSrcTcpPort(srcTcpPort);
        if (dstTcpPort > 0)
            packetBuilder.setDstTcpPort(dstTcpPort);
        PacketMatch policyMatch = packetBuilder.build();

        if (type == PolicyType.TUNNEL_FLOW) {
            SegmentRoutingPolicy srPolicy =
                    new SegmentRoutingPolicyTunnel(this,pid, type, policyMatch,
                           priority, tid);
            if (srPolicy.createPolicy()) {
                policyTable.put(pid, srPolicy);
                PolicyNotification policyNotification =
                        new PolicyNotification(srPolicy);
                policyEventChannel.addEntry(pid,
                        policyNotification);
                return true;
            }
            else {
                log.warn("Failed to create a policy");
                return false;
            }
        }
        else {
            log.warn("No other policy is supported yet.");
            return false;
        }
    }

    /**
     * Remove all policies applied to specific tunnel.
     *
     * @param srcMac
     * @param dstMac
     * @param etherType
     * @param srcIp
     * @param dstIp
     * @param ipProto
     * @param srcTcpPort
     * @param dstTcpPort
     * @param tid
     * @return
     */
    public boolean removePolicy(String pid) {
        //Sanity check
        SegmentRoutingPolicy policy =  policyTable.get(pid);
        if (policy == null) {
            log.warn("Cannot find the policy {}", pid);
            return false;
        }
        if (policy.removePolicy()) {
            policyTable.remove(pid);
            policyEventChannel.removeEntry(pid);
            log.debug("Policy {} is removed.", pid);

            return true;
        }
        else {
            log.warn("Faild to remove the policy {}", pid);
            return false;
        }

    }

    public enum removeTunnelMessages{
        SUCCESS(0, "Tunnel is removed successfully."),
        ERROR_REFERENCED(1, "Can't remove tunnel as its referenced by other policy(s)"),
        ERROR_SWITCH(2, "Switch not found in the tunnel route"),
        ERROR_DRIVER(3, "Can't remove tunnel at driver"),
        ERROR_TUNNEL(4, "Tunnel not found");

        private final int code;
        private final String description;

        private removeTunnelMessages(int code, String description) {
          this.code = code;
          this.description = description;
        }

        public String getDescription() {
           return this.description;
        }

        public int getCode() {
           return this.code;
        }

        @Override
        public String toString() {
          return "[" + this.code + ": " + this.description + "]";
        }

    }
    /**
     * Remove a tunnel
     * It removes all groups for the tunnel if the tunnel is not used for any
     * policy.
     *
     * @param tunnelId tunnel ID to remove
     */
    public removeTunnelMessages removeTunnel(String tunnelId) {

        // Check if the tunnel is used for any policy
        for (SegmentRoutingPolicy policy: policyTable.values()) {
            if (policy.getType() == PolicyType.TUNNEL_FLOW) {
                String tid = ((SegmentRoutingPolicyTunnel)policy).getTunnelId();
                if (tid.equals(tunnelId)) {
                    log.debug("Tunnel {} is still used for the policy {}.",
                    policy.getPolicyId(), tunnelId);
                    return removeTunnelMessages.ERROR_REFERENCED;
                }
            }
        }

        SegmentRoutingTunnel tunnel = tunnelTable.get(tunnelId);
        if (tunnel == null) {
            log.warn("Tunnul object does not exist {}", tunnelId);
            return removeTunnelMessages.ERROR_TUNNEL;
        }
        else {
            if (tunnel.removeTunnel()) {
                tunnelTable.remove(tunnelId);
                log.debug("Tunnel {} was removed successfully.", tunnelId);
                tunnelEventChannel.removeEntry(tunnelId);
                return removeTunnelMessages.SUCCESS;
            }
            else {
                log.warn("Faild in removing the tunnel {}", tunnelId);
                return removeTunnelMessages.ERROR_DRIVER;
            }
        }
    }

    // ************************************
    // Utility functions
    // ************************************

    /**
     * Get the next MatchAction ID
     *
     * @return MatchAction ID
     */
    // public long getNextMatchActionID() {
    // return this.matchActionId++;
    // }

    /**
     * Get ports for the adjacency SID given
     *
     * @param nodeSid  Node SID of the adjacency SID
     * @param adjacencySid Adjacency SID
     * @return List of ports
     */
    public List<Integer> getAdacencyPorts(int nodeSid, int adjacencySid) {
        HashMap<Integer, List<Integer>> adjacencySidInfo =
                adjacencySidTable.get(Integer.valueOf(nodeSid));
        if (adjacencySidInfo == null)
            return null;
        else
            return adjacencySidInfo.get(Integer.valueOf(adjacencySid));
    }

    /**
     * Check if the node ID is the adjacency ID or not
     *
     * @param nodeId to check
     * @return true if the node ID is the adjacency ID, false otherwise
     */
    public boolean isAdjacencySid(String nodeId) {
        // XXX The rule might change
        if (Integer.parseInt(nodeId) > 10000)
            return true;

        return false;
    }

    /**
     * Returns the Adjacency IDs for the node
     *
     * @param nodeSid Node SID
     * @return Collection of Adjacency ID
     */
    public Collection<Integer> getAdjacencyIds(int nodeSid) {
        HashMap<Integer, List<Integer>> adjecencyInfo =
                adjacencySidTable.get(Integer.valueOf(nodeSid));

        return adjecencyInfo.keySet();
    }

    /**
     * Send a Barrier request message and wait for the reply.
     * It waits for the reply for 2 seconds and it cause exception when timer
     * expires.
     * TODO: When it does not receive the reply within timeout, recovery action
     * is required.
     *
     * @param sw Switch to send the Barrier message
     */
    private OFBarrierReplyFuture sendBarrier(Switch sw) {
        IOF13Switch sw13 = (IOF13Switch) floodlightProvider.getMasterSwitch(
                sw.getDpid().value());
        OFBarrierReplyFuture replyFuture = null;
        if (sw13 != null) {
            try {
                replyFuture = sw13.sendBarrier();
            } catch (IOException e) {
                log.error("Error sending barrier request to switch {}",
                        sw13.getId(), e.getCause());
            }
        }

        return replyFuture;
    }

    private boolean checkBarrierReplies(List<OFBarrierReplyFuture> replies) {

        for (OFBarrierReplyFuture replyFuture: replies) {
            OFBarrierReply br = null;
            try {
                br = replyFuture.get(2, TimeUnit.SECONDS);
            } catch (TimeoutException | InterruptedException | ExecutionException e) {
                // XXX for some reason these exceptions are not being thrown
            }
            if (br == null) {
                log.warn("Did not receive barrier-reply for request ID {}",
                        replyFuture.getTransactionId());
                // XXX take corrective action
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the Adjacency Info for the node
     *
     * @param nodeSid Node SID
     * @return HashMap of <AdjacencyID, list of ports>
     */
    public HashMap<Integer, List<Integer>> getAdjacencyInfo(int nodeSid) {
        return  adjacencySidTable.get(Integer.valueOf(nodeSid));
    }

    /**
     * Parse the adjacency jason string and build the adjacency Table
     *
     * @param adjInfo  adjacency info jason string
     * @return HashMap<Adjacency ID, List of ports> object
     * @throws JSONException
     */
    private HashMap<Integer, List<Integer>> parseAdjacencySidInfo(String adjInfo)
            throws JSONException {
        JSONArray arry = new JSONArray(adjInfo);
        HashMap<Integer, List<Integer>> adjacencyInfo =
                new HashMap<Integer, List<Integer>>();

        for (int i = 0; i < arry.length(); i++) {
            Integer adjId = (Integer) arry.getJSONObject(i).get("adjSid");
            JSONArray portNos = (JSONArray) arry.getJSONObject(i).get("ports");
            if (adjId == null || portNos == null)
                continue;

            List<Integer> portNoList = new ArrayList<Integer>();
            for (int j = 0; j < portNos.length(); j++) {
                portNoList.add(Integer.valueOf(portNos.getInt(j)));
            }
            adjacencyInfo.put(adjId, portNoList);
        }
        return adjacencyInfo;
    }

    /**
     * Build the MatchActionOperationEntry according to the flag
     *
     * @param sw node ID to push for MPLS label
     * @param mplsLabel List of Switch DPIDs to forwards packets to
     * @param fwdSws PHP flag
     * @param Bos BoS flag
     * @param isTransitRouter
     * @return MatchiACtionOperationEntry object
     */
    private MatchActionOperationEntry buildMAEntry(Switch sw,
            String mplsLabel, List<String> fwdSws, boolean php,
            boolean Bos) {
        IOF13Switch sw13 = (IOF13Switch) floodlightProvider.getMasterSwitch(
                sw.getDpid().value());
        //if (sw13 == null) {
        //    return null;
        //}
        MplsMatch mplsMatch = new MplsMatch(Integer.parseInt(mplsLabel), Bos);
        List<Action> actions = new ArrayList<Action>();

        PopMplsAction popActionBos = new PopMplsAction(EthType.IPv4);
        PopMplsAction popAction = new PopMplsAction(EthType.MPLS_UNICAST);
        CopyTtlInAction copyTtlInAction = new CopyTtlInAction();
        DecNwTtlAction decNwTtlAction = new DecNwTtlAction(1);
        DecMplsTtlAction decMplsTtlAction = new DecMplsTtlAction(1);

        if (php) {
            actions.add(copyTtlInAction);
            if (Bos) {
                actions.add(popActionBos);
                actions.add(decNwTtlAction);
            }
            else {
                actions.add(popAction);
                actions.add(decMplsTtlAction);
            }
        }
        else {
            actions.add(decMplsTtlAction);
        }

        if ((sw13 instanceof OFSwitchImplDellOSR) && isTransitRouter(sw)) {
            PortNumber port = pickOnePort(sw, fwdSws);
            if (port == null) {
                log.warn("Failed to get a port from NeightborSet");
                return null;
            }
            OutputAction outputAction = new OutputAction(port);
            Switch destSwitch =
                    mutableTopology.getSwitch(new Dpid(fwdSws.get(0)));
            MacAddress srcMac =
                    MacAddress.of(sw.getStringAttribute("routerMac"));
            MacAddress dstMac =
                    MacAddress.of(destSwitch.getStringAttribute("routerMac"));
            SetSAAction setSAAction = new SetSAAction(srcMac);
            SetDAAction setDAAction = new SetDAAction(dstMac);
            actions.add(setSAAction);
            actions.add(setDAAction);
            actions.add(outputAction);
        } else {
            GroupAction groupAction = new GroupAction();
            for (String dpid: fwdSws)
                groupAction.addSwitch(new Dpid(dpid));
            actions.add(groupAction);
        }

        MatchAction matchAction = new MatchAction(maIdGenerator.getNewId(),
                new SwitchPort(sw.getDpid().value(), (short) 0), mplsMatch, actions);
        Operator operator = Operator.ADD;
        MatchActionOperationEntry maEntry =
                new MatchActionOperationEntry(operator, matchAction);

        return maEntry;
    }

    /**
     * Pick a router from the neighbor set and return the port
     * connected to the router.
     *
     * @param sw source switch
     * @param fwdSwDpids neighbor set of the switch
     * @return PortNumber connected to one of the neighbors
     */
    public PortNumber pickOnePort(Switch sw, List<String> fwdSwDpids) {
        for (Link link: sw.getOutgoingLinks()) {
            if (link.getDstSwitch().getDpid().toString().equals(fwdSwDpids.get(0)))
                return link.getSrcPort().getNumber();
        }

        return null;
    }

    /**
     * check if the router is the transit router or not
     *
     * @param sw  router switch to check
     * @return true if the switch is the transit router, false otherwise
     */
    public boolean isTransitRouter(Switch sw) {
        int i = 0;
        for(Switch neighbor: sw.getNeighbors()) {
            i++;
        }
        if (i > 1)
            return true;
        else
            return false;
    }

    /**
     * Get the forwarding Switch DPIDs to send packets to a node.
     * If ECMP in transit routers is not supported, only one switch needs to be
     * selected as the neighbor set to forward packets to.
     *
     * @param srcSw source switch
     * @param nodeId destination node Id
     * @return list of switch DPID to forward packets to
     */
    public List<Dpid> getForwardingSwitchForNodeId(Switch srcSw, String nodeId) {

        List<Dpid> fwdSws = new ArrayList<Dpid>();
        Switch destSw = null;

        destSw = getSwitchFromNodeId(nodeId);

        if (destSw == null) {
            log.debug("Cannot find the switch with ID {}", nodeId);
            return null;
        }

        ECMPShortestPathGraph ecmpSPG = new ECMPShortestPathGraph(srcSw);

        HashMap<Integer, HashMap<Switch, ArrayList<ArrayList<Dpid>>>> switchVia =
                ecmpSPG.getAllLearnedSwitchesAndVia();
        for (Integer itrIdx : switchVia.keySet()) {
            HashMap<Switch, ArrayList<ArrayList<Dpid>>> swViaMap =
                    switchVia.get(itrIdx);
            for (Switch targetSw : swViaMap.keySet()) {
                String destSwDpid = destSw.getDpid().toString();
                if (targetSw.getDpid().toString().equals(destSwDpid)) {
                    for (ArrayList<Dpid> via : swViaMap.get(targetSw)) {
                        if (via.isEmpty()) {
                            fwdSws.add(destSw.getDpid());
                        }
                        else {
                            Dpid firstVia = via.get(via.size()-1);
                            fwdSws.add(firstVia);
                            IOF13Switch targetSw13 = (IOF13Switch)floodlightProvider.getMasterSwitch(
                                    targetSw.getDpid().value());
                            if (targetSw13 instanceof OFSwitchImplDellOSR &&
                                isTransitRouter(targetSw) &&
                                isTransitRouter(mutableTopology.getSwitch(firstVia))) {
                                return fwdSws;
                            }
                        }
                    }
                }
            }
        }

        return fwdSws;
    }

    /**
     * Get switch for the node Id specified
     *
     * @param nodeId node ID for switch
     * @return Switch
     */
    public Switch getSwitchFromNodeId(String nodeId) {

        for (Switch sw : mutableTopology.getSwitches()) {
            String id = sw.getStringAttribute("nodeSid");
            if (id.equals(nodeId)) {
                return sw;
            }
        }

        return null;
    }

    /**
     * Check if the switch is the edge router or not.
     *
     * @param dpid Dpid of the switch to check
     * @return true if it is an edge router, otherwise false
     */
    private boolean IsEdgeRouter(String dpid) {

        for (Switch sw : mutableTopology.getSwitches()) {
            String dpidStr = sw.getDpid().toString();
            if (dpid.equals(dpidStr)) {
                /*
                String subnetInfo = sw.getStringAttribute("subnets");
                if (subnetInfo == null || subnetInfo.equals("[]")) {
                    return false;
                }
                else
                    return true;
                */
                String isEdge = sw.getStringAttribute("isEdgeRouter");
                if (isEdge != null) {
                    if (isEdge.equals("true"))
                        return true;
                    else
                        return false;
                }
            }
        }

        return false;
    }

    /**
     * Get MPLS label reading the config file
     *
     * @param dipid DPID of the switch
     * @return MPLS label for the switch
     */
    public String getMplsLabel(String dpid) {

        String mplsLabel = null;
        for (Switch sw : mutableTopology.getSwitches()) {
            String dpidStr = sw.getDpid().toString();
            if (dpid.equals(dpidStr)) {
                mplsLabel = sw.getStringAttribute("nodeSid");
                break;
            }
        }

        return mplsLabel;
    }

    /**
     * The function checks if given IP matches to the given subnet mask
     *
     * @param addr - subnet address to match
     * @param addr1 - IP address to check
     * @return true if the IP address matches to the subnet, otherwise false
     */
    public boolean netMatch(String addr, String addr1) { // addr is subnet
                                                         // address and addr1 is
                                                         // ip address. Function
                                                         // will return true, if
                                                         // addr1 is within
                                                         // addr(subnet)

        String[] parts = addr.split("/");
        String ip = parts[0];
        int prefix;

        if (parts.length < 2) {
            prefix = 0;
        } else {
            prefix = Integer.parseInt(parts[1]);
        }

        Inet4Address a = null;
        Inet4Address a1 = null;
        try {
            a = (Inet4Address) InetAddress.getByName(ip);
            a1 = (Inet4Address) InetAddress.getByName(addr1);
        } catch (UnknownHostException e) {
        }

        byte[] b = a.getAddress();
        int ipInt = ((b[0] & 0xFF) << 24) |
                ((b[1] & 0xFF) << 16) |
                ((b[2] & 0xFF) << 8) |
                ((b[3] & 0xFF) << 0);

        byte[] b1 = a1.getAddress();
        int ipInt1 = ((b1[0] & 0xFF) << 24) |
                ((b1[1] & 0xFF) << 16) |
                ((b1[2] & 0xFF) << 8) |
                ((b1[3] & 0xFF) << 0);

        int mask = ~((1 << (32 - prefix)) - 1);

        if ((ipInt & mask) == (ipInt1 & mask)) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Add a routing rule for the host
     *
     * @param sw - Switch to add the rule
     * @param hostIpAddress Destination host IP address
     * @param hostMacAddress Destination host MAC address
     */
    public void addRouteToHost(Switch sw, int hostIpAddress, byte[] hostMacAddress) {
        ipHandler.addRouteToHost(sw, hostIpAddress, hostMacAddress);
    }

    /**
     * Add IP packet to a buffer queue
     *
     * @param ipv4
     */
    public void addPacketToPacketBuffer(IPv4 ipv4) {
        ipPacketQueue.add(ipv4);
    }

    /**
     * Retrieve all packets whose destination is the given address.
     *
     * @param destIp Destination address of packets to retrieve
     */
    public List<IPv4> getIpPacketFromQueue(byte[] destIp) {

        List<IPv4> bufferedPackets = new ArrayList<IPv4>();

        if (!ipPacketQueue.isEmpty()) {
            for (IPv4 ip : ipPacketQueue) {
                int dest = ip.getDestinationAddress();
                IPv4Address ip1 = IPv4Address.of(dest);
                IPv4Address ip2 = IPv4Address.of(destIp);
                if (ip1.equals(ip2)) {
                    bufferedPackets.add((IPv4) (ipPacketQueue.poll()).clone());
                }
            }
        }

        return bufferedPackets;
    }

    /**
     * Get MAC address to known hosts
     *
     * @param destinationAddress IP address to get MAC address
     * @return MAC Address to given IP address
     */
    public byte[] getMacAddressFromIpAddress(int destinationAddress) {

        // Can't we get the host IP address from the TopologyService ??

        Iterator<ArpEntry> iterator = arpEntries.iterator();

        IPv4Address ipAddress = IPv4Address.of(destinationAddress);
        byte[] ipAddressInByte = ipAddress.getBytes();

        while (iterator.hasNext()) {
            ArpEntry arpEntry = iterator.next();
            byte[] address = arpEntry.targetIpAddress;

            IPv4Address a = IPv4Address.of(address);
            IPv4Address b = IPv4Address.of(ipAddressInByte);

            if (a.equals(b)) {
                log.debug("Found an arp entry");
                return arpEntry.targetMacAddress;
            }
        }

        return null;
    }

    /**
     * Send an ARP request via ArpHandler
     *
     * @param destinationAddress
     * @param sw
     * @param inPort
     *
     */
    public void sendArpRequest(Switch sw, int destinationAddress, Port inPort) {
        arpHandler.sendArpRequest(sw, destinationAddress, inPort);
    }

    /**
     * Get IOF13Switch object for the DPID
     *
     * @param dpid Switch DPID
     * @return IOF13Switch object
     */
    public IOF13Switch getIOF13Switch(String dpidStr) {

        Dpid dpid = new Dpid(dpidStr);
        IOF13Switch targetSw = (IOF13Switch) floodlightProvider.getMasterSwitch(
                dpid.value());

        return targetSw;
    }

    /**
     * Get Switch object for the DPID
     *
     * @param dpid Switch DPID
     * @return Switch object
     */
    public Switch getSwitch(String dpid) {
        return mutableTopology.getSwitch(new Dpid(dpid));
    }

    public MatchActionId getMatchActionId() {
        return maIdGenerator.getNewId();
    }

    // ************************************
    // Test functions
    // ************************************

    private void runTest() {

        if (testMode == POLICY_ADD1) {
            Integer[] routeArray = {101, 105, 110};
            /*List<Dpid> routeList = new ArrayList<Dpid>();
            for (int i = 0; i < routeArray.length; i++) {
                Dpid dpid = getSwitchFromNodeId(routeArray[i]).getDpid();
                routeList.add(dpid);
            }*/

            if (createTunnel("1", Arrays.asList(routeArray))) {
                IPv4Net srcIp = new IPv4Net("10.0.1.1/24");
                IPv4Net dstIp = new IPv4Net("10.1.2.1/24");

                log.debug("Set the policy 1");
                this.createPolicy("1", null, null, Ethernet.TYPE_IPV4, srcIp,
                        dstIp, IPv4.PROTOCOL_ICMP, (short)-1, (short)-1, 10000,
                        "1");
                testMode = POLICY_ADD2;
                testTask.reschedule(5, TimeUnit.SECONDS);
            }
            else {
                // retry it
                testTask.reschedule(5, TimeUnit.SECONDS);
            }
        }
        else if (testMode == POLICY_ADD2) {
            Integer[] routeArray = {101, 102, 103, 104, 105, 108, 110};

            if (createTunnel("2", Arrays.asList(routeArray))) {
                IPv4Net srcIp = new IPv4Net("10.0.1.1/24");
                IPv4Net dstIp = new IPv4Net("10.1.2.1/24");

                log.debug("Set the policy 2");
                this.createPolicy("2", null, null, Ethernet.TYPE_IPV4, srcIp,
                        dstIp, IPv4.PROTOCOL_ICMP, (short)-1, (short)-1, 20000,
                        "2");
                //testMode = POLICY_REMOVE2;
                //testTask.reschedule(5, TimeUnit.SECONDS);
            }
            else {
                log.debug("Retry it");
                testTask.reschedule(5, TimeUnit.SECONDS);
            }
        }
        else if (testMode == POLICY_REMOVE2){
            log.debug("Remove the policy 2");
            this.removePolicy("2");
            testMode = POLICY_REMOVE1;
            testTask.reschedule(5, TimeUnit.SECONDS);
        }
        else if (testMode == POLICY_REMOVE1){
            log.debug("Remove the policy 1");
            this.removePolicy("1");

            testMode = TUNNEL_REMOVE1;
            testTask.reschedule(5, TimeUnit.SECONDS);
        }
        else if (testMode == TUNNEL_REMOVE1) {
            log.debug("Remove the tunnel 1");
            this.removeTunnel("1");

            testMode = TUNNEL_REMOVE2;
            testTask.reschedule(5, TimeUnit.SECONDS);
        }
        else if (testMode == TUNNEL_REMOVE2) {
            log.debug("Remove the tunnel 2");
            this.removeTunnel("2");
            log.debug("The end of test");
        }
    }

    private void runTest1() {

        String dpid1 = "00:00:00:00:00:00:00:01";
        String dpid2 = "00:00:00:00:00:00:00:0a";
        Switch srcSw = mutableTopology.getSwitch(new Dpid(dpid1));
        Switch dstSw = mutableTopology.getSwitch(new Dpid(dpid2));

        if (srcSw == null || dstSw == null) {
            testTask.reschedule(1, TimeUnit.SECONDS);
            log.debug("Switch is gone. Reschedule the test");
            return;
        }

        String[] routeArray = {"101", "102", "105", "108", "110"};
        List<String> routeList = new ArrayList<String>();
        for (int i = 0; i < routeArray.length; i++)
            routeList.add(routeArray[i]);

        List<String> optimizedRoute = this.getOptimizedPath(srcSw, dstSw, routeList);

        log.debug("Test set is {}", routeList.toString());
        log.debug("Result set is {}", optimizedRoute.toString());


    }

    /**
     * Debugging function to print out the Match Action Entry
     * @param sw13
     *
     * @param maEntry
     */
    public void printMatchActionOperationEntry(
            IOF13Switch sw13, MatchActionOperationEntry maEntry) {

        StringBuilder logStr = new StringBuilder("In switch " + sw13.getId() + ", ");

        MatchAction ma = maEntry.getTarget();
        Match m = ma.getMatch();
        List<Action> actions = ma.getActions();

        if (m instanceof Ipv4Match) {
            logStr.append("If the IP matches with ");
            IPv4Net ip = ((Ipv4Match) m).getDestination();
            logStr.append(ip.toString());
            logStr.append(" then ");
        }
        else if (m instanceof MplsMatch) {
            logStr.append("If the MPLS label matches with ");
            int mplsLabel = ((MplsMatch) m).getMplsLabel();
            logStr.append(mplsLabel);
            logStr.append(" then ");
        }
        else if (m instanceof PacketMatch) {
            logStr.append("if the policy match is XXX then ");
        }

        logStr.append(" do { ");
        for (Action action : actions) {
            if (action instanceof CopyTtlInAction) {
                logStr.append("copy ttl In, ");
            }
            else if (action instanceof CopyTtlOutAction) {
                logStr.append("copy ttl Out, ");
            }
            else if (action instanceof DecMplsTtlAction) {
                logStr.append("Dec MPLS TTL , ");
            }
            else if (action instanceof GroupAction) {
                logStr.append("Forward packet to < ");
                NeighborSet dpids = ((GroupAction) action).getDpids();
                logStr.append(dpids.toString() + ",");
            }
            else if (action instanceof PopMplsAction) {
                logStr.append("Pop MPLS label, ");
            }
            else if (action instanceof PushMplsAction) {
                logStr.append("Push MPLS label, ");
            }
            else if (action instanceof SetMplsIdAction) {
                int id = ((SetMplsIdAction) action).getMplsId();
                logStr.append("Set MPLS ID as " + id + ", ");
            }
        }

        log.debug(logStr.toString());

    }

    // ************************************
    // Unused classes and functions
    // ************************************

    /**
     * Temporary class to to keep ARP entry
     *
     */
    private class ArpEntry {

        byte[] targetMacAddress;
        byte[] targetIpAddress;

        private ArpEntry(byte[] macAddress, byte[] ipAddress) {
            this.targetMacAddress = macAddress;
            this.targetIpAddress = ipAddress;
        }
    }

    /**
     * This class is used only for link recovery optimization in
     * modifyEcmpRoutingRules() function.
     * TODO: please remove if the optimization is not used at all
     */
    private class SwitchPair {
        private Switch src;
        private Switch dst;

        public SwitchPair(Switch src, Switch dst) {
            this.src = src;
            this.dst = dst;
        }

        public Switch getSource() {
            return src;
        }

        public Switch getDestination() {
            return dst;
        }
    }

    /**
     * Update ARP Cache using ARP packets It is used to set destination MAC
     * address to forward packets to known hosts. But, it will be replace with
     * Host information of Topology service later.
     *
     * @param arp APR packets to use for updating ARP entries
     */
    public void updateArpCache(ARP arp) {

        ArpEntry arpEntry = new ArpEntry(arp.getSenderHardwareAddress(),
                arp.getSenderProtocolAddress());
        // TODO: Need to check the duplication
        arpEntries.add(arpEntry);
    }

    /**
     * Modify the routing rules for the lost links
     * - Recompute the path if the link failed is included in the path
     * (including src and dest).
     *
     * @param newLink
     */
    private void modifyEcmpRoutingRules(LinkData linkRemoved) {

        //HashMap<Switch, SwitchPair> linksToRecompute = new HashMap<Switch, SwitchPair>();
        Set<SwitchPair> linksToRecompute = new HashSet<SwitchPair>();

        for (ECMPShortestPathGraph ecmpSPG : graphs.values()) {
            Switch rootSw = ecmpSPG.getRootSwitch();
            HashMap<Integer, HashMap<Switch, ArrayList<Path>>> paths =
                    ecmpSPG.getCompleteLearnedSwitchesAndPaths();
            for (HashMap<Switch, ArrayList<Path>> p: paths.values()) {
                for (Switch destSw: p.keySet()) {
                    ArrayList<Path> path = p.get(destSw);
                    if  (checkPath(path, linkRemoved)) {
                        boolean found = false;
                        for (SwitchPair pair: linksToRecompute) {
                            if (pair.getSource().getDpid() == rootSw.getDpid() &&
                                    pair.getSource().getDpid() == destSw.getDpid()) {
                                found = true;
                            }
                        }
                        if (!found) {
                            linksToRecompute.add(new SwitchPair(rootSw, destSw));
                        }
                    }
                }
            }
        }

        // Recompute the path for the specific route
        for (SwitchPair pair: linksToRecompute) {

            log.debug("Recompute path from {} to {}", pair.getSource(), pair.getDestination());
            // We need the following function for optimization
            //ECMPShortestPathGraph ecmpSPG =
            //     new ECMPShortestPathGraph(pair.getSource(), pair.getDestination());
            ECMPShortestPathGraph ecmpSPG =
                    new ECMPShortestPathGraph(pair.getSource());
            populateEcmpRoutingRulesForPath(pair.getSource(), ecmpSPG, true);
        }
    }

    /**
     * Optimize the mpls label
     * The feature will be used only for policy of "avoid a specific switch".
     * Check route to each router in route backward.
     * If there is only one route to the router and the routers are included in
     * the route, remove the id from the path.
     * A-B-C-D-E  => A-B-C-D-E -> A-E
     *   |   |    => A-B-H-I   -> A-I
     *   F-G-H-I  => A-D-I > A-D-I
     */
    private List<String> getOptimizedPath(Switch srcSw, Switch dstSw, List<String> route) {

        List<String> optimizedPath = new ArrayList<String>();
        optimizedPath.addAll(route);
        ECMPShortestPathGraph ecmpSPG = new ECMPShortestPathGraph(srcSw);

        HashMap<Integer, HashMap<Switch, ArrayList<Path>>> paths =
                ecmpSPG.getCompleteLearnedSwitchesAndPaths();
        for (HashMap<Switch, ArrayList<Path>> p: paths.values()) {
            for (Switch s: p.keySet()) {
                if (s.getDpid().toString().equals(dstSw.getDpid().toString())) {
                    ArrayList<Path> ecmpPaths = p.get(s);
                    if (ecmpPaths!= null && ecmpPaths.size() == 1) {
                        for (Path path: ecmpPaths) {
                            for (LinkData link: path) {
                                String srcId = getMplsLabel(link.getSrc().getDpid().toString());
                                String dstId = getMplsLabel(link.getSrc().getDpid().toString());
                                if (optimizedPath.contains(srcId)) {
                                    optimizedPath.remove(srcId);
                                }
                                if (optimizedPath.contains(dstId)) {
                                    optimizedPath.remove(dstId);
                                }
                            }
                        }
                    }
                }
            }
        }

        return optimizedPath;

    }

    /**
     * Check if the path is affected from the link removed
     *
     * @param path Path to check
     * @param linkRemoved link removed
     * @return true if the path contains the link removed
     */
    private boolean checkPath(ArrayList<Path> path, LinkData linkRemoved) {

        for (Path ppp: path) {
            // TODO: need to check if this is a bidirectional or
            // unidirectional
            for (LinkData link: ppp) {
                if (link.getDst().getDpid().equals(linkRemoved.getDst().getDpid()) &&
                        link.getSrc().getDpid().equals(linkRemoved.getSrc().getDpid()))
                    return true;
            }
        }

        return false;
    }



}
