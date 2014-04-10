package net.onrc.onos.apps.bgproute;

import org.openflow.protocol.OFFlowMod;

// TODO This functionality should be handled by ONOS's flow layer in future.
/**
 * Collects together the DPID and OFFlowMod of a pushed flow mod. This
 * information is used if the flow mod has to be deleted in the future.
 */
public class PushedFlowMod {
    private final long dpid;
    private OFFlowMod flowMod;

    /**
     * Class constructor, taking a DPID and a flow mod.
     *
     * @param dpid the DPID of the switch the flow mod was pushed to
     * @param flowMod the OFFlowMod that was pushed to the switch
     */
    public PushedFlowMod(long dpid, OFFlowMod flowMod) {
        this.dpid = dpid;
        try {
            this.flowMod = flowMod.clone();
        } catch (CloneNotSupportedException e) {
            this.flowMod = flowMod;
        }
    }

    /**
     * Gets the DPID of the switch the flow mod was pushed to.
     *
     * @return the DPID of the switch
     */
    public long getDpid() {
        return dpid;
    }

    /**
     * Gets the OFFlowMod that was pushed to the switch.
     *
     * @return the OFFlowMod object
     */
    public OFFlowMod getFlowMod() {
        return flowMod;
    }
}
