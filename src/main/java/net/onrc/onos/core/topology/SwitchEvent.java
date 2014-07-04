package net.onrc.onos.core.topology;

import net.onrc.onos.core.topology.web.serializers.SwitchEventSerializer;
import net.onrc.onos.core.util.Dpid;

import java.nio.ByteBuffer;
import java.util.Objects;

import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Self-contained Switch Object.
 * <p/>
 * TODO: We probably want common base class/interface for Self-Contained Event Object.
 */
@JsonSerialize(using = SwitchEventSerializer.class)
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

    public Dpid getDpid() {
        return dpid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof SwitchEvent)) {
            return false;
        }

        SwitchEvent that = (SwitchEvent) o;
        return Objects.equals(this.dpid, that.dpid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dpid);
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
