/**
 *
 */
package net.onrc.onos.core.main;

import net.floodlightcontroller.core.IOFSwitchListener;

import org.openflow.protocol.OFPhysicalPort;

/**
 * Extra event handler added to IOFSwitchListener by ONOS.
 */
public interface IOFSwitchPortListener extends IOFSwitchListener {

    /**
     * Fired when ports on a switch area added.
     */
    public void switchPortAdded(Long switchId, OFPhysicalPort port);

    /**
     * Fired when ports on a switch area removed.
     */
    public void switchPortRemoved(Long switchId, OFPhysicalPort port);

}
