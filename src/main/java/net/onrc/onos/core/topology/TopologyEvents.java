package net.onrc.onos.core.topology;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import net.onrc.onos.core.topology.web.serializers.TopologyEventsSerializer;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Class for encapsulating multiple topology events.
 * This class is immutable.
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
    private final ImmutableList<MastershipEvent> addedMastershipEvents;
    private final ImmutableList<MastershipEvent> removedMastershipEvents;
    private final ImmutableList<SwitchEvent> addedSwitchEvents;
    private final ImmutableList<SwitchEvent> removedSwitchEvents;
    private final ImmutableList<PortEvent> addedPortEvents;
    private final ImmutableList<PortEvent> removedPortEvents;
    private final ImmutableList<LinkEvent> addedLinkEvents;
    private final ImmutableList<LinkEvent> removedLinkEvents;
    private final ImmutableList<HostEvent> addedHostEvents;
    private final ImmutableList<HostEvent> removedHostEvents;

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
        this.addedMastershipEvents = ImmutableList.<MastershipEvent>copyOf(
                        checkNotNull(addedMastershipEvents));
        this.removedMastershipEvents = ImmutableList.<MastershipEvent>copyOf(
                        checkNotNull(removedMastershipEvents));
        this.addedSwitchEvents = ImmutableList.<SwitchEvent>copyOf(
                        checkNotNull(addedSwitchEvents));
        this.removedSwitchEvents = ImmutableList.<SwitchEvent>copyOf(
                        checkNotNull(removedSwitchEvents));
        this.addedPortEvents = ImmutableList.<PortEvent>copyOf(
                        checkNotNull(addedPortEvents));
        this.removedPortEvents = ImmutableList.<PortEvent>copyOf(
                        checkNotNull(removedPortEvents));
        this.addedLinkEvents = ImmutableList.<LinkEvent>copyOf(
                        checkNotNull(addedLinkEvents));
        this.removedLinkEvents = ImmutableList.<LinkEvent>copyOf(
                        checkNotNull(removedLinkEvents));
        this.addedHostEvents = ImmutableList.<HostEvent>copyOf(
                        checkNotNull(addedHostEvents));
        this.removedHostEvents = ImmutableList.<HostEvent>copyOf(
                        checkNotNull(removedHostEvents));
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
        this.addedMastershipEvents = ImmutableList.<MastershipEvent>copyOf(
                        checkNotNull(addedMastershipEvents));
        this.addedSwitchEvents = ImmutableList.<SwitchEvent>copyOf(
                        checkNotNull(addedSwitchEvents));
        this.addedPortEvents = ImmutableList.<PortEvent>copyOf(
                        checkNotNull(addedPortEvents));
        this.addedLinkEvents = ImmutableList.<LinkEvent>copyOf(
                        checkNotNull(addedLinkEvents));
        this.addedHostEvents = ImmutableList.<HostEvent>copyOf(
                        checkNotNull(addedHostEvents));

        // Assign empty lists to the removed events
        this.removedMastershipEvents = ImmutableList.<MastershipEvent>of();
        this.removedSwitchEvents = ImmutableList.<SwitchEvent>of();
        this.removedPortEvents = ImmutableList.<PortEvent>of();
        this.removedLinkEvents = ImmutableList.<LinkEvent>of();
        this.removedHostEvents = ImmutableList.<HostEvent>of();
    }

    /**
     * Gets the immutable collection of added Mastership Events.
     *
     * @return the immutable collection of added Mastership Events.
     */
    public Collection<MastershipEvent> getAddedMastershipEvents() {
        return addedMastershipEvents;
    }

    /**
     * Gets the immutable collection of removed Mastership Events.
     *
     * @return the immutable collection of removed Mastership Events.
     */
    public Collection<MastershipEvent> getRemovedMastershipEvents() {
        return removedMastershipEvents;
    }

    /**
     * Gets the immutable collection of added Switch Events.
     *
     * @return the immutable collection of added Switch Events.
     */
    public Collection<SwitchEvent> getAddedSwitchEvents() {
        return addedSwitchEvents;
    }

    /**
     * Gets the immutable collection of removed Switch Events.
     *
     * @return the immutable collection of removed Switch Events.
     */
    public Collection<SwitchEvent> getRemovedSwitchEvents() {
        return removedSwitchEvents;
    }

    /**
     * Gets the immutable collection of added Port Events.
     *
     * @return the immutable collection of added Port Events.
     */
    public Collection<PortEvent> getAddedPortEvents() {
        return addedPortEvents;
    }

    /**
     * Gets the immutable collection of removed Port Events.
     *
     * @return the immutable collection of removed Port Events.
     */
    public Collection<PortEvent> getRemovedPortEvents() {
        return removedPortEvents;
    }

    /**
     * Gets the immutable collection of added Link Events.
     *
     * @return the immutable collection of added Link Events.
     */
    public Collection<LinkEvent> getAddedLinkEvents() {
        return addedLinkEvents;
    }

    /**
     * Gets the immutable collection of removed Link Events.
     *
     * @return the immutable collection of removed Link Events.
     */
    public Collection<LinkEvent> getRemovedLinkEvents() {
        return removedLinkEvents;
    }

    /**
     * Gets the immutable collection of added Host Events.
     *
     * @return the immutable collection of added Host Events.
     */
    public Collection<HostEvent> getAddedHostEvents() {
        return addedHostEvents;
    }

    /**
     * Gets the immutable collection of removed Host Events.
     *
     * @return the immutable collection of removed Host Events.
     */
    public Collection<HostEvent> getRemovedHostEvents() {
        return removedHostEvents;
    }
}
