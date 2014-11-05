package net.onrc.onos.core.drivermanager;

import net.onrc.onos.core.matchaction.match.Ipv4Match;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.util.IPv4Net;

import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFOxmList;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEthDst;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEthType;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmIpv4DstMasked;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;

/**
 * OFDescriptionStatistics Vendor (Manufacturer Desc.): Dell Make (Hardware
 * Desc.) : OpenFlow 1.3 Reference Userspace Switch Model (Datapath Desc.) :
 * None Software : Serial : None
 */
public class OFSwitchImplDellOSR extends OFSwitchImplSpringOpenTTP {

    /* Table IDs to be used for Dell Open Segment Routers*/
    private static final int DELL_TABLE_VLAN = 10;
    private static final int DELL_TABLE_TMAC = 20;
    private static final int DELL_TABLE_IPv4_UNICAST = 30;
    private static final int DELL_TABLE_MPLS = 25;
    private static final int DELL_TABLE_ACL = 40;

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

}