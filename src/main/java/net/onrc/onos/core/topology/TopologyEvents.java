package net.onrc.onos.core.topology;

import java.util.Collection;
import java.util.Collections;

import net.onrc.onos.core.topology.web.serializers.TopologyEventsSerializer;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Class for encapsulating multiple topology events.
 * <p/>
 * The recommended ordering rules for applying/processing the events are:
 * <p/>
 * (a) Process "removed" events before "added" events.
 * <p/>
 * (b) The processing order of the "removed" events should be:
 * removedHostEvents, removedLinkEvents, removedPortEvents,
 * removedSwitchEvents, removedMastershipEvents
 * <p/>
 * (c) The processing order of the "added" events should be:
 * addedMastershipEvents, addedSwitchEvents, addedPortEvents, addedLinkEvents,
 * addedHostEvents
 * <p/>
 * The above ordering guarantees that removing a port for example
 * will be processed before the corresponding switch itself is
 * removed.
 * <p/>
 * The above ordering guarantees that adding a port for example
 * will be processed after the corresponding switch itself is added.
 */
@JsonSerialize(using = TopologyEventsSerializer.class)
public final class TopologyEvents {
    private final Collection<MastershipEvent> addedMastershipEvents;
    private final Collection<MastershipEvent> removedMastershipEvents;
    private final Collection<SwitchEvent> addedSwitchEvents;
    private final Collection<SwitchEvent> removedSwitchEvents;
    private final Collection<PortEvent> addedPortEvents;
    private final Collection<PortEvent> removedPortEvents;
    private final Collection<LinkEvent> addedLinkEvents;
    private final Collection<LinkEvent> removedLinkEvents;
    private final Collection<HostEvent> addedHostEvents;
    private final Collection<HostEvent> removedHostEvents;

    /**
     * Constructor for added and removed events.
     *
     * @param addedMastershipEvents the collection of added Mastership Events.
     * @param removedMastershipEvents the collection of removed Mastership
     *        Events.
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
    public TopologyEvents(Collection<MastershipEvent> addedMastershipEvents,
                          Collection<MastershipEvent> removedMastershipEvents,
                          Collection<SwitchEvent> addedSwitchEvents,
                          Collection<SwitchEvent> removedSwitchEvents,
                          Collection<PortEvent> addedPortEvents,
                          Collection<PortEvent> removedPortEvents,
                          Collection<LinkEvent> addedLinkEvents,
                          Collection<LinkEvent> removedLinkEvents,
                          Collection<HostEvent> addedHostEvents,
                          Collection<HostEvent> removedHostEvents) {
        // CHECKSTYLE:ON
        this.addedMastershipEvents =
            Collections.unmodifiableCollection(addedMastershipEvents);
        this.removedMastershipEvents =
            Collections.unmodifiableCollection(removedMastershipEvents);
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
     * Constructor for added events only.
     *
     * @param addedMastershipEvents the collection of added Mastership Events.
     * @param addedSwitchEvents the collection of added Switch Events.
     * @param addedPortEvents the collection of added Port Events.
     * @param addedLinkEvents the collection of added Link Events.
     * @param addedHostEvents the collection of added Host Events.
     */
    public TopologyEvents(Collection<MastershipEvent> addedMastershipEvents,
                          Collection<SwitchEvent> addedSwitchEvents,
                          Collection<PortEvent> addedPortEvents,
                          Collection<LinkEvent> addedLinkEvents,
                          Collection<HostEvent> addedHostEvents) {
        this.addedMastershipEvents =
            Collections.unmodifiableCollection(addedMastershipEvents);
        this.removedMastershipEvents = Collections.emptyList();
        this.addedSwitchEvents =
            Collections.unmodifiableCollection(addedSwitchEvents);
        this.removedSwitchEvents = Collections.emptyList();
        this.addedPortEvents =
            Collections.unmodifiableCollection(addedPortEvents);
        this.removedPortEvents = Collections.emptyList();
        this.addedLinkEvents =
            Collections.unmodifiableCollection(addedLinkEvents);
        this.removedLinkEvents = Collections.emptyList();
        this.addedHostEvents =
            Collections.unmodifiableCollection(addedHostEvents);
        this.removedHostEvents = Collections.emptyList();
    }

    /**
     * Gets the collection of added Mastership Events.
     *
     * @return the collection of added Mastership Events.
     */
    public Collection<MastershipEvent> getAddedMastershipEvents() {
        return addedMastershipEvents;
    }

    /**
     * Gets the collection of removed Mastership Events.
     *
     * @return the collection of removed Mastership Events.
     */
    public Collection<MastershipEvent> getRemovedMastershipEvents() {
        return removedMastershipEvents;
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
    public Collection<HostEvent> getAddedHostEvents() {
        return addedHostEvents;
    }

    /**
     * Gets the collection of removed Host Events.
     *
     * @return the collection of removed Host Events.
     */
    public Collection<HostEvent> getRemovedHostEvents() {
        return removedHostEvents;
    }
}
