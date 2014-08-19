package net.onrc.onos.core.topology;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.OnosInstanceId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Self-contained Topology event Object
 * <p/>
 * TODO: For now the topology event contains one of the following events:
 * Switch Mastership, Switch, Port, Link, Host. In the future it will contain
 * multiple events in a single transaction.
 * TODO: This class should become immutable after its internals and usage
 * are finalized.
 */
public final class TopologyEvent implements TopologyBatchTarget {
    private final Type eventType;
    private final TopologyElement<?> event;
    private final OnosInstanceId onosInstanceId;   // The ONOS Instance ID

    /**
     * The topology event type.
     */
    public enum Type {
        MASTERSHIP,
        SWITCH,
        PORT,
        LINK,
        HOST,
        NOOP,                   // TODO: temporary type; should be removed
    }

    /**
     * Default constructor for serializer.
     */
    @Deprecated
    protected TopologyEvent() {
        this.eventType = Type.NOOP;
        this.event = null;
        this.onosInstanceId = null;
    }

    /**
     * Constructor for creating an empty (NO-OP) Topology Event.
     *
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    protected TopologyEvent(OnosInstanceId onosInstanceId) {
        this.eventType = Type.NOOP;
        this.event = null;
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Constructor for given Switch Mastership event.
     *
     * @param mastershipEvent the Switch Mastership event to use.
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    TopologyEvent(MastershipEvent mastershipEvent,
                  OnosInstanceId onosInstanceId) {
        this.eventType = Type.MASTERSHIP;
        this.event = checkNotNull(mastershipEvent);
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Constructor for given Switch event.
     *
     * @param switchEvent the Switch event to use.
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    TopologyEvent(SwitchEvent switchEvent, OnosInstanceId onosInstanceId) {
        this.eventType = Type.SWITCH;
        this.event = checkNotNull(switchEvent);
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Constructor for given Port event.
     *
     * @param portEvent the Port event to use.
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    TopologyEvent(PortEvent portEvent, OnosInstanceId onosInstanceId) {
        this.eventType = Type.PORT;
        this.event = checkNotNull(portEvent);
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Constructor for given Link event.
     *
     * @param linkEvent the Link event to use.
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    TopologyEvent(LinkEvent linkEvent, OnosInstanceId onosInstanceId) {
        this.eventType = Type.LINK;
        this.event = checkNotNull(linkEvent);
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Constructor for given Host event.
     *
     * @param hostEvent the Host event to use.
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    TopologyEvent(HostEvent hostEvent, OnosInstanceId onosInstanceId) {
        this.eventType = Type.HOST;
        this.event = checkNotNull(hostEvent);
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Gets the Topology Event type.
     *
     * @return the Topology Event type.
     */
    TopologyEvent.Type getEventType() {
        return this.eventType;
    }

    /**
     * Gets the Mastership event.
     *
     * @return the Mastership event.
     */
    public MastershipEvent getMastershipEvent() {
        if (eventType != Type.MASTERSHIP) {
            return null;
        }
        MastershipEvent mastershipEvent = (MastershipEvent) event;
        return mastershipEvent;
    }

    /**
     * Gets the Switch event.
     *
     * @return the Switch event.
     */
    public SwitchEvent getSwitchEvent() {
        if (eventType != Type.SWITCH) {
            return null;
        }
        SwitchEvent switchEvent = (SwitchEvent) event;
        return switchEvent;
    }

    /**
     * Gets the Port event.
     *
     * @return the Port event.
     */
    public PortEvent getPortEvent() {
        if (eventType != Type.PORT) {
            return null;
        }
        PortEvent portEvent = (PortEvent) event;
        return portEvent;
    }

    /**
     * Gets the Link event.
     *
     * @return the Link event.
     */
    public LinkEvent getLinkEvent() {
        if (eventType != Type.LINK) {
            return null;
        }
        LinkEvent linkEvent = (LinkEvent) event;
        return linkEvent;
    }

    /**
     * Gets the Host event.
     *
     * @return the Host event.
     */
    public HostEvent getHostEvent() {
        if (eventType != Type.HOST) {
            return null;
        }
        HostEvent hostEvent = (HostEvent) event;
        return hostEvent;
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
     * Tests whether the event origin DPID equals the specified DPID.
     *
     * @param dpid the DPID to compare against.
     * @return true if the event origin Dpid equals the specified DPID.
     */
    public boolean equalsOriginDpid(Dpid dpid) {
        return dpid.equals(getOriginDpid());
    }

    public Dpid getOriginDpid() {
        if (eventType == Type.NOOP) {
            return null;
        }
        return event.getOriginDpid();
    }

    /**
     * Returns the config state of the topology element.
     *
     * @return the config state of the topology element.
     */
    public ConfigState getConfigState() {
        if (eventType == Type.NOOP) {
            return ConfigState.NOT_CONFIGURED;  // Default: not configured
        }
        return event.getConfigState();
    }

    /**
     * Gets the Topology event ID as a byte array.
     *
     * @return the Topology event ID as a byte array.
     */
    public byte[] getID() {
        return getIDasByteBuffer().array();
    }

    public ByteBuffer getIDasByteBuffer() {
        ByteBuffer eventId = null;

        //
        // Get the Event ID
        //
        if (eventType == Type.NOOP) {
            String id = "NO-OP";
            eventId = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        } else {
            eventId = event.getIDasByteBuffer();
        }

        //
        // Prepare the ONOS Instance ID. The '@' separator is needed to avoid
        // potential key collisions.
        //
        byte[] onosId =
            ("@" + onosInstanceId.toString()).getBytes(StandardCharsets.UTF_8);

        // Concatenate the IDs
        ByteBuffer buf =
            ByteBuffer.allocate(eventId.capacity() + onosId.length);
        buf.put(eventId);
        buf.put(onosId);
        buf.flip();
        return buf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TopologyEvent other = (TopologyEvent) o;
        return Objects.equals(eventType, other.eventType) &&
            Objects.equals(event, other.event) &&
            Objects.equals(onosInstanceId, other.onosInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventType, event, onosInstanceId);
    }

    @Override
    public String toString() {
        String eventStr = null;

        //
        // Get the Event string
        //
        if (eventType == Type.NOOP) {
            eventStr = "NO-OP";
        } else {
            eventStr = event.toString();
        }
        return "[TopologyEvent " + eventStr + " from " +
            onosInstanceId.toString() + "]";
    }
}
