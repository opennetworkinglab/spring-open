package net.onrc.onos.core.drivermanager;

import org.projectfloodlight.openflow.protocol.OFDescStatsReply;

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
}