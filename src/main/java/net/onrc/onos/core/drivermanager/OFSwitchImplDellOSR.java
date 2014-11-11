package net.onrc.onos.core.drivermanager;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import net.floodlightcontroller.core.IOF13Switch.NeighborSet.groupPktType;
import net.onrc.onos.core.matchaction.MatchAction;
import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.GroupAction;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.action.PopMplsAction;
import net.onrc.onos.core.matchaction.action.SetDAAction;
import net.onrc.onos.core.matchaction.match.Ipv4Match;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.matchaction.match.MplsMatch;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.IPv4Net;
import net.onrc.onos.core.util.PortNumber;

import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFOxmList;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEthDst;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEthType;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmIpv4DstMasked;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;

/**
 * OFDescriptionStatistics Vendor (Manufacturer Desc.): Dell Make (Hardware
 * Desc.) : OpenFlow 1.3 Reference Userspace Switch Model (Datapath Desc.) :
 * None Software : Serial : None
 */
public class OFSwitchImplDellOSR extends OFSwitchImplSpringOpenTTP {

    /* Table IDs to be used for Dell Open Segment Routers*/
    private static final int DELL_TABLE_VLAN = 17;
    private static final int DELL_TABLE_TMAC = 18;
    private static final int DELL_TABLE_IPv4_UNICAST = 30;
    private static final int DELL_TABLE_MPLS = 25;
    private static final int DELL_TABLE_ACL = 40;

    private static final int MPLS_SWAP_FLOW = 0x00;
    private static final int MPLS_POP_NO_BOS_FLOW = 0x10;
    private static final int MPLS_POP_BOS_FLOW = 0x11;

    public OFSwitchImplDellOSR(OFDescStatsReply desc, boolean usePipeline13) {
        super(desc, usePipeline13);
        setVlanTableId(DELL_TABLE_VLAN);
        setTmacTableId(DELL_TABLE_TMAC);
        setIpv4UnicastTableId(DELL_TABLE_IPv4_UNICAST);
        setMplsTableId(DELL_TABLE_MPLS);
        setAclTableId(DELL_TABLE_ACL);
    }

    @Override
    protected OFOxmList getIPEntryMatchList(OFFactory ofFactory, Match match) {
        /* For Dell Switches, the IP entry match list shall
         * also include destination mac matching rule
         */
        Ipv4Match ipm = (Ipv4Match) match;

        IPv4Net ipdst = ipm.getDestination();
        OFOxmEthType ethTypeIp = ofFactory.oxms()
                .ethType(EthType.IPv4);
        OFOxmEthDst dmac = ofFactory.oxms().ethDst(getRouterMacAddr());
        OFOxmIpv4DstMasked ipPrefix = ofFactory.oxms()
                .ipv4DstMasked(
                        IPv4Address.of(ipdst.address().value()),
                        IPv4Address.ofCidrMaskLength(ipdst.prefixLen())
                );
        OFOxmList oxmList = OFOxmList.of(ethTypeIp, dmac, ipPrefix);

        return oxmList;
    }

    @Override
    protected void setTableMissEntries() throws IOException {
        // set all table-miss-entries
        populateTableMissEntry(vlanTableId, true, false, false, -1);
        populateTableMissEntry(tmacTableId, true, false, false, -1);
        populateTableMissEntry(ipv4UnicastTableId, false, true, true,
                aclTableId);
        populateTableMissEntry(mplsTableId, false, true, true,
                aclTableId);
        populateTableMissEntry(aclTableId, true, false, false, -1);
    }

    protected MacAddress getNeighborRouterMacAddress(Dpid ndpid,
            groupPktType outPktType) {
        if (outPktType == groupPktType.MPLS_OUTGOING)
            return getRouterMPLSMac(ndpid);
        else
            return super.getNeighborRouterMacAddress(ndpid,
                    groupPktType.IP_OUTGOING);
    }
    /* Dell Open Segment Router specific Implementation .
     * Gets the specified Router's MAC address to be used for MPLS flows
     * For Dell OSR, the MPLS MAC is IP MAC + 1
     */
    @Override
    public MacAddress getRouterMPLSMac(Dpid dpid) {
        return MacAddress.of(super.getRouterIPMac(dpid).getLong() + 1);
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
            for (groupPktType gType : groupPktType.values()) {
                NeighborSet ns = new NeighborSet();
                ns.addDpids(combo);
                ns.setOutPktType(gType);
                log.debug("createGroupsAtTransitRouter: sw {} combo {} ns {}",
                        getStringId(), combo, ns);
                nsSet.add(ns);
            }
        }
        log.debug("createGroupsAtTransitRouter: The neighborset with label "
                + "for sw {} is {}", getStringId(), nsSet);

        for (NeighborSet ns : nsSet) {
            updatePortNeighborSetMap(ns);
            int gid = createGroupForANeighborSet(ns);
            if (gid == -1) {
                log.warn("Create Group failed with -1");
            }
        }
    }

    protected void analyzeAndUpdateMplsActions(
            MatchActionOperationEntry mao) {
        MatchAction ma = mao.getTarget();
        MplsMatch mplsm = (MplsMatch) ma.getMatch();

        int flowType = 0x00;
        PortNumber outPort = null;
        for (Action action : ma.getActions()) {
            if (action instanceof PopMplsAction) {
                flowType |= 0x10;
                if (mplsm.isBos()) {
                    flowType |= 0x01;
                }
            }
            else if (action instanceof OutputAction)
                outPort = ((OutputAction) action).getPortNumber();
        }

        groupPktType outPktType = groupPktType.IP_OUTGOING;
        if ((flowType == MPLS_SWAP_FLOW) ||
                (flowType == MPLS_POP_NO_BOS_FLOW))
            outPktType = groupPktType.MPLS_OUTGOING;

        for (Action action : ma.getActions()) {
            if ((action instanceof GroupAction)
                    && (((GroupAction) action).getGroupId() == -1)) {
                ((GroupAction) action).getDpids().setOutPktType(outPktType);
            }
            else if (action instanceof SetDAAction) {
                if ((outPort != null) &&
                        (portToNeighbors.get(outPort) != null)) {
                    Dpid neighborDpid = portToNeighbors.get(outPort);
                    ((SetDAAction) action).setAddress(
                            getNeighborRouterMacAddress(neighborDpid, outPktType));
                }
            }
        }
    }
}