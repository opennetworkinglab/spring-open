package net.onrc.onos.core.topology;

import java.util.Objects;

import net.onrc.onos.core.util.OnosInstanceId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Self-contained Topology event Object
 * <p/>
 * TODO: For now the topology event contains one of the following events:
 * Switch, Port, Link, Host, Switch Mastership. In the future it will contain
 * multiple events in a single transaction.
 * TODO: This class should become immutable after its internals and usage
 * are finalized.
 */
public final class TopologyEvent {
    private final SwitchEvent switchEvent;      // Set for Switch event
    private final PortEvent portEvent;          // Set for Port event
    private final LinkEvent linkEvent;          // Set for Link event
    private final HostEvent hostEvent;          // Set for Host event
    private final MastershipEvent mastershipEvent; // Set for Mastership event
    private final OnosInstanceId onosInstanceId;   // The ONOS Instance ID

    /**
     * Default constructor for serializer.
     */
    @Deprecated
    protected TopologyEvent() {
        switchEvent = null;
        portEvent = null;
        linkEvent = null;
        hostEvent = null;
        mastershipEvent = null;
        onosInstanceId = null;
    }

    /**
     * Constructor for given Switch event.
     *
     * @param switchEvent the Switch event to use.
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    TopologyEvent(SwitchEvent switchEvent, OnosInstanceId onosInstanceId) {
        this.switchEvent = checkNotNull(switchEvent);
        this.portEvent = null;
        this.linkEvent = null;
        this.hostEvent = null;
        this.mastershipEvent = null;
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Constructor for given Port event.
     *
     * @param portEvent the Port event to use.
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    TopologyEvent(PortEvent portEvent, OnosInstanceId onosInstanceId) {
        this.switchEvent = null;
        this.portEvent = checkNotNull(portEvent);
        this.linkEvent = null;
        this.hostEvent = null;
        this.mastershipEvent = null;
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Constructor for given Link event.
     *
     * @param linkEvent the Link event to use.
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    TopologyEvent(LinkEvent linkEvent, OnosInstanceId onosInstanceId) {
        this.switchEvent = null;
        this.portEvent = null;
        this.linkEvent = checkNotNull(linkEvent);
        this.hostEvent = null;
        this.mastershipEvent = null;
        this.onosInstanceId = checkNotNull(onosInstanceId);
    }

    /**
     * Constructor for given Host event.
     *
     * @param hostEvent the Host event to use.
     * @param onosInstanceId the ONOS Instance ID that originates the event.
     */
    TopologyEvent(HostEvent hostEvent, OnosInstanceId onosInstanceId) {
        this.switchEvent = null;
        this.portEvent = null;
        this.linkEvent = null;
        this.hostEvent = checkNotNull(hostEvent);
        this.mastershipEvent = null;
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
        this.switchEvent = null;
        this.portEvent = null;
        this.linkEvent = null;
        this.hostEvent = null;
        this.mastershipEvent = checkNotNull(mastershipEvent);
        this.onosInstanceId = checkNotNull(onosInstanceId);
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
     * Gets the Mastership event.
     *
     * @return the Mastership event.
     */
    public MastershipEvent getMastershipEvent() {
        return mastershipEvent;
    }

    /**
     * Check if all events contained are equal.
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
        // TODO: For now the onosInstanceId is not used
        return Objects.equals(switchEvent, other.switchEvent) &&
                Objects.equals(portEvent, other.portEvent) &&
                Objects.equals(linkEvent, other.linkEvent) &&
                Objects.equals(hostEvent, other.hostEvent) &&
                Objects.equals(mastershipEvent, other.mastershipEvent);
    }

    @Override
    public int hashCode() {
        // TODO: For now the onosInstanceId is not used
        return Objects.hash(switchEvent, portEvent, linkEvent, hostEvent,
                            mastershipEvent);
    }

    /**
     * Get the string representation of the event.
     *
     * @return the string representation of the event.
     */
    @Override
    public String toString() {
        // TODO: For now the onosInstanceId is not used
        if (switchEvent != null) {
            return switchEvent.toString();
        }
        if (portEvent != null) {
            return portEvent.toString();
        }
        if (linkEvent != null) {
            return linkEvent.toString();
        }
        if (hostEvent != null) {
            return hostEvent.toString();
        }
        if (mastershipEvent != null) {
            return mastershipEvent.toString();
        }
        return "[Empty TopologyEvent]";
    }

    /**
     * Get the Topology event ID.
     *
     * @return the Topology event ID.
     */
    public byte[] getID() {
        // TODO: For now the onosInstanceId is not used
        if (switchEvent != null) {
            return switchEvent.getID();
        }
        if (portEvent != null) {
            return portEvent.getID();
        }
        if (linkEvent != null) {
            return linkEvent.getID();
        }
        if (hostEvent != null) {
            return hostEvent.getID();
        }
        if (mastershipEvent != null) {
            return mastershipEvent.getID();
        }
        throw new IllegalStateException("Invalid TopologyEvent ID");
    }
}
