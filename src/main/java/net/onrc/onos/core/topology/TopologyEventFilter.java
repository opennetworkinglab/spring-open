package net.onrc.onos.core.topology;

import java.util.Collection;

import net.onrc.onos.core.util.EventEntry;

/**
 * Stateful filter for filtering Topology events.
 * <p/>
 * NOTE: The filter itself keeps internal state about filtered events.
 * As part of the filtering logic, a previously suppressed event might
 * be genenerated (released) later because of some other event.
 */
public class TopologyEventFilter {
    /**
     * Filter a collection of events.
     *
     * @param events the events to filter.
     * @return a collection of filtered events.
     */
    Collection<EventEntry<TopologyEvent>> filterEvents(
                Collection<EventEntry<TopologyEvent>> events) {

        // TODO: Not implemented yet
        return events;
    }
}
