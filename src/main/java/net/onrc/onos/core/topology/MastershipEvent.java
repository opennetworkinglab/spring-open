package net.onrc.onos.core.topology;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.onrc.onos.core.topology.web.serializers.MastershipEventSerializer;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.OnosInstanceId;

import static com.google.common.base.Preconditions.checkNotNull;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Self-contained Switch Mastership event Object.
 * <p/>
 * TODO: Rename to match what it is. (Switch/Port/Link/Host)Snapshot?
 * FIXME: Current implementation directly use this object as
 *        Replication message, but should be sending update operation info.
 */
@JsonSerialize(using = MastershipEventSerializer.class)
public class MastershipEvent extends TopologyElement<MastershipEvent> {

    private final Dpid dpid;
    private final OnosInstanceId onosInstanceId;
    private final Role role;

    /**
     * Default constructor for Serializer to use.
     */
    @Deprecated
    protected MastershipEvent() {
        dpid = null;
        onosInstanceId = null;
        role = Role.SLAVE;              // Default role is SLAVE
    }

    /**
     * Creates the Switch Mastership object.
     *
     * @param dpid the Switch DPID
     * @param onosInstanceId the ONOS Instance ID
     * @param role the ONOS instance role for the switch.
     */
    public MastershipEvent(Dpid dpid, OnosInstanceId onosInstanceId,
                           Role role) {
        this.dpid = checkNotNull(dpid);
        this.onosInstanceId = checkNotNull(onosInstanceId);
        this.role = role;
    }

    /**
     * Creates an unfrozen copy of given Object.
     *
     * @param original to make copy of.
     */
    public MastershipEvent(MastershipEvent original) {
        super(original);
        this.dpid = original.dpid;
        this.onosInstanceId = original.onosInstanceId;
        this.role = original.role;
    }

    /**
     * Gets the Switch DPID.
     *
     * @return the Switch DPID.
     */
    public Dpid getDpid() {
        return dpid;
    }

    /**
     * Gets the ONOS Instance ID.
     *
     * @return the ONOS Instance ID.
     */
    public OnosInstanceId getOnosInstanceId() {
        return onosInstanceId;
    }

    /**
     * Gets the ONOS Controller Role for the Switch.
     *
     * @return the ONOS Controller Role for the Switch.
     */
    public Role getRole() {
        return role;
    }

    @Override
    public Dpid getOriginDpid() {
        return this.dpid;
    }

    @Override
    public ByteBuffer getIDasByteBuffer() {
        String keyStr = "M" + getDpid() + "@" + getOnosInstanceId();
        byte[] id = keyStr.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.wrap(id);
        return buf;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dpid, onosInstanceId);
    }

    /**
     * Compares two MastershipEvent objects.
     * MastershipEvent objects are equal if they have same DPID and same
     * ONOS Instance ID.
     *
     * @param o another object to compare to this.
     * @return true if equal, false otherwise.
     */
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

        MastershipEvent other = (MastershipEvent) o;
        return Objects.equals(dpid, other.dpid) &&
            Objects.equals(onosInstanceId, other.onosInstanceId);
    }

    @Override
    public String toString() {
        return "[MastershipEvent " + getDpid() + "@" + getOnosInstanceId() +
            "->" + getRole() + "]";
    }
}
