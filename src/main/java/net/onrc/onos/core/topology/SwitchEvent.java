package net.onrc.onos.core.topology;

import net.onrc.onos.core.topology.web.serializers.SwitchEventSerializer;
import net.onrc.onos.core.util.Dpid;

import java.nio.ByteBuffer;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Self-contained Switch Object.
 * <p/>
 * TODO: Rename to match what it is. (Switch/Port/Link/Host)Snapshot?
 * FIXME: Current implementation directly use this object as
 *        Replication message, but should be sending update operation info.
 */
@JsonSerialize(using = SwitchEventSerializer.class)
public class SwitchEvent extends TopologyElement<SwitchEvent> {
    public static final int SWITCHID_BYTES = 2 + 8;

    private final Dpid dpid;

    /**
     * Default constructor for Serializer to use.
     */
    @Deprecated
    protected SwitchEvent() {
        dpid = null;
    }

    /**
     * Constructor for given switch DPID.
     *
     * @param dpid the switch DPID to identify the switch
     */
    public SwitchEvent(Dpid dpid) {
        this.dpid = checkNotNull(dpid);
    }

    /**
     * Copy constructor.
     * <p>
     * Creates an unfrozen copy of the given SwitchEvent object.
     *
     * @param original the object to make copy of
     */
    public SwitchEvent(SwitchEvent original) {
        super(original);
        this.dpid = original.dpid;
    }

    /**
     * Gets the DPID identifying this switch.
     *
     * @return the DPID identifying this switch
     */
    public Dpid getDpid() {
        return dpid;
    }

    /**
     * Computes the switch ID for a given switch DPID.
     *
     * @param dpid the switch DPID to use
     * @return the switch ID as a ByteBuffer
     */
    public static ByteBuffer getSwitchID(Dpid dpid) {
        return getSwitchID(dpid.value());
    }

    /**
     * Computes the switch ID for a given switch DPID.
     * <p>
     * TODO: This method should be removed and replaced with the corresponding
     * getSwitchID(Dpid) method.
     *
     * @param dpid the switch DPID to use
     * @return the switch ID as a ByteBuffer
     */
    public static ByteBuffer getSwitchID(Long dpid) {
        if (dpid == null) {
            throw new IllegalArgumentException("dpid cannot be null");
        }
        return (ByteBuffer) ByteBuffer.allocate(SwitchEvent.SWITCHID_BYTES)
                .putChar('S').putLong(dpid).flip();
    }

    @Override
    public Dpid getOriginDpid() {
        return this.dpid;
    }

    @Override
    public ByteBuffer getIDasByteBuffer() {
        return getSwitchID(dpid.value());
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(dpid);
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

        SwitchEvent other = (SwitchEvent) o;
        return Objects.equals(this.dpid, other.dpid);
    }

    @Override
    public String toString() {
        return "[SwitchEvent " + dpid + "]";
    }
}
