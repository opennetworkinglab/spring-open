package net.onrc.onos.core.topology;

import net.onrc.onos.core.util.Dpid;

import java.nio.ByteBuffer;

/**
 * Self-contained Switch Object.
 * <p/>
 * TODO: We probably want common base class/interface for Self-Contained Event Object.
 */
public class SwitchEvent {
    protected final Dpid dpid;

    /**
     * Default constructor for Serializer to use.
     */
    @Deprecated
    public SwitchEvent() {
        dpid = null;
    }

    public SwitchEvent(Long dpid) {
        this.dpid = new Dpid(dpid);
    }

    public Long getDpid() {
        return dpid.value();
    }

    @Override
    public String toString() {
        return "[SwitchEvent 0x" + Long.toHexString(dpid.value()) + "]";
    }

    public static final int SWITCHID_BYTES = 2 + 8;

    public static ByteBuffer getSwitchID(Long dpid) {
        if (dpid == null) {
            throw new IllegalArgumentException("dpid cannot be null");
        }
        return (ByteBuffer) ByteBuffer.allocate(SwitchEvent.SWITCHID_BYTES).putChar('S').putLong(dpid).flip();
    }

    public byte[] getID() {
        return getSwitchID(dpid.value()).array();
    }

    public ByteBuffer getIDasByteBuffer() {
        return getSwitchID(dpid.value());
    }
}
