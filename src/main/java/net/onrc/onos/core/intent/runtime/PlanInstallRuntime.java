package net.onrc.onos.core.intent.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.OFMessageFuture;
import net.onrc.onos.core.flowprogrammer.IFlowPusherService;
import net.onrc.onos.core.intent.FlowEntry;
import net.onrc.onos.core.util.Pair;

import org.openflow.protocol.OFBarrierReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for installing plans (lists of sets of FlowEntries) into local switches.
 * In this context, a local switch is a switch for which this ONOS instance is the master.
 * It also is responsible for sending barrier messages between sets.
 */

public class PlanInstallRuntime {

    IFlowPusherService pusher;
    IFloodlightProviderService provider;
    private static final Logger log = LoggerFactory.getLogger(PlanInstallRuntime.class);

    /**
     * Constructor.
     *
     * @param provider the FloodlightProviderService for list of local switches
     * @param pusher the FlowPusherService to use for FlowEntry installation
     */
    public PlanInstallRuntime(IFloodlightProviderService provider,
                              IFlowPusherService pusher) {
        this.provider = provider;
        this.pusher = pusher;
    }

    /**
     * This class is a temporary class for collecting FlowMod installation information. It is
     * largely used for debugging purposes, and it should not be depended on for other purposes.
     * <p>
     * TODO: This class should be wrapped into a more generic debugging framework when available.
     */
    private static class FlowModCount {
        IOFSwitch sw;
        long modFlows = 0;
        long delFlows = 0;
        long errors = 0;

        /**
         * Constructor.
         *
         * @param sw the switch for FlowMod statistics collection
         */
        FlowModCount(IOFSwitch sw) {
            this.sw = sw;
        }

        /**
         * Include the FlowEntry in this switch statistics object.
         *
         * @param entry the FlowEntry to count
         */
        void addFlowEntry(FlowEntry entry) {
            switch (entry.getOperator()) {
                case ADD:
                    modFlows++;
                    break;
                case ERROR:
                    errors++;
                    break;
                case REMOVE:
                    delFlows++;
                    break;
                default:
                    break;
            }
        }

        /**
         * Returns a string representation of this object.
         *
         * @return string representation of this object
         */
        @Override
        public String toString() {
            return "sw:" + sw.getStringId() + ": modify " + modFlows + " delete " + delFlows + " error " + errors;
        }

        static Map<IOFSwitch, FlowModCount> map = new HashMap<>();

        /**
         * This function is used for collecting statistics information. It should be called for
         * every FlowEntry that is pushed to the switch for accurate statistics.
         * <p>
         * This class maintains a map of Switches and FlowModCount collection objects, which
         * are used for collection.
         * <p>
         * TODO: This should be refactored to use a more generic mechanism when available.
         *
         * @param sw the switch that entry is being pushed to
         * @param entry the FlowEntry being pushed
         */
        static void countFlowEntry(IOFSwitch sw, FlowEntry entry) {
            FlowModCount count = map.get(sw);
            if (count == null) {
                count = new FlowModCount(sw);
                map.put(sw, count);
            }
            count.addFlowEntry(entry);
        }

        /**
         * Reset the statistics collection. It should be called when required for debugging.
         */
        static void startCount() {
            map.clear();
        }

        /**
         * Print out the statistics information when required for debugging.
         */
        static void printCount() {
            StringBuilder result = new StringBuilder();

            result.append("FLOWMOD COUNT:\n");
            for (FlowModCount count : map.values()) {
                result.append(count.toString() + '\n');
            }
            if (map.values().isEmpty()) {
                result.append("No flow mods installed\n");
            }
            log.debug(result.toString());
        }
    }

    /**
     * This function should be called to install the FlowEntries in the plan.
     * <p>
     * Each set of FlowEntries can be installed together, but all entries should be installed
     * proceeded to the next set.
     * <p>
     * TODO: This method lack coordination between the other ONOS instances before proceeded
     * with the next set of entries
     *
     * @param plan list of set of FlowEntries for installation on local switches
     * @return true (we assume installation is successful)
     */
    public boolean installPlan(List<Set<FlowEntry>> plan) {
        long start = System.nanoTime();
        Map<Long, IOFSwitch> switches = provider.getSwitches();

        log.debug("IOFSwitches: {}", switches);
        FlowModCount.startCount();
        for (Set<FlowEntry> phase : plan) {
            Set<Pair<IOFSwitch, net.onrc.onos.core.util.FlowEntry>> entries = new HashSet<>();
            Set<IOFSwitch> modifiedSwitches = new HashSet<>();

            long step1 = System.nanoTime();
            // convert flow entries and create pairs
            for (FlowEntry entry : phase) {
                IOFSwitch sw = switches.get(entry.getSwitch());
                if (sw == null) {
                    // no active switch, skip this flow entry
                    log.debug("Skipping flow entry: {}", entry);
                    continue;
                }
                entries.add(new Pair<>(sw, entry.getFlowEntry()));
                modifiedSwitches.add(sw);
                FlowModCount.countFlowEntry(sw, entry);
            }
            long step2 = System.nanoTime();

            // push flow entries to switches
            log.debug("Pushing flow entries: {}", entries);
            pusher.pushFlowEntries(entries);
            long step3 = System.nanoTime();

            // insert a barrier after each phase on each modifiedSwitch
            // wait for confirmation messages before proceeding
            List<Pair<IOFSwitch, OFMessageFuture<OFBarrierReply>>> barriers = new ArrayList<>();
            for (IOFSwitch sw : modifiedSwitches) {
                barriers.add(new Pair<>(sw, pusher.barrierAsync(sw)));
            }
            for (Pair<IOFSwitch, OFMessageFuture<OFBarrierReply>> pair : barriers) {
                IOFSwitch sw = pair.getFirst();
                OFMessageFuture<OFBarrierReply> future = pair.getSecond();
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Barrier message not received for sw: {}", sw);
                }
            }
            long step4 = System.nanoTime();
            log.debug("MEASUREMENT: convert: {} ns, push: {} ns, barrierWait: {} ns",
                    step2 - step1, step3 - step2, step4 - step3);

        }
        long end = System.nanoTime();
        log.debug("MEASUREMENT: Install plan: {} ns", (end - start));
        FlowModCount.printCount();

        // TODO: we assume that the plan installation succeeds for now
        return true;
    }
}
