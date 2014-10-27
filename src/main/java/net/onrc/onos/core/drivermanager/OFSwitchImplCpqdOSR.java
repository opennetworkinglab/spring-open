package net.onrc.onos.core.drivermanager;

import org.projectfloodlight.openflow.protocol.OFDescStatsReply;

/**
 * OFDescriptionStatistics Vendor (Manufacturer Desc.): Stanford University,
 * Ericsson Research and CPqD Research. Make (Hardware Desc.) : OpenFlow 1.3
 * Reference Userspace Switch Model (Datapath Desc.) : None Software : Serial :
 * None
 */
public class OFSwitchImplCpqdOSR extends OFSwitchImplSpringOpenTTP {

    public OFSwitchImplCpqdOSR(OFDescStatsReply desc, boolean usePipeline13) {
        super(desc, usePipeline13);
    }
}