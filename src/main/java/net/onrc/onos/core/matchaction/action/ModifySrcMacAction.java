package net.onrc.onos.core.matchaction.action;

import static com.google.common.base.Preconditions.checkNotNull;
import net.floodlightcontroller.util.MACAddress;

import com.google.common.base.Objects;

/**
 * An action object to modify source MAC address.
 * <p>
 * This class does not have a switch ID. The switch ID is handled by
 * MatchAction, Flow or Intent class.
 */
public class ModifySrcMacAction implements Action {
    private final MACAddress srcMac;

    /**
     * Default constructor for Kryo deserialization.
     */
    @Deprecated
    protected ModifySrcMacAction() {
        srcMac = null;
    }

    /**
     * Constructor.
     *
     * @param srcMac source MAC address after the modification
     */
    public ModifySrcMacAction(MACAddress srcMac) {
        this.srcMac = checkNotNull(srcMac);
    }

    /**
     * Gets the source MAC address.
     *
     * @return the source MAC address
     */
    public MACAddress getSrcMac() {
        return srcMac;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(srcMac);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ModifySrcMacAction that = (ModifySrcMacAction) obj;
        return Objects.equal(this.srcMac, that.srcMac);
    }
}
