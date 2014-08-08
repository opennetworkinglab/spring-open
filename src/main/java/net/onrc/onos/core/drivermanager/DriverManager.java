package net.onrc.onos.core.drivermanager;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.OFSwitchImplBase;

import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple implementation of a driver manager that differentiates between
 * connected switches using the OF Description Statistics Reply message.
 */
public final class DriverManager {

    private static final Logger log = LoggerFactory.getLogger(DriverManager.class);

    /**
     * Return an IOFSwitch object based on switch's manufacturer description
     * from OFDescStatsReply.
     *
     * @param desc DescriptionStatistics reply from the switch
     * @return A IOFSwitch instance if the driver found an implementation for
     *         the given description. Otherwise it returns OFSwitchImplBase
     */
    public static IOFSwitch getOFSwitchImpl(OFDescStatsReply desc, OFVersion ofv) {
        String vendor = desc.getMfrDesc();
        String hw = desc.getHwDesc();
        if (vendor.startsWith("Stanford University, Ericsson Research and CPqD Research")
                &&
                hw.startsWith("OpenFlow 1.3 Reference Userspace Switch")) {
            return new OFSwitchImplCPqD13(desc);
        }

        if (vendor.startsWith("Nicira") &&
                hw.startsWith("Open vSwitch")) {
            if (ofv == OFVersion.OF_10) {
                return new OFSwitchImplOVS10(desc);
            } else if (ofv == OFVersion.OF_13) {
                return new OFSwitchImplOVS13(desc);
            }
        }

        log.warn("DriverManager could not identify switch desc: {}. "
                + "Assigning OFSwitchImplBase", desc);
        OFSwitchImplBase base = new OFSwitchImplBase();
        base.setSwitchDescription(desc);
        // XXX S must set counter here - unidentified switch
        return base;
    }

    /**
     * Private constructor to avoid instantiation.
     */
    private DriverManager() {
    }
}
