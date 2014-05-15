package net.onrc.onos.core.intent.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.onrc.onos.core.intent.Intent.IntentState;
public class IntentStateList {
    protected Map<String, IntentState> intentMap;
    public Set<Long> domainSwitchDpids;

    public IntentStateList() {
        intentMap = new HashMap<String, IntentState>();
        domainSwitchDpids = new HashSet<Long>();
    }

    public IntentState put(String id, IntentState state) {
        return intentMap.put(id, state);
    }

    public Set<Entry<String, IntentState>> entrySet() {
        return intentMap.entrySet();
    }

    public void clear() {
        intentMap.clear();
    }
}
