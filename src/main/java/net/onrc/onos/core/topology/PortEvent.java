package net.onrc.onos.core.topology;

import net.onrc.onos.core.topology.web.serializers.PortEventSerializer;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import static com.google.common.base.Preconditions.checkNotNull;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Self-contained Port event Object.
 * <p/>
 * TODO: Rename to match what it is. (Switch/Port/Link/Host)Snapshot?
 * FIXME: Current implementation directly use this object as
 *        Replication message, but should be sending update operation info.
 */
@JsonSerialize(using = PortEventSerializer.class)
public class PortEvent extends TopologyElement<PortEvent> {

    private final SwitchPort id;
    // TODO Add Hardware Address

    // TODO: Where should the attribute names be defined?
    /**
     * Attribute name for description.
     */
    public static final String DESCRIPTION = "description";


    /**
     * Default constructor for Serializer to use.
     */
    @Deprecated
    protected PortEvent() {
        id = null;
    }

    /**
     * Creates the port object.
     *
     * @param switchPort SwitchPort to identify this port
     */
    public PortEvent(SwitchPort switchPort) {
        this.id = checkNotNull(switchPort);
    }

    /**
     * Creates the port object.
     *
     * @param dpid SwitchPort to identify this port
     * @param number PortNumber to identify this port
     */
    public PortEvent(Dpid dpid, PortNumber number) {
        this.id = new SwitchPort(dpid, number);
    }

    /**
     * Creates an unfrozen copy of given Object.
     *
     * @param original to make copy of.
     */
    public PortEvent(PortEvent original) {
        super(original);
        this.id = original.id;
    }

    /**
     * Gets the SwitchPort identifying this port.
     *
     * @return SwitchPort
     */
    public SwitchPort getSwitchPort() {
        return id;
    }

    /**
     * Gets the Dpid of the switch this port belongs to.
     *
     * @return DPID
     */
    public Dpid getDpid() {
        return id.getDpid();
    }

    /**
     * Gets the port number.
     *
     * @return port number
     */
    public PortNumber getPortNumber() {
        return id.getPortNumber();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (getClass() != o.getClass()) {
            return false;
        }
        PortEvent other = (PortEvent) o;

        // compare attributes
        if (!super.equals(o)) {
            return false;
        }

        return Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "[PortEvent 0x" + getDpid() + "@" + getPortNumber() + "]";
    }

    public static final int PORTID_BYTES = SwitchEvent.SWITCHID_BYTES + 2 + 8;

    public static ByteBuffer getPortID(Dpid dpid, PortNumber number) {
        checkNotNull(dpid);
        checkNotNull(number);
        return getPortID(dpid.value(), number.value());
    }

    public static ByteBuffer getPortID(Long dpid, Long number) {
        if (dpid == null) {
            throw new IllegalArgumentException("dpid cannot be null");
        }
        if (number == null) {
            throw new IllegalArgumentException("number cannot be null");
        }
        return (ByteBuffer) ByteBuffer.allocate(PortEvent.PORTID_BYTES)
                .putChar('S').putLong(dpid)
                .putChar('P').putLong(number).flip();
    }

    public byte[] getID() {
        return getIDasByteBuffer().array();
    }

    public ByteBuffer getIDasByteBuffer() {
        return getPortID(getDpid(), getPortNumber());
    }
}
