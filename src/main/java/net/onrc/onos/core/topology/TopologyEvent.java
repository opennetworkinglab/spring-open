package net.onrc.onos.core.topology;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

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
public final class TopologyEvent {
    private final MastershipEvent mastershipEvent; // Set for Mastership event
    private final SwitchEvent switchEvent;      // Set for Switch event
    private final PortEvent portEvent;          // Set for Port event
    private final LinkEvent linkEvent;          // Set for Link event
    private final HostEvent hostEvent;          // Set for Host event
    private final OnosInstanceId onosInstanceId;   // The ONOS Instance ID

    /**
     * Default constructor for serializer.
     */
    @Deprecated
    protected TopologyEvent() {
        mastershipEvent = null;
        switchEvent = null;
        portEvent = null;
        linkEvent = null;
        hostEvent = null;
        onosInstanceId = null;
    }

    /**
     * Constructor for given Switch Mastership event.
     *
     * @param mastershipEvent the Switch Mastership event to use.
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    TopologyEvent(MastershipEvent mastershipEvent,
                  OnosInstanceId onosInstanceId) {
        this.mastershipEvent = checkNotNull(mastershipEvent);
        this.switchEvent = null;
        this.portEvent = null;
        this.linkEvent = null;
        this.hostEvent = null;
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Constructor for given Switch event.
     *
     * @param switchEvent the Switch event to use.
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    TopologyEvent(SwitchEvent switchEvent, OnosInstanceId onosInstanceId) {
        this.mastershipEvent = null;
        this.switchEvent = checkNotNull(switchEvent);
        this.portEvent = null;
        this.linkEvent = null;
        this.hostEvent = null;
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Constructor for given Port event.
     *
     * @param portEvent the Port event to use.
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    TopologyEvent(PortEvent portEvent, OnosInstanceId onosInstanceId) {
        this.mastershipEvent = null;
        this.switchEvent = null;
        this.portEvent = checkNotNull(portEvent);
        this.linkEvent = null;
        this.hostEvent = null;
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Constructor for given Link event.
     *
     * @param linkEvent the Link event to use.
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    TopologyEvent(LinkEvent linkEvent, OnosInstanceId onosInstanceId) {
        this.mastershipEvent = null;
        this.switchEvent = null;
        this.portEvent = null;
        this.linkEvent = checkNotNull(linkEvent);
        this.hostEvent = null;
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Constructor for given Host event.
     *
     * @param hostEvent the Host event to use.
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    TopologyEvent(HostEvent hostEvent, OnosInstanceId onosInstanceId) {
        this.mastershipEvent = null;
        this.switchEvent = null;
        this.portEvent = null;
        this.linkEvent = null;
        this.hostEvent = checkNotNull(hostEvent);
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Gets the Mastership event.
     *
     * @return the Mastership event.
     */
    public MastershipEvent getMastershipEvent() {
        return mastershipEvent;
    }

    /**
     * Gets the Switch event.
     *
     * @return the Switch event.
     */
    public SwitchEvent getSwitchEvent() {
        return switchEvent;
    }

    /**
     * Gets the Port event.
     *
     * @return the Port event.
     */
    public PortEvent getPortEvent() {
        return portEvent;
    }

    /**
     * Gets the Link event.
     *
     * @return the Link event.
     */
    public LinkEvent getLinkEvent() {
        return linkEvent;
    }

    /**
     * Gets the Host event.
     *
     * @return the Host event.
     */
    public HostEvent getHostEvent() {
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
     * Checks if all events contained are equal.
     *
     * @param obj TopologyEvent to compare against
     */
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

        TopologyEvent other = (TopologyEvent) obj;
        return Objects.equals(mastershipEvent, other.mastershipEvent) &&
            Objects.equals(switchEvent, other.switchEvent) &&
            Objects.equals(portEvent, other.portEvent) &&
            Objects.equals(linkEvent, other.linkEvent) &&
            Objects.equals(hostEvent, other.hostEvent) &&
            Objects.equals(onosInstanceId, other.onosInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mastershipEvent, switchEvent, portEvent,
                            linkEvent, hostEvent, onosInstanceId);
    }

    /**
     * Gets the string representation of the event.
     *
     * @return the string representation of the event.
     */
    @Override
    public String toString() {
        String eventStr = null;

        //
        // Get the Event string
        //
        do {
            if (mastershipEvent != null) {
                eventStr = mastershipEvent.toString();
                break;
            }
            if (switchEvent != null) {
                eventStr = switchEvent.toString();
                break;
            }
            if (portEvent != null) {
                eventStr = portEvent.toString();
                break;
            }
            if (linkEvent != null) {
                eventStr = linkEvent.toString();
                break;
            }
            if (hostEvent != null) {
                eventStr = hostEvent.toString();
                break;
            }
            // No event found
            return "[Empty TopologyEvent]";
        } while (false);

        return "[TopologyEvent " + eventStr + " from " +
            onosInstanceId.toString() + "]";
    }

    /**
     * Gets the Topology event ID as a byte array.
     *
     * @return the Topology event ID as a byte array.
     */
    public byte[] getID() {
        return getIDasByteBuffer().array();
    }

    /**
     * Gets the Topology event ID as a ByteBuffer.
     *
     * @return the Topology event ID as a ByteBuffer.
     */
    public ByteBuffer getIDasByteBuffer() {
        ByteBuffer eventId = null;

        //
        // Get the Event ID
        //
        do {
            if (mastershipEvent != null) {
                eventId = mastershipEvent.getIDasByteBuffer();
                break;
            }
            if (switchEvent != null) {
                eventId = switchEvent.getIDasByteBuffer();
                break;
            }
            if (portEvent != null) {
                eventId = portEvent.getIDasByteBuffer();
                break;
            }
            if (linkEvent != null) {
                eventId = linkEvent.getIDasByteBuffer();
                break;
            }
            if (hostEvent != null) {
                eventId = hostEvent.getIDasByteBuffer();
                break;
            }
            // No event found
            throw new IllegalStateException("Invalid TopologyEvent ID");
        } while (false);

        //
        // Prepare the ONOS Instance ID. The '@' separator is needed to avoid
        // potential key collisions.
        //
        byte[] onosId = ("@" + onosInstanceId.toString()).getBytes(StandardCharsets.UTF_8);

        // Concatenate the IDs
        ByteBuffer buf =
            ByteBuffer.allocate(eventId.capacity() + onosId.length);
        buf.put(eventId);
        buf.put(onosId);
        buf.flip();
        return buf;
    }
}
