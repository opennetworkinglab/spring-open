/**
 *
 */
package net.onrc.onos.core.main;

import org.projectfloodlight.openflow.protocol.OFPortDesc;
import net.floodlightcontroller.core.IOFSwitchListener;


/**
 * Extra event handler added to IOFSwitchListener by ONOS.
 */
public interface IOFSwitchPortListener extends IOFSwitchListener {

    /**
     * Fired when ports on a switch area added.
     */
    public void switchPortAdded(Long switchId, OFPortDesc port);

    /**
     * Fired when ports on a switch area removed.
     */
    public void switchPortRemoved(Long switchId, OFPortDesc port);

}
