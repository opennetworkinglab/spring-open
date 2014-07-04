package net.onrc.onos.core.topology;

import net.onrc.onos.core.topology.web.serializers.PortEventSerializer;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.apache.commons.lang.Validate;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Self-contained Port event Object.
 * <p/>
 * TODO: We probably want common base class/interface for Self-Contained Event Object.
 */
@JsonSerialize(using = PortEventSerializer.class)
public class PortEvent {

    protected final SwitchPort id;
    // TODO Add Hardware Address
    // TODO Add Description

    /**
     * Default constructor for Serializer to use.
     */
    @Deprecated
    protected PortEvent() {
        id = null;
    }

    public PortEvent(SwitchPort switchPort) {
        this.id = switchPort;
    }

    public PortEvent(Dpid dpid, PortNumber number) {
        this.id = new SwitchPort(dpid, number);
    }

    public PortEvent(Long dpid, Long number) {
        this.id = new SwitchPort(dpid, number);
    }

    public Dpid getDpid() {
        return id.getDpid();
    }

    public PortNumber getPortNumber() {
        return id.getPortNumber();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof PortEvent)) {
            return false;
        }

        PortEvent that = (PortEvent) o;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "[PortEvent 0x" + getDpid() + "@" + getPortNumber() + "]";
    }

    public static final int PORTID_BYTES = SwitchEvent.SWITCHID_BYTES + 2 + 8;

    public static ByteBuffer getPortID(Dpid dpid, PortNumber number) {
        Validate.notNull(dpid);
        Validate.notNull(number);
        return getPortID(dpid.value(), (long) number.value());
    }

    public static ByteBuffer getPortID(Long dpid, Long number) {
        if (dpid == null) {
            throw new IllegalArgumentException("dpid cannot be null");
        }
        if (number == null) {
            throw new IllegalArgumentException("number cannot be null");
        }
        return (ByteBuffer) ByteBuffer.allocate(PortEvent.PORTID_BYTES).putChar('S').putLong(dpid)
                .putChar('P').putLong(number).flip();
    }

    public byte[] getID() {
        return getIDasByteBuffer().array();
    }

    public ByteBuffer getIDasByteBuffer() {
        return getPortID(getDpid(), getPortNumber());
    }

}
