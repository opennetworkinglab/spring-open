package net.onrc.onos.api.intent;

import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.util.SwitchPort;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A packet layer Intent for a connectivity from a set of ports to a set of
 * ports.
 * <p>
 * TODO: Design methods to support the ReactiveForwarding and the SDN-IP. <br>
 * NOTE: Should this class support modifier methods? Should this object a
 * read-only object?
 */
public class PacketConnectivityIntent extends Intent {
    protected Set<SwitchPort> srcSwitchPorts;
    protected PacketMatch match;
    protected Set<SwitchPort> dstSwitchPorts;
    protected boolean canSetupOpticalFlow;
    protected int idleTimeoutValue;
    protected int hardTimeoutValue;

    /**
     * Creates a connectivity intent for the packet layer.
     * <p>
     * When the "canSetupOpticalFlow" option is true, this intent will compute
     * the packet/optical converged path, decompose it to the OpticalPathFlow
     * and the PacketPathFlow objects, and execute the operations to add them
     * considering the dependency between the packet and optical layers.
     *
     * @param id ID for this new Intent object.
     * @param srcSwitchPorts The set of source switch ports.
     * @param match Traffic specifier for this object.
     * @param dstSwitchPorts The set of destination switch ports.
     * @param canSetupOpticalFlow The flag whether this intent can create
     *        optical flows if needed.
     */
    public PacketConnectivityIntent(IntentId id,
            Collection<SwitchPort> srcSwitchPorts, PacketMatch match,
            Collection<SwitchPort> dstSwitchPorts, boolean canSetupOpticalFlow) {
        super(id);
        this.srcSwitchPorts = new HashSet<SwitchPort>(srcSwitchPorts);
        this.match = match;
        this.dstSwitchPorts = new HashSet<SwitchPort>(dstSwitchPorts);
        this.canSetupOpticalFlow = canSetupOpticalFlow;
        this.idleTimeoutValue = 0;
        this.hardTimeoutValue = 0;

        // TODO: check consistency between these parameters.
    }

    /**
     * Gets the set of source switch ports.
     *
     * @return the set of source switch ports.
     */
    public Collection<SwitchPort> getSrcSwitchPorts() {
        return Collections.unmodifiableCollection(srcSwitchPorts);
    }

    /**
     * Gets the traffic specifier.
     *
     * @return The traffic specifier.
     */
    public PacketMatch getMatch() {
        return match;
    }

    /**
     * Gets the set of destination switch ports.
     *
     * @return the set of destination switch ports.
     */
    public Collection<SwitchPort> getDstSwitchPorts() {
        return Collections.unmodifiableCollection(dstSwitchPorts);
    }

    /**
     * Adds the specified port to the set of source ports.
     *
     * @param port SwitchPort object to be added
     */
    public void addSrcSwitchPort(SwitchPort port) {
        // TODO implement it.
    }

    /**
     * Adds the specified port to the set of destination ports.
     *
     * @param port SwitchPort object to be added
     */
    public void addDstSwitchPort(SwitchPort port) {
        // TODO implement it.
    }

    /**
     * Removes the specified port from the set of source ports.
     *
     * @param port SwitchPort object to be removed
     */
    public void removeSrcSwitchPort(SwitchPort port) {
        // TODO implement it.
    }

    /**
     * Removes the specified port from the set of destination ports.
     *
     * @param port SwitchPort object to be removed
     */
    public void removeDstSwitchPort(SwitchPort port) {
        // TODO implement it.
    }

    /**
     * Sets idle-timeout value.
     *
     * @param timeout Idle-timeout value (seconds)
     */
    public void setIdleTimeout(int timeout) {
        idleTimeoutValue = timeout;
    }

    /**
     * Sets hard-timeout value.
     *
     * @param timeout Hard-timeout value (seconds)
     */
    public void setHardTimeout(int timeout) {
        hardTimeoutValue = timeout;
    }

    /**
     * Gets idle-timeout value.
     *
     * @return Idle-timeout value (seconds)
     */
    public int getIdleTimeout() {
        return idleTimeoutValue;
    }

    /**
     * Gets hard-timeout value.
     *
     * @return Hard-timeout value (seconds)
     */
    public int getHardTimeout() {
        return hardTimeoutValue;
    }

    /**
     * Returns whether this intent can create optical flows if needed.
     *
     * @return whether this intent can create optical flows.
     */
    public boolean canSetupOpticalFlow() {
        return canSetupOpticalFlow;
    }
}
