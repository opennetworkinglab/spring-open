package net.onrc.onos.core.topology;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.registry.RegistryException;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.EventEntry;
import net.onrc.onos.core.util.OnosInstanceId;

import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Topology Event pre-processor. It is used by the Topology Manager for
 * pre-processing Topology events before applying them to the Topology.
 * <p/>
 * The pre-processor itself keeps internal state about the most recent
 * ADD events. It also might keep state about reordered events that cannot
 * be applied.
 * <p/>
 * As part of the pre-processing logic, a previously suppressed event might
 * be genenerated later because of some other event.
 */
public class TopologyEventPreprocessor {
    private static final Logger log = LoggerFactory
        .getLogger(TopologyEventPreprocessor.class);
    private final IControllerRegistryService registryService;

    //
    // Reordered ADD events that need to be reapplied
    //
    // TODO: For now, this field is accessed by the TopologyManager as well
    // This should be refactored, and change them to private.
    //
    Map<ByteBuffer, TopologyEvent> reorderedEvents = new HashMap<>();

    //
    // Topology ADD event state per ONOS instance
    //
    private Map<OnosInstanceId, OnosInstanceLastAddEvents> instanceState =
        new HashMap<>();

    //
    // Switch mastership state (updated by the topology events)
    //
    Map<Dpid, OnosInstanceId> switchMastership = new HashMap<>();

    /**
     * Constructor for a given Registry Service.
     *
     * @param registryService the Registry Service to use.
     */
    TopologyEventPreprocessor(IControllerRegistryService registryService) {
        this.registryService = registryService;
    }

    /**
     * Class to store the last ADD Topology Events per ONOS Instance.
     */
    private final class OnosInstanceLastAddEvents {
        private final OnosInstanceId onosInstanceId;

        // The last ADD events received from this ONOS instance
        Map<ByteBuffer, TopologyEvent> topologyEvents = new HashMap<>();

        /**
         * Constructor for a given ONOS Instance ID.
         *
         * @param onosInstanceId the ONOS Instance ID.
         */
        OnosInstanceLastAddEvents(OnosInstanceId onosInstanceId) {
            this.onosInstanceId = checkNotNull(onosInstanceId);
        }

        /**
         * Processes an event originated by this ONOS instance.
         *
         * @param event the event to process.
         * @return true if the event should be applied to the final Topology
         * as well, otherwise false.
         */
        boolean processEvent(EventEntry<TopologyEvent> event) {
            TopologyEvent topologyEvent = event.eventData();
            ByteBuffer id = topologyEvent.getIDasByteBuffer();
            OnosInstanceId masterId = null;

            // Get the Master of the Origin DPID
            Dpid dpid = topologyEvent.getOriginDpid();
            if (dpid != null) {
                masterId = switchMastership.get(dpid);
            }

            //
            // Apply the event based on its type
            //
            switch (event.eventType()) {
            case ENTRY_ADD:
                topologyEvents.put(id, topologyEvent);
                reorderedEvents.remove(id);
                // Allow the ADD only if the event was originated by the Master
                return onosInstanceId.equals(masterId);

            case ENTRY_REMOVE:
                reorderedEvents.remove(id);
                // Don't allow the REMOVE event if there was no ADD before
                if (topologyEvents.remove(id) == null) {
                    return false;
                }
                //
                // Allow the REMOVE if the event was originated by the Master,
                // or there is no Master at all.
                //
                if (masterId == null) {
                    return true;
                }
                return onosInstanceId.equals(masterId);

            default:
                log.error("Unknown topology event {}", event.eventType());
            }

            return false;
        }

        /**
         * Gets the postponed events for a given DPID.
         * Those are the events that couldn't be applied earlier to the
         * Topology, because the ONOS Instance originating the events
         * was not the Master for the Switch.
         *
         * @param dpid the DPID to use.
         * @return a list of postponed events for the given DPID.
         */
        List<EventEntry<TopologyEvent>> getPostponedEvents(Dpid dpid) {
            List<EventEntry<TopologyEvent>> result = new LinkedList<>();

            //
            // Search all events, and keep only those that match the DPID
            //
            // TODO: This could be slow, and the code should be optimized
            // for speed. The processing complexity is O(N*N) where N is
            // the number of Switches: for each Switch Mastership we call
            // getPostponedEvents(), and then for each call we
            // search all previously added events.
            // The code can be optimized by adding additional lookup map:
            //  Dpid -> List<TopologyEvent>
            //
            for (TopologyEvent te : topologyEvents.values()) {
                if (dpid.equals(te.getOriginDpid())) {
                    result.add(new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD, te));
                }
            }

            return result;
        }
    }

    /**
     * Extracts previously reordered events that should be applied again
     * to the Topology.
     *
     * @return a list of previously reordered events.
     */
    List<EventEntry<TopologyEvent>> extractReorderedEvents() {
        List<EventEntry<TopologyEvent>> result = new LinkedList<>();

        //
        // Search all previously reordered events, and extract only if
        // the originator is the Master.
        //
        List<TopologyEvent> leftoverEvents = new LinkedList<>();
        for (TopologyEvent te : reorderedEvents.values()) {
            Dpid dpid = te.getOriginDpid();
            OnosInstanceId masterId = null;
            if (dpid != null) {
                masterId = switchMastership.get(dpid);
            }
            if (te.getOnosInstanceId().equals(masterId)) {
                result.add(new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD, te));
            } else {
                leftoverEvents.add(te);
            }
        }

        //
        // Add back the leftover events
        //
        reorderedEvents.clear();
        for (TopologyEvent te : leftoverEvents) {
            reorderedEvents.put(te.getIDasByteBuffer(), te);
        }

        return result;
    }

    /**
     * Pre-processes a list of events.
     *
     * @param events the events to pre-process.
     * @return a list of pre-processed events.
     */
    List<EventEntry<TopologyEvent>> processEvents(
                List<EventEntry<TopologyEvent>> events) {
        List<EventEntry<TopologyEvent>> result = new LinkedList<>();

        //
        // Process the events
        //
        for (EventEntry<TopologyEvent> event : events) {
            List<EventEntry<TopologyEvent>> postponedEvents = null;

            // Ignore NO-OP events
            if (event.isNoop()) {
                continue;
            }

            TopologyEvent topologyEvent = event.eventData();
            OnosInstanceId onosInstanceId = topologyEvent.getOnosInstanceId();

            log.debug("Topology event {}: {}", event.eventType(),
                      topologyEvent);

            // Get the ONOS instance state
            OnosInstanceLastAddEvents instance =
                instanceState.get(onosInstanceId);
            if (instance == null) {
                instance = new OnosInstanceLastAddEvents(onosInstanceId);
                instanceState.put(onosInstanceId, instance);
            }

            //
            // Update the Switch Mastership state:
            //  - If ADD a MASTER and the Mastership is confirmed by the
            //    Registry Service, then add to the Mastership map and fetch
            //    the postponed events from the originating ONOS Instance.
            //  - Otherwise, remove from the Mastership map, but only if it is
            //    the current MASTER.
            //
            MastershipEvent mastershipEvent =
                topologyEvent.getMastershipEvent();
            if (mastershipEvent != null) {
                Dpid dpid = mastershipEvent.getDpid();
                boolean newMaster = false;

                if ((event.eventType() == EventEntry.Type.ENTRY_ADD) &&
                    (mastershipEvent.getRole() == Role.MASTER)) {
                    //
                    // Check with the Registry Service as well
                    //
                    try {
                        String rc =
                            registryService.getControllerForSwitch(dpid.value());
                        if ((rc != null) &&
                            onosInstanceId.equals(new OnosInstanceId(rc))) {
                            newMaster = true;
                        }
                    } catch (RegistryException e) {
                        log.error("Caught RegistryException while pre-processing Mastership Event", e);
                    }
                }

                if (newMaster) {
                    // Add to the map
                    switchMastership.put(dpid, onosInstanceId);
                    postponedEvents = instance.getPostponedEvents(dpid);
                } else {
                    // Eventually remove from the map
                    OnosInstanceId oldId = switchMastership.get(dpid);
                    if (onosInstanceId.equals(oldId)) {
                        switchMastership.remove(dpid);
                    }
                }
            }

            //
            // Process the event and eventually store it in the
            // per-Instance state.
            //
            if (instance.processEvent(event)) {
                result.add(event);
            }

            // Add the postponed events (if any)
            if (postponedEvents != null) {
                result.addAll(postponedEvents);
            }
        }

        // Extract and add the previously reordered events
        result.addAll(extractReorderedEvents());

        return reorderEventsForTopology(result);
    }

    /**
     * Classifies and reorders a list of events, and suppresses matching
     * events.
     * <p/>
     * The result events can be applied to the Topology in the following
     * order: REMOVE events followed by ADD events. The ADD events are in the
     * natural order to build a Topology: MastershipEvent, SwitchEvent,
     * PortEvent, LinkEvent, HostEvent. The REMOVE events are in the reverse
     * order.
     *
     * @param events the events to classify and reorder.
     * @return the classified and reordered events.
     */
    private List<EventEntry<TopologyEvent>> reorderEventsForTopology(
                List<EventEntry<TopologyEvent>> events) {
        // Local state for computing the final set of events
        Map<ByteBuffer, EventEntry<TopologyEvent>> addedMastershipEvents =
            new HashMap<>();
        Map<ByteBuffer, EventEntry<TopologyEvent>> removedMastershipEvents =
            new HashMap<>();
        Map<ByteBuffer, EventEntry<TopologyEvent>> addedSwitchEvents =
            new HashMap<>();
        Map<ByteBuffer, EventEntry<TopologyEvent>> removedSwitchEvents =
            new HashMap<>();
        Map<ByteBuffer, EventEntry<TopologyEvent>> addedPortEvents =
            new HashMap<>();
        Map<ByteBuffer, EventEntry<TopologyEvent>> removedPortEvents =
            new HashMap<>();
        Map<ByteBuffer, EventEntry<TopologyEvent>> addedLinkEvents =
            new HashMap<>();
        Map<ByteBuffer, EventEntry<TopologyEvent>> removedLinkEvents =
            new HashMap<>();
        Map<ByteBuffer, EventEntry<TopologyEvent>> addedHostEvents =
            new HashMap<>();
        Map<ByteBuffer, EventEntry<TopologyEvent>> removedHostEvents =
            new HashMap<>();

        //
        // Classify and suppress matching events
        //
        // NOTE: We intentionally use the event payload as the key ID
        // (i.e., we exclude the ONOS Instance ID from the key),
        // so we can suppress transient events across multiple ONOS instances.
        //
        for (EventEntry<TopologyEvent> event : events) {
            TopologyEvent topologyEvent = event.eventData();

            // Get the event itself
            MastershipEvent mastershipEvent =
                topologyEvent.getMastershipEvent();
            SwitchEvent switchEvent = topologyEvent.getSwitchEvent();
            PortEvent portEvent = topologyEvent.getPortEvent();
            LinkEvent linkEvent = topologyEvent.getLinkEvent();
            HostEvent hostEvent = topologyEvent.getHostEvent();

            //
            // Extract the events
            //
            switch (event.eventType()) {
            case ENTRY_ADD:
                if (mastershipEvent != null) {
                    ByteBuffer id = mastershipEvent.getIDasByteBuffer();
                    addedMastershipEvents.put(id, event);
                    removedMastershipEvents.remove(id);
                }
                if (switchEvent != null) {
                    ByteBuffer id = switchEvent.getIDasByteBuffer();
                    addedSwitchEvents.put(id, event);
                    removedSwitchEvents.remove(id);
                }
                if (portEvent != null) {
                    ByteBuffer id = portEvent.getIDasByteBuffer();
                    addedPortEvents.put(id, event);
                    removedPortEvents.remove(id);
                }
                if (linkEvent != null) {
                    ByteBuffer id = linkEvent.getIDasByteBuffer();
                    addedLinkEvents.put(id, event);
                    removedLinkEvents.remove(id);
                }
                if (hostEvent != null) {
                    ByteBuffer id = hostEvent.getIDasByteBuffer();
                    addedHostEvents.put(id, event);
                    removedHostEvents.remove(id);
                }
                break;
            case ENTRY_REMOVE:
                if (mastershipEvent != null) {
                    ByteBuffer id = mastershipEvent.getIDasByteBuffer();
                    addedMastershipEvents.remove(id);
                    removedMastershipEvents.put(id, event);
                }
                if (switchEvent != null) {
                    ByteBuffer id = switchEvent.getIDasByteBuffer();
                    addedSwitchEvents.remove(id);
                    removedSwitchEvents.put(id, event);
                }
                if (portEvent != null) {
                    ByteBuffer id = portEvent.getIDasByteBuffer();
                    addedPortEvents.remove(id);
                    removedPortEvents.put(id, event);
                }
                if (linkEvent != null) {
                    ByteBuffer id = linkEvent.getIDasByteBuffer();
                    addedLinkEvents.remove(id);
                    removedLinkEvents.put(id, event);
                }
                if (hostEvent != null) {
                    ByteBuffer id = hostEvent.getIDasByteBuffer();
                    addedHostEvents.remove(id);
                    removedHostEvents.put(id, event);
                }
                break;
            default:
                log.error("Unknown topology event {}", event.eventType());
            }
        }

        //
        // Prepare the result by adding the events in the appropriate order:
        //  - First REMOVE, then ADD
        //  - The REMOVE order is: Host, Link, Port, Switch, Mastership
        //  - The ADD order is the reverse: Mastership, Switch, Port, Link,
        //    Host
        //
        List<EventEntry<TopologyEvent>> result = new LinkedList<>();
        result.addAll(removedHostEvents.values());
        result.addAll(removedLinkEvents.values());
        result.addAll(removedPortEvents.values());
        result.addAll(removedSwitchEvents.values());
        result.addAll(removedMastershipEvents.values());
        //
        result.addAll(addedMastershipEvents.values());
        result.addAll(addedSwitchEvents.values());
        result.addAll(addedPortEvents.values());
        result.addAll(addedLinkEvents.values());
        result.addAll(addedHostEvents.values());

        return result;
    }
}
