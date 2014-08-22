package net.onrc.onos.apps.sdnip;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.api.newintent.IntentService;
import net.onrc.onos.api.newintent.MultiPointToSinglePointIntent;
import net.onrc.onos.apps.proxyarp.IArpRequester;
import net.onrc.onos.apps.proxyarp.IProxyArpService;
import net.onrc.onos.apps.sdnip.RibUpdate.Operation;
import net.onrc.onos.apps.sdnip.web.SdnIpWebRoutable;
import net.onrc.onos.apps.sdnip.web.SdnIpWebRoutableNew;
import net.onrc.onos.core.intent.IntentOperation;
import net.onrc.onos.core.intent.IntentOperationList;
import net.onrc.onos.core.intent.ShortestPathIntent;
import net.onrc.onos.core.intent.runtime.IPathCalcRuntimeService;
import net.onrc.onos.core.linkdiscovery.ILinkDiscoveryService;
import net.onrc.onos.core.main.config.IConfigInfoService;
import net.onrc.onos.core.matchaction.action.ModifyDstMacAction;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.newintent.IdBlockAllocatorBasedIntentIdGenerator;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.IPv4;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.configuration.ConfigurationRuntimeException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;

/**
 * This class sets up BGP paths, handles RIB updates and relative intents.
 * TODO: Thread-safe.
 */
public class SdnIp implements IFloodlightModule, ISdnIpService,
        IArpRequester, IConfigInfoService {

    private static final Logger log = LoggerFactory.getLogger(SdnIp.class);

    private ILinkDiscoveryService linkDiscoveryService;
    private IRestApiService restApi;
    private IProxyArpService proxyArp;

    private InvertedRadixTree<RibEntry> bgpRoutes;
    private InvertedRadixTree<Interface> interfaceRoutes;

    private BlockingQueue<RibUpdate> ribUpdates;

    private String bgpdRestIp;
    private String routerId;
    private static final String DEFAULT_CONFIG_FILENAME = "config.json";
    private String currentConfigFilename = DEFAULT_CONFIG_FILENAME;

    /* ShortestPath Intent Variables */
    private final String caller = "SdnIp";
    private IControllerRegistryService controllerRegistryService;
    private IntentService intentService;
    private IPathCalcRuntimeService pathRuntime;
    /* Shortest Intent Path Variables */
    private IdBlockAllocatorBasedIntentIdGenerator intentIdGenerator;
    //private static final short ARP_PRIORITY = 20;

    //private static final short BGP_PORT = 179;

    // Configuration stuff
    private List<String> switches;
    private Map<String, Interface> interfaces;
    private Map<InetAddress, BgpPeer> bgpPeers;
    private SwitchPort bgpdAttachmentPoint;
    private MACAddress bgpdMacAddress;
    private short vlan;
    private Set<SwitchPort> externalNetworkSwitchPorts;

    private SetMultimap<InetAddress, RibUpdate> prefixesWaitingOnArp;

    private ExecutorService bgpUpdatesExecutor;

    private ConcurrentHashMap<Prefix, MultiPointToSinglePointIntent> pushedRouteIntents;

    /**
     * SDN-IP application has a configuration file. This method is to read all
     * the info from this file.
     *
     * @param configFilename the name of configuration file for SDN-IP application
     */
    private void readConfiguration(String configFilename) {
        File gatewaysFile = new File(configFilename);
        ObjectMapper mapper = new ObjectMapper();

        try {
            Configuration config = mapper.readValue(gatewaysFile, Configuration.class);

            switches = config.getSwitches();
            interfaces = new HashMap<>();
            for (Interface intf : config.getInterfaces()) {
                interfaces.put(intf.getName(), intf);
                externalNetworkSwitchPorts.add(new SwitchPort(intf.getDpid(),
                        intf.getPort()));
            }
            bgpPeers = new HashMap<>();
            for (BgpPeer peer : config.getPeers()) {
                bgpPeers.put(peer.getIpAddress(), peer);
            }

            bgpdAttachmentPoint = new SwitchPort(
                    new Dpid(config.getBgpdAttachmentDpid()),
                    PortNumber.uint16(config.getBgpdAttachmentPort()));

            bgpdMacAddress = config.getBgpdMacAddress();
            vlan = config.getVlan();
        } catch (JsonParseException | JsonMappingException e) {
            log.error("Error in JSON file", e);
            throw new ConfigurationRuntimeException("Error in JSON file", e);
        } catch (IOException e) {
            log.error("Error reading JSON file", e);
            throw new ConfigurationRuntimeException("Error in JSON file", e);
        }

        // Populate the interface InvertedRadixTree
        for (Interface intf : interfaces.values()) {
            Prefix prefix = new Prefix(intf.getIpAddress().getAddress(),
                    intf.getPrefixLength());
            interfaceRoutes.put(prefix.toBinaryString(), intf);
        }
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<>();
        l.add(ISdnIpService.class);
        l.add(IConfigInfoService.class);
        return l;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<>();
        m.put(ISdnIpService.class, this);
        m.put(IConfigInfoService.class, this);
        return m;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<>();
        l.add(IRestApiService.class);
        l.add(IControllerRegistryService.class);
        l.add(IPathCalcRuntimeService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context)
            throws FloodlightModuleException {

        bgpRoutes = new ConcurrentInvertedRadixTree<>(
                new DefaultByteArrayNodeFactory());
        interfaceRoutes = new ConcurrentInvertedRadixTree<>(
                new DefaultByteArrayNodeFactory());
        externalNetworkSwitchPorts = new HashSet<SwitchPort>();

        ribUpdates = new LinkedBlockingQueue<>();

        // Register REST handler.
        restApi = context.getServiceImpl(IRestApiService.class);
        proxyArp = context.getServiceImpl(IProxyArpService.class);

        controllerRegistryService = context
                .getServiceImpl(IControllerRegistryService.class);
        pathRuntime = context.getServiceImpl(IPathCalcRuntimeService.class);
        linkDiscoveryService = context.getServiceImpl(ILinkDiscoveryService.class);

        intentIdGenerator = new IdBlockAllocatorBasedIntentIdGenerator(
                controllerRegistryService);

        // TODO: initialize intentService

        pushedRouteIntents = new ConcurrentHashMap<>();

        prefixesWaitingOnArp = Multimaps.synchronizedSetMultimap(
                HashMultimap.<InetAddress, RibUpdate>create());

        //flowCache = new FlowCache(floodlightProvider);

        bgpUpdatesExecutor = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder().setNameFormat("bgp-updates-%d").build());

        // Read in config values
        bgpdRestIp = context.getConfigParams(this).get("BgpdRestIp");
        if (bgpdRestIp == null) {
            log.error("BgpdRestIp property not found in config file");
            throw new ConfigurationRuntimeException(
                    "BgpdRestIp property not found in config file");
        } else {
            log.info("BgpdRestIp set to {}", bgpdRestIp);
        }

        routerId = context.getConfigParams(this).get("RouterId");
        if (routerId == null) {
            log.error("RouterId property not found in config file");
            throw new ConfigurationRuntimeException(
                    "RouterId property not found in config file");
        } else {
            log.info("RouterId set to {}", routerId);
        }

        String configFilenameParameter = context.getConfigParams(this).get("configfile");
        if (configFilenameParameter != null) {
            currentConfigFilename = configFilenameParameter;
        }
        log.debug("Config file set to {}", currentConfigFilename);

        readConfiguration(currentConfigFilename);
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        restApi.addRestletRoutable(new SdnIpWebRoutable());
        restApi.addRestletRoutable(new SdnIpWebRoutableNew());

        // Retrieve the RIB from BGPd during startup
        retrieveRib();
    }

    @Override
    public RadixTree<RibEntry> getPtree() {
        return bgpRoutes;
    }

    @Override
    public void clearPtree() {
        log.warn("Clear table operation not supported");
    }

    @Override
    public String getBgpdRestIp() {
        return bgpdRestIp;
    }

    @Override
    public String getRouterId() {
        return routerId;
    }

    /**
     * SDN-IP application will fetch all rib entries from BGPd when it starts.
     * Especially when we restart SDN-IP application while the BGPd has been
     * running all the time. Then before SDN-IP application re-connects to BGPd,
     * there are already lots of rib entries in BGPd.
     */
    private void retrieveRib() {
        String url = "http://" + bgpdRestIp + "/wm/bgp/" + routerId;
        String response = RestClient.get(url);

        if ("".equals(response)) {
            return;
        }

        try {
            response = response.replaceAll("\"", "'");
            JSONObject jsonObj = (JSONObject) JSONSerializer.toJSON(response);
            JSONArray ribArray = jsonObj.getJSONArray("rib");
            String inboundRouterId = jsonObj.getString("router-id");

            int size = ribArray.size();

            log.info("Retrived RIB of {} entries from BGPd", size);

            for (int j = 0; j < size; j++) {
                JSONObject ribEntry = ribArray.getJSONObject(j);
                String prefix = ribEntry.getString("prefix");
                String nexthop = ribEntry.getString("nexthop");

                // Insert each rib entry into the local rib
                String[] substring = prefix.split("/");
                String prefix1 = substring[0];
                String mask1 = substring[1];

                Prefix p;
                try {
                    p = new Prefix(prefix1, Integer.parseInt(mask1));
                } catch (NumberFormatException e) {
                    log.warn("Wrong mask format in RIB JSON: {}", mask1);
                    continue;
                } catch (IllegalArgumentException e1) {
                    log.warn("Wrong prefix format in RIB JSON: {}", prefix1);
                    continue;
                }

                RibEntry rib = new RibEntry(inboundRouterId, nexthop);

                try {
                    ribUpdates.put(new RibUpdate(Operation.UPDATE, p, rib));
                } catch (InterruptedException e) {
                    log.debug("Interrupted while pushing onto update queue");
                }
            }
        } catch (JSONException e) {
            // TODO don't parse JSON manually
            log.error("Error parsing inital route table JSON:", e);
        }
    }

    /**
     * Put RIB update to RIB update queue.
     *
     * @param update RIB update
     */
    @Override
    public void newRibUpdate(RibUpdate update) {
        try {
            ribUpdates.put(update);
        } catch (InterruptedException e) {
            log.debug("Interrupted while putting on ribUpdates queue", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Process adding RIB update.
     * Put new RIB update into InvertedRadixTree. If there was an existing nexthop
     * for this prefix, but the next hop was different, then execute deleting old
     * RIB update. If the next hop is the SDN domain, we do not handle it at the
     * moment. Otherwise, execute adding RIB.
     *
     * @param update RIB update
     */
    protected void processRibAdd(RibUpdate update) {
        synchronized (this) {
            Prefix prefix = update.getPrefix();

            log.debug("Processing prefix add {}", prefix);

            RibEntry rib = bgpRoutes.put(prefix.toBinaryString(), update.getRibEntry());

            if (rib != null && !rib.equals(update.getRibEntry())) {
                // There was an existing nexthop for this prefix. This update
                // supersedes that, so we need to remove the old flows for this
                // prefix from the switches
                executeDeleteRoute(prefix, rib);
            }

            if (update.getRibEntry().getNextHop().equals(
                    InetAddresses.forString("0.0.0.0"))) {
                // Route originated by SDN domain
                // We don't handle these at the moment
                log.debug("Own route {} to {}", prefix,
                        update.getRibEntry().getNextHop().getHostAddress());
                return;
            }

            executeRibAdd(update);
        }
    }

    /**
     * Execute adding RIB update.
     * Find out the egress Interface and MAC address of next hop router for this
     * RIB update. If the MAC address can not be found in ARP cache, then this
     * prefix will be put in prefixesWaitingOnArp queue. Otherwise, new flow
     * intent will be created and installed.
     *
     * @param update RIB update
     */
    private void executeRibAdd(RibUpdate update) {

        Prefix prefix = update.getPrefix();
        RibEntry rib = update.getRibEntry();

        InetAddress nextHopIpAddress = rib.getNextHop();

        // See if we know the MAC address of the next hop
        MACAddress nextHopMacAddress = proxyArp.getMacAddress(nextHopIpAddress);

        if (nextHopMacAddress == null) {
            prefixesWaitingOnArp.put(nextHopIpAddress,
                    new RibUpdate(Operation.UPDATE, prefix, rib));
            proxyArp.sendArpRequest(nextHopIpAddress, this, true);
            return;
        }

        addRouteIntentToNextHop(prefix, nextHopIpAddress, nextHopMacAddress);
    }

    /**
     * Adds a route intent given a prefix and a next hop IP address. This
     * method will find the egress interface for the intent.
     *
     * @param prefix IP prefix of the route to add
     * @param nextHopIpAddress IP address of the next hop
     * @param nextHopMacAddress MAC address of the next hop
     */
    private void addRouteIntentToNextHop(Prefix prefix, InetAddress nextHopIpAddress,
            MACAddress nextHopMacAddress) {

        // Find the attachment point (egress interface) of the next hop
        Interface egressInterface;
        if (bgpPeers.containsKey(nextHopIpAddress)) {
            // Route to a peer
            log.debug("Route to peer {}", nextHopIpAddress);
            BgpPeer peer = bgpPeers.get(nextHopIpAddress);
            egressInterface = interfaces.get(peer.getInterfaceName());
        } else {
            // Route to non-peer
            log.debug("Route to non-peer {}", nextHopIpAddress);
            egressInterface = getOutgoingInterface(nextHopIpAddress);
            if (egressInterface == null) {
                log.warn("No outgoing interface found for {}", nextHopIpAddress
                        .getHostAddress());
                return;
            }
        }

        doAddRouteIntent(prefix, egressInterface, nextHopMacAddress);
    }

    /**
     * Install a flow intent for a prefix.
     * Intent will match dst IP prefix and rewrite dst MAC address at all other
     * border switches, then forward packets according to dst MAC address.
     *
     * @param prefix IP prefix from BGP route
     * @param egressInterface egress Interface connected to next hop router
     * @param nextHopMacAddress MAC address of next hop router
     */
    private void doAddRouteIntent(Prefix prefix, Interface egressInterface,
            MACAddress nextHopMacAddress) {
        log.debug("Adding intent for prefix {}, next hop mac {}",
                prefix, nextHopMacAddress);

        MultiPointToSinglePointIntent pushedIntent = pushedRouteIntents.get(prefix);

        // Just for testing.
        if (pushedIntent != null) {
            log.error("There should not be a pushed intent: {}", pushedIntent);
        }

        SwitchPort egressPort = egressInterface.getSwitchPort();

        Set<SwitchPort> ingressPorts = new HashSet<SwitchPort>();

        for (Interface intf : interfaces.values()) {
            if (!intf.equals(egressInterface)) {
                SwitchPort srcPort = intf.getSwitchPort();
                ingressPorts.add(srcPort);
            }
        }

        // Match the destination IP prefix at the first hop
        PacketMatchBuilder builder = new PacketMatchBuilder();
        builder.setDstIp(new IPv4(InetAddresses
                .coerceToInteger(prefix.getInetAddress())),
                (short) prefix.getPrefixLength());
        PacketMatch packetMatch = builder.build();

        // Rewrite the destination MAC address
        ModifyDstMacAction modifyDstMacAction =
                new ModifyDstMacAction(nextHopMacAddress);

        MultiPointToSinglePointIntent intent =
                new MultiPointToSinglePointIntent(intentIdGenerator.getNewId(),
                        packetMatch, modifyDstMacAction, ingressPorts, egressPort);

        intentService.submit(intent);

        // Maintain the Intent
        pushedRouteIntents.put(prefix, intent);
    }

    /**
     * Remove prefix from InvertedRadixTree, if success, then try to delete the
     * relative intent.
     *
     * @param update RIB update
     */
    protected void processRibDelete(RibUpdate update) {
        synchronized (this) {
            Prefix prefix = update.getPrefix();

            // if (ptree.remove(prefix, update.getRibEntry())) {

            // TODO check the change of logic here - remove doesn't check that
            // the rib entry was what we expected (and we can't do this
            // concurrently)

            if (bgpRoutes.remove(prefix.toBinaryString())) {
                /*
                 * Only delete flows if an entry was actually removed from the tree.
                 * If no entry was removed, the <prefix, nexthop> wasn't there so
                 * it's probably already been removed and we don't need to do anything
                 */
                executeDeleteRoute(prefix, update.getRibEntry());

            }

            prefixesWaitingOnArp.removeAll(prefix.getInetAddress());
            // TODO cancel the request in the ARP manager as well
        }
    }

    /**
     * Delete prefix intent installed.
     *
     * @param prefix IP prefix withdrew in a rib update announcement
     * @param ribEntry next hop information
     */
    private void executeDeleteRoute(Prefix prefix, RibEntry ribEntry) {
        log.debug("Deleting {} to {}", prefix, ribEntry.getNextHop());

        MultiPointToSinglePointIntent intent = pushedRouteIntents.remove(prefix);

        if (intent == null) {
            log.debug("There is no intent in pushedRouteIntents to delete for " +
                    "prefix: {}", prefix);
        } else {
            intentService.withdraw(intent);
            log.debug("Deleted the pushedRouteIntent for prefix: {}", prefix);
        }
    }

    /**
     * Setup the Paths to the BGP Daemon. Run a loop for all of the bgpPeers
     * Push flow from BGPd to the peer Push flow from peer to BGPd Parameters to
     * pass to the intent are as follows: String id, long srcSwitch, long
     * srcPort, long srcMac, int srcIP, long dstSwitch, long dstPort, long
     * dstMac, int dstIP
     */
    private void setupBgpPaths() {
        IntentOperationList operations = new IntentOperationList();
        for (BgpPeer bgpPeer : bgpPeers.values()) {
            Interface peerInterface = interfaces.get(bgpPeer.getInterfaceName());
            // Inet4Address.
            int srcIP = InetAddresses.coerceToInteger(peerInterface.getIpAddress());
            int dstIP = InetAddresses.coerceToInteger(bgpPeer.getIpAddress());
            String fwdIntentId = caller + ":"
                    + controllerRegistryService.getNextUniqueId();
            String bwdIntentId = caller + ":"
                    + controllerRegistryService.getNextUniqueId();
            SwitchPort srcPort =
                    new SwitchPort(bgpdAttachmentPoint.getDpid(),
                            bgpdAttachmentPoint.getPortNumber());
            // TODO: replace the code below with peerInterface.getSwitchPort()
            // when using poingToPointIntent
            SwitchPort dstPort =
                    new SwitchPort(new Dpid(peerInterface.getDpid()),
                            new PortNumber(peerInterface.getSwitchPort().getPortNumber()));

            // TODO: add TCP port number 179 into intent for BGP

            ShortestPathIntent fwdIntent = new ShortestPathIntent(fwdIntentId,
                    srcPort.getDpid().value(), srcPort.getPortNumber().value(),
                    ShortestPathIntent.EMPTYMACADDRESS, srcIP,
                    dstPort.getDpid().value(), dstPort.getPortNumber().value(),
                    ShortestPathIntent.EMPTYMACADDRESS, dstIP);
            ShortestPathIntent bwdIntent = new ShortestPathIntent(bwdIntentId,
                    dstPort.getDpid().value(), dstPort.getPortNumber().value(),
                    ShortestPathIntent.EMPTYMACADDRESS, dstIP,
                    srcPort.getDpid().value(), srcPort.getPortNumber().value(),
                    ShortestPathIntent.EMPTYMACADDRESS, srcIP);
            IntentOperation.Operator operator = IntentOperation.Operator.ADD;
            operations.add(operator, fwdIntent);
            operations.add(operator, bwdIntent);
        }
        pathRuntime.executeIntentOperations(operations);
    }

    /**
     * This method handles the prefixes which are waiting for ARP replies for
     * MAC addresses of next hops.
     *
     * @param ipAddress next hop router IP address, for which we sent ARP request out
     * @param macAddress MAC address which is relative to the ipAddress
     */
    @Override
    public void arpResponse(InetAddress ipAddress, MACAddress macAddress) {
        log.debug("Received ARP response: {} => {}",
                ipAddress.getHostAddress(), macAddress);

        /*
         * We synchronize on this to prevent changes to the InvertedRadixTree
         * while we're pushing intent. If the InvertedRadixTree changes, the
         * InvertedRadixTree and intent could get out of sync.
         */
        synchronized (this) {

          Set<RibUpdate> prefixesToPush = prefixesWaitingOnArp.removeAll(ipAddress);

            for (RibUpdate update : prefixesToPush) {
                // These will always be adds

                RibEntry rib = bgpRoutes.getValueForExactKey(
                        update.getPrefix().toBinaryString());
                if (rib != null && rib.equals(update.getRibEntry())) {
                    log.debug("Pushing prefix {} next hop {}", update.getPrefix(),
                            rib.getNextHop().getHostAddress());
                    // We only push prefix flows if the prefix is still in the
                    // InvertedRadixTree and the next hop is the same as our update.
                    // The prefix could have been removed while we were waiting
                    // for the ARP, or the next hop could have changed.
                    addRouteIntentToNextHop(update.getPrefix(), ipAddress,
                            macAddress);
                } else {
                    log.debug("Received ARP response, but {},{} is no longer in " +
                            "InvertedRadixTree", update.getPrefix(),
                            update.getRibEntry());
                }
            }
        }
    }


    /*private void setupArpFlows() {
        OFMatch match = new OFMatch();
        match.setDataLayerType(Ethernet.TYPE_ARP);
        match.setWildcards(match.getWildcards() & ~OFMatch.OFPFW_DL_TYPE);

        OFFlowMod fm = new OFFlowMod();
        fm.setMatch(match);

        OFActionOutput action = new OFActionOutput();
        action.setPort(OFPort.OFPP_CONTROLLER.getValue());
        action.setMaxLength((short) 0xffff);
        List<OFAction> actions = new ArrayList<>(1);
        actions.add(action);
        fm.setActions(actions);

        fm.setIdleTimeout((short) 0)
                .setHardTimeout((short) 0)
                .setBufferId(OFPacketOut.BUFFER_ID_NONE)
                .setCookie(0)
                .setCommand(OFFlowMod.OFPFC_ADD)
                .setPriority(ARP_PRIORITY)
                .setLengthU(OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH);

        for (String strdpid : switches) {
            flowCache.write(HexString.toLong(strdpid), fm);
        }
    }

    private void setupDefaultDropFlows() {
        OFFlowMod fm = new OFFlowMod();
        fm.setMatch(new OFMatch());
        fm.setActions(new ArrayList<OFAction>()); // No action means drop

        fm.setIdleTimeout((short) 0)
                .setHardTimeout((short) 0)
                .setBufferId(OFPacketOut.BUFFER_ID_NONE)
                .setCookie(0)
                .setCommand(OFFlowMod.OFPFC_ADD)
                .setPriority((short) 0)
                .setLengthU(OFFlowMod.MINIMUM_LENGTH);

        OFFlowMod fmLLDP;
        OFFlowMod fmBDDP;
        try {
            fmLLDP = fm.clone();
            fmBDDP = fm.clone();
        } catch (CloneNotSupportedException e1) {
            log.error("Error cloning flow mod", e1);
            return;
        }

        OFMatch matchLLDP = new OFMatch();
        matchLLDP.setDataLayerType((short) 0x88cc);
        matchLLDP.setWildcards(matchLLDP.getWildcards() & ~OFMatch.OFPFW_DL_TYPE);
        fmLLDP.setMatch(matchLLDP);

        OFMatch matchBDDP = new OFMatch();
        matchBDDP.setDataLayerType((short) 0x8942);
        matchBDDP.setWildcards(matchBDDP.getWildcards() & ~OFMatch.OFPFW_DL_TYPE);
        fmBDDP.setMatch(matchBDDP);

        OFActionOutput action = new OFActionOutput();
        action.setPort(OFPort.OFPP_CONTROLLER.getValue());
        action.setMaxLength((short) 0xffff);
        List<OFAction> actions = new ArrayList<>(1);
        actions.add(action);

        fmLLDP.setActions(actions);
        fmBDDP.setActions(actions);

        fmLLDP.setPriority(ARP_PRIORITY);
        fmLLDP.setLengthU(OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH);
        fmBDDP.setPriority(ARP_PRIORITY);
        fmBDDP.setLengthU(OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH);

        List<OFFlowMod> flowModList = new ArrayList<>(3);
        flowModList.add(fm);
        flowModList.add(fmLLDP);
        flowModList.add(fmBDDP);

        for (String strdpid : switches) {
            flowCache.write(HexString.toLong(strdpid), flowModList);
        }
    }*/

    /**
     * The SDN-IP application is started from this method.
     */
    @Override
    public void beginRouting() {
        log.debug("Topology is now ready, beginning routing function");

        // TODO
        /*setupArpFlows();
        setupDefaultDropFlows();*/

        setupBgpPaths();

        // Suppress link discovery on external-facing router ports
        for (Interface intf : interfaces.values()) {
            linkDiscoveryService.disableDiscoveryOnPort(intf.getDpid(), intf.getPort());
        }

        bgpUpdatesExecutor.execute(new Runnable() {
            @Override
            public void run() {
                doUpdatesThread();
            }
        });
    }

    /**
     * Thread for handling RIB updates.
     */
    private void doUpdatesThread() {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    RibUpdate update = ribUpdates.take();
                    switch (update.getOperation()) {
                    case UPDATE:
                        if (validateUpdate(update)) {
                            processRibAdd(update);
                        } else {
                            log.debug("Rib UPDATE out of order: {} via {}",
                                    update.getPrefix(), update.getRibEntry().getNextHop());
                        }
                        break;
                    case DELETE:
                        if (validateUpdate(update)) {
                            processRibDelete(update);
                        } else {
                            log.debug("Rib DELETE out of order: {} via {}",
                                    update.getPrefix(), update.getRibEntry().getNextHop());
                        }
                        break;
                    default:
                        log.error("Unknown operation {}", update.getOperation());
                        break;
                    }
                } catch (InterruptedException e) {
                    log.debug("Interrupted while taking from updates queue", e);
                    interrupted = true;
                } catch (Exception e) {
                    log.debug("exception", e);
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Judge whether a RIB update is in correct order.
     *
     * @param update RIB update
     * @return boolean whether the RIB update is in in correct order
     */
    private boolean validateUpdate(RibUpdate update) {
        RibEntry newEntry = update.getRibEntry();
        RibEntry oldEntry = bgpRoutes.getValueForExactKey(
                update.getPrefix().toBinaryString());

        // If there is no existing entry we must assume this is the most recent
        // update. However this might not always be the case as we might have a
        // POST then DELETE reordering.
        // if (oldEntry == null ||
        // !newEntry.getNextHop().equals(oldEntry.getNextHop())) {
        if (oldEntry == null) {
            return true;
        }

        // This handles the case where routes are gathered in the initial
        // request because they don't have sequence number info
        if (newEntry.getSysUpTime() == -1 && newEntry.getSequenceNum() == -1) {
            return true;
        }

        if (newEntry.getSysUpTime() > oldEntry.getSysUpTime()) {
            return true;
        }

        return newEntry.getSysUpTime() == oldEntry.getSysUpTime() &&
                newEntry.getSequenceNum() > oldEntry.getSequenceNum();
    }

    /**
     * To find the Interface which has longest matchable IP prefix (sub-network
     *  prefix) to next hop IP address.
     *
     * @param address the IP address of next hop router
     * @return Interface the Interface which has longest matchable IP prefix
     */
    private Interface longestInterfacePrefixMatch(InetAddress address) {
        Prefix prefixToSearchFor = new Prefix(address.getAddress(),
                Prefix.MAX_PREFIX_LENGTH);
        Iterator<Interface> it =
                interfaceRoutes.getValuesForKeysPrefixing(
                        prefixToSearchFor.toBinaryString()).iterator();
        Interface intf = null;
        // Find the last prefix, which will be the longest prefix
        while (it.hasNext()) {
            intf = it.next();
        }

        return intf;
    }

    /*
     * IConfigInfoService methods
     */

    @Override
    public boolean isInterfaceAddress(InetAddress address) {
        Interface intf = longestInterfacePrefixMatch(address);
        return (intf != null && intf.getIpAddress().equals(address));
    }

    @Override
    public boolean inConnectedNetwork(InetAddress address) {
        Interface intf = longestInterfacePrefixMatch(address);
        return (intf != null && !intf.getIpAddress().equals(address));
    }

    @Override
    public boolean fromExternalNetwork(long inDpid, short inPort) {
        for (Interface intf : interfaces.values()) {
            if (intf.getDpid() == inDpid && intf.getPort() == inPort) {
                return true;
            }
        }
        return false;
    }

    /**
     * To find the relative egress Interface for a next hop IP address.
     *
     * @param dstIpAddress the IP address of next hop router
     */
    @Override
    public Interface getOutgoingInterface(InetAddress dstIpAddress) {
        return longestInterfacePrefixMatch(dstIpAddress);
    }

    @Override
    public boolean hasLayer3Configuration() {
        return !interfaces.isEmpty();
    }

    @Override
    public MACAddress getRouterMacAddress() {
        return bgpdMacAddress;
    }

    @Override
    public short getVlan() {
        return vlan;
    }

    @Override
    public Set<SwitchPort> getExternalSwitchPorts() {
        return Collections.unmodifiableSet(externalNetworkSwitchPorts);
    }

}
