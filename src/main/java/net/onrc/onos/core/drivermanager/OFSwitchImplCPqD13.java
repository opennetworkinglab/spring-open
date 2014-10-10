package net.onrc.onos.core.drivermanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.floodlightcontroller.core.IOF13Switch;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.SwitchDriverSubHandshakeAlreadyStarted;
import net.floodlightcontroller.core.SwitchDriverSubHandshakeCompleted;
import net.floodlightcontroller.core.SwitchDriverSubHandshakeNotStarted;
import net.floodlightcontroller.core.internal.OFSwitchImplBase;
import net.floodlightcontroller.util.MACAddress;
import net.floodlightcontroller.util.OrderedCollection;
import net.onrc.onos.core.configmanager.INetworkConfigService;
import net.onrc.onos.core.configmanager.INetworkConfigService.NetworkConfigState;
import net.onrc.onos.core.configmanager.INetworkConfigService.SwitchConfigStatus;
import net.onrc.onos.core.configmanager.NetworkConfig.LinkConfig;
import net.onrc.onos.core.configmanager.NetworkConfig.SwitchConfig;
import net.onrc.onos.core.configmanager.NetworkConfigManager;
import net.onrc.onos.core.configmanager.PktLinkConfig;
import net.onrc.onos.core.configmanager.SegmentRouterConfig;
import net.onrc.onos.core.matchaction.MatchAction;
import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.matchaction.MatchActionOperations;
import net.onrc.onos.core.matchaction.MatchActionOperations.Operator;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.CopyTtlInAction;
import net.onrc.onos.core.matchaction.action.CopyTtlOutAction;
import net.onrc.onos.core.matchaction.action.DecMplsTtlAction;
import net.onrc.onos.core.matchaction.action.DecNwTtlAction;
import net.onrc.onos.core.matchaction.action.GroupAction;
import net.onrc.onos.core.matchaction.action.ModifyDstMacAction;
import net.onrc.onos.core.matchaction.action.ModifySrcMacAction;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.action.PopMplsAction;
import net.onrc.onos.core.matchaction.match.Ipv4Match;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.matchaction.match.MplsMatch;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.IPv4Net;
import net.onrc.onos.core.util.PortNumber;

import org.projectfloodlight.openflow.protocol.OFAsyncGetReply;
import org.projectfloodlight.openflow.protocol.OFBarrierRequest;
import org.projectfloodlight.openflow.protocol.OFBucket;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFGroupDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFGroupFeaturesStatsReply;
import org.projectfloodlight.openflow.protocol.OFGroupType;
import org.projectfloodlight.openflow.protocol.OFMatchV3;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFOxmList;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.match.Match.Builder;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEthDst;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEthSrc;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEthType;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmInPort;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmIpv4DstMasked;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmMplsLabel;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmVlanVid;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.util.HexString;

/**
 * OFDescriptionStatistics Vendor (Manufacturer Desc.): Stanford University,
 * Ericsson Research and CPqD Research. Make (Hardware Desc.) : OpenFlow 1.3
 * Reference Userspace Switch Model (Datapath Desc.) : None Software : Serial :
 * None
 */
public class OFSwitchImplCPqD13 extends OFSwitchImplBase implements IOF13Switch {
    private AtomicBoolean driverHandshakeComplete;
    private AtomicBoolean haltStateMachine;
    private OFFactory factory;
    private static final int OFPCML_NO_BUFFER = 0xffff;
    // Configuration of asynch messages to controller. We need different
    // asynch messages depending on role-equal or role-master.
    // We don't want to get anything if we are slave.
    private static final long SET_FLOW_REMOVED_MASK_MASTER = 0xf;
    private static final long SET_PACKET_IN_MASK_MASTER = 0x7;
    private static final long SET_PORT_STATUS_MASK_MASTER = 0x7;
    private static final long SET_FLOW_REMOVED_MASK_EQUAL = 0x0;
    private static final long SET_PACKET_IN_MASK_EQUAL = 0x0;
    private static final long SET_PORT_STATUS_MASK_EQUAL = 0x7;
    private static final long SET_ALL_SLAVE = 0x0;

    private static final long TEST_FLOW_REMOVED_MASK = 0xf;
    private static final long TEST_PACKET_IN_MASK = 0x7;
    private static final long TEST_PORT_STATUS_MASK = 0x7;

    private static final int TABLE_VLAN = 0;
    private static final int TABLE_TMAC = 1;
    private static final int TABLE_IPv4_UNICAST = 2;
    private static final int TABLE_MPLS = 3;
    private static final int TABLE_ACL = 5;

    private static final short MAX_PRIORITY = (short) 0xffff;
    private static final short PRIORITY_MULTIPLIER = (short) 2046;
    private static final short MIN_PRIORITY = 0x0;

    private long barrierXidToWaitFor = -1;
    private DriverState driverState;
    private final boolean usePipeline13;
    private SegmentRouterConfig srConfig;
    private ConcurrentMap<Dpid, Set<PortNumber>> neighbors;
    private ConcurrentMap<PortNumber, Dpid> portToNeighbors;
    private List<Integer> segmentIds;
    private boolean isEdgeRouter;
    private int sid;
    private ConcurrentMap<NeighborSet, EcmpInfo> ecmpGroups;
    private ConcurrentMap<PortNumber, ArrayList<NeighborSet>> portNeighborSetMap;
    private AtomicInteger groupid;



    public OFSwitchImplCPqD13(OFDescStatsReply desc, boolean usePipeline13) {
        super();
        haltStateMachine = new AtomicBoolean(false);
        driverState = DriverState.INIT;
        driverHandshakeComplete = new AtomicBoolean(false);
        setSwitchDescription(desc);
        neighbors = new ConcurrentHashMap<Dpid, Set<PortNumber>>();
        portToNeighbors = new ConcurrentHashMap<PortNumber, Dpid>();
        ecmpGroups = new ConcurrentHashMap<NeighborSet, EcmpInfo>();
        portNeighborSetMap =
                new ConcurrentHashMap<PortNumber, ArrayList<NeighborSet>>();
        segmentIds = new ArrayList<Integer>();
        isEdgeRouter = false;
        groupid = new AtomicInteger(0);
        this.usePipeline13 = usePipeline13;
    }

    // *****************************
    // OFSwitchImplBase
    // *****************************


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "OFSwitchImplCPqD13 [" + ((channel != null)
                ? channel.getRemoteAddress() : "?")
                + " DPID[" + ((stringId != null) ? stringId : "?") + "]]";
    }

    @Override
    public void startDriverHandshake() throws IOException {
        log.debug("Starting driver handshake for sw {}", getStringId());
        if (startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeAlreadyStarted();
        }
        startDriverHandshakeCalled = true;
        factory = getFactory();
        if (!usePipeline13) {
            // Send packet-in to controller if a packet misses the first table
            populateTableMissEntry(0, true, false, false, 0);
            driverHandshakeComplete.set(true);
        } else {
            nextDriverState();
        }
    }

    @Override
    public boolean isDriverHandshakeComplete() {
        if (!startDriverHandshakeCalled)
            throw new SwitchDriverSubHandshakeNotStarted();
        return driverHandshakeComplete.get();
    }

    @Override
    public void processDriverHandshakeMessage(OFMessage m) {
        if (!startDriverHandshakeCalled)
            throw new SwitchDriverSubHandshakeNotStarted();
        if (isDriverHandshakeComplete())
            throw new SwitchDriverSubHandshakeCompleted(m);
        try {
            processOFMessage(this, m);
        } catch (IOException e) {
            log.error("Error generated when processing OFMessage", e.getCause());
        }
    }

    @Override
    public String getSwitchDriverState() {
        return driverState.toString();
    }

    public void removePortFromGroups(PortNumber port) {
        /* FIX: removePortFromGroups is not working */

        log.debug("removePortFromGroups: Remove port {} from Switch {}",
                port, getStringId());
        ArrayList<NeighborSet> portNSSet = portNeighborSetMap.get(port);
        if (portNSSet == null)
        {
            /* No Groups are created with this port yet */
            log.warn("removePortFromGroups: No groups exist with Switch {} port {}",
                            getStringId(), port);
            return;
        }
        log.debug("removePortFromGroups: Neighborsets that the port {} is part"
                + "of on Switch {} are {}",
                port, getStringId(), portNSSet);

        for (NeighborSet ns : portNSSet) {
            /* Delete the first matched bucket */
            EcmpInfo portEcmpInfo = ecmpGroups.get(ns);
            Iterator<BucketInfo> it = portEcmpInfo.buckets.iterator();
            log.debug("removePortFromGroups: Group {} on Switch {} has {} buckets",
                    portEcmpInfo.groupId, getStringId(),
                    portEcmpInfo.buckets.size());
            while (it.hasNext()) {
                BucketInfo bucket = it.next();
                if (bucket.outport.equals(port)) {
                    it.remove();
                }
            }
            log.debug("removePortFromGroups: Modifying Group on Switch {} "
                    + "and Neighborset {} with {}",
                    getStringId(), ns, portEcmpInfo);
            modifyEcmpGroup(portEcmpInfo);
        }
        /* Don't delete the entry from portNeighborSetMap because
          * when the port is up again this info is needed
          */
        return;
    }

    public void addPortToGroups(PortNumber port) {
        log.debug("addPortToGroups: Add port {} to Switch {}",
                port, getStringId());
        ArrayList<NeighborSet> portNSSet = portNeighborSetMap.get(port);
        if (portNSSet == null) {
            /* Unknown Port  */
            log.warn("addPortToGroups: Switch {} port {} is unknown",
                            getStringId(), port);
            return;
        }
        log.debug("addPortToGroups: Neighborsets that the port {} is part"
                + "of on Switch {} are {}",
                port, getStringId(), portNSSet);

        Dpid neighborDpid = portToNeighbors.get(port);
        for (NeighborSet ns : portNSSet) {
            EcmpInfo portEcmpInfo = ecmpGroups.get(ns);
            /* Find if this port is already part of any bucket
             * in this group
             * NOTE: This is needed because in some cases
             * (such as for configured network nodes), both driver and
             * application detect the network elements and creates the
             * buckets in the same group. This check is to avoid
             * duplicate bucket creation in such scenarios
             */
            List<BucketInfo> buckets = portEcmpInfo.buckets;
            if (buckets == null) {
                buckets = new ArrayList<BucketInfo>();
                portEcmpInfo.buckets = buckets;
            } else {
                Iterator<BucketInfo> it = buckets.iterator();
                boolean matchingBucketExist = false;
                while (it.hasNext()) {
                    BucketInfo bucket = it.next();
                    if (bucket.outport.equals(port)) {
                        matchingBucketExist = true;
                        break;
                    }
                }
                if (matchingBucketExist) {
                    log.warn("addPortToGroups: On Switch {} duplicate "
                            + "portAdd is called for port {} with buckets {}",
                            getStringId(), port, buckets);
                    continue;
                }
            }
            BucketInfo b = new BucketInfo(neighborDpid,
                    MacAddress.of(srConfig.getRouterMac()),
                    getNeighborRouterMacAddress(neighborDpid),
                    port,
                    ns.getEdgeLabel());
            buckets.add(b);
            log.debug("addPortToGroups: Modifying Group on Switch {} "
                    + "and Neighborset {} with {}",
                    getStringId(), ns, portEcmpInfo);
            modifyEcmpGroup(portEcmpInfo);
        }
        return;
    }

    @Override
    public OrderedCollection<PortChangeEvent> processOFPortStatus(OFPortStatus ps) {
        OrderedCollection<PortChangeEvent> events = super.processOFPortStatus(ps);
        for (PortChangeEvent e : events) {
            switch (e.type) {
            case DELETE:
            case DOWN:
                log.debug("processOFPortStatus: sw {} Port {} DOWN",
                        getStringId(), e.port.getPortNo().getPortNumber());
                removePortFromGroups(PortNumber.uint32(
                        e.port.getPortNo().getPortNumber()));
                break;
            case UP:
                log.debug("processOFPortStatus: sw {} Port {} UP",
                        getStringId(), e.port.getPortNo().getPortNumber());
                addPortToGroups(PortNumber.uint32(
                        e.port.getPortNo().getPortNumber()));
            }
        }
        return events;
    }

    // *****************************
    // Driver handshake state-machine
    // *****************************

    enum DriverState {
        INIT,
        SET_TABLE_MISS_ENTRIES,
        SET_TABLE_VLAN_TMAC,
        SET_GROUPS,
        VERIFY_GROUPS,
        SET_ADJACENCY_LABELS,
        EXIT
    }

    protected void nextDriverState() throws IOException {
        DriverState currentState = driverState;
        if (haltStateMachine.get()) {
            return;
        }
        switch (currentState) {
        case INIT:
            driverState = DriverState.SET_TABLE_MISS_ENTRIES;
            setTableMissEntries();
            sendHandshakeBarrier();
            break;
        case SET_TABLE_MISS_ENTRIES:
            driverState = DriverState.SET_TABLE_VLAN_TMAC;
            getNetworkConfig();
            populateTableVlan();
            populateTableTMac();
            sendHandshakeBarrier();
            break;
        case SET_TABLE_VLAN_TMAC:
            driverState = DriverState.SET_GROUPS;
            createGroups();
            sendHandshakeBarrier();
            break;
        case SET_GROUPS:
            driverState = DriverState.VERIFY_GROUPS;
            verifyGroups();
            break;
        case VERIFY_GROUPS:
            driverState = DriverState.SET_ADJACENCY_LABELS;
            assignAdjacencyLabels();
            break;
        case SET_ADJACENCY_LABELS:
            driverState = DriverState.EXIT;
            driverHandshakeComplete.set(true);
            break;
        case EXIT:
        default:
            driverState = DriverState.EXIT;
            log.error("Driver handshake has exited for sw: {}", getStringId());
        }
    }

    void processOFMessage(IOFSwitch sw, OFMessage m) throws IOException {
        switch (m.getType()) {
        case BARRIER_REPLY:
            processBarrierReply(m);
            break;

        case ERROR:
            processErrorMessage(m);
            break;

        case GET_ASYNC_REPLY:
            OFAsyncGetReply asrep = (OFAsyncGetReply) m;
            decodeAsyncGetReply(asrep);
            break;

        case PACKET_IN:
            // not ready to handle packet-ins
            break;

        case QUEUE_GET_CONFIG_REPLY:
            // not doing queue config yet
            break;

        case STATS_REPLY:
            processStatsReply((OFStatsReply) m);
            break;

        case ROLE_REPLY: // channelHandler should handle this
        case PORT_STATUS: // channelHandler should handle this
        case FEATURES_REPLY: // don't care
        case FLOW_REMOVED: // don't care
        default:
            log.debug("Received message {} during switch-driver subhandshake "
                    + "from switch {} ... Ignoring message", m, sw.getStringId());
        }
    }

    private void processStatsReply(OFStatsReply sr) {
        switch (sr.getStatsType()) {
        case AGGREGATE:
            break;
        case DESC:
            break;
        case EXPERIMENTER:
            break;
        case FLOW:
            break;
        case GROUP_DESC:
            processGroupDesc((OFGroupDescStatsReply) sr);
            break;
        case GROUP_FEATURES:
            processGroupFeatures((OFGroupFeaturesStatsReply) sr);
            break;
        case METER_CONFIG:
            break;
        case METER_FEATURES:
            break;
        case PORT_DESC:
            break;
        case TABLE_FEATURES:
            break;
        default:
            break;

        }
    }

    private void processErrorMessage(OFMessage m) {
        log.error("Switch {} Error {} in DriverState", getStringId(),
                (OFErrorMsg) m, driverState);
    }

    private void processBarrierReply(OFMessage m) throws IOException {
        if (m.getXid() == barrierXidToWaitFor) {
            // Driver state-machine progresses to the next state.
            // If Barrier messages is not received, then eventually
            // the ChannelHandler state machine will timeout, and the switch
            // will be disconnected.
            nextDriverState();
        } else {
            log.error("Received incorrect barrier-message xid {} (expected: {}) in "
                    + "switch-driver state {} for switch {}", m, barrierXidToWaitFor,
                    driverState, getStringId());
        }
    }

    private void processGroupDesc(OFGroupDescStatsReply gdsr) {
        log.info("Sw: {} Group Desc {}", getStringId(), gdsr);
        try {
            nextDriverState();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // *****************************
    // Utility methods
    // *****************************

    void setTableMissEntries() throws IOException {
        // set all table-miss-entries
        populateTableMissEntry(TABLE_VLAN, true, false, false, -1);
        populateTableMissEntry(TABLE_TMAC, true, false, false, -1);
        populateTableMissEntry(TABLE_IPv4_UNICAST, false, true, true,
                TABLE_ACL);
        populateTableMissEntry(TABLE_MPLS, false, true, true,
                TABLE_ACL);
        populateTableMissEntry(TABLE_ACL, false, false, false, -1);
    }

    private void sendHandshakeBarrier() throws IOException {
        long xid = getNextTransactionId();
        barrierXidToWaitFor = xid;
        OFBarrierRequest br = getFactory()
                .buildBarrierRequest()
                .setXid(xid)
                .build();
        write(br, null);
    }

    /**
     * Adds a table-miss-entry to a pipeline table.
     * <p>
     * The table-miss-entry can be added with 'write-actions' or
     * 'apply-actions'. It can also add a 'goto-table' instruction. By default
     * if none of the booleans in the call are set, then the table-miss entry is
     * added with no instructions, which means that if a packet hits the
     * table-miss-entry, pipeline execution will stop, and the action set
     * associated with the packet will be executed.
     *
     * @param tableToAdd the table to where the table-miss-entry will be added
     * @param toControllerNow as an APPLY_ACTION instruction
     * @param toControllerWrite as a WRITE_ACTION instruction
     * @param toTable as a GOTO_TABLE instruction
     * @param tableToSend the table to send as per the GOTO_TABLE instruction it
     *        needs to be set if 'toTable' is true. Ignored of 'toTable' is
     *        false.
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private void populateTableMissEntry(int tableToAdd, boolean toControllerNow,
            boolean toControllerWrite,
            boolean toTable, int tableToSend) throws IOException {
        OFOxmList oxmList = OFOxmList.EMPTY;
        OFMatchV3 match = factory.buildMatchV3()
                .setOxmList(oxmList)
                .build();
        OFAction outc = factory.actions()
                .buildOutput()
                .setPort(OFPort.CONTROLLER)
                .setMaxLen(OFPCML_NO_BUFFER)
                .build();
        List<OFInstruction> instructions = new ArrayList<OFInstruction>();
        if (toControllerNow) {
            // table-miss instruction to send to controller immediately
            OFInstruction instr = factory.instructions()
                    .buildApplyActions()
                    .setActions(Collections.singletonList(outc))
                    .build();
            instructions.add(instr);
        }

        if (toControllerWrite) {
            // table-miss instruction to write-action to send to controller
            // this will be executed whenever the action-set gets executed
            OFInstruction instr = factory.instructions()
                    .buildWriteActions()
                    .setActions(Collections.singletonList(outc))
                    .build();
            instructions.add(instr);
        }

        if (toTable) {
            // table-miss instruction to goto-table x
            OFInstruction instr = factory.instructions()
                    .gotoTable(TableId.of(tableToSend));
            instructions.add(instr);
        }

        if (!toControllerNow && !toControllerWrite && !toTable) {
            // table-miss has no instruction - at which point action-set will be
            // executed - if there is an action to output/group in the action
            // set
            // the packet will be sent there, otherwise it will be dropped.
            instructions = (List<OFInstruction>) Collections.EMPTY_LIST;
        }

        OFMessage tableMissEntry = factory.buildFlowAdd()
                .setTableId(TableId.of(tableToAdd))
                .setMatch(match) // match everything
                .setInstructions(instructions)
                .setPriority(MIN_PRIORITY)
                .setBufferId(OFBufferId.NO_BUFFER)
                .setIdleTimeout(0)
                .setHardTimeout(0)
                .setXid(getNextTransactionId())
                .build();
        write(tableMissEntry, null);
    }

    private void getNetworkConfig() {
        INetworkConfigService ncs = floodlightProvider.getNetworkConfigService();
        SwitchConfigStatus scs = ncs.checkSwitchConfig(new Dpid(getId()));
        if (scs.getConfigState() == NetworkConfigState.ACCEPT_ADD) {
            srConfig = (SegmentRouterConfig) scs.getSwitchConfig();
            isEdgeRouter = srConfig.isEdgeRouter();
            sid = srConfig.getNodeSid();
        } else {
            log.error("Switch not configured as Segment-Router");
        }

        List<LinkConfig> linkConfigList = ncs.getConfiguredAllowedLinks();
        setNeighbors(linkConfigList);

        if (isEdgeRouter) {
            List<SwitchConfig> switchList = ncs.getConfiguredAllowedSwitches();
            getAllNodeSegmentIds(switchList);
        }
    }

    private void populateTableVlan() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        for (OFPortDesc p : getPorts()) {
            int pnum = p.getPortNo().getPortNumber();
            if (U32.of(pnum).compareTo(U32.of(OFPort.MAX.getPortNumber())) < 1) {
                OFOxmInPort oxp = factory.oxms().inPort(p.getPortNo());
                OFOxmVlanVid oxv = factory.oxms()
                        .vlanVid(OFVlanVidMatch.UNTAGGED);
                OFOxmList oxmList = OFOxmList.of(oxp, oxv);
                OFMatchV3 match = factory.buildMatchV3()
                        .setOxmList(oxmList).build();

                // TODO: match on vlan-tagged packets for vlans configured on
                // subnet ports and strip-vlan

                // Do not need to add vlans
                /*int vlanid = getVlanConfig(pnum);
                OFOxmVlanVid vidToSet = factory.oxms()
                        .vlanVid(OFVlanVidMatch.ofVlan(vlanid));
                OFAction pushVlan = factory.actions().pushVlan(EthType.VLAN_FRAME);
                OFAction setVlan = factory.actions().setField(vidToSet);
                List<OFAction> actionlist = new ArrayList<OFAction>();
                actionlist.add(pushVlan);
                actionlist.add(setVlan);
                OFInstruction appAction = factory.instructions().buildApplyActions()
                        .setActions(actionlist).build();*/

                OFInstruction gotoTbl = factory.instructions().buildGotoTable()
                        .setTableId(TableId.of(TABLE_TMAC)).build();
                List<OFInstruction> instructions = new ArrayList<OFInstruction>();
                // instructions.add(appAction);
                instructions.add(gotoTbl);
                OFMessage flowEntry = factory.buildFlowAdd()
                        .setTableId(TableId.of(TABLE_VLAN))
                        .setMatch(match)
                        .setInstructions(instructions)
                        .setPriority(1000) // does not matter - all rules
                                           // exclusive
                        .setBufferId(OFBufferId.NO_BUFFER)
                        .setIdleTimeout(0)
                        .setHardTimeout(0)
                        .setXid(getNextTransactionId())
                        .build();
                msglist.add(flowEntry);
            }
        }
        write(msglist);
        log.debug("Adding {} port/vlan-rules in sw {}", msglist.size(), getStringId());
    }

    private void populateTableTMac() throws IOException {
        // match for router-mac and ip-packets
        OFOxmEthType oxe = factory.oxms().ethType(EthType.IPv4);
        OFOxmEthDst dmac = factory.oxms().ethDst(getRouterMacAddr());
        OFOxmList oxmListIp = OFOxmList.of(dmac, oxe);
        OFMatchV3 matchIp = factory.buildMatchV3()
                .setOxmList(oxmListIp).build();
        OFInstruction gotoTblIp = factory.instructions().buildGotoTable()
                .setTableId(TableId.of(TABLE_IPv4_UNICAST)).build();
        List<OFInstruction> instructionsIp = Collections.singletonList(gotoTblIp);
        OFMessage ipEntry = factory.buildFlowAdd()
                .setTableId(TableId.of(TABLE_TMAC))
                .setMatch(matchIp)
                .setInstructions(instructionsIp)
                .setPriority(1000) // strict priority required lower than
                                   // multicastMac
                .setBufferId(OFBufferId.NO_BUFFER)
                .setIdleTimeout(0)
                .setHardTimeout(0)
                .setXid(getNextTransactionId())
                .build();

        // match for router-mac and mpls packets
        OFOxmEthType oxmpls = factory.oxms().ethType(EthType.MPLS_UNICAST);
        OFOxmList oxmListMpls = OFOxmList.of(dmac, oxmpls);
        OFMatchV3 matchMpls = factory.buildMatchV3()
                .setOxmList(oxmListMpls).build();
        OFInstruction gotoTblMpls = factory.instructions().buildGotoTable()
                .setTableId(TableId.of(TABLE_MPLS)).build();
        List<OFInstruction> instructionsMpls = Collections.singletonList(gotoTblMpls);
        OFMessage mplsEntry = factory.buildFlowAdd()
                .setTableId(TableId.of(TABLE_TMAC))
                .setMatch(matchMpls)
                .setInstructions(instructionsMpls)
                .setPriority(1001) // strict priority required lower than
                                   // multicastMac
                .setBufferId(OFBufferId.NO_BUFFER)
                .setIdleTimeout(0)
                .setHardTimeout(0)
                .setXid(getNextTransactionId())
                .build();

        log.debug("Adding termination-mac-rules in sw {}", getStringId());
        List<OFMessage> msglist = new ArrayList<OFMessage>(2);
        msglist.add(ipEntry);
        msglist.add(mplsEntry);
        write(msglist);
    }

    private MacAddress getRouterMacAddr() {
        if (srConfig != null) {
            return MacAddress.of(srConfig.getRouterMac());
        } else {
            // return a dummy mac address - it will not be used
            return MacAddress.of("00:00:00:00:00:00");
        }
    }

    private boolean isEdgeRouter(Dpid ndpid) {
        INetworkConfigService ncs = floodlightProvider.getNetworkConfigService();
        SwitchConfigStatus scs = ncs.checkSwitchConfig(ndpid);
        if (scs.getConfigState() == NetworkConfigState.ACCEPT_ADD) {
            return ((SegmentRouterConfig) scs.getSwitchConfig()).isEdgeRouter();
        } else {
            // TODO: return false if router not allowed
            return false;
        }
    }

    private MacAddress getNeighborRouterMacAddress(Dpid ndpid) {
        INetworkConfigService ncs = floodlightProvider.getNetworkConfigService();
        SwitchConfigStatus scs = ncs.checkSwitchConfig(ndpid);
        if (scs.getConfigState() == NetworkConfigState.ACCEPT_ADD) {
            return MacAddress.of(((SegmentRouterConfig) scs.getSwitchConfig())
                    .getRouterMac());
        } else {
            // return a dummy mac address - it will not be used
            return MacAddress.of("00:00:00:00:00:00");
        }
    }

    private void setNeighbors(List<LinkConfig> linkConfigList) {
        for (LinkConfig lg : linkConfigList) {
            if (!lg.getType().equals(NetworkConfigManager.PKT_LINK)) {
                continue;
            }
            PktLinkConfig plg = (PktLinkConfig) lg;
            if (plg.getDpid1() == getId()) {
                addNeighborAtPort(new Dpid(plg.getDpid2()),
                        PortNumber.uint32(plg.getPort1()));
            } else if (plg.getDpid2() == getId()) {
                addNeighborAtPort(new Dpid(plg.getDpid1()),
                        PortNumber.uint32(plg.getPort2()));
            }
        }
    }

    private void addNeighborAtPort(Dpid neighborDpid, PortNumber portToNeighbor) {
        /* Update NeighborToPort database */
        if (neighbors.get(neighborDpid) != null) {
            neighbors.get(neighborDpid).add(portToNeighbor);
        } else {
            Set<PortNumber> ports = new HashSet<PortNumber>();
            ports.add(portToNeighbor);
            neighbors.put(neighborDpid, ports);
        }

        /* Update portToNeighbors database */
        if (portToNeighbors.get(portToNeighbor) == null)
            portToNeighbors.put(portToNeighbor, neighborDpid);
    }

    private void getAllNodeSegmentIds(List<SwitchConfig> switchList) {
        for (SwitchConfig sc : switchList) {
            /* TODO: Do we need to check if the SwitchConfig is of
             * type SegmentRouter?
             */
            if (sc.getDpid() == getId()) {
                continue;
            }
            segmentIds.add(((SegmentRouterConfig) sc).getNodeSid());
        }
        log.debug("getAllNodeSegmentIds: at sw {} are {}",
                getStringId(), segmentIds);
    }

    private boolean isSegmentIdSameAsNodeSegmentId(Dpid dpid, int sId) {
        INetworkConfigService ncs = floodlightProvider.getNetworkConfigService();
        SwitchConfigStatus scs = ncs.checkSwitchConfig(dpid);
        if (scs.getConfigState() == NetworkConfigState.ACCEPT_ADD) {
            return (((SegmentRouterConfig) scs.getSwitchConfig()).
                    getNodeSid() == sId);
        } else {
            // TODO: return false if router not allowed
            return false;
        }
    }

    private Set<Set<Dpid>> getAllNeighborSets(Set<Dpid> neighbors) {
        List<Dpid> list = new ArrayList<Dpid>(neighbors);
        Set<Set<Dpid>> sets = new HashSet<Set<Dpid>>();
        /* get the number of elements in the neighbors */
        int elements = list.size();
        /* the number of members of a power set is 2^n
         * including the empty set
         */
        int powerElements = (1 << elements);

        /* run a binary counter for the number of power elements */
        for (long i = 1; i < powerElements; i++) {
            Set<Dpid> dpidSubSet = new HashSet<Dpid>();
            for (int j = 0; j < elements; j++) {
                if ((i >> j) % 2 == 1) {
                    dpidSubSet.add(list.get(j));
                }
            }
            /* NOTE: Avoid any pairings of edge routers only
             * at a backbone router */
            boolean avoidEdgeRouterPairing = true;
            if ((!isEdgeRouter) && (dpidSubSet.size() > 1)) {
                for (Dpid dpid : dpidSubSet) {
                    if (!isEdgeRouter(dpid)) {
                        avoidEdgeRouterPairing = false;
                        break;
                    }
                }
            }
            else
                avoidEdgeRouterPairing = false;

            if (!avoidEdgeRouterPairing)
                sets.add(dpidSubSet);
        }
        return sets;
    }

    private void createGroupForANeighborSet(NeighborSet ns, int groupId) {
        List<BucketInfo> buckets = new ArrayList<BucketInfo>();
        for (Dpid d : ns.getDpids()) {
            for (PortNumber sp : neighbors.get(d)) {
                BucketInfo b = new BucketInfo(d,
                        MacAddress.of(srConfig.getRouterMac()),
                        getNeighborRouterMacAddress(d), sp,
                        ns.getEdgeLabel());
                buckets.add(b);

                /* Update Port Neighborset map */
                ArrayList<NeighborSet> portNeighborSets =
                        portNeighborSetMap.get(sp);
                if (portNeighborSets == null) {
                    portNeighborSets = new ArrayList<NeighborSet>();
                    portNeighborSets.add(ns);
                    portNeighborSetMap.put(sp, portNeighborSets);
                }
                else
                    portNeighborSets.add(ns);
            }
        }
        EcmpInfo ecmpInfo = new EcmpInfo(groupId, buckets);
        setEcmpGroup(ecmpInfo);
        ecmpGroups.put(ns, ecmpInfo);
        log.debug(
                "createGroupForANeighborSet: Creating ecmp group {} in sw {} "
                        + "for neighbor set {} with: {}",
                groupId, getStringId(), ns, ecmpInfo);
        return;
    }

    /**
     * createGroups creates ECMP groups for all ports on this router connected
     * to other routers (in the OF network). The information for ports is
     * gleaned from the configured links. If no links are configured no groups
     * will be created, and it is up to the caller of the IOF13Switch API to
     * create groups.
     * <p>
     * By default all ports connected to the same neighbor router will be part
     * of the same ECMP group. In addition, groups will be created for all
     * possible combinations of neighbor routers.
     * <p>
     * For example, consider this router (R0) connected to 3 neighbors (R1, R2,
     * and R3). The following groups will be created in R0:
     * <li>1) all ports to R1,
     * <li>2) all ports to R2,
     * <li>3) all ports to R3,
     * <li>4) all ports to R1 and R2
     * <li>5) all ports to R1 and R3
     * <li>6) all ports to R2 and R3
     * <li>7) all ports to R1, R2, and R3
     */
    private void createGroups() {
        Set<Dpid> dpids = neighbors.keySet();
        if (dpids == null || dpids.isEmpty()) {
            return;
        }
        /* Create all possible Neighbor sets from this router
         * NOTE: Avoid any pairings of edge routers only
         */
        Set<Set<Dpid>> powerSet = getAllNeighborSets(dpids);
        log.debug("createGroups: The size of neighbor powerset for sw {} is {}",
                getStringId(), powerSet.size());
        Set<NeighborSet> nsSet = new HashSet<NeighborSet>();
        for (Set<Dpid> combo : powerSet) {
            if (combo.isEmpty())
                continue;
            if (isEdgeRouter && !segmentIds.isEmpty()) {
                for (Integer sId : segmentIds) {
                    NeighborSet ns = new NeighborSet();
                    ns.addDpids(combo);
                    /* Check if the edge label being set is of the
                     * same node in the Neighbor set
                     */
                    if ((combo.size() != 1) ||
                            (!isSegmentIdSameAsNodeSegmentId(
                                    combo.iterator().next(), sId))) {
                        ns.setEdgeLabel(sId);
                    }
                    nsSet.add(ns);
                }
            } else {
                NeighborSet ns = new NeighborSet();
                ns.addDpids(combo);
                nsSet.add(ns);
            }
        }
        log.debug("createGroups: The neighborset with label for sw {} is {}",
                getStringId(), nsSet);

        for (NeighborSet ns : nsSet) {
            createGroupForANeighborSet(ns, groupid.incrementAndGet());
        }
    }

    private class EcmpInfo {
        int groupId;
        List<BucketInfo> buckets;

        EcmpInfo(int gid, List<BucketInfo> bucketInfos) {
            groupId = gid;
            buckets = bucketInfos;
        }

        @Override
        public String toString() {
            return "groupId: " + groupId + ", buckets: " + buckets;
        }
    }

    private class BucketInfo {
        Dpid neighborDpid;
        MacAddress srcMac;
        MacAddress dstMac;
        PortNumber outport;
        int mplsLabel;

        BucketInfo(Dpid nDpid, MacAddress smac, MacAddress dmac,
                PortNumber p, int label) {
            neighborDpid = nDpid;
            srcMac = smac;
            dstMac = dmac;
            outport = p;
            mplsLabel = label;
        }

        @Override
        public String toString() {
            return " {neighborDpid: " + neighborDpid + ", dstMac: " + dstMac +
                    ", srcMac: " + srcMac + ", outport: " + outport +
                    "mplsLabel: " + mplsLabel + "}";
        }
    }

    private void setEcmpGroup(EcmpInfo ecmpInfo) {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        OFGroup group = OFGroup.of(ecmpInfo.groupId);

        List<OFBucket> buckets = new ArrayList<OFBucket>();
        for (BucketInfo b : ecmpInfo.buckets) {
            OFOxmEthDst dmac = factory.oxms()
                    .ethDst(b.dstMac);
            OFAction setDA = factory.actions().buildSetField()
                    .setField(dmac).build();
            OFOxmEthSrc smac = factory.oxms()
                    .ethSrc(b.srcMac);
            OFAction setSA = factory.actions().buildSetField()
                    .setField(smac).build();
            OFAction outp = factory.actions().buildOutput()
                    .setPort(OFPort.of(b.outport.shortValue()))
                    .build();
            List<OFAction> actions = new ArrayList<OFAction>();
            actions.add(setSA);
            actions.add(setDA);
            actions.add(outp);
            if (b.mplsLabel != -1) {
                OFAction pushLabel = factory.actions().buildPushMpls()
                        .setEthertype(EthType.MPLS_UNICAST).build();
                OFOxmMplsLabel lid = factory.oxms()
                        .mplsLabel(U32.of(b.mplsLabel));
                OFAction setLabel = factory.actions().buildSetField()
                        .setField(lid).build();
                OFAction copyTtl = factory.actions().copyTtlOut();
                OFAction decrTtl = factory.actions().decMplsTtl();
                actions.add(pushLabel);
                actions.add(setLabel);
                actions.add(copyTtl);
                actions.add(decrTtl);
            }
            OFBucket ofb = factory.buildBucket()
                    .setWeight(1)
                    .setActions(actions)
                    .build();
            buckets.add(ofb);
        }

        OFMessage gm = factory.buildGroupAdd()
                .setGroup(group)
                .setBuckets(buckets)
                .setGroupType(OFGroupType.SELECT)
                .setXid(getNextTransactionId())
                .build();
        msglist.add(gm);
        try {
            write(msglist);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void modifyEcmpGroup(EcmpInfo ecmpInfo) {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        OFGroup group = OFGroup.of(ecmpInfo.groupId);

        List<OFBucket> buckets = new ArrayList<OFBucket>();
        for (BucketInfo b : ecmpInfo.buckets) {
            OFOxmEthDst dmac = factory.oxms()
                    .ethDst(b.dstMac);
            OFAction setDA = factory.actions().buildSetField()
                    .setField(dmac).build();
            OFOxmEthSrc smac = factory.oxms()
                    .ethSrc(b.srcMac);
            OFAction setSA = factory.actions().buildSetField()
                    .setField(smac).build();
            OFAction outp = factory.actions().buildOutput()
                    .setPort(OFPort.of(b.outport.shortValue()))
                    .build();
            List<OFAction> actions = new ArrayList<OFAction>();
            actions.add(setSA);
            actions.add(setDA);
            actions.add(outp);
            if (b.mplsLabel != -1) {
                OFAction pushLabel = factory.actions().buildPushMpls()
                        .setEthertype(EthType.MPLS_UNICAST).build();
                OFOxmMplsLabel lid = factory.oxms()
                        .mplsLabel(U32.of(b.mplsLabel));
                OFAction setLabel = factory.actions().buildSetField()
                        .setField(lid).build();
                OFAction copyTtl = factory.actions().copyTtlOut();
                OFAction decrTtl = factory.actions().decMplsTtl();
                actions.add(pushLabel);
                actions.add(setLabel);
                actions.add(copyTtl);
                actions.add(decrTtl);
            }
            OFBucket ofb = factory.buildBucket()
                    .setWeight(1)
                    .setActions(actions)
                    .build();
            buckets.add(ofb);
        }

        OFMessage gm = factory.buildGroupModify()
                .setGroup(group)
                .setBuckets(buckets)
                .setGroupType(OFGroupType.SELECT)
                .setXid(getNextTransactionId())
                .build();
        msglist.add(gm);
        try {
            write(msglist);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void verifyGroups() throws IOException {
        sendGroupDescRequest();
    }

    private void sendGroupDescRequest() throws IOException {
        OFMessage gdr = factory.buildGroupDescStatsRequest()
                .setXid(getNextTransactionId())
                .build();
        write(gdr, null);
    }

    private void assignAdjacencyLabels() {
        // TODO
        try {
            nextDriverState();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private OFAction getOFAction(Action action) {
        OFAction ofAction = null;
        if (action instanceof OutputAction) {
            OutputAction outputAction = (OutputAction) action;
            OFPort port = OFPort.of((int) outputAction.getPortNumber().value());
            ofAction = factory.actions().output(port, Short.MAX_VALUE);
        } else if (action instanceof ModifyDstMacAction) {
            long dstMac = ((ModifyDstMacAction) action).getDstMac().toLong();
            OFOxmEthDst dmac = factory.oxms()
                    .ethDst(MacAddress.of(dstMac));
            ofAction = factory.actions().buildSetField()
                    .setField(dmac).build();
        } else if (action instanceof ModifySrcMacAction) {
            long srcMac = ((ModifySrcMacAction) action).getSrcMac().toLong();
            OFOxmEthSrc smac = factory.oxms()
                    .ethSrc(MacAddress.of(srcMac));
            ofAction = factory.actions().buildSetField()
                    .setField(smac).build();
            /*} else if (action instanceof PushMplsAction) {
                ofAction = factory.actions().pushMpls(EthType.MPLS_UNICAST);
            } else if (action instanceof SetMplsIdAction) {
                int labelid = ((SetMplsIdAction) action).getMplsId();
                OFOxmMplsLabel lid = factory.oxms()
                        .mplsLabel(U32.of(labelid));
                ofAction = factory.actions().buildSetField()
                        .setField(lid).build();
            */} else if (action instanceof PopMplsAction) {
            EthType ethertype = ((PopMplsAction) action).getEthType();
            ofAction = factory.actions().popMpls(ethertype);
        } else if (action instanceof GroupAction) {
            NeighborSet ns = ((GroupAction) action).getDpids();
            EcmpInfo ei = ecmpGroups.get(ns);
            if (ei != null) {
                int gid = ei.groupId;
                ofAction = factory.actions().buildGroup()
                        .setGroup(OFGroup.of(gid))
                        .build();
            } else {
                log.debug("Unable to find ecmp group for neighbors {} at "
                        + "switch {} and hence creating it", ns, getStringId());
                createGroupForANeighborSet(ns, groupid.incrementAndGet());
            }
        } else if (action instanceof DecNwTtlAction) {
            ofAction = factory.actions().decNwTtl();
        } else if (action instanceof DecMplsTtlAction) {
            ofAction = factory.actions().decMplsTtl();
        } else if (action instanceof CopyTtlInAction) {
            ofAction = factory.actions().copyTtlIn();
        } else if (action instanceof CopyTtlOutAction) {
            ofAction = factory.actions().copyTtlOut();
        } else {
            log.warn("Unsupported Action type: {}", action.getClass().getName());
            return null;
        }

        // not supported by loxigen
        // OFAction setBos =
        // factory.actions().buildSetField().setField(bos).build();

        return ofAction;
    }

    private OFMessage getIpEntry(MatchActionOperationEntry mao) {
        MatchAction ma = mao.getTarget();
        Operator op = mao.getOperator();
        Ipv4Match ipm = (Ipv4Match) ma.getMatch();

        // set match
        IPv4Net ipdst = ipm.getDestination();
        OFOxmEthType ethTypeIp = factory.oxms()
                .ethType(EthType.IPv4);
        OFOxmIpv4DstMasked ipPrefix = factory.oxms()
                .ipv4DstMasked(
                        IPv4Address.of(ipdst.address().value()),
                        IPv4Address.ofCidrMaskLength(ipdst.prefixLen())
                );
        OFOxmList oxmList = OFOxmList.of(ethTypeIp, ipPrefix);
        OFMatchV3 match = factory.buildMatchV3()
                .setOxmList(oxmList).build();

        // set actions
        List<OFAction> writeActions = new ArrayList<OFAction>();
        for (Action action : ma.getActions()) {
            OFAction ofAction = getOFAction(action);
            if (ofAction != null) {
                writeActions.add(ofAction);
            }
        }

        // set instructions
        OFInstruction writeInstr = factory.instructions().buildWriteActions()
                .setActions(writeActions).build();
        OFInstruction gotoInstr = factory.instructions().buildGotoTable()
                .setTableId(TableId.of(TABLE_ACL)).build();
        List<OFInstruction> instructions = new ArrayList<OFInstruction>();
        instructions.add(writeInstr);
        instructions.add(gotoInstr);

        // set flow priority to emulate longest prefix match
        int priority = ipdst.prefixLen() * PRIORITY_MULTIPLIER;
        if (ipdst.prefixLen() == (short) 32) {
            priority = MAX_PRIORITY;
        }

        // set flow-mod
        OFFlowMod.Builder fmBuilder = null;
        switch (op) {
        case ADD:
            fmBuilder = factory.buildFlowAdd();
            break;
        case REMOVE:
            fmBuilder = factory.buildFlowDeleteStrict();
            break;
        case MODIFY: // TODO
            fmBuilder = factory.buildFlowModifyStrict();
            break;
        default:
            log.warn("Unsupported MatchAction Operator: {}", op);
            return null;
        }
        OFMessage ipFlow = fmBuilder
                .setTableId(TableId.of(TABLE_IPv4_UNICAST))
                .setMatch(match)
                .setInstructions(instructions)
                .setPriority(priority)
                .setBufferId(OFBufferId.NO_BUFFER)
                .setIdleTimeout(0)
                .setHardTimeout(0)
                .setXid(getNextTransactionId())
                .build();
        log.debug("{} ip-rule {}-{} in sw {}",
                (op == MatchActionOperations.Operator.ADD) ? "Adding" : "Deleting",
                match, writeActions,
                getStringId());
        return ipFlow;
    }

    private OFMessage getMplsEntry(MatchActionOperationEntry mao) {
        MatchAction ma = mao.getTarget();
        Operator op = mao.getOperator();
        MplsMatch mplsm = (MplsMatch) ma.getMatch();

        // set match
        OFOxmEthType ethTypeMpls = factory.oxms()
                .ethType(EthType.MPLS_UNICAST);
        OFOxmMplsLabel labelid = factory.oxms()
                .mplsLabel(U32.of(mplsm.getMplsLabel()));
        OFOxmList oxmList = OFOxmList.of(ethTypeMpls, labelid);
        OFMatchV3 matchlabel = factory.buildMatchV3()
                .setOxmList(oxmList).build();

        // set actions
        List<OFAction> writeActions = new ArrayList<OFAction>();
        for (Action action : ma.getActions()) {
            OFAction ofAction = getOFAction(action);
            if (ofAction != null) {
                writeActions.add(ofAction);
            }
        }

        // set instructions
        OFInstruction writeInstr = factory.instructions().buildWriteActions()
                .setActions(writeActions).build();
        OFInstruction gotoInstr = factory.instructions().buildGotoTable()
                .setTableId(TableId.of(TABLE_ACL)).build();
        List<OFInstruction> instructions = new ArrayList<OFInstruction>();
        instructions.add(writeInstr);
        instructions.add(gotoInstr);

        // set flow-mod
        OFFlowMod.Builder fmBuilder = null;
        switch (op) {
        case ADD:
            fmBuilder = factory.buildFlowAdd();
            break;
        case REMOVE:
            fmBuilder = factory.buildFlowDeleteStrict();
            break;
         case MODIFY: // TODO
            fmBuilder = factory.buildFlowModifyStrict();
            break;
        default:
            log.warn("Unsupported MatchAction Operator: {}", op);
            return null;
        }

        OFMessage mplsFlow = fmBuilder
                .setTableId(TableId.of(TABLE_MPLS))
                .setMatch(matchlabel)
                .setInstructions(instructions)
                .setPriority(MAX_PRIORITY) // exact match and exclusive
                .setBufferId(OFBufferId.NO_BUFFER)
                .setIdleTimeout(0)
                .setHardTimeout(0)
                .setXid(getNextTransactionId())
                .build();
        log.debug("{} mpls-rule {}-{} in sw {}",
                (op == MatchActionOperations.Operator.ADD) ? "Adding" : "Deleting",
                matchlabel, writeActions,
                getStringId());
        return mplsFlow;
    }

    private OFMessage getAclEntry(MatchActionOperationEntry mao) {
        MatchAction ma = mao.getTarget();
        Operator op = mao.getOperator();
        PacketMatch packetMatch = (PacketMatch) ma.getMatch();
        Builder matchBuilder = factory.buildMatch();

        // set match
        int inport = 0;
        if (ma.getSwitchPort() != null) {
            inport = (int) ma.getSwitchPort().getPortNumber().value();
        }
        final MACAddress srcMac = packetMatch.getSrcMacAddress();
        final MACAddress dstMac = packetMatch.getDstMacAddress();
        final Short etherType = packetMatch.getEtherType();
        final IPv4Net srcIp = packetMatch.getSrcIpAddress();
        final IPv4Net dstIp = packetMatch.getDstIpAddress();
        final Byte ipProto = packetMatch.getIpProtocolNumber();
        final Short srcTcpPort = packetMatch.getSrcTcpPortNumber();
        final Short dstTcpPort = packetMatch.getDstTcpPortNumber();
        if (inport > 0) {
            matchBuilder.setExact(MatchField.IN_PORT,
                    OFPort.of(inport));
        }
        if (srcMac != null) {
            matchBuilder.setExact(MatchField.ETH_SRC, MacAddress.of(srcMac.toLong()));
        }
        if (dstMac != null) {
            matchBuilder.setExact(MatchField.ETH_DST, MacAddress.of(dstMac.toLong()));
        }
        if (etherType != null) {
            matchBuilder.setExact(MatchField.ETH_TYPE, EthType.of(etherType));
        }
        if (srcIp != null) {
            matchBuilder.setMasked(MatchField.IPV4_SRC,
                    IPv4Address.of(srcIp.address().value())
                            .withMaskOfLength(srcIp.prefixLen()));
        }
        if (dstIp != null) {
            matchBuilder.setMasked(MatchField.IPV4_DST,
                    IPv4Address.of(dstIp.address().value())
                            .withMaskOfLength(dstIp.prefixLen()));
        }
        if (ipProto != null) {
            matchBuilder.setExact(MatchField.IP_PROTO, IpProtocol.of(ipProto));
        }
        if (srcTcpPort != null) {
            matchBuilder.setExact(MatchField.TCP_SRC, TransportPort.of(srcTcpPort));
        }
        if (dstTcpPort != null) {
            matchBuilder.setExact(MatchField.TCP_DST, TransportPort.of(dstTcpPort));
        }

        // set actions
        List<OFAction> applyActions = new ArrayList<OFAction>();
        for (Action action : ma.getActions()) {
            OFAction ofAction = getOFAction(action);
            if (ofAction != null) {
                applyActions.add(ofAction);
            }
        }

        // set instructions
        OFInstruction clearInstr = factory.instructions().clearActions();
        OFInstruction applyInstr = factory.instructions().buildApplyActions()
                .setActions(applyActions).build();
        List<OFInstruction> instructions = new ArrayList<OFInstruction>();
        instructions.add(clearInstr);
        instructions.add(applyInstr);

        // set flow-mod
        OFFlowMod.Builder fmBuilder = null;
        switch (op) {
        case ADD:
            fmBuilder = factory.buildFlowAdd();
            break;
        case REMOVE:
            fmBuilder = factory.buildFlowDeleteStrict();
            break;
        case MODIFY: // TODO
            fmBuilder = factory.buildFlowModifyStrict();
            break;
        default:
            log.warn("Unsupported MatchAction Operator: {}", op);
            return null;
        }

        OFMessage aclFlow = fmBuilder
                .setTableId(TableId.of(TABLE_ACL))
                .setMatch(matchBuilder.build())
                .setInstructions(instructions)
                .setPriority(MAX_PRIORITY / 2) // TODO: wrong - should be MA
                                               // priority
                .setBufferId(OFBufferId.NO_BUFFER)
                .setIdleTimeout(0)
                .setHardTimeout(0)
                .setXid(getNextTransactionId())
                .build();
        return aclFlow;
    }

    // *****************************
    // IOF13Switch
    // *****************************

    @Override
    public void pushFlow(MatchActionOperationEntry matchActionOp) throws IOException {
        OFMessage ofm = getFlow(matchActionOp);
        if (ofm != null) {
            write(Collections.singletonList(ofm));
        }
    }

    private OFMessage getFlow(MatchActionOperationEntry matchActionOp) {
        final MatchAction matchAction = matchActionOp.getTarget();
        final Match match = matchAction.getMatch();
        if (match instanceof Ipv4Match) {
            return getIpEntry(matchActionOp);
        } else if (match instanceof MplsMatch) {
            return getMplsEntry(matchActionOp);
        } else if (match instanceof PacketMatch) {
            return getAclEntry(matchActionOp);
        } else {
            log.error("Unknown match type {} pushed to switch {}", match,
                    getStringId());
        }
        return null;
    }

    @Override
    public void pushFlows(Collection<MatchActionOperationEntry> matchActionOps)
            throws IOException {
        List<OFMessage> flowMods = new ArrayList<OFMessage>();
        for (MatchActionOperationEntry matchActionOp : matchActionOps) {
            OFMessage ofm = getFlow(matchActionOp);
            if (ofm != null) {
                flowMods.add(ofm);
            }
        }
        write(flowMods);
    }

    @Override
    public int getEcmpGroupId(NeighborSet ns) {
        EcmpInfo ei = ecmpGroups.get(ns);
        if (ei == null) {
            return -1;
        } else {
            return ei.groupId;
        }
    }

    // *****************************
    // Unused
    // *****************************

    @SuppressWarnings("unused")
    private void setAsyncConfig() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>(3);
        OFMessage setAC = null;

        if (role == Role.MASTER) {
            setAC = factory.buildAsyncSet()
                    .setFlowRemovedMaskEqualMaster(SET_FLOW_REMOVED_MASK_MASTER)
                    .setPacketInMaskEqualMaster(SET_PACKET_IN_MASK_MASTER)
                    .setPortStatusMaskEqualMaster(SET_PORT_STATUS_MASK_MASTER)
                    .setFlowRemovedMaskSlave(SET_ALL_SLAVE)
                    .setPacketInMaskSlave(SET_ALL_SLAVE)
                    .setPortStatusMaskSlave(SET_ALL_SLAVE)
                    .setXid(getNextTransactionId())
                    .build();
        } else if (role == Role.EQUAL) {
            setAC = factory.buildAsyncSet()
                    .setFlowRemovedMaskEqualMaster(SET_FLOW_REMOVED_MASK_EQUAL)
                    .setPacketInMaskEqualMaster(SET_PACKET_IN_MASK_EQUAL)
                    .setPortStatusMaskEqualMaster(SET_PORT_STATUS_MASK_EQUAL)
                    .setFlowRemovedMaskSlave(SET_ALL_SLAVE)
                    .setPacketInMaskSlave(SET_ALL_SLAVE)
                    .setPortStatusMaskSlave(SET_ALL_SLAVE)
                    .setXid(getNextTransactionId())
                    .build();
        }
        msglist.add(setAC);

        OFMessage br = factory.buildBarrierRequest()
                .setXid(getNextTransactionId())
                .build();
        msglist.add(br);

        OFMessage getAC = factory.buildAsyncGetRequest()
                .setXid(getNextTransactionId())
                .build();
        msglist.add(getAC);

        write(msglist);
    }

    @SuppressWarnings("unused")
    private void decodeAsyncGetReply(OFAsyncGetReply rep) {
        long frm = rep.getFlowRemovedMaskEqualMaster();
        long frs = rep.getFlowRemovedMaskSlave();
        long pim = rep.getPacketInMaskEqualMaster();
        long pis = rep.getPacketInMaskSlave();
        long psm = rep.getPortStatusMaskEqualMaster();
        long pss = rep.getPortStatusMaskSlave();

        if (role == Role.MASTER || role == Role.EQUAL) { // should separate
            log.info("FRM:{}", HexString.toHexString((frm & TEST_FLOW_REMOVED_MASK)));
            log.info("PIM:{}", HexString.toHexString((pim & TEST_PACKET_IN_MASK)));
            log.info("PSM:{}", HexString.toHexString((psm & TEST_PORT_STATUS_MASK)));
        }

    }

    @SuppressWarnings("unused")
    private void getTableFeatures() throws IOException {
        OFMessage gtf = factory.buildTableFeaturesStatsRequest()
                .setXid(getNextTransactionId())
                .build();
        write(gtf, null);
    }

    @SuppressWarnings("unused")
    private void sendGroupFeaturesRequest() throws IOException {
        OFMessage gfr = factory.buildGroupFeaturesStatsRequest()
                .setXid(getNextTransactionId())
                .build();
        write(gfr, null);
    }

    private void processGroupFeatures(OFGroupFeaturesStatsReply gfsr) {
        log.info("Sw: {} Group Features {}", getStringId(), gfsr);
    }

    @Override
    public TableId getTableId(String tableType) {
        tableType = tableType.toLowerCase();
        if(tableType.contentEquals("ip")){
            return TableId.of(OFSwitchImplCPqD13.TABLE_IPv4_UNICAST);
        }
        else if (tableType.contentEquals("mpls")){
            return TableId.of(OFSwitchImplCPqD13.TABLE_MPLS);
        }
        else if (tableType.contentEquals("acl")){
            return TableId.of(OFSwitchImplCPqD13.TABLE_ACL);
        }
        else{
            log.warn("Invalid tableType: {}", tableType);
            return null;
        }
    }

}
