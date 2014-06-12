package net.onrc.onos.core.intent.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.intent.FlowEntry;
import net.onrc.onos.core.intent.Intent;
import net.onrc.onos.core.intent.IntentOperation;
import net.onrc.onos.core.intent.IntentOperation.Operator;
import net.onrc.onos.core.intent.IntentOperationList;
import net.onrc.onos.core.intent.PathIntent;
import net.onrc.onos.core.intent.ShortestPathIntent;
import net.onrc.onos.core.topology.LinkEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The PlanCalcRuntime class receives a list of intents and operations (IntentOperationList),
 * and does the following:
 * <ul>
 * <li> Convert the Intent into FlowEntries.
 * <li> Determine the dependency between FlowEntries.
 * <li> Create sets of FlowEntries to be installed by the OpenFlow protocol.
 * </ul>
 */

public class PlanCalcRuntime {

    private static final Logger log = LoggerFactory.getLogger(PlanCalcRuntime.class);

    /**
     * The method produces a plan from a list of IntentOperations.
     * <p>
     * In this context, a plan is a list of sets of flow entries. Flow entries within
     * a set can be installed in parallel. Each set of flow entries is dependent on
     * all previous sets in the list.
     *
     * @param intentOps list of IntentOperations (Intent + Operation) for plan computation
     * @return plan (list of set of flow entries)
     */
    public List<Set<FlowEntry>> computePlan(IntentOperationList intentOps) {
        long start = System.nanoTime();
        List<Collection<FlowEntry>> flowEntries = computeFlowEntries(intentOps);
        long step1 = System.nanoTime();
        List<Set<FlowEntry>> plan = buildPhases(flowEntries);
        long step2 = System.nanoTime();
        log.debug("MEASUREMENT: Compute flow entries: {} ns, Build phases: {} ns",
                (step1 - start), (step2 - step1));
        return plan;
    }

    /**
     * Converts PathIntents to FlowEntries so they can be installed.
     * <p>
     * Note: This method only supports ShortestPathIntents at the moment.
     *
     * @param intentOps list of IntentOperations
     * @return a list of FlowEntry objects for each Intent in intentOps
     */
    private List<Collection<FlowEntry>> computeFlowEntries(IntentOperationList intentOps) {
        List<Collection<FlowEntry>> flowEntries = new LinkedList<>();
        for (IntentOperation i : intentOps) {
            if (!(i.intent instanceof PathIntent)) {
                log.warn("Not a path intent: {}", i);
                continue;
            }
            PathIntent intent = (PathIntent) i.intent;
            Intent parent = intent.getParentIntent();
            long srcPort, dstPort;
            long lastDstSw = -1, lastDstPort = -1, firstSrcSw = -1;
            MACAddress srcMac, dstMac;
            int idleTimeout = 0, hardTimeout = 0, firstSwitchIdleTimeout = 0, firstSwitchHardTimeout = 0;
            Long cookieId = null;
            int srcIP, dstIP;
            if (parent instanceof ShortestPathIntent) {
                ShortestPathIntent pathIntent = (ShortestPathIntent) parent;
                srcPort = pathIntent.getSrcPortNumber();

                if (pathIntent.getSrcMac() != ShortestPathIntent.EMPTYMACADDRESS) {
                    srcMac = MACAddress.valueOf(pathIntent.getSrcMac());
                } else {
                    srcMac = null;
                }

                if (pathIntent.getDstMac() != ShortestPathIntent.EMPTYMACADDRESS) {
                    dstMac = MACAddress.valueOf(pathIntent.getDstMac());
                } else {
                    dstMac = null;
                }

                srcIP = pathIntent.getSrcIp();
                dstIP = pathIntent.getDstIp();

                lastDstSw = pathIntent.getDstSwitchDpid();
                firstSrcSw = pathIntent.getSrcSwitchDpid();
                lastDstPort = pathIntent.getDstPortNumber();
                idleTimeout = pathIntent.getIdleTimeout();
                hardTimeout = pathIntent.getHardTimeout();
                firstSwitchIdleTimeout = pathIntent.getFirstSwitchIdleTimeout();
                firstSwitchHardTimeout = pathIntent.getFirstSwitchHardTimeout();
                try {
                    cookieId = Long.valueOf(pathIntent.getId());
                } catch (NumberFormatException e) {
                    log.trace("NumberFormatException : ", e);
                }
            } else {
                log.warn("Unsupported Intent: {}", parent);
                continue;
            }
            List<FlowEntry> entries = new ArrayList<>();
            for (LinkEvent linkEvent : intent.getPath()) {
                long sw = linkEvent.getSrc().getDpid();
                dstPort = linkEvent.getSrc().getNumber();
                FlowEntry fe = new FlowEntry(sw, srcPort, dstPort, srcMac, dstMac,
                                             srcIP, dstIP, i.operator);
                if (sw != firstSrcSw) {
                    fe.setIdleTimeout(idleTimeout);
                    fe.setHardTimeout(hardTimeout);
                } else {
                    fe.setIdleTimeout(firstSwitchIdleTimeout);
                    fe.setHardTimeout(firstSwitchHardTimeout);
                }
                if (cookieId != null) {
                    log.trace("cookieId is set: {}", cookieId);
                    fe.setFlowEntryId(cookieId);
                }
                entries.add(fe);
                srcPort = linkEvent.getDst().getNumber();
            }
            if (lastDstSw >= 0 && lastDstPort >= 0) {
                long sw = lastDstSw;
                dstPort = lastDstPort;
                FlowEntry fe = new FlowEntry(sw, srcPort, dstPort, srcMac, dstMac,
                                             srcIP, dstIP, i.operator);
                if (cookieId != null) {
                    log.trace("cookieId is set: {}", cookieId);
                    fe.setFlowEntryId(cookieId);
                }
                if (sw != firstSrcSw) {
                    fe.setIdleTimeout(idleTimeout);
                    fe.setHardTimeout(hardTimeout);
                } else {
                    fe.setIdleTimeout(firstSwitchIdleTimeout);
                    fe.setHardTimeout(firstSwitchHardTimeout);
                }
                entries.add(fe);
            }
            // reverse order of flow entries so they are installed backwards
            Collections.reverse(entries);
            flowEntries.add(entries);
        }
        return flowEntries;
    }

    /**
     * TODO
     *
     * Note: This method is for a testing purpose. Please leave it right now.
     *
     * @param flowEntries
     * @return
     */
    @SuppressWarnings("unused")
    private List<Set<FlowEntry>> simpleBuildPhases(List<Collection<FlowEntry>> flowEntries) {
        List<Set<FlowEntry>> plan = new ArrayList<>();
        Set<FlowEntry> phase = new HashSet<>();
        for (Collection<FlowEntry> c : flowEntries) {
            phase.addAll(c);
        }
        plan.add(phase);

        return plan;
    }

    /**
     * Merges the lists generated by computeFlowEntries() into install phases.
     * <p>
     * This function will also remove duplicate entries.
     *
     * @param flowEntries list of lists of flowEntries
     * @return a list of sets of FlowEntries to be installed
     */
    private List<Set<FlowEntry>> buildPhases(List<Collection<FlowEntry>> flowEntries) {
        Map<FlowEntry, Integer> map = new HashMap<>();
        List<Set<FlowEntry>> plan = new ArrayList<>();
        // merge equal FlowEntries
        //TODO: explore ways to merge FlowEntries that contain the same match condition
        for (Collection<FlowEntry> c : flowEntries) {
            for (FlowEntry e : c) {
                Integer i = map.get(e);
                if (i == null) {
                    i = Integer.valueOf(0);
                }
                switch (e.getOperator()) {
                    case ADD:
                        i += 1;
                        break;
                    case REMOVE:
                        i -= 1;
                        break;
                    default:
                        break;
                }
                map.put(e, i);
            }
        }

        // really simple first iteration of plan
        //TODO: optimize the map in phases
        Set<FlowEntry> phase = new HashSet<>();
        for (Map.Entry<FlowEntry, Integer> entry : map.entrySet()) {
            FlowEntry e = entry.getKey();
            Integer i = entry.getValue();
            if (i == 0) {
                continue;
            } else if (i > 0) {
                e.setOperator(Operator.ADD);
            } else if (i < 0) {
                e.setOperator(Operator.REMOVE);
            }
            phase.add(e);
        }
        plan.add(phase);

        return plan;
    }
}
