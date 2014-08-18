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
    public static final int PORTID_BYTES = SwitchEvent.SWITCHID_BYTES + 2 + 8;

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
     * Constructor for given SwitchPort.
     *
     * @param switchPort the SwitchPort to identify the port
     */
    public PortEvent(SwitchPort switchPort) {
        this.id = checkNotNull(switchPort);
    }

    /**
     * Constructor for given switch DPID and port number.
     *
     * @param dpid the DPID of the switch the port belongs to
     * @param number the PortNumber to identify the port
     */
    public PortEvent(Dpid dpid, PortNumber number) {
        this.id = new SwitchPort(dpid, number);
    }

    /**
     * Copy constructor.
     * <p>
     * Creates an unfrozen copy of the given PortEvent object.
     *
     * @param original the object to make copy of
     */
    public PortEvent(PortEvent original) {
        super(original);
        this.id = original.id;
    }

    /**
     * Gets the SwitchPort identifying this port.
     *
     * @return the SwitchPort identifying this port
     */
    public SwitchPort getSwitchPort() {
        return id;
    }

    /**
     * Gets the DPID of the switch this port belongs to.
     *
     * @return the DPID of the switch this port belongs to
     */
    public Dpid getDpid() {
        return id.getDpid();
    }

    /**
     * Gets the port number.
     *
     * @return the port number
     */
    public PortNumber getPortNumber() {
        return id.getPortNumber();
    }

    /**
     * Computes the port ID for a given switch DPID and a port number.
     *
     * @param dpid the switch DPID to use
     * @param number the port number to use
     * @return the port ID as a ByteBuffer
     */
    public static ByteBuffer getPortID(Dpid dpid, PortNumber number) {
        checkNotNull(dpid);
        checkNotNull(number);
        return getPortID(dpid.value(), number.value());
    }

    /**
     * Computes the port ID for a given switch DPID and a port number.
     * <p>
     * TODO: This method should be removed and replaced with the corresponding
     * getPortID(Dpid, PortNumber) method.
     *
     * @param dpid the switch DPID to use
     * @param number the port number to use
     * @return the port ID as a ByteBuffer
     */
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

    @Override
    public Dpid getOriginDpid() {
        return this.id.getDpid();
    }

    @Override
    public ByteBuffer getIDasByteBuffer() {
        return getPortID(getDpid(), getPortNumber());
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        // Compare attributes
        if (!super.equals(o)) {
            return false;
        }

        PortEvent other = (PortEvent) o;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return "[PortEvent " + getDpid() + "@" + getPortNumber() + "]";
    }
}
