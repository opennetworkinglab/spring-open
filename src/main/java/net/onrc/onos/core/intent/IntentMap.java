package net.onrc.onos.core.intent;

import java.util.Collection;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;

import net.onrc.onos.core.intent.Intent.IntentState;
import net.onrc.onos.core.intent.runtime.IntentStateList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The IntentMap contains all of the Intents in the system. It is also
 * responsible for handling notifications to interested listeners when
 * Intents are changed/updated.
 */
public class IntentMap {
    private final HashSet<ChangedListener> listeners = new HashSet<>();
    private final HashMap<String, Intent> intents = new HashMap<>();
    private final LinkedList<ChangedEvent> events = new LinkedList<>();
    private static final Logger log = LoggerFactory.getLogger(IntentMap.class);

    public enum ChangedEventType {
        /**
         * Added new intent.
         */
        ADDED,

        /**
         * Removed existing intent.
         * The specified intent is an instance of Intent class (not a child class)
         * Only id and state are valid.
         */
        REMOVED,

        /**
         * Changed state of existing intent.
         * The specified intent is an instance of Intent class (not a child class)
         * Only id and state are valid.
         */
        STATE_CHANGED,
    }

    /**
     * This class is a wrapper for Intent event notifications.
     */
    public static class ChangedEvent {

        /**
         * Constructor.
         *
         * @param eventType the type of event
         * @param intent the affected Intent
         */
        public ChangedEvent(ChangedEventType eventType, Intent intent) {
            this.eventType = eventType;
            this.intent = intent;
        }

        public ChangedEventType eventType;
        public Intent intent;

        /**
         * Get a string representation of the object.
         * <p/>
         * The string has the following form:
         * [eventType=XXX intent=XXX]
         *
         * @return a string representation of the object.
         */
        @Override
        public String toString() {
            StringBuilder ret = new StringBuilder();
            ret.append("[eventType=");
            ret.append(this.eventType.toString());
            ret.append(" intent=");
            ret.append(this.intent.toString());
            ret.append("]");
            return ret.toString();
        }
    }

    /**
     * An interface to listen to Intent updates.
     */
    public interface ChangedListener extends EventListener {
        void intentsChange(LinkedList<ChangedEvent> events);
    }

    //================================================================================
    // public methods
    //================================================================================

    /**
     * Performs the specified operations on a list of Intents.
     *
     * @param operations list of Intents and associated operations
     */
    public void executeOperations(IntentOperationList operations) {
        for (IntentOperation operation : operations) {
            switch (operation.operator) {
            case ADD:
                handleAddOperation(operation);
                break;
            case REMOVE:
                handleRemoveOperation(operation);
                break;
            case ERROR:
                handleErrorOperation(operation);
                break;
            default:
                log.error("Unknown intent operation {}", operation.operator);
                break;
            }
        }
        notifyEvents();
    }

    /**
     * Purge all Intents that are in a state allowing them to be removed.
     */
    public void purge() {
        LinkedList<String> removeIds = new LinkedList<>();

        // Collect the IDs of all intents that can be removed
        for (Entry<String, Intent> entry : intents.entrySet()) {
            Intent intent = entry.getValue();
            if (intent.getState() == IntentState.DEL_ACK
                    || intent.getState() == IntentState.INST_NACK) {
                removeIds.add(intent.getId());
            }
        }

        purge(removeIds);
    }

    /**
     * Purge a collection of Intents specified by Intent IDs.
     *
     * NOTE: The caller needs to make sure those intents are in a state
     * that allows them to be removed.
     *
     * @param removeIds the collection of Intent IDs to purge.
     */
    public void purge(Collection<String> removeIds) {
        for (String intentId : removeIds) {
            removeIntent(intentId);
        }
        notifyEvents();
    }

    /**
     * Update the states of intents in the map to match those provided.
     *
     * @param states list of new Intent states
     */
    public void changeStates(IntentStateList states) {
        for (Entry<String, IntentState> state : states.entrySet()) {
            setState(state.getKey(), state.getValue());
        }
        notifyEvents();
    }

    /**
     * Get an Intent from the map.
     *
     * @param intentId ID of requested Intent
     * @return Intent with specified ID
     */
    public Intent getIntent(String intentId) {
        return intents.get(intentId);
    }

    /**
     * Get all Intents from the map.
     *
     * @return a collection including all Intents
     */
    public Collection<Intent> getAllIntents() {
        return intents.values();
    }

    /**
     * Add a change listener.
     *
     * @param listener new listener to change events
     */
    public void addChangeListener(ChangedListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a change listener.
     *
     * @param listener the listener to be removed
     */
    public void removeChangedListener(ChangedListener listener) {
        listeners.remove(listener);
    }

    //================================================================================
    // methods that affect intents map (protected)
    //================================================================================

    /**
     * Put an intent in the map.
     *
     * @param intent intent to be added
     */
    protected void putIntent(Intent intent) {
        if (intents.containsKey(intent.getId())) {
            removeIntent(intent.getId());
        }
        intents.put(intent.getId(), intent);
        events.add(new ChangedEvent(ChangedEventType.ADDED, intent));
    }

    /**
     * Remove an intent from the map.
     *
     * @param intentId intent to be removed
     */
    protected void removeIntent(String intentId) {
        Intent intent = intents.remove(intentId);
        if (intent == null) {
            return;
        }
        events.add(new ChangedEvent(ChangedEventType.REMOVED, intent));
    }

    /**
     * Change the state of an Intent in the map.
     *
     * @param intentId ID of the Intent to change
     * @param state new state for the specified Intent
     */
    protected void setState(String intentId, IntentState state) {
        Intent intent = intents.get(intentId);
        if (intent == null) {
            return;
        }
        intent.setState(state);
        events.add(new ChangedEvent(ChangedEventType.STATE_CHANGED, intent));
    }

    //================================================================================
    // helper methods (protected)
    //================================================================================

    /**
     * Helper to add Intents to the map.
     *
     * @param operation the Intent to be added
     */
    protected void handleAddOperation(IntentOperation operation) {
        putIntent(operation.intent);
    }

    /**
     * Helper to remove Intents from the map.
     *
     * @param operation the Intent to be removed
     */
    protected void handleRemoveOperation(IntentOperation operation) {
        Intent intent = getIntent(operation.intent.getId());
        if (intent == null) {
            // TODO error handling
            return;
        } else {
            setState(intent.getId(), IntentState.DEL_REQ);
        }
    }

    /**
     * Helper to handle Intents in the error state.
     *
     * @param operation the Intent to be resolved
     */
    protected void handleErrorOperation(IntentOperation operation) {
        //TODO put error message into the intent

        ErrorIntent errorIntent = (ErrorIntent) operation.intent;
        Intent targetIntent = intents.get(errorIntent.getId());
        if (targetIntent == null) {
            // TODO error handling
            return;
        }

        switch (targetIntent.getState()) {
        case CREATED:
        case INST_REQ:
        case INST_ACK:
        case REROUTE_REQ:
            setState(targetIntent.getId(), IntentState.INST_NACK);
            break;
        case DEL_REQ:
            setState(targetIntent.getId(), IntentState.DEL_PENDING);
            break;
        case INST_NACK:
        case DEL_PENDING:
        case DEL_ACK:
            // do nothing
            break;
        default:
            log.error("Unknown intent state {}", targetIntent.getState());
            break;
        }
    }

    /**
     * Notify all registered listeners of events in queue.
     */
    protected void notifyEvents() {
        if (events.isEmpty()) {
            return;
        }

        for (ChangedListener listener : listeners) {
            listener.intentsChange(events);
        }
        events.clear();
    }
}
