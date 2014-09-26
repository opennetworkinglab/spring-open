package net.onrc.onos.core.drivermanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.floodlightcontroller.core.IOF13Switch;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.SwitchDriverSubHandshakeAlreadyStarted;
import net.floodlightcontroller.core.SwitchDriverSubHandshakeCompleted;
import net.floodlightcontroller.core.SwitchDriverSubHandshakeNotStarted;
import net.floodlightcontroller.core.internal.OFSwitchImplBase;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.configmanager.INetworkConfigService;
import net.onrc.onos.core.configmanager.INetworkConfigService.NetworkConfigState;
import net.onrc.onos.core.configmanager.INetworkConfigService.SwitchConfigStatus;
import net.onrc.onos.core.configmanager.NetworkConfig.LinkConfig;
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
import net.onrc.onos.core.matchaction.action.PushMplsAction;
import net.onrc.onos.core.matchaction.action.SetMplsIdAction;
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
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.util.HexString;

import com.google.common.collect.Sets;

/**
 * OFDescriptionStatistics Vendor (Manufacturer Desc.): Stanford University,
 * Ericsson Research and CPqD Research. Make (Hardware Desc.) : OpenFlow 1.3
 * Reference Userspace Switch Model (Datapath Desc.) : None Software : Serial :
 * None
 */
public class OFSwitchImplCPqD13 extends OFSwitchImplBase implements IOF13Switch {
    private static final int VLAN_ID_OFFSET = 16;
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
    private static final int TABLE_META = 4;
    private static final int TABLE_ACL = 5;

    private static final short MAX_PRIORITY = (short) 0xffff;
    private static final short PRIORITY_MULTIPLIER = (short) 2046;
    private static final short MIN_PRIORITY = 0x0;
    private static final U64 METADATA_MASK = U64.of(Long.MAX_VALUE << 1 | 0x1);

    ConcurrentHashMap<Integer, OFGroup> l2groups;

    private long barrierXidToWaitFor = -1;
    private DriverState driverState;
    private final boolean usePipeline13;
    private SegmentRouterConfig srConfig;
    private ConcurrentMap<Dpid, Set<PortNumber>> neighbors;
    private ConcurrentMap<NeighborSet, EcmpInfo> ecmpGroups;



    public OFSwitchImplCPqD13(OFDescStatsReply desc, boolean usePipeline13) {
        super();
        haltStateMachine = new AtomicBoolean(false);
        driverState = DriverState.INIT;
        driverHandshakeComplete = new AtomicBoolean(false);
        l2groups = new ConcurrentHashMap<Integer, OFGroup>();
        setSwitchDescription(desc);
        neighbors = new ConcurrentHashMap<Dpid, Set<PortNumber>>();
        ecmpGroups = new ConcurrentHashMap<NeighborSet, EcmpInfo>();
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
        if (haltStateMachine.get())
            return;
        switch (currentState) {
        case INIT:
            driverState = DriverState.SET_TABLE_MISS_ENTRIES;
            setTableMissEntries();
            sendBarrier();
            break;
        case SET_TABLE_MISS_ENTRIES:
            driverState = DriverState.SET_TABLE_VLAN_TMAC;
            getNetworkConfig();
            populateTableVlan();
            populateTableTMac();
            sendBarrier();
            break;
        case SET_TABLE_VLAN_TMAC:
            driverState = DriverState.SET_GROUPS;
            createGroups();
            sendBarrier();
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

    private void sendBarrier() throws IOException {
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
        } else {
            log.error("Switch not configured as Segment-Router");
        }

        List<LinkConfig> linkConfigList = ncs.getConfiguredAllowedLinks();
        setNeighbors(linkConfigList);
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
        List<PortNumber> portlist = new ArrayList<PortNumber>();
        for (LinkConfig lg : linkConfigList) {
            if (!lg.getType().equals(NetworkConfigManager.PKT_LINK)) {
                return;
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
        if (neighbors.get(neighborDpid) != null) {
            neighbors.get(neighborDpid).add(portToNeighbor);
        } else {
            Set<PortNumber> ports = new HashSet<PortNumber>();
            ports.add(portToNeighbor);
            neighbors.put(neighborDpid, ports);
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
        // temp map of ecmp groupings
        /*        Map<NeighborSet, List<BucketInfo>> temp =
                        new HashMap<NeighborSet, List<BucketInfo>>();
        */
        // get all combinations of neighbors
        Set<Set<Dpid>> powerSet = Sets.powerSet(dpids);
        int groupid = 1;
        for (Set<Dpid> combo : powerSet) {
            if (combo.isEmpty()) {
                // eliminate the empty set in the power set
                continue;
            }
            List<BucketInfo> buckets = new ArrayList<BucketInfo>();
            NeighborSet ns = new NeighborSet();
            for (Dpid d : combo) {
                ns.addDpid(d);
                for (PortNumber sp : neighbors.get(d)) {
                    BucketInfo b = new BucketInfo(d,
                            MacAddress.of(srConfig.getRouterMac()),
                            getNeighborRouterMacAddress(d), sp);
                    buckets.add(b);
                }
            }
            EcmpInfo ecmpInfo = new EcmpInfo(groupid++, buckets);
            setEcmpGroup(ecmpInfo);
            ecmpGroups.put(ns, ecmpInfo);
            log.debug("Creating ecmp group in sw {}: {}", getStringId(), ecmpInfo);
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

        BucketInfo(Dpid nDpid, MacAddress smac, MacAddress dmac, PortNumber p) {
            neighborDpid = nDpid;
            srcMac = smac;
            dstMac = dmac;
            outport = p;
        }

        @Override
        public String toString() {
            return " {neighborDpid: " + neighborDpid + ", dstMac: " + dstMac +
                    ", srcMac: " + srcMac + ", outport: " + outport + "}";
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
        } else if (action instanceof PushMplsAction) {
            ofAction = factory.actions().pushMpls(EthType.MPLS_UNICAST);
        } else if (action instanceof SetMplsIdAction) {
            int labelid = ((SetMplsIdAction) action).getMplsId();
            OFOxmMplsLabel lid = factory.oxms()
                    .mplsLabel(U32.of(labelid));
            ofAction = factory.actions().buildSetField()
                    .setField(lid).build();
        } else if (action instanceof PopMplsAction) {
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
                log.error("Unable to find ecmp group for neighbors {} at "
                        + "switch {}", ns, getStringId());
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
        // case MODIFY: // TODO
        // fmBuilder = factory.buildFlowModifyStrict();
        // break;
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
        // case MODIFY: // TODO
        // fmBuilder = factory.buildFlowModifyStrict();
        // break;
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
        // case MODIFY: // TODO
        // fmBuilder = factory.buildFlowModifyStrict();
        // break;
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
    // Old Hardcoded Stuff
    // *****************************

    private void configureSwitch() throws IOException {
        // setAsyncConfig();
        // getTableFeatures();
        sendGroupFeaturesRequest();
        setL2Groups();
        sendBarrier();
        setL3Groups();
        setL25Groups();
        // setEcmpGroup();
        sendGroupDescRequest();
        populateTableVlan();
        populateTableTMac();
        populateIpTable();
        populateMplsTable();
        populateTableMissEntry(TABLE_ACL, false, false, false, -1);
        sendBarrier();
    }

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

    private void getTableFeatures() throws IOException {
        OFMessage gtf = factory.buildTableFeaturesStatsRequest()
                .setXid(getNextTransactionId())
                .build();
        write(gtf, null);
    }

    private void sendGroupFeaturesRequest() throws IOException {
        OFMessage gfr = factory.buildGroupFeaturesStatsRequest()
                .setXid(getNextTransactionId())
                .build();
        write(gfr, null);
    }


    /*Create L2 interface groups for all physical ports
     Naming convention followed is the same as OF-DPA spec
     eg. port 1 with allowed vlan 10, is enveloped in group with id,
         0x0 00a 0001, where the uppermost 4 bits identify an L2 interface,
         the next 12 bits identify the vlan-id, and the lowermost 16 bits
         identify the port number.*/
    private void setL2Groups() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        for (OFPortDesc p : getPorts()) {
            int pnum = p.getPortNo().getPortNumber();
            int portVlan = getVlanConfig(pnum);
            if (U32.of(pnum).compareTo(U32.of(OFPort.MAX.getPortNumber())) < 1) {
                OFGroup gl2 = OFGroup.of(pnum | (portVlan << VLAN_ID_OFFSET));
                OFAction out = factory.actions().buildOutput()
                        .setPort(p.getPortNo()).build();
                OFAction popVlan = factory.actions().popVlan();
                List<OFAction> actions = new ArrayList<OFAction>();
                // actions.add(popVlan);
                actions.add(out);
                OFBucket bucket = factory.buildBucket()
                        .setActions(actions).build();
                List<OFBucket> buckets = Collections.singletonList(bucket);
                OFMessage gmAdd = factory.buildGroupAdd()
                        .setGroup(gl2)
                        .setBuckets(buckets)
                        .setGroupType(OFGroupType.INDIRECT)
                        .setXid(getNextTransactionId())
                        .build();
                msglist.add(gmAdd);
                l2groups.put(pnum, gl2);
            }
        }
        log.debug("Creating {} L2 groups in sw {}", msglist.size(), getStringId());
        write(msglist);
    }

    private int getVlanConfig(int portnum) {
        int portVlan = 10 * portnum;
        if ((getId() == 0x1 && portnum == 6) ||
                (getId() == 0x2) ||
                (getId() == 0x3 && portnum == 2)) {
            portVlan = 192; // 0xc0
        }
        return portVlan;
    }


    // only for ports connected to other routers
    private OFAction getDestAction(int portnum) {
        OFAction setDA = null;
        MacAddress dAddr = null;
        if (getId() == 0x1 && portnum == 6) { // connected to switch 2
            dAddr = MacAddress.of("00:00:02:02:02:80");
        }
        if (getId() == 0x1 && portnum == 7) { // connected to switch 2
            dAddr = MacAddress.of("00:00:02:02:02:80");
        }
        if (getId() == 0x2) {
            if (portnum == 1) { // connected to sw 1
                dAddr = MacAddress.of("00:00:01:01:01:80");
            } else if (portnum == 2) { // connected to sw 3
                dAddr = MacAddress.of("00:00:07:07:07:80");
            }
        }
        if (getId() == 0x3) {
            if (portnum == 2) { // connected to switch 2
                dAddr = MacAddress.of("00:00:02:02:02:80");
            }
        }

        if (dAddr != null) {
            OFOxmEthDst dstAddr = factory.oxms().ethDst(dAddr);
            setDA = factory.actions().buildSetField()
                    .setField(dstAddr).build();
        }
        return setDA;
    }

    /*
     * L3 groups are created for all router ports and they all point to corresponding
     * L2 groups. Only the ports that connect to other routers will have the
     * DA set.
     */
    private void setL3Groups() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        for (OFGroup gl2 : l2groups.values()) {
            int gnum = gl2.getGroupNumber();
            int portnum = gnum & 0x0000ffff;
            int vlanid = ((gnum & 0x0fff0000) >> VLAN_ID_OFFSET);
            MacAddress sAddr = getRouterMacAddr();

            OFGroup gl3 = OFGroup.of(0x20000000 | portnum);
            OFAction group = factory.actions().buildGroup()
                    .setGroup(gl2).build();
            OFOxmEthSrc srcAddr = factory.oxms().ethSrc(sAddr);
            OFAction setSA = factory.actions().buildSetField()
                    .setField(srcAddr).build();
            OFOxmVlanVid vid = factory.oxms().vlanVid(OFVlanVidMatch.ofVlan(vlanid));
            OFAction setVlan = factory.actions().buildSetField()
                    .setField(vid).build();
            OFAction decTtl = factory.actions().decNwTtl();

            List<OFAction> actions = new ArrayList<OFAction>();
            actions.add(decTtl); // decrement the IP TTL/do-checksum/check TTL
                                 // and MTU
            // actions.add(setVlan); // set the vlan-id of the exit-port (and
            // l2group)
            actions.add(setSA); // set this routers mac address
            // make L3Unicast group setDA for known (configured) ports
            // that connect to other routers
            OFAction setDA = getDestAction(portnum);
            if (setDA != null)
                actions.add(setDA);
            actions.add(group);

            OFBucket bucket = factory.buildBucket()
                    .setActions(actions).build();
            List<OFBucket> buckets = Collections.singletonList(bucket);
            OFMessage gmAdd = factory.buildGroupAdd()
                    .setGroup(gl3)
                    .setBuckets(buckets)
                    .setGroupType(OFGroupType.INDIRECT)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(gmAdd);
        }
        write(msglist);
        log.debug("Creating {} L3 groups in sw {}", msglist.size(), getStringId());
    }

    /*
     * L2.5 or mpls-unicast groups are only created for those router ports
     * connected to other router ports. They differ from the corresponding
     * L3-unicast group only by the fact that they decrement the MPLS TTL
     * instead of the IP ttl
     */
    private void setL25Groups() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        for (OFGroup gl2 : l2groups.values()) {
            int gnum = gl2.getGroupNumber();
            int portnum = gnum & 0x0000ffff;
            int vlanid = ((gnum & 0x0fff0000) >> VLAN_ID_OFFSET);
            MacAddress sAddr = getRouterMacAddr();
            OFAction setDA = getDestAction(portnum);
            // setDA will only be non-null for ports connected to routers
            if (setDA != null) {
                OFGroup gl3 = OFGroup.of(0xa0000000 | portnum); // different id
                                                                // for mpls
                                                                // group
                OFAction group = factory.actions().buildGroup()
                        .setGroup(gl2).build();
                OFOxmEthSrc srcAddr = factory.oxms().ethSrc(sAddr);
                OFAction setSA = factory.actions().buildSetField()
                        .setField(srcAddr).build();
                OFOxmVlanVid vid = factory.oxms().vlanVid(OFVlanVidMatch.ofVlan(vlanid));
                OFAction setVlan = factory.actions().buildSetField()
                        .setField(vid).build();
                OFAction decMplsTtl = factory.actions().decMplsTtl();
                List<OFAction> actions = new ArrayList<OFAction>();
                actions.add(decMplsTtl); // decrement the MPLS
                                         // TTL/do-checksum/check TTL and MTU
                // actions.add(setVlan); // set the vlan-id of the exit-port
                // (and
                                      // l2group)
                actions.add(setSA); // set this routers mac address
                actions.add(setDA);
                actions.add(group);
                OFBucket bucket = factory.buildBucket()
                        .setActions(actions).build();
                List<OFBucket> buckets = Collections.singletonList(bucket);
                OFMessage gmAdd = factory.buildGroupAdd()
                        .setGroup(gl3)
                        .setBuckets(buckets)
                        .setGroupType(OFGroupType.INDIRECT)
                        .setXid(getNextTransactionId())
                        .build();
                msglist.add(gmAdd);
            }
        }
        write(msglist);
        log.debug("Creating {} MPLS groups in sw {}", msglist.size(), getStringId());
    }


    private void processGroupFeatures(OFGroupFeaturesStatsReply gfsr) {
        log.info("Sw: {} Group Features {}", getStringId(), gfsr);
    }



    private List<String> getMyIps() { // send to controller
        List<String> myIps = new ArrayList<String>();
        if (getId() == 0x1) {
            myIps.add("10.0.2.128");
            myIps.add("10.0.3.128");
            myIps.add("10.0.1.128");
            myIps.add("192.168.0.1");
        }
        if (getId() == 0x2) {
            myIps.add("192.168.0.2");
        }
        if (getId() == 0x3) {
            myIps.add("192.168.0.3");
            myIps.add("7.7.7.128");
        }
        return myIps;
    }

    private List<String> getMySubnetIps() { // send to controller
        List<String> subnetIps = new ArrayList<String>();
        if (getId() == 0x1) {
            subnetIps.add("10.0.2.0");
            subnetIps.add("10.0.3.0");
            subnetIps.add("10.0.1.0");
        }
        if (getId() == 0x2) {
        }
        if (getId() == 0x3) {
            subnetIps.add("7.7.7.0");
        }
        return subnetIps;
    }

    private class RouteEntry {
        String prefix;
        String mask;
        int nextHopPort;
        String dstMac;
        int label;

        public RouteEntry(String prefix, String mask, int nextHopPort, int label) {
            this.prefix = prefix;
            this.mask = mask;
            this.nextHopPort = nextHopPort;
            this.label = label;
        }

        public RouteEntry(String prefix, int nextHopPort, String dstMac) {
            this.prefix = prefix;
            this.nextHopPort = nextHopPort;
            this.dstMac = dstMac;
        }
    }

    // send out of mpls-group where the next-hop mac-da is already set
    private List<RouteEntry> getRouterNextHopIps() {
        List<RouteEntry> routerNextHopIps = new ArrayList<RouteEntry>();
        if (getId() == 0x1) {
            routerNextHopIps
                    .add(new RouteEntry("192.168.0.2", "255.255.255.255", 6, 102));
            routerNextHopIps
                    .add(new RouteEntry("192.168.0.3", "255.255.255.255", 6, 103));
            routerNextHopIps.add(new RouteEntry("7.7.7.0", "255.255.255.0", 6, 103));
        }
        if (getId() == 0x2) {
            /* These are required for normal IP routing without labels.
            routerNextHopIps.add(new RouteEntry("192.168.0.1","255.255.255.255",1));
            routerNextHopIps.add(new RouteEntry("192.168.0.3","255.255.255.255",2));
            routerNextHopIps.add(new RouteEntry("10.0.1.0","255.255.255.0",1));
            routerNextHopIps.add(new RouteEntry("10.0.2.0","255.255.255.0",1));
            routerNextHopIps.add(new RouteEntry("10.0.3.0","255.255.255.0",1));
            routerNextHopIps.add(new RouteEntry("7.7.7.0","255.255.255.0",2));*/
        }
        if (getId() == 0x3) {
            routerNextHopIps
                    .add(new RouteEntry("192.168.0.2", "255.255.255.255", 2, 102));
            routerNextHopIps
                    .add(new RouteEntry("192.168.0.1", "255.255.255.255", 2, 101));
            routerNextHopIps.add(new RouteEntry("10.0.1.0", "255.255.255.0", 2, 101));
            routerNextHopIps.add(new RouteEntry("10.0.2.0", "255.255.255.0", 2, 101));
            routerNextHopIps.add(new RouteEntry("10.0.3.0", "255.255.255.0", 2, 101));
        }
        return routerNextHopIps;
    }

    // known host mac-addr, setDA/send out of l3group
    private List<RouteEntry> getHostNextHopIps() {
        List<RouteEntry> hostNextHopIps = new ArrayList<RouteEntry>();
        if (getId() == 0x1) {
            //hostNextHopIps.add(new RouteEntry("10.0.1.1", 1, "00:00:00:00:01:01")); // Just for Test - SSH
            hostNextHopIps.add(new RouteEntry("10.0.2.1", 4, "00:00:00:00:02:01"));
            hostNextHopIps.add(new RouteEntry("10.0.3.1", 5, "00:00:00:00:03:01"));
        }
        if (getId() == 0x2) {
        }
        if (getId() == 0x3) {
            hostNextHopIps.add(new RouteEntry("7.7.7.7", 1, "00:00:07:07:07:07"));
        }
        return hostNextHopIps;
    }

    private void populateIpTable() throws IOException {
        // populateMyIps();
        // populateMySubnets();
        populateRoutes();
        populateHostRoutes();

        // match for everything else to send to ACL table. Essentially
        // the table miss flow entry
        populateTableMissEntry(TABLE_IPv4_UNICAST, false, true,
                true, TABLE_ACL);
    }

    private void populateMyIps() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        // first all my ip's as exact-matches
        // write-action instruction to send to controller
        List<String> myIps = getMyIps();
        for (int i = 0; i < myIps.size(); i++) {
            OFOxmEthType ethTypeIp = factory.oxms()
                    .ethType(EthType.IPv4);
            OFOxmIpv4DstMasked ipPrefix = factory.oxms()
                    .ipv4DstMasked(IPv4Address.of(myIps.get(i)), IPv4Address.NO_MASK);
            OFOxmList oxmListSlash32 = OFOxmList.of(ethTypeIp, ipPrefix);
            OFMatchV3 match = factory.buildMatchV3()
                    .setOxmList(oxmListSlash32).build();
            OFAction outc = factory.actions().buildOutput()
                    .setPort(OFPort.CONTROLLER).setMaxLen(OFPCML_NO_BUFFER)
                    .build();
            OFInstruction writeInstr = factory.instructions().buildWriteActions()
                    .setActions(Collections.singletonList(outc)).build();
            OFInstruction gotoInstr = factory.instructions().buildGotoTable()
                    .setTableId(TableId.of(TABLE_ACL)).build();
            List<OFInstruction> instructions = new ArrayList<OFInstruction>();
            instructions.add(writeInstr);
            instructions.add(gotoInstr);
            OFMessage myIpEntry = factory.buildFlowAdd()
                    .setTableId(TableId.of(TABLE_IPv4_UNICAST))
                    .setMatch(match)
                    .setInstructions(instructions)
                    .setPriority(MAX_PRIORITY) // highest priority for exact
                                               // match
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setIdleTimeout(0)
                    .setHardTimeout(0)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(myIpEntry);
        }
        write(msglist);
        log.debug("Adding {} my-ip-rules in sw {}", msglist.size(), getStringId());
    }

    private void populateMySubnets() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        // next prefix-based subnet-IP's configured on my interfaces
        // need to ARP for exact-IP, so write-action instruction to send to
        // controller
        // this has different mask and priority than earlier case
        List<String> subnetIps = getMySubnetIps();
        for (int i = 0; i < subnetIps.size(); i++) {
            OFOxmEthType ethTypeIp = factory.oxms()
                    .ethType(EthType.IPv4);
            OFOxmIpv4DstMasked ipPrefix = factory.oxms().ipv4DstMasked(
                    IPv4Address.of(subnetIps.get(i)),
                    IPv4Address.of(0xffffff00)); // '/24' mask
            OFOxmList oxmListSlash24 = OFOxmList.of(ethTypeIp, ipPrefix);
            OFMatchV3 match = factory.buildMatchV3()
                    .setOxmList(oxmListSlash24).build();
            OFAction outc = factory.actions().buildOutput()
                    .setPort(OFPort.CONTROLLER).setMaxLen(OFPCML_NO_BUFFER)
                    .build();
            OFInstruction writeInstr = factory.instructions().buildWriteActions()
                    .setActions(Collections.singletonList(outc)).build();
            OFInstruction gotoInstr = factory.instructions().buildGotoTable()
                    .setTableId(TableId.of(TABLE_ACL)).build();
            List<OFInstruction> instructions = new ArrayList<OFInstruction>();
            instructions.add(writeInstr);
            instructions.add(gotoInstr);
            OFMessage myIpEntry = factory.buildFlowAdd()
                    .setTableId(TableId.of(TABLE_IPv4_UNICAST))
                    .setMatch(match)
                    .setInstructions(instructions)
                    .setPriority((short) 0xfff0)
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setIdleTimeout(0)
                    .setHardTimeout(0)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(myIpEntry);
        }
        write(msglist);
        log.debug("Adding {} subnet-ip-rules in sw {}", msglist.size(), getStringId());
        msglist.clear();
    }

    private void populateRoutes() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        // addresses where I know the next-hop's mac-address because it is a
        // router port - so I have an L3 interface to it (and an MPLS interface)
        List<RouteEntry> routerNextHopIps = getRouterNextHopIps();
        for (int i = 0; i < routerNextHopIps.size(); i++) {
            OFOxmEthType ethTypeIp = factory.oxms()
                    .ethType(EthType.IPv4);
            OFOxmIpv4DstMasked ipPrefix = factory.oxms()
                    .ipv4DstMasked(
                            IPv4Address.of(routerNextHopIps.get(i).prefix),
                            IPv4Address.of(routerNextHopIps.get(i).mask)
                    );
            OFOxmList oxmListSlash32 = OFOxmList.of(ethTypeIp, ipPrefix);
            OFMatchV3 match = factory.buildMatchV3()
                    .setOxmList(oxmListSlash32).build();
            OFAction outg = factory.actions().buildGroup()
                    .setGroup(OFGroup.of(0xa0000000 | // mpls group id
                            routerNextHopIps.get(i).nextHopPort))
                    .build();
            // lots of actions before forwarding to mpls group, and
            // unfortunately
            // they need to be apply-actions

            OFAction pushlabel = factory.actions().pushMpls(EthType.MPLS_UNICAST);
            OFOxmMplsLabel l = factory.oxms()
                    .mplsLabel(U32.of(routerNextHopIps.get(i).label));
            OFAction setlabelid = factory.actions().buildSetField()
                    .setField(l).build();
            OFAction copyTtlOut = factory.actions().copyTtlOut();
            // OFAction setBos =
            // factory.actions().buildSetField().setField(bos).build();

            List<OFAction> writeActions = new ArrayList<OFAction>();
            writeActions.add(pushlabel);
            writeActions.add(copyTtlOut);
            writeActions.add(setlabelid);
            // writeActions.add(setBos); no support in loxigen

            // List<OFAction> applyActions = new ArrayList<OFAction>();
            // applyActions.add(pushlabel);
            // applyActions.add(copyTtlOut);
            // OFInstruction applyInstr =
            // factory.instructions().buildApplyActions()
            // .setActions(applyActions).build();

            if (getId() == 0x1) {
                OFAction group47 = factory.actions().buildGroup()
                        .setGroup(OFGroup.of(47)).build();
                writeActions.add(group47);
            } else {
                writeActions.add(outg); // group will decr mpls-ttl, set
                                        // mac-sa/da,
                                    // vlan
            }

            OFInstruction writeInstr = factory.instructions().buildWriteActions()
                    .setActions(writeActions).build();
            OFInstruction gotoInstr = factory.instructions().buildGotoTable()
                    .setTableId(TableId.of(TABLE_ACL)).build();
            List<OFInstruction> instructions = new ArrayList<OFInstruction>();
            instructions.add(writeInstr);

            // necessary to match in pseudo-table to overcome cpqd 1.3 flaw
            /*OFInstruction writeMeta = factory.instructions().buildWriteMetadata()
                    .setMetadata(U64.of(routerNextHopIps.get(i).label))
                    .setMetadataMask(METADATA_MASK).build();
            OFInstruction gotoInstr = factory.instructions().buildGotoTable()
                    .setTableId(TableId.of(TABLE_META)).build();*/
            /*instructions.add(applyInstr);
            instructions.add(writeMeta);*/

            instructions.add(gotoInstr);
            int priority = -1;
            if (routerNextHopIps.get(i).mask.equals("255.255.255.255"))
                priority = MAX_PRIORITY;
            else
                priority = (short) 0xfff0;
            OFMessage myIpEntry = factory.buildFlowAdd()
                    .setTableId(TableId.of(TABLE_IPv4_UNICAST))
                    .setMatch(match)
                    .setInstructions(instructions)
                    .setPriority(priority)
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setIdleTimeout(0)
                    .setHardTimeout(0)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(myIpEntry);

            // need to also handle psuedo-table entries to match-metadata and
            // set mpls
            // label-id
            /*OFOxmEthType ethTypeMpls = factory.oxms()
                    .ethType(EthType.MPLS_UNICAST);
            OFOxmMetadataMasked meta = factory.oxms()
                    .metadataMasked(
                            OFMetadata.ofRaw(routerNextHopIps.get(i).label),
                            OFMetadata.NO_MASK);
            OFOxmList oxmListMeta = OFOxmList.of(ethTypeMpls, meta);
            OFMatchV3 matchMeta = factory.buildMatchV3()
                    .setOxmList(oxmListMeta).build();
            List<OFAction> writeActions2 = new ArrayList<OFAction>();
            writeActions2.add(setlabelid);
            OFAction outg2 = factory.actions().buildGroup()
                    .setGroup(OFGroup.of(routerNextHopIps.get(i).nextHopPort |
                                            (192 << VLAN_ID_OFFSET)))
                    .build();
            writeActions2.add(outg2);
            OFInstruction writeInstr2 = factory.instructions().buildWriteActions()
                    .setActions(writeActions2).build();
            OFInstruction gotoInstr2 = factory.instructions().buildGotoTable()
                    .setTableId(TableId.of(TABLE_ACL)).build();
            List<OFInstruction> instructions2 = new ArrayList<OFInstruction>();
            // unfortunately have to apply this action too
            OFInstruction applyInstr2 = factory.instructions().buildApplyActions()
                    .setActions(writeActions2).build();
            instructions2.add(applyInstr2); */
            // instructions2.add(writeInstr2);
            // instructions2.add(gotoInstr2);

            /*OFMatchV3 match3 = factory.buildMatchV3()
                    .setOxmList(OFOxmList.of(meta)).build();
            OFInstruction clearInstruction = factory.instructions().clearActions();
            List<OFInstruction> instructions3 = new ArrayList<OFInstruction>();
            OFAction outc = factory.actions().buildOutput()
                    .setPort(OFPort.CONTROLLER).setMaxLen(OFPCML_NO_BUFFER)
                    .build();
            OFInstruction writec = factory.instructions()
                    .writeActions(Collections.singletonList(outc));
            instructions3.add(clearInstruction);
            instructions3.add(writec);
            instructions3.add(gotoInstr2); */
            /*OFMessage myMetaEntry = factory.buildFlowAdd()
                    .setTableId(TableId.of(TABLE_META))
                    .setMatch(matchMeta)
                    .setInstructions(instructions2)
                    .setPriority(MAX_PRIORITY)
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setIdleTimeout(0)
                    .setHardTimeout(0)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(myMetaEntry); */

        }
        write(msglist);
        log.debug("Adding {} next-hop-router-rules in sw {}", msglist.size(),
                getStringId());

        // add a table-miss entry to table 4 for debugging - leave it out
        // unclear packet state - causes switch to crash
        // populateTableMissEntry(TABLE_META, false, true,
        // true, TABLE_ACL);
    }

    private void populateHostRoutes() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        // addresses where I know the next hop's mac-address and I can set the
        // destination mac in the match-instruction.write-action
        // either I sent out arp-request or I got an arp-request from this host
        List<RouteEntry> hostNextHopIps = getHostNextHopIps();
        for (int i = 0; i < hostNextHopIps.size(); i++) {
            OFOxmEthType ethTypeIp = factory.oxms()
                    .ethType(EthType.IPv4);
            OFOxmIpv4DstMasked ipPrefix = factory.oxms()
                    .ipv4DstMasked(
                            IPv4Address.of(hostNextHopIps.get(i).prefix),
                            IPv4Address.NO_MASK); // host addr should be /32
            OFOxmList oxmListSlash32 = OFOxmList.of(ethTypeIp, ipPrefix);
            OFMatchV3 match = factory.buildMatchV3()
                    .setOxmList(oxmListSlash32).build();
            OFAction setDmac = null, outg = null;
            OFOxmEthDst dmac = factory.oxms()
                    .ethDst(MacAddress.of(hostNextHopIps.get(i).dstMac));
            setDmac = factory.actions().buildSetField()
                    .setField(dmac).build();
            outg = factory.actions().buildGroup()
                    .setGroup(OFGroup.of(0x20000000 | hostNextHopIps.get(i).nextHopPort)) // l3group
                                                                                          // id
                    .build();
            List<OFAction> writeActions = new ArrayList<OFAction>();
            writeActions.add(setDmac);
            writeActions.add(outg);
            OFInstruction writeInstr = factory.instructions().buildWriteActions()
                    .setActions(writeActions).build();
            OFInstruction gotoInstr = factory.instructions().buildGotoTable()
                    .setTableId(TableId.of(TABLE_ACL)).build();
            List<OFInstruction> instructions = new ArrayList<OFInstruction>();
            instructions.add(writeInstr);
            instructions.add(gotoInstr);
            OFMessage myIpEntry = factory.buildFlowAdd()
                    .setTableId(TableId.of(TABLE_IPv4_UNICAST))
                    .setMatch(match)
                    .setInstructions(instructions)
                    .setPriority(MAX_PRIORITY) // highest priority for exact
                                               // match
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setIdleTimeout(0)
                    .setHardTimeout(0)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(myIpEntry);
        }
        write(msglist);
        log.debug("Adding {} next-hop-host-rules in sw {}", msglist.size(), getStringId());
    }

    private class MplsEntry {
        int labelid;
        int portnum;

        public MplsEntry(int labelid, int portnum) {
            this.labelid = labelid;
            this.portnum = portnum;
        }
    }

    private List<MplsEntry> getMplsEntries() {
        List<MplsEntry> myLabels = new ArrayList<MplsEntry>();
        if (getId() == 0x1) {
            myLabels.add(new MplsEntry(101, OFPort.CONTROLLER.getPortNumber()));
            myLabels.add(new MplsEntry(103, 6));
        }
        if (getId() == 0x2) {
            myLabels.add(new MplsEntry(103, 2));
            myLabels.add(new MplsEntry(102, OFPort.CONTROLLER.getPortNumber()));
            myLabels.add(new MplsEntry(101, 1));
        }
        if (getId() == 0x3) {
            myLabels.add(new MplsEntry(103, OFPort.CONTROLLER.getPortNumber()));
            myLabels.add(new MplsEntry(101, 2));
        }
        return myLabels;
    }

    private void populateMplsTable() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        List<MplsEntry> lfibEntries = getMplsEntries();
        for (int i = 0; i < lfibEntries.size(); i++) {
            OFOxmEthType ethTypeMpls = factory.oxms()
                    .ethType(EthType.MPLS_UNICAST);
            OFOxmMplsLabel labelid = factory.oxms()
                    .mplsLabel(U32.of(lfibEntries.get(i).labelid));
            OFOxmList oxmList = OFOxmList.of(ethTypeMpls, labelid);
            OFMatchV3 matchlabel = factory.buildMatchV3()
                    .setOxmList(oxmList).build();
            OFAction poplabel = factory.actions().popMpls(EthType.IPv4);
            OFAction copyttlin = factory.actions().copyTtlIn();
            OFAction sendTo = null;
            if (lfibEntries.get(i).portnum == OFPort.CONTROLLER.getPortNumber()) {
                sendTo = factory.actions().output(OFPort.CONTROLLER,
                        OFPCML_NO_BUFFER);
            } else {
                // after popping send to L3 intf, not MPLS intf
                sendTo = factory.actions().group(OFGroup.of(
                        0x20000000 | lfibEntries.get(i).portnum));
            }
            List<OFAction> writeActions = new ArrayList<OFAction>();
            writeActions.add(copyttlin);
            writeActions.add(poplabel);
            writeActions.add(sendTo);
            OFInstruction writeInstr = factory.instructions().buildWriteActions()
                    .setActions(writeActions).build();
            OFInstruction gotoInstr = factory.instructions().buildGotoTable()
                    .setTableId(TableId.of(TABLE_ACL)).build();
            List<OFInstruction> instructions = new ArrayList<OFInstruction>();
            instructions.add(writeInstr);
            instructions.add(gotoInstr);
            OFMessage myMplsEntry = factory.buildFlowAdd()
                    .setTableId(TableId.of(TABLE_MPLS))
                    .setMatch(matchlabel)
                    .setInstructions(instructions)
                    .setPriority(MAX_PRIORITY) // exact match and exclusive
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setIdleTimeout(0)
                    .setHardTimeout(0)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(myMplsEntry);
        }
        write(msglist);
        log.debug("Adding {} mpls-forwarding-rules in sw {}", msglist.size(),
                getStringId());

        // match for everything else to send to ACL table. Essentially
        // the table miss flow entry
        populateTableMissEntry(TABLE_MPLS, false, true,
                true, TABLE_ACL);

    }


}
