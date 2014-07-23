package net.onrc.onos.api.registry;

// TODO: The "Role" enums should be moved to this file
import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.onrc.onos.core.util.Dpid;

/**
 * Switch mastership listener interface for controller role changes for
 * local switches.
 * <p/>
 * The interface can be used to track only switches that are connected
 * to this ONOS instance.
 */
public interface ILocalSwitchMastershipListener {
    /**
     * The role of this controller has changed for a switch.
     * <p/>
     * This is the method that is called when the switch connects to the
     * controller, and when the role of the controller has changed.
     *
     * @param dpid the DPID of the switch.
     * @param role the new role of this controller for the switch.
     */
    void controllerRoleChanged(Dpid dpid, Role role);

    /**
     * The switch has disconnected, and it is not tracked anymore.
     *
     * @param dpid the DPID of the switch.
     */
    void switchDisconnected(Dpid dpid);
}
