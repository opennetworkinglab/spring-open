package net.onrc.onos.core.topology;

import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.OnosInstanceId;

// TODO probably "Base" prefix is not required
/**
 * Interface to access switch mastership information in the network view.
 */
public interface BaseMastership {

    /**
     * Gets the master instance ID for a switch.
     *
     * @param dpid switch dpid
     * @return master instance ID or null if there is no master
     */
    public OnosInstanceId getSwitchMaster(Dpid dpid);

// We may need something like below in the future
//    public List<ImmutablePair<OnosInstanceId, Role>> getSwitchMasterCandidates(Dpid dpid);
}
