package net.onrc.onos.core.matchaction.action;

import static com.google.common.base.Preconditions.checkNotNull;
import net.floodlightcontroller.util.MACAddress;

import com.google.common.base.Objects;

/**
 * An action object to modify destination MAC address.
 * <p>
 * This class does not have a switch ID. The switch ID is handled by
 * MatchAction, Flow or Intent class.
 */
public class ModifyDstMacAction implements Action {
    private final MACAddress dstMac;

    /**
     * Default constructor for Kryo deserialization.
     */
    @Deprecated
    protected ModifyDstMacAction() {
        dstMac = null;
    }

    /**
     * Constructor.
     *
     * @param dstMac destination MAC address after the modification
     */
    public ModifyDstMacAction(MACAddress dstMac) {
        this.dstMac = checkNotNull(dstMac);
    }

    /**
     * Gets the destination MAC address.
     *
     * @return the destination MAC address
     */
    public MACAddress getDstMac() {
        return dstMac;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dstMac);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ModifyDstMacAction that = (ModifyDstMacAction) obj;
        return Objects.equal(this.dstMac, that.dstMac);
    }
}
