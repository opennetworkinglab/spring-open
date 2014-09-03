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
    public TopologyEvent(OnosInstanceId onosInstanceId) {
        this.eventType = Type.NOOP;
        this.event = null;
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Constructor for given Switch Mastership event.
     *
     * @param mastershipData the Switch Mastership event to use.
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    public TopologyEvent(MastershipData mastershipData,
                         OnosInstanceId onosInstanceId) {
        this.eventType = Type.MASTERSHIP;
        this.event = checkNotNull(mastershipData);
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Constructor for given Switch event.
     *
     * @param switchData the Switch event to use.
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    public TopologyEvent(SwitchData switchData,
                         OnosInstanceId onosInstanceId) {
        this.eventType = Type.SWITCH;
        this.event = checkNotNull(switchData);
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Constructor for given Port event.
     *
     * @param portData the Port event to use.
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    public TopologyEvent(PortData portData, OnosInstanceId onosInstanceId) {
        this.eventType = Type.PORT;
        this.event = checkNotNull(portData);
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Constructor for given Link event.
     *
     * @param linkData the Link event to use.
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    public TopologyEvent(LinkData linkData, OnosInstanceId onosInstanceId) {
        this.eventType = Type.LINK;
        this.event = checkNotNull(linkData);
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Constructor for given Host event.
     *
     * @param hostData the Host event to use.
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    public TopologyEvent(HostData hostData, OnosInstanceId onosInstanceId) {
        this.eventType = Type.HOST;
        this.event = checkNotNull(hostData);
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Gets the Topology Event type.
     *
     * @return the Topology Event type.
     */
    public TopologyEvent.Type getEventType() {
        return this.eventType;
    }

    /**
     * Gets the Mastership event.
     *
     * @return the Mastership event.
     */
    public MastershipData getMastershipData() {
        if (eventType != Type.MASTERSHIP) {
            return null;
        }
        MastershipData mastershipData = (MastershipData) event;
        return mastershipData;
    }

    /**
     * Gets the Switch event.
     *
     * @return the Switch event.
     */
    public SwitchData getSwitchData() {
        if (eventType != Type.SWITCH) {
            return null;
        }
        SwitchData switchData = (SwitchData) event;
        return switchData;
    }

    /**
     * Gets the Port event.
     *
     * @return the Port event.
     */
    public PortData getPortData() {
        if (eventType != Type.PORT) {
            return null;
        }
        PortData portData = (PortData) event;
        return portData;
    }

    /**
     * Gets the Link event.
     *
     * @return the Link event.
     */
    public LinkData getLinkData() {
        if (eventType != Type.LINK) {
            return null;
        }
        LinkData linkData = (LinkData) event;
        return linkData;
    }

    /**
     * Gets the Host event.
     *
     * @return the Host event.
     */
    public HostData getHostData() {
        if (eventType != Type.HOST) {
            return null;
        }
        HostData hostData = (HostData) event;
        return hostData;
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
