package net.onrc.onos.core.topology;

import java.util.Collection;
import java.util.Collections;

/**
 * Class for encapsulating multiple topology events.
 * <p/>
 * The recommended ordering rules for applying/processing the events are:
 * <p/>
 * (a) Process "removed" events before "added" events.
 * <p/>
 * (b) The processing order of the "removed" events should be:
 * removedHostEvents, removedLinkEvents, removedPortEvents,
 * removedSwitchEvents
 * <p/>
 * (c) The processing order of the "added" events should be:
 * addedSwitchEvents, addedPortEvents, addedLinkEvents,
 * addedHostEvents
 * <p/>
 * The above ordering guarantees that removing a port for example
 * will be processed before the corresponding switch itself is
 * removed.
 * <p/>
 * The above ordering guarantees that adding a port for example
 * will be processed after the corresponding switch itself is added.
 */
public final class TopologyEvents {
    private final long timestamp;
    private final Collection<SwitchEvent> addedSwitchEvents;
    private final Collection<SwitchEvent> removedSwitchEvents;
    private final Collection<PortEvent> addedPortEvents;
    private final Collection<PortEvent> removedPortEvents;
    private final Collection<LinkEvent> addedLinkEvents;
    private final Collection<LinkEvent> removedLinkEvents;
    private final Collection<DeviceEvent> addedHostEvents;
    private final Collection<DeviceEvent> removedHostEvents;

    /**
     * Constructor.
     *
     * @param timestamp the timestamp for the event.
     * @param addedSwitchEvents the collection of added Switch Events.
     * @param removedSwitchEvents the collection of removed Switch Events.
     * @param addedPortEvents the collection of added Port Events.
     * @param removedPortEvents the collection of removed Port Events.
     * @param addedLinkEvents the collection of added Link Events.
     * @param removedLinkEvents the collection of removed Link Events.
     * @param addedHostEvents the collection of added Host Events.
     * @param removedHostEvents the collection of removed Host Events.
     */
    // CHECKSTYLE:OFF suppress the warning about too many parameters
    public TopologyEvents(long timestamp,
                          Collection<SwitchEvent> addedSwitchEvents,
                          Collection<SwitchEvent> removedSwitchEvents,
                          Collection<PortEvent> addedPortEvents,
                          Collection<PortEvent> removedPortEvents,
                          Collection<LinkEvent> addedLinkEvents,
                          Collection<LinkEvent> removedLinkEvents,
                          Collection<DeviceEvent> addedHostEvents,
                          Collection<DeviceEvent> removedHostEvents) {
        // CHECKSTYLE:ON
        this.timestamp = timestamp;
        this.addedSwitchEvents =
            Collections.unmodifiableCollection(addedSwitchEvents);
        this.removedSwitchEvents =
            Collections.unmodifiableCollection(removedSwitchEvents);
        this.addedPortEvents =
            Collections.unmodifiableCollection(addedPortEvents);
        this.removedPortEvents =
            Collections.unmodifiableCollection(removedPortEvents);
        this.addedLinkEvents =
            Collections.unmodifiableCollection(addedLinkEvents);
        this.removedLinkEvents =
            Collections.unmodifiableCollection(removedLinkEvents);
        this.addedHostEvents =
            Collections.unmodifiableCollection(addedHostEvents);
        this.removedHostEvents =
            Collections.unmodifiableCollection(removedHostEvents);
    }

    /**
     * Gets the timestamp for the events.
     *
     * @return the timestamp for the events.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the collection of added Switch Events.
     *
     * @return the collection of added Switch Events.
     */
    public Collection<SwitchEvent> getAddedSwitchEvents() {
        return addedSwitchEvents;
    }

    /**
     * Gets the collection of removed Switch Events.
     *
     * @return the collection of removed Switch Events.
     */
    public Collection<SwitchEvent> getRemovedSwitchEvents() {
        return removedSwitchEvents;
    }

    /**
     * Gets the collection of added Port Events.
     *
     * @return the collection of added Port Events.
     */
    public Collection<PortEvent> getAddedPortEvents() {
        return addedPortEvents;
    }

    /**
     * Gets the collection of removed Port Events.
     *
     * @return the collection of removed Port Events.
     */
    public Collection<PortEvent> getRemovedPortEvents() {
        return removedPortEvents;
    }

    /**
     * Gets the collection of added Link Events.
     *
     * @return the collection of added Link Events.
     */
    public Collection<LinkEvent> getAddedLinkEvents() {
        return addedLinkEvents;
    }

    /**
     * Gets the collection of removed Link Events.
     *
     * @return the collection of removed Link Events.
     */
    public Collection<LinkEvent> getRemovedLinkEvents() {
        return removedLinkEvents;
    }

    /**
     * Gets the collection of added Host Events.
     *
     * @return the collection of added Host Events.
     */
    public Collection<DeviceEvent> getAddedHostEvents() {
        return addedHostEvents;
    }

    /**
     * Gets the collection of removed Host Events.
     *
     * @return the collection of removed Host Events.
     */
    public Collection<DeviceEvent> getRemovedHostEvents() {
        return removedHostEvents;
    }
}
