package net.onrc.onos.core.intent;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import net.onrc.onos.core.topology.Link;
import net.onrc.onos.core.topology.LinkEvent;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

/**
 * In addition to maintaining the Intent ID to Intent mapping of its
 * superclass, this class maintains a mapping from switch port to
 * PathIntent. It is used to quickly identify Intents that are affected
 * when a network event involves a particular switch port.
 */
public class PathIntentMap extends IntentMap {
    private final HashMap<Dpid, HashMap<PortNumber, HashSet<PathIntent>>> intents;

    /**
     * Constructor.
     */
    public PathIntentMap() {
        intents = new HashMap<>();
    }

    /**
     * Retrieve all PathIntents that contain the specified switch port.
     *
     * @param swPort the switch port to retrieve Intents for.
     * @return a set of all intents that contain swPort
     */
    private HashSet<PathIntent> get(SwitchPort swPort) {
        Dpid dpid = swPort.getDpid();
        PortNumber port = swPort.getPortNumber();
        HashMap<PortNumber, HashSet<PathIntent>> portToIntents = intents.get(dpid);
        if (portToIntents == null) {
            portToIntents = new HashMap<>();
            intents.put(dpid, portToIntents);
        }
        HashSet<PathIntent> targetIntents = portToIntents.get(port);
        if (targetIntents == null) {
            targetIntents = new HashSet<>();
            portToIntents.put(port, targetIntents);
        }
        return targetIntents;
    }

    /**
     * Add a PathIntent to a particular switch port.
     *
     * @param swPort switch port
     * @param intent Path Intent
     */
    private void put(SwitchPort swPort, PathIntent intent) {
        get(swPort).add(intent);
    }

    /**
     * Add a PathIntent to the map. The function will automatically
     * update the map with all switch ports contained in the Intent.
     *
     * @param intent the PathIntent
     */
    @Override
    protected void putIntent(Intent intent) {
        if (!(intent instanceof PathIntent)) {
            return; // TODO throw exception
        }
        super.putIntent(intent);

        PathIntent pathIntent = (PathIntent) intent;
        for (LinkEvent linkEvent : pathIntent.getPath()) {
            put(linkEvent.getSrc(), (PathIntent) intent);
            put(linkEvent.getDst(), (PathIntent) intent);
        }
    }

    /**
     * Removes a PathIntent from the map, including all switch ports.
     *
     * @param intentId the ID of the PathIntent to be removed
     */
    @Override
    protected void removeIntent(String intentId) {
        PathIntent intent = (PathIntent) getIntent(intentId);
        for (LinkEvent linkEvent : intent.getPath()) {
            get(linkEvent.getSrc()).remove(intent);
            get(linkEvent.getDst()).remove(intent);
        }
        super.removeIntent(intentId);
    }

    /**
     * Retrieve all intents that use a particular link.
     *
     * @param linkEvent the link to look up
     * @return a collection of PathIntents that use the link
     */
    public Collection<PathIntent> getIntentsByLink(LinkEvent linkEvent) {
        return getIntentsByPort(
                linkEvent.getSrc().getDpid(),
                linkEvent.getSrc().getPortNumber());
    }

    /**
     * Retrieve all intents that use a particular port.
     *
     * @param dpid the switch's DPID
     * @param portNumber the switch's port
     * @return a collection of PathIntents that use the port
     */
    public Collection<PathIntent> getIntentsByPort(Dpid dpid, PortNumber portNumber) {
        HashMap<PortNumber, HashSet<PathIntent>> portToIntents = intents.get(dpid);
        if (portToIntents != null) {
            HashSet<PathIntent> targetIntents = portToIntents.get(portNumber);
            if (targetIntents != null) {
                return Collections.unmodifiableCollection(targetIntents);
            }
        }
        return new HashSet<>();
    }

    /**
     * Retrieve all intents that use a particular switch.
     *
     * @param dpid the switch's DPID
     * @return a collection of PathIntents that use the switch
     */
    public Collection<PathIntent> getIntentsByDpid(Dpid dpid) {
        HashSet<PathIntent> result = new HashSet<>();
        HashMap<PortNumber, HashSet<PathIntent>> portToIntents = intents.get(dpid);
        if (portToIntents != null) {
            for (HashSet<PathIntent> targetIntents : portToIntents.values()) {
                result.addAll(targetIntents);
            }
        }
        return result;
    }

    /**
     * Calculate available bandwidth of specified link.
     *
     * @param link the Link
     * @return the available bandwidth
     */
    public Double getAvailableBandwidth(Link link) {
        if (link == null) {
            return null;
        }
        Double bandwidth = link.getCapacity();
        LinkEvent linkEvent = new LinkEvent(link);
        if (!bandwidth.isInfinite()) {
            for (PathIntent intent : getIntentsByLink(linkEvent)) {
                Double intentBandwidth = intent.getBandwidth();
                if (intentBandwidth == null || intentBandwidth.isInfinite() || intentBandwidth.isNaN()) {
                    continue;
                }
                bandwidth -= intentBandwidth;
            }
        }
        return bandwidth;
    }
}
