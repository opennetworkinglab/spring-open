package net.onrc.onos.api.intent;

import java.util.List;

import net.onrc.onos.api.batchoperation.BatchOperation;
import net.onrc.onos.api.flowmanager.IFlow;
import net.onrc.onos.core.util.SwitchPort;

/**
 * An optical layer Intent for a connectivity from a transponder port to another
 * transponder port.
 * <p>
 * This class doesn't accepts lambda specifier. This class computes path between
 * ports and assign lambda automatically. The lambda can be specified using
 * OpticalPathFlow class.
 */
public class OpticalConnectivityIntent extends Intent {
    protected SwitchPort srcSwitchPort;
    protected SwitchPort dstSwitchPort;

    /**
     * Constructor.
     *
     * @param id ID for this new Intent object.
     * @param srcSwitchPort The source transponder port.
     * @param dstSwitchPort The destination transponder port.
     */
    public OpticalConnectivityIntent(IntentId id,
            SwitchPort srcSwitchPort, SwitchPort dstSwitchPort) {
        super(id);
        this.srcSwitchPort = srcSwitchPort;
        this.dstSwitchPort = dstSwitchPort;
    }

    /**
     * Gets source transponder port.
     *
     * @return The source transponder port.
     */
    public SwitchPort getSrcSwitchPort() {
        return srcSwitchPort;
    }

    /**
     * Gets destination transponder port.
     *
     * @return The source transponder port.
     */
    public SwitchPort getDstSwitchPort() {
        return dstSwitchPort;
    }

    @Override
    public List<BatchOperation<IFlow>> compile() {
        // TODO Auto-generated method stub
        return null;
    }
}
