package net.onrc.onos.core.drivermanager;

import net.floodlightcontroller.core.internal.OFSwitchImplBase;

import org.projectfloodlight.openflow.protocol.OFDescStatsReply;

/**
 * OFDescriptionStatistics Vendor (Manufacturer Desc.): Nicira, Inc. Make
 * (Hardware Desc.) : Open vSwitch Model (Datapath Desc.) : None Software :
 * 1.11.90 (or whatever version + build) Serial : None
 */
public class OFSwitchImplOVS10 extends OFSwitchImplBase {

    public OFSwitchImplOVS10(OFDescStatsReply desc) {
        super();
        setSwitchDescription(desc);
        setAttribute(SWITCH_SUPPORTS_NX_ROLE, true);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "OFSwitchImplOVS10 [" + ((channel != null)
                ? channel.getRemoteAddress() : "?")
                + " DPID[" + ((stringId != null) ? stringId : "?") + "]]";
    }
}
