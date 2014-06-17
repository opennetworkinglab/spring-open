package net.onrc.onos.core.intent.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.onrc.onos.core.intent.Intent.IntentState;

/**
 * Used by PathCalcRuntimeModule and PlanInstallModule
 * to notify path intents' state changes.
 */
public class IntentStateList {
    protected Map<String, IntentState> intentMap;
    public Set<Long> domainSwitchDpids;

    /**
     * Constructor to make new IntentStateList.
     */
    public IntentStateList() {
        intentMap = new HashMap<String, IntentState>();
        domainSwitchDpids = new HashSet<Long>();
    }

    /**
     * Adds or modifies intent's state.
     *
     * @param id an intent ID for the state.
     * @param state a state for the intent.
     * @return the previous state, or null if there was no intents.
     */
    public IntentState put(String id, IntentState state) {
        return intentMap.put(id, state);
    }

    /**
     * Returns a set of view of the intent states.
     *
     * @return a set of intent IDs and intent states.
     */
    public Set<Entry<String, IntentState>> entrySet() {
        return intentMap.entrySet();
    }

    /**
     * Removes all of intent states from this object.
     */
    public void clear() {
        intentMap.clear();
    }
}
