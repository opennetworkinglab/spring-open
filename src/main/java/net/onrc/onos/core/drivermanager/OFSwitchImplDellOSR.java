package net.onrc.onos.core.drivermanager;

import org.projectfloodlight.openflow.protocol.OFDescStatsReply;

/**
 * OFDescriptionStatistics Vendor (Manufacturer Desc.): Dell Make (Hardware
 * Desc.) : OpenFlow 1.3 Reference Userspace Switch Model (Datapath Desc.) :
 * None Software : Serial : None
 */
public class OFSwitchImplDellOSR extends OFSwitchImplSpringOpenTTP {

    public OFSwitchImplDellOSR(OFDescStatsReply desc, boolean usePipeline13) {
        super(desc, usePipeline13);
    }
}