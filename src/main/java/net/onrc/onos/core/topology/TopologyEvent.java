package net.onrc.onos.core.topology;

import java.util.Objects;


/**
 * Self-contained Topology event Object
 * <p/>
 * TODO: For now the topology event contains one of the following events:
 * Switch, Port, Link, Host, Switch Mastership. In the future it will contain
 * multiple events in a single transaction.
 */
public class TopologyEvent {
    SwitchEvent switchEvent = null;             // Set for Switch event
    PortEvent portEvent = null;                 // Set for Port event
    LinkEvent linkEvent = null;                 // Set for Link event
    HostEvent hostEvent = null;                 // Set for Host event
    MastershipEvent mastershipEvent = null;     // Set for Mastership event

    /**
     * Default constructor.
     */
    public TopologyEvent() {
    }

    /**
     * Constructor for given Switch event.
     *
     * @param switchEvent the Switch event to use.
     */
    TopologyEvent(SwitchEvent switchEvent) {
        this.switchEvent = switchEvent;
    }

    /**
     * Constructor for given Port event.
     *
     * @param portEvent the Port event to use.
     */
    TopologyEvent(PortEvent portEvent) {
        this.portEvent = portEvent;
    }

    /**
     * Constructor for given Link event.
     *
     * @param linkEvent the Link event to use.
     */
    TopologyEvent(LinkEvent linkEvent) {
        this.linkEvent = linkEvent;
    }

    /**
     * Constructor for given Host event.
     *
     * @param hostEvent the Host event to use.
     */
    TopologyEvent(HostEvent hostEvent) {
        this.hostEvent = hostEvent;
    }

    /**
     * Constructor for given Switch Mastership event.
     *
     * @param mastershipEvent the Switch Mastership event to use.
     */
    TopologyEvent(MastershipEvent mastershipEvent) {
        this.mastershipEvent = mastershipEvent;
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
        return Objects.equals(switchEvent, other.switchEvent) &&
                Objects.equals(portEvent, other.portEvent) &&
                Objects.equals(linkEvent, other.linkEvent) &&
                Objects.equals(hostEvent, other.hostEvent) &&
                Objects.equals(mastershipEvent, other.mastershipEvent);
    }

    @Override
    public int hashCode() {
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
