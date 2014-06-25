package net.onrc.onos.core.topology;

import net.onrc.onos.core.topology.web.serializers.SwitchPortSerializer;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;

import org.apache.commons.lang.Validate;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Self-contained Port event Object.
 * <p/>
 * TODO: We probably want common base class/interface for Self-Contained Event Object.
 */
public class PortEvent {

    // TODO eliminate this class and use util.SwitchPort if possible
    @JsonSerialize(using = SwitchPortSerializer.class)
    public static class SwitchPort {
        public final Long dpid;
        public final Long number;

        /**
         * Default constructor for Serializer to use.
         */
        @Deprecated
        public SwitchPort() {
            dpid = null;
            number = null;
        }

        public SwitchPort(Long dpid, Long number) {
            this.dpid = dpid;
            this.number = number;
        }

        public SwitchPort(Dpid dpid, PortNumber number) {
            this(dpid.value(), (long) number.value());
        }

        public Dpid getDpid() {
            return new Dpid(dpid);
        }

        public PortNumber getNumber() {
            return new PortNumber(number.shortValue());
        }

        @Override
        public String toString() {
            return "(" + Long.toHexString(dpid) + "@" + number + ")";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((dpid == null) ? 0 : dpid.hashCode());
            result = prime * result
                    + ((number == null) ? 0 : number.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            SwitchPort other = (SwitchPort) obj;
            if (dpid == null) {
                if (other.dpid != null) {
                    return false;
                }
            } else if (!dpid.equals(other.dpid)) {
                return false;
            }
            if (number == null) {
                if (other.number != null) {
                    return false;
                }
            } else if (!number.equals(other.number)) {
                return false;
            }
            return true;
        }
    }

    protected final SwitchPort id;
    // TODO Add Hardware Address
    // TODO Add Description

    /**
     * Default constructor for Serializer to use.
     */
    @Deprecated
    public PortEvent() {
        id = null;
    }

    public PortEvent(Long dpid, Long number) {
        this.id = new SwitchPort(dpid, number);
    }

    public PortEvent(Dpid dpid, PortNumber number) {
        this.id = new SwitchPort(dpid, number);
    }

    public Dpid getDpid() {
        return id.getDpid();
    }

    public PortNumber getNumber() {
        return id.getNumber();
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
        return "[PortEvent 0x" + Long.toHexString(id.dpid) + "@" + id.number + "]";
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
        return getPortID(getDpid(), getNumber()).array();
    }

    public ByteBuffer getIDasByteBuffer() {
        return getPortID(getDpid(), getNumber());
    }
}
