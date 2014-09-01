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
 * removedHostDataEntries, removedLinkDataEntries, removedPortDataEntries,
 * removedSwitchDataEntries, removedMastershipEvents
 * <p/>
 * (c) The processing order of the "added" events should be:
 * addedMastershipEvents, addedSwitchDataEntries, addedPortDataEntries,
 * addedLinkDataEntries, addedHostDataEntries
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
    private final ImmutableList<SwitchData> addedSwitchDataEntries;
    private final ImmutableList<SwitchData> removedSwitchDataEntries;
    private final ImmutableList<PortData> addedPortDataEntries;
    private final ImmutableList<PortData> removedPortDataEntries;
    private final ImmutableList<LinkData> addedLinkDataEntries;
    private final ImmutableList<LinkData> removedLinkDataEntries;
    private final ImmutableList<HostData> addedHostDataEntries;
    private final ImmutableList<HostData> removedHostDataEntries;

    /**
     * Constructor for added and removed events.
     *
     * @param addedMastershipEvents the collection of added Mastership Events.
     * @param removedMastershipEvents the collection of removed Mastership
     *        Events.
     * @param addedSwitchDataEntries the collection of added Switch Data
     * Entries.
     * @param removedSwitchDataEntries the collection of removed Switch Data
     * Entries.
     * @param addedPortDataEntries the collection of added Port Data Entries.
     * @param removedPortDataEntries the collection of removed Port Data
     * Entries.
     * @param addedLinkDataEntries the collection of added Link Data Entries.
     * @param removedLinkDataEntries the collection of removed Link Data
     * Entries.
     * @param addedHostDataEntries the collection of added Host Data Entries.
     * @param removedHostDataEntries the collection of removed Host Data
     * Entries.
     */
    public TopologyEvents(Collection<MastershipEvent> addedMastershipEvents,
                          Collection<MastershipEvent> removedMastershipEvents,
                          Collection<SwitchData> addedSwitchDataEntries,
                          Collection<SwitchData> removedSwitchDataEntries,
                          Collection<PortData> addedPortDataEntries,
                          Collection<PortData> removedPortDataEntries,
                          Collection<LinkData> addedLinkDataEntries,
                          Collection<LinkData> removedLinkDataEntries,
                          Collection<HostData> addedHostDataEntries,
                          Collection<HostData> removedHostDataEntries) {
        this.addedMastershipEvents = ImmutableList.<MastershipEvent>copyOf(
                        checkNotNull(addedMastershipEvents));
        this.removedMastershipEvents = ImmutableList.<MastershipEvent>copyOf(
                        checkNotNull(removedMastershipEvents));
        this.addedSwitchDataEntries = ImmutableList.<SwitchData>copyOf(
                        checkNotNull(addedSwitchDataEntries));
        this.removedSwitchDataEntries = ImmutableList.<SwitchData>copyOf(
                        checkNotNull(removedSwitchDataEntries));
        this.addedPortDataEntries = ImmutableList.<PortData>copyOf(
                        checkNotNull(addedPortDataEntries));
        this.removedPortDataEntries = ImmutableList.<PortData>copyOf(
                        checkNotNull(removedPortDataEntries));
        this.addedLinkDataEntries = ImmutableList.<LinkData>copyOf(
                        checkNotNull(addedLinkDataEntries));
        this.removedLinkDataEntries = ImmutableList.<LinkData>copyOf(
                        checkNotNull(removedLinkDataEntries));
        this.addedHostDataEntries = ImmutableList.<HostData>copyOf(
                        checkNotNull(addedHostDataEntries));
        this.removedHostDataEntries = ImmutableList.<HostData>copyOf(
                        checkNotNull(removedHostDataEntries));
    }

    /**
     * Constructor for added events only.
     *
     * @param addedMastershipEvents the collection of added Mastership Events.
     * @param addedSwitchDataEntries the collection of added Switch Events.
     * @param addedPortDataEntries the collection of added Port Events.
     * @param addedLinkDataEntries the collection of added Link Events.
     * @param addedHostDataEntries the collection of added Host Events.
     */
    public TopologyEvents(Collection<MastershipEvent> addedMastershipEvents,
                          Collection<SwitchData> addedSwitchDataEntries,
                          Collection<PortData> addedPortDataEntries,
                          Collection<LinkData> addedLinkDataEntries,
                          Collection<HostData> addedHostDataEntries) {
        this.addedMastershipEvents = ImmutableList.<MastershipEvent>copyOf(
                        checkNotNull(addedMastershipEvents));
        this.addedSwitchDataEntries = ImmutableList.<SwitchData>copyOf(
                        checkNotNull(addedSwitchDataEntries));
        this.addedPortDataEntries = ImmutableList.<PortData>copyOf(
                        checkNotNull(addedPortDataEntries));
        this.addedLinkDataEntries = ImmutableList.<LinkData>copyOf(
                        checkNotNull(addedLinkDataEntries));
        this.addedHostDataEntries = ImmutableList.<HostData>copyOf(
                        checkNotNull(addedHostDataEntries));

        // Assign empty lists to the removed events
        this.removedMastershipEvents = ImmutableList.<MastershipEvent>of();
        this.removedSwitchDataEntries = ImmutableList.<SwitchData>of();
        this.removedPortDataEntries = ImmutableList.<PortData>of();
        this.removedLinkDataEntries = ImmutableList.<LinkData>of();
        this.removedHostDataEntries = ImmutableList.<HostData>of();
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
    public Collection<SwitchData> getAddedSwitchDataEntries() {
        return addedSwitchDataEntries;
    }

    /**
     * Gets the immutable collection of removed Switch Events.
     *
     * @return the immutable collection of removed Switch Events.
     */
    public Collection<SwitchData> getRemovedSwitchDataEntries() {
        return removedSwitchDataEntries;
    }

    /**
     * Gets the immutable collection of added Port Events.
     *
     * @return the immutable collection of added Port Events.
     */
    public Collection<PortData> getAddedPortDataEntries() {
        return addedPortDataEntries;
    }

    /**
     * Gets the immutable collection of removed Port Events.
     *
     * @return the immutable collection of removed Port Events.
     */
    public Collection<PortData> getRemovedPortDataEntries() {
        return removedPortDataEntries;
    }

    /**
     * Gets the immutable collection of added Link Events.
     *
     * @return the immutable collection of added Link Events.
     */
    public Collection<LinkData> getAddedLinkDataEntries() {
        return addedLinkDataEntries;
    }

    /**
     * Gets the immutable collection of removed Link Events.
     *
     * @return the immutable collection of removed Link Events.
     */
    public Collection<LinkData> getRemovedLinkDataEntries() {
        return removedLinkDataEntries;
    }

    /**
     * Gets the immutable collection of added Host Events.
     *
     * @return the immutable collection of added Host Events.
     */
    public Collection<HostData> getAddedHostDataEntries() {
        return addedHostDataEntries;
    }

    /**
     * Gets the immutable collection of removed Host Events.
     *
     * @return the immutable collection of removed Host Events.
     */
    public Collection<HostData> getRemovedHostDataEntries() {
        return removedHostDataEntries;
    }
}
