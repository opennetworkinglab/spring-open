package net.onrc.onos.core.drivermanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.floodlightcontroller.core.IOF13Switch;
import net.floodlightcontroller.core.IOF13Switch.GroupChain;
import net.floodlightcontroller.core.IOF13Switch.GroupChainParams;
import net.floodlightcontroller.core.IOF13Switch.NeighborSet.groupPktType;
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
import net.onrc.onos.core.configmanager.SegmentRouterConfig.AdjacencySid;
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
import net.onrc.onos.core.matchaction.action.PushMplsAction;
import net.onrc.onos.core.matchaction.action.SetDAAction;
import net.onrc.onos.core.matchaction.action.SetMplsBosAction;
import net.onrc.onos.core.matchaction.action.SetMplsIdAction;
import net.onrc.onos.core.matchaction.action.SetSAAction;
import net.onrc.onos.core.matchaction.match.Ipv4Match;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.matchaction.match.MplsMatch;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.IPv4Net;
import net.onrc.onos.core.util.PortNumber;

import org.codehaus.jackson.map.ObjectMapper;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFAsyncGetReply;
import org.projectfloodlight.openflow.protocol.OFBarrierRequest;
import org.projectfloodlight.openflow.protocol.OFBucket;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFGroupDescStatsEntry;
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
import org.projectfloodlight.openflow.protocol.action.OFActionGroup;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.match.Match.Builder;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEthDst;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEthSrc;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEthType;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmInPort;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmIpv4DstMasked;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmMplsBos;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmMplsLabel;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmVlanVid;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBooleanValue;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.util.HexString;

import com.google.common.primitives.Longs;

/**
 * OFDescriptionStatistics Vendor (Manufacturer Desc.): Stanford University,
 * Ericsson Research and CPqD Research. Make (Hardware Desc.) : OpenFlow 1.3
 * Reference Userspace Switch Model (Datapath Desc.) : None Software : Serial :
 * None
 */
public class OFSwitchImplSpringOpenTTP extends OFSwitchImplBase implements IOF13Switch {
    private AtomicBoolean driverHandshakeComplete;
    private AtomicBoolean haltStateMachine;
    protected OFFactory factory;
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
    protected ConcurrentMap<Dpid, Set<PortNumber>> neighbors;
    protected ConcurrentMap<PortNumber, Dpid> portToNeighbors;
    private List<Integer> segmentIds;
    private boolean isEdgeRouter;
    private ConcurrentMap<NeighborSet, EcmpInfo> nsToGroups;
    private ConcurrentMap<Integer, EcmpInfo> userDefinedGroups;
    private ConcurrentMap<PortNumber, HashSet<NeighborSet>> portNeighborSetMap;
    private ConcurrentMap<List<BucketInfo>, List<Integer>> auditBucketToGroupIds;
    private ConcurrentMap<Integer, EcmpInfo> ecmpGroups;
    private AtomicInteger groupid;
    private Map<String, String> publishAttributes;

    /* Set the default values. These variables will get
     * overwritten based on the switch vendor type
     */
    protected int vlanTableId = TABLE_VLAN;
    protected int tmacTableId = TABLE_TMAC;
    protected int ipv4UnicastTableId = TABLE_IPv4_UNICAST;
    protected int mplsTableId = TABLE_MPLS;
    protected int aclTableId = TABLE_ACL;

    public OFSwitchImplSpringOpenTTP(OFDescStatsReply desc, boolean usePipeline13) {
        super();
        haltStateMachine = new AtomicBoolean(false);
        driverState = DriverState.INIT;
        driverHandshakeComplete = new AtomicBoolean(false);
        setSwitchDescription(desc);
        neighbors = new ConcurrentHashMap<Dpid, Set<PortNumber>>();
        portToNeighbors = new ConcurrentHashMap<PortNumber, Dpid>();
        nsToGroups = new ConcurrentHashMap<NeighborSet, EcmpInfo>();
        ecmpGroups = new ConcurrentHashMap<Integer, EcmpInfo>();
        userDefinedGroups = new ConcurrentHashMap<Integer, EcmpInfo>();
        portNeighborSetMap =
                new ConcurrentHashMap<PortNumber, HashSet<NeighborSet>>();
        segmentIds = new ArrayList<Integer>();
        isEdgeRouter = false;
        groupid = new AtomicInteger(0);
        this.usePipeline13 = usePipeline13;
        publishAttributes = null;
    }

    // *****************************
    // OFSwitchImplBase
    // *****************************

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "OFSwitchImplSpringOpenTTP [" + ((channel != null)
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
        AUDIT_GROUPS,
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
            if (getRole() == Role.SLAVE) {
                driverState = DriverState.AUDIT_GROUPS;
                verifyGroups();
                break;
            }
            driverState = DriverState.SET_TABLE_MISS_ENTRIES;
            setTableMissEntries();
            sendHandshakeBarrier();
            break;
        case SET_TABLE_MISS_ENTRIES:
            driverState = DriverState.SET_TABLE_VLAN_TMAC;
            boolean isConfigured = getNetworkConfig();
            if (!isConfigured) {
                return; // this will result in a handshake timeout
            }
            populateTableVlan();
            populateTableTMac();
            // testDifferentLabelStacks();
            sendHandshakeBarrier();
            break;
        case SET_TABLE_VLAN_TMAC:
            driverState = DriverState.AUDIT_GROUPS;
            verifyGroups();
            break;
        case AUDIT_GROUPS:
            if (getRole() == Role.SLAVE) {
                driverState = DriverState.EXIT;
                driverHandshakeComplete.set(true);
                break;
            }
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

    private BucketInfo getDriverBucketFromOFBucket(OFBucket bucket) {
        List<OFAction> actions = bucket.getActions();
        int bucketPort = -1;
        int bucketLabel = -1;
        int bucketToGroup = -1;
        MacAddress dlSrc = null;
        MacAddress dlDst = null;
        for (OFAction action : actions) {
            if (action.getType().compareTo(OFActionType.OUTPUT) == 0) {
                bucketPort = ((OFActionOutput) action).getPort()
                        .getPortNumber();
            }
            else if (action.getType().compareTo(OFActionType.GROUP) == 0) {
                bucketToGroup = ((OFActionGroup) action).getGroup()
                        .getGroupNumber();
            }
            else if (action.getType().compareTo(OFActionType.SET_FIELD) == 0) {
                if (((OFActionSetField) action).getField().toString()
                        .contains("OFOxmMplsLabelVer13")) {
                    bucketLabel = Integer.decode(
                            ((OFActionSetField) action).
                                    getField().getValue().toString());
                }
                else if (((OFActionSetField) action).getField().toString()
                        .contains("OFOxmEthSrcVer13")) {
                    dlSrc = MacAddress.of(((OFActionSetField) action)
                            .getField().getValue().toString());
                }
                else if (((OFActionSetField) action).getField().toString()
                        .contains("OFOxmEthDstVer13")) {
                    dlDst = MacAddress.of(((OFActionSetField) action)
                            .getField().getValue().toString());
                }
            }
        }

        if ((bucketPort == -1) && (bucketLabel == -1)
                && (bucketToGroup == -1)) {
            log.warn("processGroupDesc: A group with no port, no label, no to group");
        }
        BucketInfo driverBucket = new BucketInfo(null, dlSrc, dlDst,
                PortNumber.uint32(bucketPort), bucketLabel, true,
                bucketToGroup);

        return driverBucket;
    }

    private void processGroupDesc(OFGroupDescStatsReply gdsr) {
        log.info("Sw: {} Group Desc {} in driverState {} ", getStringId(), gdsr,
                driverState);

        if (driverState == DriverState.AUDIT_GROUPS)
        {
            auditBucketToGroupIds = new ConcurrentHashMap<List<BucketInfo>, List<Integer>>();
            for (OFGroupDescStatsEntry entry : gdsr.getEntries()) {
                int groupId = entry.getGroup().getGroupNumber();
                OFGroupType groupType = entry.getGroupType();
                List<OFBucket> buckets = entry.getBuckets();
                List<BucketInfo> driverBuckets = new ArrayList<BucketInfo>();
                for (OFBucket bucket : buckets) {
                    driverBuckets.add(getDriverBucketFromOFBucket(bucket));
                }
                Collections.sort(driverBuckets);
                List<Integer> groupIds = auditBucketToGroupIds.get(driverBuckets);
                if (groupIds == null)
                    groupIds = new ArrayList<Integer>();
                groupIds.add(groupId);
                auditBucketToGroupIds.put(driverBuckets, groupIds);
                ecmpGroups.put(groupId, new EcmpInfo(groupId, groupType, driverBuckets));
            }
            log.debug("processGroupDesc: Groups that are present in "
                    + "switch {} are {} ", getStringId(), ecmpGroups);
        }
        else {
            // TODO -- actually do verification
        }
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

    protected void setTableMissEntries() throws IOException {
        // set all table-miss-entries
        populateTableMissEntry(vlanTableId, true, false, false, -1);
        populateTableMissEntry(tmacTableId, true, false, false, -1);
        populateTableMissEntry(ipv4UnicastTableId, false, true, true,
                aclTableId);
        populateTableMissEntry(mplsTableId, false, true, true,
                aclTableId);
        populateTableMissEntry(aclTableId, false, false, false, -1);
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
    protected void populateTableMissEntry(int tableToAdd, boolean toControllerNow,
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

    private boolean getNetworkConfig() {
        INetworkConfigService ncs = floodlightProvider.getNetworkConfigService();
        SwitchConfigStatus scs = ncs.checkSwitchConfig(new Dpid(getId()));
        if (scs.getConfigState() != NetworkConfigState.ACCEPT_ADD) {
            log.error("Switch {} is not configured as Segment-Router.. aborting",
                    getStringId());
            return false;
        }
        srConfig = (SegmentRouterConfig) scs.getSwitchConfig();
        if (srConfig == null) {
            log.error("Switch {} configuration is null.. aborting", getStringId());
            return false;
        }
        isEdgeRouter = srConfig.isEdgeRouter();
        List<LinkConfig> linkConfigList = ncs.getConfiguredAllowedLinks();
        setNeighbors(linkConfigList);

        if (isEdgeRouter) {
            List<SwitchConfig> switchList = ncs.getConfiguredAllowedSwitches();
            getAllNodeSegmentIds(switchList);
        }
        return true;
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
                        .setTableId(TableId.of(tmacTableId)).build();
                List<OFInstruction> instructions = new ArrayList<OFInstruction>();
                // instructions.add(appAction);
                instructions.add(gotoTbl);
                OFMessage flowEntry = factory.buildFlowAdd()
                        .setTableId(TableId.of(vlanTableId))
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
                .setTableId(TableId.of(ipv4UnicastTableId)).build();
        List<OFInstruction> instructionsIp = Collections.singletonList(gotoTblIp);
        OFMessage ipEntry = factory.buildFlowAdd()
                .setTableId(TableId.of(tmacTableId))
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
                .setTableId(TableId.of(mplsTableId)).build();
        List<OFInstruction> instructionsMpls = Collections.singletonList(gotoTblMpls);
        OFMessage mplsEntry = factory.buildFlowAdd()
                .setTableId(TableId.of(tmacTableId))
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

    protected MacAddress getRouterMacAddr() {
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

    /* Base implementation returns the configured router MAC only */
    protected MacAddress getNeighborRouterMacAddress(Dpid ndpid,
            groupPktType outPktType) {
        return getNeighborRouterMacAddress(ndpid);
    }

    /* Default implementation of this interface.
     * Gets the specified Router's MAC address to be used for IP flows
     * To be overridden depending on the underlying switch type
     */
    public MacAddress getRouterIPMac(Dpid dpid) {
        return getNeighborRouterMacAddress(dpid);
    }

    /* Default implementation of this interface.
     * Gets the specified Router's MAC address to be used for MPLS flows
     * To be overridden depending on the underlying switch type
     */
    public MacAddress getRouterMPLSMac(Dpid dpid) {
        return getNeighborRouterMacAddress(dpid);
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

    protected Set<Set<Dpid>> getPowerSetOfNeighbors(Set<Dpid> neighbors) {
        List<Dpid> list = new ArrayList<Dpid>(neighbors);
        Set<Set<Dpid>> sets = new HashSet<Set<Dpid>>();
        /* get the number of elements in the neighbors */
        int elements = list.size();
        /* the number of members of a power set is 2^n
         * including the empty set
         */
        int powerElements = (1 << elements);

        /* run a binary counter for the number of power elements
         * NOTE: Exclude empty set
         */
        for (long i = 1; i < powerElements; i++) {
            Set<Dpid> dpidSubSet = new HashSet<Dpid>();
            for (int j = 0; j < elements; j++) {
                if ((i >> j) % 2 == 1) {
                    dpidSubSet.add(list.get(j));
                }
            }
            sets.add(dpidSubSet);
        }
        return sets;
    }

    protected Set<Set<Dpid>> filterEdgeRouterOnlyPairings(Set<Set<Dpid>> sets) {
        Set<Set<Dpid>> fiteredSets = new HashSet<Set<Dpid>>();
        for (Set<Dpid> dpidSubSet : sets) {
            if (dpidSubSet.size() > 1) {
                boolean avoidEdgeRouterPairing = true;
                for (Dpid dpid : dpidSubSet) {
                    if (!isEdgeRouter(dpid)) {
                        avoidEdgeRouterPairing = false;
                        break;
                    }
                }
                if (!avoidEdgeRouterPairing)
                    fiteredSets.add(dpidSubSet);
            }
            else
                fiteredSets.add(dpidSubSet);

        }
        return fiteredSets;
    }

    private Set<Set<Dpid>> getAllNeighborSets(Set<Dpid> neighbors) {
        Set<Set<Dpid>> sets = getPowerSetOfNeighbors(neighbors);
        if (!isEdgeRouter) {
            /* NOTE: Avoid any pairings of edge routers only
             * at a backbone router */
            sets = filterEdgeRouterOnlyPairings(sets);
        }
        return sets;
    }

    protected int createGroupForANeighborSet(NeighborSet ns) {
        int groupId = -1;
        List<BucketInfo> buckets = new ArrayList<BucketInfo>();
        for (Dpid d : ns.getDpids()) {
            for (PortNumber sp : neighbors.get(d)) {
                if (portEnabled((int) sp.value())) {
                    BucketInfo b = new BucketInfo(d,
                            MacAddress.of(srConfig.getRouterMac()),
                            getNeighborRouterMacAddress(d, ns.getOutPktType()),
                            sp, ns.getEdgeLabel(), true, -1);
                    buckets.add(b);
                }
            }
        }
        if (buckets.isEmpty()) {
            log.warn(
                    "createGroupForANeighborSet: No group created "
                            + "in sw {} for neighbor set {} "
                            + "as there are no buckets",
                    getStringId(), ns);
        }
        else {
            Collections.sort(buckets);
            List<Integer> groupIds = auditBucketToGroupIds.get(buckets);
            if (groupIds != null) {
                /* Pop the top most group Ids for this list of buckets */
                groupId = groupIds.get(0);
                groupIds.remove(0);
                if (groupIds.isEmpty())
                    auditBucketToGroupIds.remove(buckets);
                EcmpInfo ecmpInfo = new EcmpInfo(groupId, OFGroupType.SELECT, buckets);
                nsToGroups.put(ns, ecmpInfo);
                log.debug(
                        "createGroupForANeighborSet: No group created in sw {} "
                                + "for neighborset {} as group already "
                                + "exists with groupId {}",
                        getStringId(), ns, groupId);
            }
            else {
                groupId = getNextFreeGroupId();
                EcmpInfo ecmpInfo = new EcmpInfo(groupId, OFGroupType.SELECT, buckets);
                setEcmpGroup(ecmpInfo);
                nsToGroups.put(ns, ecmpInfo);
                ecmpGroups.put(groupId, ecmpInfo);
                log.debug(
                        "createGroupForANeighborSet: Creating ecmp group {} in sw {} "
                                + "for neighbor set {} with: {}",
                        groupId, getStringId(), ns, ecmpInfo);
            }
        }
        return groupId;
    }

    /* Update Port Neighborset map */
    public void updatePortNeighborSetMap(NeighborSet ns) {
        for (Dpid d : ns.getDpids()) {
            for (PortNumber sp : neighbors.get(d)) {
                HashSet<NeighborSet> portNeighborSets =
                        portNeighborSetMap.get(sp);
                if (portNeighborSets == null) {
                    portNeighborSets = new HashSet<NeighborSet>();
                    portNeighborSets.add(ns);
                    portNeighborSetMap.put(sp, portNeighborSets);
                }
                else
                    portNeighborSets.add(ns);
            }
        }
    }

    protected List<Integer> getSegmentIdsTobePairedWithNeighborSet(
            Set<Dpid> dpids) {

        List<Integer> nsSegmentIds = new ArrayList<Integer>();

        /* Add one entry for "no label" (-1) to the list if
         * dpid list has not more than one node/neighbor as
         * there will never be a case a packet going to more than one
         * neighbor without a label at an edge router
         */
        if (dpids.size() == 1) {
            nsSegmentIds.add(-1);
        }
        if (!segmentIds.isEmpty()) {
            /* Filter out SegmentIds matching with the
             * nodes in the combo
             */
            for (Integer sId : segmentIds) {
                boolean filterOut = false;
                /* Check if the edge label being set is of
                 * any node in the Neighbor set
                 */
                for (Dpid dpid : dpids) {
                    if (isSegmentIdSameAsNodeSegmentId(dpid, sId)) {
                        filterOut = true;
                        break;
                    }
                }
                if (!filterOut)
                    nsSegmentIds.add(sId);
            }
        }

        return nsSegmentIds;
    }

    private int getNextFreeGroupId() {
        int groupId = groupid.incrementAndGet();

        while (ecmpGroups.get(groupId) != null) {
            groupId = groupid.incrementAndGet();
        }

        return groupId;
    }

    protected void createGroupsAtEdgeRouter(Set<Dpid> dpids) {
        /* Create all possible Neighbor sets from this router
         */
        Set<Set<Dpid>> powerSet = getPowerSetOfNeighbors(dpids);
        log.debug("createGroupsAtEdgeRouter: The size of neighbor powerset "
                + "for sw {} is {}", getStringId(), powerSet.size());
        Set<NeighborSet> nsSet = new HashSet<NeighborSet>();
        for (Set<Dpid> combo : powerSet) {
            if (combo.isEmpty())
                continue;
            List<Integer> groupSegmentIds =
                    getSegmentIdsTobePairedWithNeighborSet(combo);
            if (!groupSegmentIds.isEmpty()) {
                for (Integer sId : groupSegmentIds) {
                    NeighborSet ns = new NeighborSet();
                    ns.addDpids(combo);
                    ns.setEdgeLabel(sId);
                    log.debug("createGroupsAtEdgeRouter: sw {} combo {} sId {} ns {}",
                            getStringId(), combo, sId, ns);
                    nsSet.add(ns);
                }
            }
        }
        log.debug("createGroupsAtEdgeRouter: The neighborset with label for sw {} is {}",
                getStringId(), nsSet);

        for (NeighborSet ns : nsSet) {
            updatePortNeighborSetMap(ns);
            // synchronizeGroupsBwControllerAndSwitch();

            /* Create the bucket array from the neighbor set */
            List<BucketInfo> buckets = new ArrayList<BucketInfo>();
            for (Dpid d : ns.getDpids()) {
                for (PortNumber sp : neighbors.get(d)) {
                    if (portEnabled((int) sp.value())) {
                        BucketInfo b = new BucketInfo(d,
                                MacAddress.of(srConfig.getRouterMac()),
                                getNeighborRouterMacAddress(d, ns.getOutPktType()),
                                sp, ns.getEdgeLabel(), true, -1);
                        buckets.add(b);
                    }
                    else
                    {
                        log.debug("createGroupsAtEdgeRouter: Sw {} "
                                + "Port {} to Neighbor {} is not "
                                + "enabled and hence not including in buckets",
                                getStringId(), sp, d);
                    }
                }
            }

            /* Check if switch already had a group with the same list of
             * buckets. If so, knock off that group from auditBucketToGroupIds
             * database and insert into nsToGroups database
             */
            Collections.sort(buckets);
            log.debug(
                    "createGroupsAtEdgeRouter: Sw {} auditBucketToGroupIds {} ",
                    getStringId(), auditBucketToGroupIds);
            log.debug(
                    "createGroupsAtEdgeRouter: Sw {} Search buckets {} in auditBucketToGroupIds",
                    getStringId(), buckets);
            List<Integer> groupIds = auditBucketToGroupIds.get(buckets);
            int groupId;
            if (groupIds != null) {
                /* Pop the top most group Ids for this list of buckets */
                groupId = groupIds.get(0);
                groupIds.remove(0);
                if (groupIds.isEmpty())
                    auditBucketToGroupIds.remove(buckets);
                EcmpInfo ecmpInfo = new EcmpInfo(groupId, OFGroupType.SELECT, buckets);
                nsToGroups.put(ns, ecmpInfo);
                log.debug(
                        "createGroupsAtEdgeRouter: No group created in sw {} "
                                + "for neighborset {} as group already "
                                + "exists with groupId {}",
                        getStringId(), ns, groupId);
            }
            else
            {
                if (buckets.isEmpty()) {
                    log.warn(
                            "createGroupsAtEdgeRouter: No group created "
                                    + "in sw {} for neighbor set {} "
                                    + "as there are no buckets",
                            getStringId(), ns);
                }
                else {
                    groupId = getNextFreeGroupId();
                    EcmpInfo ecmpInfo = new EcmpInfo(groupId, OFGroupType.SELECT, buckets);
                    setEcmpGroup(ecmpInfo);
                    nsToGroups.put(ns, ecmpInfo);
                    ecmpGroups.put(groupId, ecmpInfo);
                    log.debug(
                            "createGroupsAtEdgeRouter: Creating ecmp group {} in sw {} "
                                    + "for neighbor set {} with: {}",
                            groupId, getStringId(), ns, ecmpInfo);
                }
            }
        }

        /* Handle rest of the entries in the auditBucketToGroupIds
         * (if any left over) NOTE: These may be user defined groups
         * or run time created groups
         */
        //auditBucketToGroupIds.
        // for
    }

    protected void createGroupsAtTransitRouter(Set<Dpid> dpids) {
        /* Create all possible Neighbor sets from this router
         * NOTE: Avoid any pairings of edge routers only
         */
        Set<Set<Dpid>> sets = getPowerSetOfNeighbors(dpids);
        sets = filterEdgeRouterOnlyPairings(sets);
        log.debug("createGroupsAtTransitRouter: The size of neighbor powerset "
                + "for sw {} is {}", getStringId(), sets.size());
        Set<NeighborSet> nsSet = new HashSet<NeighborSet>();
        for (Set<Dpid> combo : sets) {
            if (combo.isEmpty())
                continue;
            NeighborSet ns = new NeighborSet();
            ns.addDpids(combo);
            log.debug("createGroupsAtTransitRouter: sw {} combo {} ns {}",
                    getStringId(), combo, ns);
            nsSet.add(ns);
        }
        log.debug("createGroupsAtTransitRouter: The neighborset with label "
                + "for sw {} is {}", getStringId(), nsSet);

        for (NeighborSet ns : nsSet) {
            updatePortNeighborSetMap(ns);
            // synchronizeGroupsBwControllerAndSwitch();

            /* Create the bucket array from the neighbor set */
            List<BucketInfo> buckets = new ArrayList<BucketInfo>();
            for (Dpid d : ns.getDpids()) {
                for (PortNumber sp : neighbors.get(d)) {
                    if (portEnabled((int) sp.value())) {
                        BucketInfo b = new BucketInfo(d,
                                MacAddress.of(srConfig.getRouterMac()),
                                getNeighborRouterMacAddress(d, ns.getOutPktType()),
                                sp, ns.getEdgeLabel(), true, -1);
                        buckets.add(b);
                    }
                }
            }

            /* Check if switch already had a group with the same list of
             * buckets. If so, knock off that group from auditBucketToGroupIds
             * database and insert into nsToGroups database
             */
            Collections.sort(buckets);
            List<Integer> groupIds = auditBucketToGroupIds.get(buckets);
            int groupId;
            if (groupIds != null) {
                /* Pop the top most group Ids for this list of buckets */
                groupId = groupIds.get(0);
                groupIds.remove(0);
                if (groupIds.isEmpty())
                    auditBucketToGroupIds.remove(buckets);
                EcmpInfo ecmpInfo = new EcmpInfo(groupId, OFGroupType.SELECT, buckets);
                nsToGroups.put(ns, ecmpInfo);
                log.debug(
                        "createGroupsAtTransitRouter: No group created in sw {} "
                                + "for neighborset {} as group already "
                                + "exists with groupId {}",
                        getStringId(), ns, groupId);
            }
            else
            {
                if (buckets.isEmpty()) {
                    log.warn(
                            "createGroupsAtTransitRouter: No group created "
                                    + "in sw {} for neighbor set {} "
                                    + "as there are no buckets",
                            getStringId(), ns);
                }
                else {
                    groupId = getNextFreeGroupId();
                    EcmpInfo ecmpInfo = new EcmpInfo(groupId, OFGroupType.SELECT, buckets);
                    setEcmpGroup(ecmpInfo);
                    nsToGroups.put(ns, ecmpInfo);
                    ecmpGroups.put(groupId, ecmpInfo);
                    log.debug(
                            "createGroupsAtTransitRouter: Creating ecmp group {} in sw {} "
                                    + "for neighbor set {} with: {}",
                            groupId, getStringId(), ns, ecmpInfo);
                }
            }
        }
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

        if (isEdgeRouter) {
            createGroupsAtEdgeRouter(dpids);
        }
        else
            createGroupsAtTransitRouter(dpids);
    }

    private class EcmpInfo {
        int groupId;
        OFGroupType groupType;
        List<BucketInfo> buckets;

        EcmpInfo(int gid, OFGroupType gType, List<BucketInfo> bucketInfos) {
            groupId = gid;
            groupType = gType;
            buckets = bucketInfos;
        }

        @Override
        public String toString() {
            return "groupId: " + groupId + ", buckets: " + buckets;
        }
    }

    private class BucketInfo implements Comparable<BucketInfo> {
        Dpid neighborDpid;
        MacAddress srcMac;
        MacAddress dstMac;
        PortNumber outport;
        int togroupNo;
        int mplsLabel;
        boolean bos;

        BucketInfo(Dpid nDpid, MacAddress smac, MacAddress dmac,
                PortNumber p, int label, boolean bos, int gotoGroupNo) {
            neighborDpid = nDpid;
            srcMac = smac;
            dstMac = dmac;
            outport = p;
            mplsLabel = label;
            this.bos = bos;
            togroupNo = gotoGroupNo;
        }


        @Override
        public String toString() {
            return " {neighborDpid: " + neighborDpid + ", dstMac: " + dstMac +
                    ", srcMac: " + srcMac + ", outport: " + outport +
                    ", togroupNo: " + togroupNo +
                    ", mplsLabel: " + mplsLabel + "}";
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof BucketInfo)) {
                return false;
            }
            BucketInfo that = (BucketInfo) o;
            return ((this.outport.value() == that.outport.value()) &&
                    (this.mplsLabel == that.mplsLabel) &&
                    (this.togroupNo == that.togroupNo));
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + Longs.hashCode(outport.value());
            result = 31 * result + Longs.hashCode(mplsLabel);
            result = 31 * result + Longs.hashCode(togroupNo);
            return result;
        }

        @Override
        public int compareTo(BucketInfo that) {
            if (this.outport.value() < that.outport.value())
                return -1;
            else if (this.outport.value() > that.outport.value())
                return 1;
            else {
                if (this.mplsLabel < that.mplsLabel)
                    return -1;
                else if (this.mplsLabel > that.mplsLabel)
                    return 1;
                else
                    return 0;
            }
        }

    }

    private void setEcmpGroup(EcmpInfo ecmpInfo) {
        if ((ecmpInfo.buckets == null) || ecmpInfo.buckets.isEmpty()) {
            /* Only push the GroupAdd to the switch if the
             * bucket list is non-empty
             */
            return;
        }

        List<OFMessage> msglist = new ArrayList<OFMessage>();
        OFGroup group = OFGroup.of(ecmpInfo.groupId);

        List<OFBucket> buckets = new ArrayList<OFBucket>();
        for (BucketInfo b : ecmpInfo.buckets) {
            List<OFAction> actions = new ArrayList<OFAction>();
            if (b.dstMac != null) {
                OFOxmEthDst dmac = factory.oxms()
                        .ethDst(b.dstMac);
                OFAction setDA = factory.actions().buildSetField()
                        .setField(dmac).build();
                actions.add(setDA);
            }
            if (b.srcMac != null) {
                OFOxmEthSrc smac = factory.oxms()
                        .ethSrc(b.srcMac);
                OFAction setSA = factory.actions().buildSetField()
                        .setField(smac).build();
                actions.add(setSA);
            }
            if (b.outport != null) {
                OFAction outp = factory.actions().buildOutput()
                        .setPort(OFPort.of(b.outport.shortValue()))
                        .build();
                actions.add(outp);
            }
            if (b.mplsLabel != -1) {
                OFAction pushLabel = factory.actions().buildPushMpls()
                        .setEthertype(EthType.MPLS_UNICAST).build();
                OFBooleanValue bosValue = null;
                if (b.bos)
                    bosValue = OFBooleanValue.TRUE;
                else
                    bosValue = OFBooleanValue.FALSE;
                OFOxmMplsBos bosX = factory.oxms()
                        .mplsBos(bosValue);
                OFAction setBX = factory.actions().buildSetField()
                        .setField(bosX).build();
                OFOxmMplsLabel lid = factory.oxms()
                        .mplsLabel(U32.of(b.mplsLabel));
                OFAction setLabel = factory.actions().buildSetField()
                        .setField(lid).build();
                OFAction copyTtl = factory.actions().copyTtlOut();
                OFAction decrTtl = factory.actions().decMplsTtl();
                actions.add(pushLabel);
                actions.add(setLabel);
                actions.add(setBX);
                actions.add(copyTtl);
                // decrement TTL only when the first MPLS label is pushed
                if (b.bos)
                    actions.add(decrTtl);
            }
            if (b.togroupNo > 0) {
                OFAction groupTo = factory.actions().buildGroup()
                        .setGroup(OFGroup.of(b.togroupNo))
                        .build();
                actions.add(groupTo);
            }
            OFBucket.Builder bldr = factory.buildBucket();
            bldr.setActions(actions);
            if (ecmpInfo.groupType == OFGroupType.SELECT)
                bldr.setWeight(1);
            bldr.setWatchGroup(OFGroup.ANY);
            bldr.setWatchPort(OFPort.ANY);
            OFBucket ofb = bldr.build();
            buckets.add(ofb);
        }

        OFMessage gm = factory.buildGroupAdd()
                .setGroup(group)
                .setBuckets(buckets)
                .setGroupType(ecmpInfo.groupType)
                .setXid(getNextTransactionId())
                .build();
        msglist.add(gm);
        log.debug("GroupAdd in sw {} groupId {}",
                getStringId(), ecmpInfo.groupId);
        try {
            write(msglist);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void deleteGroup(EcmpInfo groupInfo) {

        List<OFMessage> msglist = new ArrayList<OFMessage>();
        OFGroup group = OFGroup.of(groupInfo.groupId);

        OFMessage gm = factory.buildGroupDelete()
                .setGroup(group)
                //.setGroupType(groupInfo.groupType) /* Due to a bug in CPqD
                // switch */
                .setGroupType(OFGroupType.SELECT)
                .setXid(getNextTransactionId())
                .build();
        msglist.add(gm);
        log.debug("GroupDelete in sw {} groupId {}",
                getStringId(), groupInfo.groupId);
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
            List<OFAction> actions = new ArrayList<OFAction>();
            if (b.dstMac != null) {
                OFOxmEthDst dmac = factory.oxms()
                        .ethDst(b.dstMac);
                OFAction setDA = factory.actions().buildSetField()
                        .setField(dmac).build();
                actions.add(setDA);
            }
            if (b.srcMac != null) {
                OFOxmEthSrc smac = factory.oxms()
                        .ethSrc(b.srcMac);
                OFAction setSA = factory.actions().buildSetField()
                        .setField(smac).build();
                actions.add(setSA);
            }
            if (b.outport != null) {
                OFAction outp = factory.actions().buildOutput()
                        .setPort(OFPort.of(b.outport.shortValue()))
                        .build();
                actions.add(outp);
            }
            if (b.mplsLabel != -1) {
                OFAction pushLabel = factory.actions().buildPushMpls()
                        .setEthertype(EthType.MPLS_UNICAST).build();
                OFBooleanValue bosValue = null;
                if (b.bos)
                    bosValue = OFBooleanValue.TRUE;
                else
                    bosValue = OFBooleanValue.FALSE;
                OFOxmMplsBos bosX = factory.oxms()
                        .mplsBos(bosValue);
                OFAction setBX = factory.actions().buildSetField()
                        .setField(bosX).build();
                OFOxmMplsLabel lid = factory.oxms()
                        .mplsLabel(U32.of(b.mplsLabel));
                OFAction setLabel = factory.actions().buildSetField()
                        .setField(lid).build();
                OFAction copyTtl = factory.actions().copyTtlOut();
                OFAction decrTtl = factory.actions().decMplsTtl();
                actions.add(pushLabel);
                actions.add(setLabel);
                actions.add(setBX);
                actions.add(copyTtl);
                // decrement TTL only when the first MPLS label is pushed
                if (b.bos)
                    actions.add(decrTtl);
            }
            if (b.togroupNo > 0) {
                OFAction groupTo = factory.actions().buildGroup()
                        .setGroup(OFGroup.of(b.togroupNo))
                        .build();
                actions.add(groupTo);
            }
            OFBucket.Builder bldr = factory.buildBucket();
            bldr.setActions(actions);
            if (ecmpInfo.groupType == OFGroupType.SELECT)
                bldr.setWeight(1);
            OFBucket ofb = bldr.build();
            buckets.add(ofb);
        }

        OFMessage gm = factory.buildGroupModify()
                .setGroup(group)
                .setBuckets(buckets)
                .setGroupType(ecmpInfo.groupType)
                .setXid(getNextTransactionId())
                .build();
        msglist.add(gm);
        log.debug("GroupMod in sw {} groupId {}",
                getStringId(), ecmpInfo.groupId);
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
        List<AdjacencySid> autogenAdjSids = new ArrayList<AdjacencySid>();
        publishAttributes = new HashMap<String, String>();
        for (OFPortDesc p : getPorts()) {
            int pnum = p.getPortNo().getPortNumber();

            if (U32.ofRaw(pnum).compareTo(U32.ofRaw(OFPort.MAX.getPortNumber())) >= 1) {
                continue;
            }
            // create unique adj-sid assuming that operator only
            // enters adjSids for multiple-ports and only in the range
            // 1-10k XXX make sure that happens
            int adjSid = srConfig.getNodeSid() * 1000 + pnum;
            AdjacencySid as = new AdjacencySid(adjSid,
                    Collections.singletonList(pnum));
            autogenAdjSids.add(as);
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            publishAttributes.put("autogenAdjSids",
                    mapper.writeValueAsString(autogenAdjSids));
        } catch (IOException e1) {
            log.error("Error while writing adjacency labels: {}", e1.getCause());
        }

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
        } else if (action instanceof PushMplsAction) {
            ofAction = factory.actions().pushMpls(EthType.MPLS_UNICAST);
        } else if (action instanceof SetMplsIdAction) {
            int labelid = ((SetMplsIdAction) action).getMplsId();
            OFOxmMplsLabel lid = factory.oxms()
                    .mplsLabel(U32.of(labelid));
            ofAction = factory.actions().buildSetField()
                    .setField(lid).build();
        } else if (action instanceof SetMplsBosAction) {
            OFBooleanValue val = OFBooleanValue.of(
                    ((SetMplsBosAction) action).isSet());
            OFOxmMplsBos bos = factory.oxms().mplsBos(val);
            OFAction setBos = factory.actions().buildSetField()
                    .setField(bos).build();
        } else if (action instanceof PopMplsAction) {
            EthType ethertype = ((PopMplsAction) action).getEthType();
            ofAction = factory.actions().popMpls(ethertype);
        } else if (action instanceof GroupAction) {
            int gid = -1;
            GroupAction ga = (GroupAction)action;
            if (ga.getGroupId() > 0) {
                gid = ga.getGroupId();
            }
            else {
                NeighborSet ns = ((GroupAction) action).getDpids();
                EcmpInfo ei = nsToGroups.get(ns);
                if (ei == null) {
                    log.debug("Unable to find ecmp group for neighbors {} at "
                            + "switch {} and hence creating it", ns, getStringId());
                    updatePortNeighborSetMap(ns);
                    gid = createGroupForANeighborSet(ns);
                    if (gid == -1) {
                        log.warn("Create Group failed with -1");
                    }
                }
                else
                    gid = ei.groupId;
            }
            if (gid > 0) {
                ofAction = factory.actions().buildGroup()
                    .setGroup(OFGroup.of(gid))
                    .build();
            }
        } else if (action instanceof DecNwTtlAction) {
            ofAction = factory.actions().decNwTtl();
        } else if (action instanceof DecMplsTtlAction) {
            ofAction = factory.actions().decMplsTtl();
        } else if (action instanceof CopyTtlInAction) {
            ofAction = factory.actions().copyTtlIn();
        } else if (action instanceof CopyTtlOutAction) {
            ofAction = factory.actions().copyTtlOut();
        } else if (action instanceof SetDAAction) {
            OFOxmEthDst dmac = factory.oxms()
                    .ethDst(((SetDAAction)action).getAddress());
            ofAction = factory.actions().buildSetField()
                    .setField(dmac).build();
        } else if (action instanceof SetSAAction) {
            OFOxmEthSrc smac = factory.oxms()
                    .ethSrc(((SetSAAction)action).getAddress());
            ofAction = factory.actions().buildSetField()
                    .setField(smac).build();
        } else {
            log.warn("Unsupported Action type: {}", action.getClass().getName());
            return null;
        }

        return ofAction;
    }

    /* Default implementation of building match for
     * IP table entry. This method can be overwritten
     * depending on the switch
     */
    protected OFOxmList getIPEntryMatchList(OFFactory ofFactory, Match match) {
        Ipv4Match ipm = (Ipv4Match) match;

        IPv4Net ipdst = ipm.getDestination();
        OFOxmEthType ethTypeIp = ofFactory.oxms()
                .ethType(EthType.IPv4);
        OFOxmIpv4DstMasked ipPrefix = ofFactory.oxms()
                .ipv4DstMasked(
                        IPv4Address.of(ipdst.address().value()),
                        IPv4Address.ofCidrMaskLength(ipdst.prefixLen())
                );
        OFOxmList oxmList = OFOxmList.of(ethTypeIp, ipPrefix);

        return oxmList;
    }

    private OFMessage getIpEntry(MatchActionOperationEntry mao) {
        MatchAction ma = mao.getTarget();
        Operator op = mao.getOperator();
        Ipv4Match ipm = (Ipv4Match) ma.getMatch();
        IPv4Net ipdst = ipm.getDestination();

        /* Making this functionality extensible depending on the
         * type of the switch
         */
        /*
        // set match
        OFOxmEthType ethTypeIp = factory.oxms()
                .ethType(EthType.IPv4);
        OFOxmIpv4DstMasked ipPrefix = factory.oxms()
                .ipv4DstMasked(
                        IPv4Address.of(ipdst.address().value()),
                        IPv4Address.ofCidrMaskLength(ipdst.prefixLen())
                );
        OFOxmList oxmList = OFOxmList.of(ethTypeIp, ipPrefix);
        */
        OFMatchV3 match = factory.buildMatchV3()
                .setOxmList(getIPEntryMatchList(factory, ipm)).build();

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
                .setTableId(TableId.of(aclTableId)).build();
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
                .setTableId(TableId.of(ipv4UnicastTableId))
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

    protected void analyzeAndUpdateMplsActions(
            MatchActionOperationEntry mao) {
        /* For base switch implementation, use the MPLS actions
         * as specified by the application
         */
        return;
    }

    private OFMessage getMplsEntry(MatchActionOperationEntry mao) {
        analyzeAndUpdateMplsActions(mao);
        MatchAction ma = mao.getTarget();
        Operator op = mao.getOperator();
        MplsMatch mplsm = (MplsMatch) ma.getMatch();

        // set match
        OFOxmEthType ethTypeMpls = factory.oxms()
                .ethType(EthType.MPLS_UNICAST);
        OFOxmMplsLabel labelid = factory.oxms()
                .mplsLabel(U32.of(mplsm.getMplsLabel()));
        OFOxmMplsBos bos = factory.oxms()
                .mplsBos(OFBooleanValue.of(mplsm.isBos()));
        OFOxmList oxmList = OFOxmList.of(ethTypeMpls, labelid, bos);
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
                .setTableId(TableId.of(aclTableId)).build();
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
                .setTableId(TableId.of(mplsTableId))
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
        List<OFAction> writeActions = new ArrayList<OFAction>();
        for (Action action : ma.getActions()) {
            OFAction ofAction = getOFAction(action);
            if (ofAction != null) {
                writeActions.add(ofAction);
            }
        }

        // set instructions
        OFInstruction clearInstr = factory.instructions().clearActions();
        OFInstruction writeInstr = factory.instructions().buildWriteActions()
                .setActions(writeActions).build();
        List<OFInstruction> instructions = new ArrayList<OFInstruction>();
        instructions.add(clearInstr);
        instructions.add(writeInstr);

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
                .setTableId(TableId.of(aclTableId))
                .setMatch(matchBuilder.build())
                .setInstructions(instructions)
                .setPriority(ma.getPriority()) // exact match and exclusive
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
        EcmpInfo ei = nsToGroups.get(ns);
        if (ei == null) {
            return -1;
        } else {
            return ei.groupId;
        }
    }

    @Override
    public TableId getTableId(String tableType) {
        tableType = tableType.toLowerCase();
        if (tableType.contentEquals("ip")) {
            return TableId.of(ipv4UnicastTableId);
        }
        else if (tableType.contentEquals("mpls")) {
            return TableId.of(mplsTableId);
        }
        else if (tableType.contentEquals("acl")) {
            return TableId.of(aclTableId);
        }
        else {
            log.warn("Invalid tableType: {}", tableType);
            return null;
        }
    }

    private EcmpInfo createIndirectGroup(int groupId, MacAddress srcMac,
            MacAddress dstMac, PortNumber outPort, int gotoGroupNo,
            int mplsLabel, boolean bos) {
        List<BucketInfo> buckets = new ArrayList<BucketInfo>();
        BucketInfo b = new BucketInfo(null, srcMac, dstMac, outPort,
                mplsLabel, bos, gotoGroupNo);
        buckets.add(b);

        EcmpInfo ecmpInfo = new EcmpInfo(groupId, OFGroupType.INDIRECT, buckets);
        setEcmpGroup(ecmpInfo);
        log.debug(
                "createIndirectGroup: Creating indirect group {} in sw {} "
                        + "with: {}", groupId, getStringId(), ecmpInfo);
        return ecmpInfo;
    }

    private EcmpInfo createInnermostLabelGroup(int innermostGroupId,
            List<PortNumber> ports, int mplsLabel, boolean bos,
            HashMap<PortNumber, Integer> lastSetOfGroupIds) {
        List<BucketInfo> buckets = new ArrayList<BucketInfo>();
        for (PortNumber sp : ports) {
            Dpid neighborDpid = portToNeighbors.get(sp);
            BucketInfo b = new BucketInfo(neighborDpid,
                    MacAddress.of(srConfig.getRouterMac()),
                    getNeighborRouterMacAddress(neighborDpid), null,
                    mplsLabel, bos,
                    lastSetOfGroupIds.get(sp));
            buckets.add(b);
        }
        EcmpInfo ecmpInfo = new EcmpInfo(innermostGroupId,
                OFGroupType.SELECT, buckets);
        setEcmpGroup(ecmpInfo);
        log.debug(
                "createInnermostLabelGroup: Creating select group {} in sw {} "
                        + "with: {}", innermostGroupId, getStringId(), ecmpInfo);
        return ecmpInfo;
    }
    @Override
    /**
     * Create a group chain with the specified label stack for a given set of
     * ports. This API can be used by user to create groups for a tunnel based
     * policy routing scenario. NOTE: This API can not be used if a group to be
     * created with different label stacks for each port in the given set of
     * ports. Use XXX API for this purpose
     *
     * @param labelStack list of router segment Ids to be pushed. Can be empty.
     *        labelStack is processed from left to right with leftmost
     *        representing the outermost label and rightmost representing
     *        innermost label to be pushed
     * @param ports List of ports on this switch to get to the first router in
     *        the labelStack
     * @return group identifier
     */
    public List<Integer> createGroup(List<Integer> labelStack, List<PortNumber> ports) {

        log.debug("createGroup in sw {} with labelStack {} and ports {}",
                getStringId(), labelStack, ports);
        if (ports == null) {
            log.warn("createGroup in sw {} with wrong input parameters", getStringId());
            return null;
        }

        List<PortNumber> activePorts = new ArrayList<PortNumber>();
        for (PortNumber port : ports) {
            if (portEnabled((int) port.value()))
                activePorts.add(port);
        }
        if (activePorts.isEmpty()) {
            log.warn("createGroup in sw {} with no active ports", getStringId());
            return null;
        }

        int labelStackSize = (labelStack != null) ? labelStack.size() : 0;
        List<Integer> groupIdList = new ArrayList<Integer>();

        /* Filter out disabled ports */

        HashMap<PortNumber, Integer> lastSetOfGroupIds =
                new HashMap<PortNumber, Integer>();
        int innermostGroupId = -1;
        /* If it is empty label stack or label stack with only one label,
         * Create a single select group with buckets for each port in the list
         * of specified ports and specified label if any and return the
         * created group id
         */
        if (labelStackSize < 2) {
            int curLabel = -1;
            boolean bos = false;
            if (labelStackSize == 1) {
                curLabel = labelStack.get(0).intValue();
                bos = true;
            }

            List<BucketInfo> buckets = new ArrayList<BucketInfo>();
            for (PortNumber sp : activePorts) {
                Dpid neighborDpid = portToNeighbors.get(sp);
                BucketInfo b = new BucketInfo(neighborDpid,
                        MacAddress.of(srConfig.getRouterMac()),
                        getNeighborRouterMacAddress(neighborDpid),
                        sp, curLabel, bos, -1);
                buckets.add(b);
            }
            innermostGroupId = getNextFreeGroupId();
            EcmpInfo ecmpInfo = new EcmpInfo(innermostGroupId,
                    OFGroupType.SELECT, buckets);
            setEcmpGroup(ecmpInfo);
            userDefinedGroups.put(innermostGroupId, ecmpInfo);

            groupIdList.add(innermostGroupId);
            return groupIdList;
        }

        /* If the label stack has two or more labels, then a chain of groups
         * to be created.
         * Step1: Create for each port in the list of specified ports,
         * an indirect group with the outermost label. These groups are the
         * end of the chain and hence don't reference to any other groups
         * Step2: Create for each port in the list of specified ports, an
         * indirect group with middle labels (if any). These groups will
         * have references to group ids that are created in the previous
         * iteration for the same ports
         * Step3: Create a select group with all ports and innermost label.
         * This group will have references to indirect group ids that are
         * created in the previous iteration for the same ports
         */
        for (int i = 0; i < labelStackSize; i++) {
            for (PortNumber sp : activePorts) {
                if (i == 0) {
                    /* Outermost label processing */
                    int currGroupId = getNextFreeGroupId();
                    EcmpInfo indirectGroup = createIndirectGroup(currGroupId,
                            null, null, sp, -1,
                            labelStack.get(i).intValue(), false);
                    lastSetOfGroupIds.put(sp, currGroupId);
                    userDefinedGroups.put(currGroupId, indirectGroup);
                    groupIdList.add(currGroupId);
                }
                else if (i == (labelStackSize - 1)) {
                    /* Innermost label processing */
                    innermostGroupId = getNextFreeGroupId();
                    EcmpInfo topLevelGroup = createInnermostLabelGroup(
                            innermostGroupId,
                            activePorts,
                            labelStack.get(i).intValue(), true,
                            lastSetOfGroupIds);
                    userDefinedGroups.put(
                            innermostGroupId, topLevelGroup);
                    groupIdList.add(innermostGroupId);
                    break;
                }
                else {
                    /* Middle label processing */
                    int currGroupId = getNextFreeGroupId();
                    EcmpInfo indirectGroup = createIndirectGroup(currGroupId,
                            null, null, null,
                            lastSetOfGroupIds.get(sp),
                            labelStack.get(i).intValue(), false);
                    /* Overwrite with this iteration's group IDs */
                    lastSetOfGroupIds.put(sp, currGroupId);
                    userDefinedGroups.put(currGroupId, indirectGroup);
                    groupIdList.add(currGroupId);
                }
            }
        }
        log.debug("createGroup in sw{}: group created with innermost group id {}",
                getStringId(), innermostGroupId);
        return groupIdList;
    }
    
    public List<GroupChain> createGroupChain(List<GroupChainParams> groupChainParams) {
    	List<GroupChain> groupChains = new ArrayList<GroupChain>();
    	
        List<BucketInfo> buckets = new ArrayList<BucketInfo>();
    	for (GroupChainParams i: groupChainParams) {
    		List<PortNumber> ports = i.getPorts();
    		if (ports == null) {
                log.warn("createGroupChain in sw {} with wrong "
                		+ "input parameters", getStringId());
    			return null;
    		}

    		List<PortNumber> activePorts = new ArrayList<PortNumber>();
            for (PortNumber port : ports) {
                if (portEnabled((int) port.value()))
                    activePorts.add(port);
            }
            if (activePorts.isEmpty()) {
                log.warn("createGroupChain in sw {} with no "
                		+ "active ports for groupChainParams id {}", 
                		getStringId(), i.getId());
                continue;
            }
            
            int labelStackSize = (i.getLabelStack() != null) ? 
            		i.getLabelStack().size() : 0;
            GroupChain groupChain = new GroupChain(i.getId());
            groupChains.add(groupChain);
            
    		if (labelStackSize > 0) {
				for (PortNumber sp : activePorts) {
					int previousGroupId = -1;
	    			for (int idx=0; idx < i.getLabelStack().size(); idx++) {
	    				if (idx == (labelStackSize - 1)) {
	                        int label = i.getLabelStack().get(idx).intValue();
	                        Dpid neighborDpid = portToNeighbors.get(sp);
	                        BucketInfo b = new BucketInfo(neighborDpid,
	                                MacAddress.of(srConfig.getRouterMac()),
	                                getNeighborRouterMacAddress(neighborDpid),
	                                sp, label, true, previousGroupId);
	                        buckets.add(b);
	    				}
	    				else {
							int currGroupId = getNextFreeGroupId();
							EcmpInfo indirectGroup = createIndirectGroup(currGroupId,
									null, null, sp, previousGroupId,
									i.getLabelStack().get(idx).intValue(), false);
							previousGroupId = currGroupId;
							userDefinedGroups.put(currGroupId, indirectGroup);
							groupChain.addGroupToChain(sp, currGroupId);
	    				}
    				}
    			}
    		}
    		else
    		{
                for (PortNumber sp : activePorts) {
                    Dpid neighborDpid = portToNeighbors.get(sp);
                    BucketInfo b = new BucketInfo(neighborDpid,
                            MacAddress.of(srConfig.getRouterMac()),
                            getNeighborRouterMacAddress(neighborDpid),
                            sp, -1, false, -1);
                    buckets.add(b);
                }
    		}
    	}
    	
    	if (!buckets.isEmpty()) {
            int innermostGroupId = getNextFreeGroupId();
    		EcmpInfo ecmpInfo = new EcmpInfo(innermostGroupId,
    				OFGroupType.SELECT, buckets);
    		setEcmpGroup(ecmpInfo);
            userDefinedGroups.put(innermostGroupId, ecmpInfo);
            for (GroupChain groupChain:groupChains) {
            	groupChain.setInnermostGroupId(innermostGroupId);
            }
    		log.debug(
    				"createInnermostLabelGroup: Creating select group {} in sw {} "
    						+ "with: {}", innermostGroupId, getStringId(), ecmpInfo);
    	}
    	
    	return groupChains;
    }

    /**
     * Remove the specified group
     *
     * @param groupId group identifier
     * @return success/fail
     */
    public boolean removeGroup(int groupId) {
        EcmpInfo group = userDefinedGroups.get(groupId);
        if (group == null) {
            log.warn("removeGroup in sw {}: with invalid group id", getStringId());
            return false;
        }
        deleteGroup(group);
        /*
        for (BucketInfo bucket : group.buckets) {
            int currGroupIdToBeDeleted = bucket.togroupNo;
            while (currGroupIdToBeDeleted != -1) {
                // Assuming indirect groups with single buckets
                int nextGroupIdToBeDeleted =
                        userDefinedGroups.get(currGroupIdToBeDeleted).
                        buckets.get(0).togroupNo;
                EcmpInfo groupToBeDeleted =
                        userDefinedGroups.get(currGroupIdToBeDeleted);
                deleteGroup(groupToBeDeleted);
                userDefinedGroups.remove(currGroupIdToBeDeleted);
                currGroupIdToBeDeleted = nextGroupIdToBeDeleted;
            }
        }
        */

        userDefinedGroups.remove(groupId);
        log.debug("removeGroup in sw {}: removed group with group id {}",
                getStringId(), groupId);
        return true;
    }

    @Override
    public Map<String, String> getPublishAttributes() {
        return publishAttributes;
    }

    public void removePortFromGroups(PortNumber port) {
        log.debug("removePortFromGroups: Remove port {} from Switch {}",
                port, getStringId());
        HashSet<NeighborSet> portNSSet = portNeighborSetMap.get(port);
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
            EcmpInfo portEcmpInfo = nsToGroups.get(ns);
            if (portEcmpInfo == null) {
                /* No groups present for this neighbor set */
                continue;
            }
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
        if (!portEnabled((int) (port.value())))
        {
            log.warn("addPortToGroups: Switch {} port {} is not ACTIVE",
                    getStringId(), port);
            return;
        }
        HashSet<NeighborSet> portNSSet = portNeighborSetMap.get(port);
        if (portNSSet == null) {
            /* Unknown Port  */
            log.warn("addPortToGroups: Switch {} port {} is unknown or "
                    + "there were no groups existing with the neighbor dpid"
                    + "that it is reachable through this port",
                    getStringId(), port);
            return;
        }
        log.debug("addPortToGroups: Neighborsets that the port {} is part"
                + "of on Switch {} are {}",
                port, getStringId(), portNSSet);

        Dpid neighborDpid = portToNeighbors.get(port);
        for (NeighborSet ns : portNSSet) {
            EcmpInfo portEcmpInfo = nsToGroups.get(ns);
            /* Find if this port is already part of any bucket
             * in this group
             * NOTE: This is needed because in some cases
             * (such as for configured network nodes), both driver and
             * application detect the network elements and creates the
             * buckets in the same group. This check is to avoid
             * duplicate bucket creation in such scenarios
             */
            List<BucketInfo> buckets = null;

            if (portEcmpInfo != null) {
                buckets = portEcmpInfo.buckets;
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
            }
            else
            {
                /* The group is getting created for the first time
                 * for this neighborset
                 */
                buckets = new ArrayList<BucketInfo>();
            }
            BucketInfo b = new BucketInfo(neighborDpid,
                    MacAddress.of(srConfig.getRouterMac()),
                    getNeighborRouterMacAddress(neighborDpid, ns.getOutPktType()),
                    port,
                    ns.getEdgeLabel(), true, -1);
            buckets.add(b);
            if (portEcmpInfo != null) {
                log.debug("addPortToGroups: Modifying Group on Switch {} "
                        + "and Neighborset {} with {}",
                        getStringId(), ns, portEcmpInfo);
                modifyEcmpGroup(portEcmpInfo);
            }
            else {
                Collections.sort(buckets);
                List<Integer> groupIds = auditBucketToGroupIds.get(buckets);
                int groupId;
                if (groupIds != null) {
                    /* Pop the top most group Ids for this list of buckets */
                    groupId = groupIds.get(0);
                    groupIds.remove(0);
                    if (groupIds.isEmpty())
                        auditBucketToGroupIds.remove(buckets);
                    EcmpInfo ecmpInfo = new EcmpInfo(groupId, OFGroupType.SELECT, buckets);
                    nsToGroups.put(ns, ecmpInfo);
                    log.debug(
                            "addPortToGroups: No group created in sw {} "
                                    + "for neighborset {} as group already "
                                    + "exists with groupId {}",
                            getStringId(), ns, groupId);
                }
                else {
                    groupId = getNextFreeGroupId();
                    portEcmpInfo = new EcmpInfo(groupId, OFGroupType.SELECT, buckets);
                    log.debug("addPortToGroups: Creating Group on Switch {} "
                            + "and Neighborset {} with {}",
                            getStringId(), ns, portEcmpInfo);
                    setEcmpGroup(portEcmpInfo);
                    nsToGroups.put(ns, portEcmpInfo);
                    ecmpGroups.put(groupId, portEcmpInfo);
                }
            }

        }
        return;
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

    @SuppressWarnings("unused")
    private void testMultipleLabels() {
        if (getId() == 1) {
            List<OFMessage> msglist = new ArrayList<OFMessage>();

            // first all the indirect groups

            // the group to switch 2 with outer label
            OFGroup g1 = OFGroup.of(201);
            OFOxmEthDst dmac1 = factory.oxms().ethDst(MacAddress.of("00:00:02:02:02:80"));
            OFAction push1 = factory.actions().pushMpls(EthType.MPLS_UNICAST);
            OFOxmMplsLabel lid1 = factory.oxms()
                    .mplsLabel(U32.of(105)); // outer label
            OFAction setMpls1 = factory.actions().buildSetField()
                    .setField(lid1).build();
            OFOxmMplsBos bos1 = factory.oxms()
                    .mplsBos(OFBooleanValue.FALSE);
            OFAction setB1 = factory.actions().buildSetField()
                    .setField(bos1).build();
            OFAction setDA1 = factory.actions().buildSetField()
                    .setField(dmac1).build();
            OFAction outp1 = factory.actions().buildOutput()
                    .setPort(OFPort.of(2))
                    .build();
            List<OFAction> a1 = new ArrayList<OFAction>();
            a1.add(push1);
            a1.add(setMpls1);
            a1.add(setB1);
            a1.add(setDA1);
            a1.add(outp1);
            OFBucket b1 = factory.buildBucket()
                    .setActions(a1)
                    .build();
            OFMessage gm1 = factory.buildGroupAdd()
                    .setGroup(g1)
                    .setBuckets(Collections.singletonList(b1))
                    .setGroupType(OFGroupType.INDIRECT)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(gm1);

            // the group to switch 3 with outer label
            OFGroup g2 = OFGroup.of(301);
            OFOxmEthDst dmac2 = factory.oxms().ethDst(MacAddress.of("00:00:03:03:03:80"));
            OFAction push2 = factory.actions().pushMpls(EthType.MPLS_UNICAST);
            OFOxmMplsLabel lid2 = factory.oxms()
                    .mplsLabel(U32.of(104)); // outer label
            OFAction setMpls2 = factory.actions().buildSetField()
                    .setField(lid2).build();
            OFOxmMplsBos bos2 = factory.oxms()
                    .mplsBos(OFBooleanValue.FALSE);
            OFAction setB2 = factory.actions().buildSetField()
                    .setField(bos2).build();
            OFAction setDA2 = factory.actions().buildSetField()
                    .setField(dmac2).build();
            OFAction outp2 = factory.actions().buildOutput()
                    .setPort(OFPort.of(3))
                    .build();
            List<OFAction> a2 = new ArrayList<OFAction>();
            a2.add(push2);
            a2.add(setMpls2);
            a2.add(setB2);
            a2.add(setDA2);
            a2.add(outp2);
            OFBucket b2 = factory.buildBucket()
                    .setActions(a2)
                    .build();
            OFMessage gm2 = factory.buildGroupAdd()
                    .setGroup(g2)
                    .setBuckets(Collections.singletonList(b2))
                    .setGroupType(OFGroupType.INDIRECT)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(gm2);

            // now add main ECMP group with inner labels
            OFGroup group = OFGroup.of(786);
            List<OFBucket> buckets = new ArrayList<OFBucket>();
            for (int i = 0; i < 2; i++) { // 2 buckets

                List<OFAction> actions = new ArrayList<OFAction>();
                OFOxmEthSrc smac = factory.oxms()
                        .ethSrc(MacAddress.of("00:00:01:01:01:80"));
                OFAction setSA = factory.actions().buildSetField()
                        .setField(smac).build();
                actions.add(setSA);

                if (i == 0) {
                    // send to switch 2
                    OFAction pushX = factory.actions().pushMpls(EthType.MPLS_UNICAST);
                    OFOxmMplsLabel lidX = factory.oxms()
                            .mplsLabel(U32.of(106)); // inner label
                    OFAction setX = factory.actions().buildSetField()
                            .setField(lidX).build();
                    OFOxmMplsBos bosX = factory.oxms()
                            .mplsBos(OFBooleanValue.TRUE);
                    OFAction setBX = factory.actions().buildSetField()
                            .setField(bosX).build();
                    OFAction ogX = factory.actions().buildGroup()
                            .setGroup(g1).build();
                    actions.add(pushX);
                    actions.add(setX);
                    actions.add(setBX);
                    actions.add(ogX);

                } else {
                    // send to switch 3
                    OFAction pushY = factory.actions().pushMpls(EthType.MPLS_UNICAST);
                    OFOxmMplsLabel lidY = factory.oxms()
                            .mplsLabel(U32.of(106)); // inner label
                    OFAction setY = factory.actions().buildSetField()
                            .setField(lidY).build();
                    OFOxmMplsBos bosY = factory.oxms()
                            .mplsBos(OFBooleanValue.TRUE);
                    OFAction setBY = factory.actions().buildSetField()
                            .setField(bosY).build();
                    OFAction ogY = factory.actions().buildGroup()
                            .setGroup(g2).build();
                    actions.add(pushY);
                    actions.add(setY);
                    actions.add(setBY);
                    actions.add(ogY);
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

            // create an ACL entry to use this ecmp group
            Builder matchBuilder = factory.buildMatch();
            matchBuilder.setExact(MatchField.ETH_TYPE, EthType.of(0x800));
            matchBuilder.setMasked(MatchField.IPV4_DST,
                    IPv4Address.of("7.7.7.0")
                            .withMaskOfLength(24));

            OFAction grp = factory.actions().buildGroup()
                    .setGroup(OFGroup.of(786))
                    .build();
            List<OFAction> writeActions = Collections.singletonList(grp);

            OFInstruction clearInstr = factory.instructions().clearActions();
            OFInstruction writeInstr = factory.instructions().buildWriteActions()
                    .setActions(writeActions).build();
            List<OFInstruction> instructions = new ArrayList<OFInstruction>();
            instructions.add(clearInstr);
            instructions.add(writeInstr);

            OFFlowMod.Builder fmBuilder = factory.buildFlowAdd();

            OFMessage aclFlow = fmBuilder
                    .setTableId(TableId.of(aclTableId))
                    .setMatch(matchBuilder.build())
                    .setInstructions(instructions)
                    .setPriority(10) // TODO: wrong - should be MA
                                     // priority
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setIdleTimeout(0)
                    .setHardTimeout(0)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(aclFlow);

            try {
                write(msglist);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unused")
    private void testDifferentLabelStacks() {
        if (getId() == 281483173139394L) { // 106
            List<OFMessage> msglist = new ArrayList<OFMessage>();

            // first all the indirect groups

            // the group to switch 105 with outer label
            OFGroup g1 = OFGroup.of(201);
            OFAction push1 = factory.actions().pushMpls(EthType.MPLS_UNICAST);
            OFOxmMplsLabel lid1 = factory.oxms()
                    .mplsLabel(U32.of(102)); // outer label
            OFAction setMpls1 = factory.actions().buildSetField()
                    .setField(lid1).build();
            OFOxmMplsBos bos1 = factory.oxms()
                    .mplsBos(OFBooleanValue.FALSE);
            OFAction setB1 = factory.actions().buildSetField()
                    .setField(bos1).build();
            OFAction outp1 = factory.actions().buildOutput()
                    .setPort(OFPort.of(7))
                    .build();
            List<OFAction> a1 = new ArrayList<OFAction>();
            a1.add(push1);
            a1.add(setMpls1);
            a1.add(setB1);
            a1.add(outp1);
            OFBucket b1 = factory.buildBucket()
                    .setActions(a1)
                    .build();
            OFMessage gm1 = factory.buildGroupAdd()
                    .setGroup(g1)
                    .setBuckets(Collections.singletonList(b1))
                    .setGroupType(OFGroupType.INDIRECT)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(gm1);

            // the group to switch 104 with outer label
            OFGroup g2 = OFGroup.of(301);
            OFAction push2 = factory.actions().pushMpls(EthType.MPLS_UNICAST);
            OFOxmMplsLabel lid2 = factory.oxms()
                    .mplsLabel(U32.of(103)); // outer label
            OFAction setMpls2 = factory.actions().buildSetField()
                    .setField(lid2).build();
            OFOxmMplsBos bos2 = factory.oxms()
                    .mplsBos(OFBooleanValue.FALSE);
            OFAction setB2 = factory.actions().buildSetField()
                    .setField(bos2).build();
            OFAction outp2 = factory.actions().buildOutput()
                    .setPort(OFPort.of(4))
                    .build();
            List<OFAction> a2 = new ArrayList<OFAction>();
            a2.add(push2);
            a2.add(setMpls2);
            a2.add(setB2);
            a2.add(outp2);
            OFBucket b2 = factory.buildBucket()
                    .setActions(a2)
                    .build();
            OFMessage gm2 = factory.buildGroupAdd()
                    .setGroup(g2)
                    .setBuckets(Collections.singletonList(b2))
                    .setGroupType(OFGroupType.INDIRECT)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(gm2);

            // now add main ECMP group with inner labels
            OFGroup group = OFGroup.of(786);
            List<OFBucket> buckets = new ArrayList<OFBucket>();
            for (int i = 0; i < 2; i++) { // 2 buckets

                List<OFAction> actions = new ArrayList<OFAction>();
                OFOxmEthSrc smac = factory.oxms()
                        .ethSrc(MacAddress.of("00:01:e8:8b:93:c5"));
                OFAction setSA = factory.actions().buildSetField()
                        .setField(smac).build();
                actions.add(setSA);

                if (i == 0) {
                    // send to switch 105
                    OFAction pushX = factory.actions().pushMpls(EthType.MPLS_UNICAST);
                    OFOxmMplsLabel lidX = factory.oxms()
                            .mplsLabel(U32.of(101)); // inner label
                    OFAction setX = factory.actions().buildSetField()
                            .setField(lidX).build();
                    OFOxmMplsBos bosX = factory.oxms()
                            .mplsBos(OFBooleanValue.TRUE);
                    OFAction setBX = factory.actions().buildSetField()
                            .setField(bosX).build();
                    OFAction ogX = factory.actions().buildGroup()
                            .setGroup(g1).build();
                    OFOxmEthDst dmac1 = factory.oxms().ethDst(
                            MacAddress.of("00:01:e8:8b:93:bf"));
                    OFAction setDA1 = factory.actions().buildSetField()
                            .setField(dmac1).build();
                    actions.add(setDA1);
                    actions.add(pushX);
                    actions.add(setX);
                    actions.add(setBX);
                    actions.add(ogX);


                } else {
                    // send to switch 104
                    OFOxmEthDst dmac2 = factory.oxms().ethDst(
                            MacAddress.of("00:01:e8:8b:93:b0"));
                    OFAction setDA2 = factory.actions().buildSetField()
                            .setField(dmac2).build();
                    OFAction pushY = factory.actions().pushMpls(EthType.MPLS_UNICAST);
                    OFOxmMplsLabel lidY = factory.oxms()
                            .mplsLabel(U32.of(101)); // inner label
                    OFAction setY = factory.actions().buildSetField()
                            .setField(lidY).build();
                    OFOxmMplsBos bosY = factory.oxms()
                            .mplsBos(OFBooleanValue.TRUE);
                    OFAction setBY = factory.actions().buildSetField()
                            .setField(bosY).build();
                    OFAction ogY = factory.actions().buildGroup()
                            .setGroup(g2).build();
                    actions.add(setDA2);
                    actions.add(pushY);
                    actions.add(setY);
                    actions.add(setBY);
                    actions.add(ogY);
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

            // create two ACL entries to use this ecmp group
            Builder matchBuilder1 = factory.buildMatch();
            matchBuilder1.setExact(MatchField.ETH_TYPE, EthType.of(0x800));
            matchBuilder1.setMasked(MatchField.IPV4_DST,
                    IPv4Address.of("10.200.1.0")
                            .withMaskOfLength(24));

            Builder matchBuilder2 = factory.buildMatch();
            matchBuilder2.setExact(MatchField.ETH_TYPE, EthType.of(0x800));
            matchBuilder2.setMasked(MatchField.IPV4_DST,
                    IPv4Address.of("10.200.3.0")
                            .withMaskOfLength(24));

            OFAction grp = factory.actions().buildGroup()
                    .setGroup(OFGroup.of(786))
                    .build();
            List<OFAction> writeActions = Collections.singletonList(grp);

            OFInstruction clearInstr = factory.instructions().clearActions();
            OFInstruction writeInstr = factory.instructions().buildWriteActions()
                    .setActions(writeActions).build();
            List<OFInstruction> instructions = new ArrayList<OFInstruction>();
            instructions.add(clearInstr);
            instructions.add(writeInstr);

            OFFlowMod.Builder fmBuilder = factory.buildFlowAdd();

            OFMessage aclFlow1 = fmBuilder
                    .setTableId(TableId.of(aclTableId))
                    .setMatch(matchBuilder1.build())
                    .setInstructions(instructions)
                    .setPriority(1000) // TODO: wrong - should be MA
                                     // priority
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setIdleTimeout(0)
                    .setHardTimeout(0)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(aclFlow1);

            OFFlowMod.Builder fmBuilder2 = factory.buildFlowAdd();
            OFMessage aclFlow2 = fmBuilder2
                    .setTableId(TableId.of(aclTableId))
                    .setMatch(matchBuilder2.build())
                    .setInstructions(instructions)
                    .setPriority(33333) // TODO: wrong - should be MA
                                       // priority
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setIdleTimeout(0)
                    .setHardTimeout(0)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(aclFlow2);

            try {
                write(msglist);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    protected int getVlanTableId() {
        return vlanTableId;
    }

    protected void setVlanTableId(int vlanTableId) {
        this.vlanTableId = vlanTableId;
    }

    protected int getTmacTableId() {
        return tmacTableId;
    }

    protected void setTmacTableId(int tmacTableId) {
        this.tmacTableId = tmacTableId;
    }

    protected int getIpv4UnicastTableId() {
        return ipv4UnicastTableId;
    }

    protected void setIpv4UnicastTableId(int ipv4UnicastTableId) {
        this.ipv4UnicastTableId = ipv4UnicastTableId;
    }

    protected int getMplsTableId() {
        return mplsTableId;
    }

    protected void setMplsTableId(int mplsTableId) {
        this.mplsTableId = mplsTableId;
    }

    protected int getAclTableId() {
        return aclTableId;
    }

    protected void setAclTableId(int aclTableId) {
        this.aclTableId = aclTableId;
    }
}
