package net.onrc.onos.core.intent.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.api.intent.ApplicationIntent;
import net.onrc.onos.core.datagrid.IDatagridService;
import net.onrc.onos.core.datagrid.IEventChannel;
import net.onrc.onos.core.datagrid.IEventChannelListener;
import net.onrc.onos.core.intent.ConstrainedShortestPathIntent;
import net.onrc.onos.core.intent.Intent;
import net.onrc.onos.core.intent.Intent.IntentState;
import net.onrc.onos.core.intent.IntentMap;
import net.onrc.onos.core.intent.IntentMap.ChangedEvent;
import net.onrc.onos.core.intent.IntentMap.ChangedListener;
import net.onrc.onos.core.intent.IntentOperation;
import net.onrc.onos.core.intent.IntentOperation.Operator;
import net.onrc.onos.core.intent.IntentOperationList;
import net.onrc.onos.core.intent.PathIntent;
import net.onrc.onos.core.intent.PathIntentMap;
import net.onrc.onos.core.intent.ShortestPathIntent;
import net.onrc.onos.core.intent.runtime.web.IntentWebRoutable;
import net.onrc.onos.core.metrics.OnosMetrics;
import net.onrc.onos.core.metrics.OnosMetrics.MetricsComponent;
import net.onrc.onos.core.metrics.OnosMetrics.MetricsFeature;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.topology.ITopologyListener;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.LinkEvent;
import net.onrc.onos.core.topology.PortEvent;
import net.onrc.onos.core.topology.SwitchEvent;
import net.onrc.onos.core.topology.TopologyEvents;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.LinkTuple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;

/**
 * The PathCalcRuntimeModule contains the PathCalcRuntime and PersistIntent.
 * <p>
 * It is responsible for converting operations for application level intents
 * into operations for path level intents and send the converted operations
 * to PlanCalcRuntimeModule in order to calculate flow entries and install them.
 */
public class PathCalcRuntimeModule implements IFloodlightModule,
                                    IPathCalcRuntimeService,
                                    ITopologyListener,
                                    IEventChannelListener<Long, IntentStateList> {

    /**
     * Logging object for performance measurement.
     * TODO: merge this into measurement framework
     */
    static class PerfLog {
        private String step;
        private long time;

        public PerfLog(String step) {
            this.step = step;
            this.time = System.nanoTime();
        }

        public void logThis() {
            log.debug("Time:{}, Step:{}", time, step);
        }
    }

    /**
     * Formatted logger for performance measurement.
     * TODO: merge this into measurement framework
     */
    static class PerfLogger {
        private LinkedList<PerfLog> logData = new LinkedList<>();

        public PerfLogger(String logPhase) {
            log("start_" + logPhase);
        }

        public void log(String step) {
            logData.add(new PerfLog(step));
        }

        public void flushLog() {
            log("finish");
            for (PerfLog perfLog : logData) {
                perfLog.logThis();
            }
            logData.clear();
        }
    }

    /**
     * A class to track the status of high-level intents.
     * Currently, it is used for monitoring and measurement purposes.
     */
    private class HighLevelIntentsTracker implements ChangedListener {
        @Override
        public void intentsChange(LinkedList<ChangedEvent> events) {
            //
            // Process the events one-by-one and collect measurements.
            //
            for (ChangedEvent event : events) {
                log.debug("HighLevelIntentsTracker: Intent ID {}, eventType {}, intentState {}",
                          event.intent.getId(), event.eventType,
                          event.intent.getState());

                //
                // Update the metrics
                //
                switch (event.eventType) {
                case ADDED:
                    break;
                case REMOVED:
                    break;
                case STATE_CHANGED:
                    IntentState state = event.intent.getState();
                    switch (state) {
                        case INST_REQ:
                            break;
                        case INST_ACK:
                            intentAddProcessingRate.mark(1);
                            intentAddEndTimestampEpochMs = System.currentTimeMillis();
                            break;
                        case INST_NACK:
                            break;
                        case DEL_REQ:
                            break;
                        case DEL_ACK:
                            intentRemoveProcessingRate.mark(1);
                            intentRemoveEndTimestampEpochMs = System.currentTimeMillis();
                            break;
                        case DEL_PENDING:
                            break;
                        case REROUTE_REQ:
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
                }
            }
        }
    }

    /**
     * A class to track the deletion of intents and purge them as appropriate.
     */
    private class DeleteIntentsTracker implements ChangedListener {
        @Override
        public void intentsChange(LinkedList<ChangedEvent> events) {
            List<String> removeIntentIds = new LinkedList<String>();
            List<String> removePathIds = new LinkedList<String>();

            //
            // Process the events one-by-one and collect the Intent IDs of
            // those intents that should be purged.
            //
            for (ChangedEvent event : events) {
                log.debug("DeleteIntentsTracker: Intent ID {}, eventType {}",
                          event.intent.getId(), event.eventType);
                PathIntent pathIntent = (PathIntent) pathIntents.getIntent(event.intent.getId());
                if (pathIntent == null) {
                    continue;
                }

                //
                // Test whether the new Intent state allows the Intent
                // to be purged.
                //
                boolean shouldPurge = false;
                switch (event.eventType) {
                case ADDED:
                    break;
                case REMOVED:
                    break;
                case STATE_CHANGED:
                    IntentState state = pathIntent.getState();
                    switch (state) {
                        case INST_REQ:
                            break;
                        case INST_ACK:
                            break;
                        case INST_NACK:
                            shouldPurge = true;
                            break;
                        case DEL_REQ:
                            break;
                        case DEL_ACK:
                            shouldPurge = true;
                            break;
                        case DEL_PENDING:
                            break;
                        case REROUTE_REQ:
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
                }

                if (shouldPurge) {
                    removePathIds.add(pathIntent.getId());
                    Intent parentIntent = pathIntent.getParentIntent();
                    if (parentIntent != null) {
                        //
                        // Remove the High-level Intent only if it was
                        // explicitly deleted by the user via the API.
                        //
                        String intentId = parentIntent.getId();
                        if (removedApplicationIntentIds.contains(intentId)) {
                            removeIntentIds.add(intentId);
                            removedApplicationIntentIds.remove(intentId);
                        }
                    }
                }
            }

            // Purge the intents
            if (!removeIntentIds.isEmpty()) {
                highLevelIntents.purge(removeIntentIds);
            }
            if (!removePathIds.isEmpty()) {
                pathIntents.purge(removePathIds);
            }
        }
    }

    private PathCalcRuntime runtime;
    private IDatagridService datagridService;
    private ITopologyService topologyService;
    private IntentMap highLevelIntents;
    private PathIntentMap pathIntents;
    private IControllerRegistryService controllerRegistry;
    private PersistIntent persistIntent;
    private IRestApiService restApi;

    private IEventChannel<Long, IntentOperationList> opEventChannel;
    private final ReentrantLock lock = new ReentrantLock(true);
    private static final String INTENT_OP_EVENT_CHANNEL_NAME = "onos.pathintent";
    private static final String INTENT_STATE_EVENT_CHANNEL_NAME = "onos.pathintent_state";
    private static final Logger log = LoggerFactory.getLogger(PathCalcRuntimeModule.class);

    private HashSet<LinkTuple> unmatchedLinkEvents = new HashSet<>();
    private ConcurrentMap<String, Set<Long>> intentInstalledMap = new ConcurrentHashMap<String, Set<Long>>();
    private ConcurrentMap<String, Intent> staleIntents = new ConcurrentHashMap<String, Intent>();
    private DeleteIntentsTracker deleteIntentsTracker = new DeleteIntentsTracker();
    private Set<String> removedApplicationIntentIds = new HashSet<String>();
    private HighLevelIntentsTracker highLevelIntentsTracker = new HighLevelIntentsTracker();

    //
    // Metrics
    //
    private static final MetricsComponent METRICS_COMPONENT =
        OnosMetrics.registerComponent("Intents");
    private static final MetricsFeature METRICS_FEATURE_ADD_OPERATION =
        METRICS_COMPONENT.registerFeature("AddOperation");
    private static final MetricsFeature METRICS_FEATURE_REMOVE_OPERATION =
        METRICS_COMPONENT.registerFeature("RemoveOperation");
    //
    // Timestamp of the incoming Add Intent API operation (ms from the Epoch)
    private volatile long intentAddBeginTimestampEpochMs = 0;
    private final Gauge<Long> gaugeIntentAddBeginTimestampEpochMs =
        OnosMetrics.registerMetric(METRICS_COMPONENT,
                                   METRICS_FEATURE_ADD_OPERATION,
                                   "BeginOperationTimestamp.EpochMs",
                                   new Gauge<Long>() {
                                       @Override
                                       public Long getValue() {
                                           return intentAddBeginTimestampEpochMs;
                                       }
                                   });
    // Timestamp of the Add Intent operation completion (ms from the Epoch)
    private volatile long intentAddEndTimestampEpochMs = 0;
    private final Gauge<Long> gaugeIntentAddEndTimestampEpochMs =
        OnosMetrics.registerMetric(METRICS_COMPONENT,
                                   METRICS_FEATURE_ADD_OPERATION,
                                   "EndOperationTimestamp.EpochMs",
                                   new Gauge<Long>() {
                                       @Override
                                       public Long getValue() {
                                           return intentAddEndTimestampEpochMs;
                                       }
                                   });
    // Timestamp of the incoming Remove Intent API operation (ms from the Epoch)
    private volatile long intentRemoveBeginTimestampEpochMs = 0;
    private final Gauge<Long> gaugeIntentRemoveBeginTimestampEpochMs =
        OnosMetrics.registerMetric(METRICS_COMPONENT,
                                   METRICS_FEATURE_REMOVE_OPERATION,
                                   "BeginOperationTimestamp.EpochMs",
                                   new Gauge<Long>() {
                                       @Override
                                       public Long getValue() {
                                           return intentRemoveBeginTimestampEpochMs;
                                       }
                                   });
    // Timestamp of the Remove Intent operation completion (ms from the Epoch)
    private volatile long intentRemoveEndTimestampEpochMs = 0;
    private final Gauge<Long> gaugeIntentRemoveEndTimestampEpochMs =
        OnosMetrics.registerMetric(METRICS_COMPONENT,
                                   METRICS_FEATURE_REMOVE_OPERATION,
                                   "EndOperationTimestamp.EpochMs",
                                   new Gauge<Long>() {
                                       @Override
                                       public Long getValue() {
                                           return intentRemoveEndTimestampEpochMs;
                                       }
                                   });
    //
    // Rate of the incoming Add Intent API operations
    private final Meter intentAddIncomingRate =
        OnosMetrics.createMeter(METRICS_COMPONENT,
                                METRICS_FEATURE_ADD_OPERATION,
                                "IncomingRate");
    // Rate of processing the Add Intent operations
    private final Meter intentAddProcessingRate =
        OnosMetrics.createMeter(METRICS_COMPONENT,
                                METRICS_FEATURE_ADD_OPERATION,
                                "ProcessingRate");
    // Rate of the incoming Remove Intent API operations
    private final Meter intentRemoveIncomingRate =
        OnosMetrics.createMeter(METRICS_COMPONENT,
                                METRICS_FEATURE_REMOVE_OPERATION,
                                "IncomingRate");
    // Rate of processing the Remove Intent operations
    private final Meter intentRemoveProcessingRate =
        OnosMetrics.createMeter(METRICS_COMPONENT,
                                METRICS_FEATURE_REMOVE_OPERATION,
                                "ProcessingRate");

    // ================================================================================
    // private methods
    // ================================================================================

    /**
     * Creates operations (IntentOperationList) for Application-level
     * intents that should be rerouted because of topology change,
     * and execute the created operations.
     *
     * @param oldPaths a list of invalid path intents (which should be rerouted)
     */
    private void reroutePaths(Collection<Intent> oldPaths) {
        if (oldPaths == null || oldPaths.isEmpty()) {
            return;
        }

        IntentOperationList reroutingOperation = new IntentOperationList();
        for (Intent intent : oldPaths) {
            PathIntent pathIntent = (PathIntent) intent;
            if (pathIntent.isPathFrozen()) {
                continue;
            }
            Intent parentIntent = pathIntent.getParentIntent();
            if (parentIntent == null) {
                continue;
            }
            if (pathIntent.getState().equals(IntentState.INST_ACK)) {
                if (!reroutingOperation.contains(parentIntent)) {
                    // reroute now
                    reroutingOperation.add(Operator.ADD, parentIntent);
                }
            } else if (pathIntent.getState().equals(IntentState.INST_REQ)) {
                // reroute after the completion of the current execution
                staleIntents.put(parentIntent.getId(), parentIntent);
                log.debug("pending reroute execution for intent ID:{}", parentIntent.getId());
            }
        }
        executeIntentOperations(reroutingOperation);
    }

    /**
     * Checks whether the entire path's flow entries are installed or not.
     *
     * @param pathIntent : The pathIntent to be checked
     * @param installedDpids : The dpids installed on one ONOS instance
     * @return The result of whether a pathIntent has been installed or not.
     */
    private boolean isFlowInstalled(PathIntent pathIntent, Set<Long> installedDpids) {
        String pathIntentId = pathIntent.getId();

        if (intentInstalledMap.containsKey(pathIntentId)) {
            if (!installedDpids.isEmpty()) {
                intentInstalledMap.get(pathIntentId).addAll(installedDpids);
            }
        } else {
            // This is the creation of an entry.
            intentInstalledMap.put(pathIntentId, installedDpids);
        }

        Set<Long> allSwitchesForPath = new HashSet<Long>();
        ShortestPathIntent spfIntent = (ShortestPathIntent) pathIntent.getParentIntent();

        for (LinkEvent linkEvent : pathIntent.getPath()) {
            long sw = linkEvent.getSrc().getDpid().value();
            allSwitchesForPath.add(sw);
        }
        allSwitchesForPath.add(spfIntent.getDstSwitchDpid());

        if (log.isDebugEnabled()) {
            log.debug("checking flow installation. ID:{}, dpids:{}, installed:{}",
                    pathIntentId,
                    allSwitchesForPath,
                    intentInstalledMap.get(pathIntentId));
        }

        if (allSwitchesForPath.equals(intentInstalledMap.get(pathIntentId))) {
            intentInstalledMap.remove(pathIntentId);
            return true;
        }

        return false;
    }

    /**
     * Enumerates switch dpids along the specified path and inside the specified domain.
     *
     * @param pathIntent the path for enumeration
     * @param domainSwitchDpids a set of the domain switch dpids
     * @return a set of switch dpids along the specified path and inside the specified domain
     */
    private Set<Long> calcInstalledDpids(PathIntent pathIntent, Set<Long> domainSwitchDpids) {
        Set<Long> allSwitchesForPath = new HashSet<Long>();
        ShortestPathIntent spfIntent = (ShortestPathIntent) pathIntent.getParentIntent();

        for (LinkEvent linkEvent : pathIntent.getPath()) {
            long sw = linkEvent.getSrc().getDpid().value();

            if (domainSwitchDpids.contains(sw)) {
                allSwitchesForPath.add(sw);
            }
        }

        if (domainSwitchDpids.contains(spfIntent.getDstSwitchDpid())) {
            allSwitchesForPath.add(spfIntent.getDstSwitchDpid());
        }

        if (log.isTraceEnabled()) {
            log.trace("All switches for a path {}, domain switch dpids {}", allSwitchesForPath, domainSwitchDpids);
        }

        return allSwitchesForPath;
    }

    // ================================================================================
    // IFloodlightModule implementations
    // ================================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<>(1);
        l.add(IPathCalcRuntimeService.class);
        return l;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<>();
        m.put(IPathCalcRuntimeService.class, this);
        return m;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<>(2);
        l.add(IDatagridService.class);
        l.add(IRestApiService.class);
        l.add(ITopologyService.class);
        return l;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        datagridService = context.getServiceImpl(IDatagridService.class);
        topologyService = context.getServiceImpl(ITopologyService.class);
        controllerRegistry = context.getServiceImpl(IControllerRegistryService.class);
        restApi = context.getServiceImpl(IRestApiService.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startUp(FloodlightModuleContext context) {
        highLevelIntents = new IntentMap();
        highLevelIntents.addChangeListener(highLevelIntentsTracker);
        runtime = new PathCalcRuntime(topologyService.getTopology());
        pathIntents = new PathIntentMap();
        pathIntents.addChangeListener(deleteIntentsTracker);
        opEventChannel = datagridService.createChannel(
                INTENT_OP_EVENT_CHANNEL_NAME, Long.class, IntentOperationList.class);
        datagridService.addListener(INTENT_STATE_EVENT_CHANNEL_NAME, this, Long.class, IntentStateList.class);
        topologyService.addListener(this, false);
        persistIntent = new PersistIntent(controllerRegistry);
        restApi.addRestletRoutable(new IntentWebRoutable());
    }

    // ======================================================================
    // IPathCalcRuntimeService implementations
    // ======================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addApplicationIntents(
                        final String appId,
                        Collection<ApplicationIntent> appIntents) {
        //
        // Update the metrics
        //
        if (!appIntents.isEmpty()) {
            this.intentAddBeginTimestampEpochMs = System.currentTimeMillis();
            this.intentAddIncomingRate.mark(appIntents.size());
        }

        //
        // Process all intents one-by-one
        //
        // TODO: The Intent Type should be enum instead of a string,
        // and we should use a switch statement below to process the
        // different type of intents.
        //
        IntentOperationList intentOperations = new IntentOperationList();
        for (ApplicationIntent appIntent : appIntents) {
            String appIntentId = appId + ":" + appIntent.getIntentId();

            IntentOperation.Operator operator = IntentOperation.Operator.ADD;
            Dpid srcSwitchDpid = new Dpid(appIntent.getSrcSwitchDpid());
            Dpid dstSwitchDpid = new Dpid(appIntent.getDstSwitchDpid());

            if (appIntent.getIntentType().equals("SHORTEST_PATH")) {
                //
                // Process Shortest-Path Intent
                //
                ShortestPathIntent spi =
                    new ShortestPathIntent(appIntentId,
                                           srcSwitchDpid.value(),
                                           appIntent.getSrcSwitchPort(),
                                           MACAddress.valueOf(appIntent.getMatchSrcMac()).toLong(),
                                           dstSwitchDpid.value(),
                                           appIntent.getDstSwitchPort(),
                                           MACAddress.valueOf(appIntent.getMatchDstMac()).toLong());
                spi.setPathFrozen(appIntent.isStaticPath());
                intentOperations.add(operator, spi);
            } else if (appIntent.getIntentType().equals("CONSTRAINED_SHORTEST_PATH")) {
                //
                // Process Constrained Shortest-Path Intent
                //
                ConstrainedShortestPathIntent cspi =
                    new ConstrainedShortestPathIntent(appIntentId,
                                                      srcSwitchDpid.value(),
                                                      appIntent.getSrcSwitchPort(),
                                                      MACAddress.valueOf(appIntent.getMatchSrcMac()).toLong(),
                                                      dstSwitchDpid.value(),
                                                      appIntent.getDstSwitchPort(),
                                                      MACAddress.valueOf(appIntent.getMatchDstMac()).toLong(),
                                                      appIntent.getBandwidth());
                cspi.setPathFrozen(appIntent.isStaticPath());
                intentOperations.add(operator, cspi);
            } else {
                log.error("Unknown Application Intent Type: {}",
                          appIntent.getIntentType());
                return false;
            }
            removedApplicationIntentIds.remove(appIntentId);
        }
        // Apply the Intent Operations
        executeIntentOperations(intentOperations);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeApplicationIntents(final String appId,
                                            Collection<String> intentIds) {
        //
        // Prepare the timestamp for metrics
        //
        long timestampEpochMs = System.currentTimeMillis();

        IntentMap intentMap = getHighLevelIntents();
        List<String> removeIntentIds = new LinkedList<String>();

        //
        // Process all intents one-by-one
        //
        IntentOperationList operations = new IntentOperationList();
        for (String intentId : intentIds) {
            String appIntentId = appId + ":" + intentId;
            Intent intent = intentMap.getIntent(appIntentId);
            if (intent != null) {
                if (intent.getState() == IntentState.INST_NACK) {
                    // TODO: A hack to remove intents stuck in INST_NACK state
                    removeIntentIds.add(intent.getId());
                    continue;
                }
                operations.add(IntentOperation.Operator.REMOVE, intent);
                removedApplicationIntentIds.add(appIntentId);
            }
        }

        //
        // Update the metrics
        //
        if (!operations.isEmpty()) {
            this.intentRemoveBeginTimestampEpochMs = timestampEpochMs;
            this.intentRemoveIncomingRate.mark(operations.size());
        }

        //
        // Purge intents
        //
        if (!removeIntentIds.isEmpty()) {

            lock.lock(); // TODO optimize locking using smaller steps
            try {
                highLevelIntents.purge(removeIntentIds);
            } finally {
                lock.unlock();
            }
        }

        executeIntentOperations(operations);

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeAllApplicationIntents(final String appId) {
        //
        // Prepare the timestamp for metrics
        //
        long timestampEpochMs = System.currentTimeMillis();

        Collection<Intent> allHighLevelIntents =
            getHighLevelIntents().getAllIntents();

        //
        // Remove all intents
        //
        List<String> removeIntentIds = new LinkedList<String>();
        IntentOperationList operations = new IntentOperationList();
        for (Intent intent : allHighLevelIntents) {
            if (intent.getState() == IntentState.INST_NACK) {
                // TODO: A hack to remove intents stuck in INST_NACK state
                removeIntentIds.add(intent.getId());
                continue;
            }
            operations.add(IntentOperation.Operator.REMOVE, intent);
            removedApplicationIntentIds.add(intent.getId());
        }

        //
        // Update the metrics
        //
        if (!operations.isEmpty()) {
            this.intentRemoveBeginTimestampEpochMs = timestampEpochMs;
            this.intentRemoveIncomingRate.mark(operations.size());
        }

        //
        // Purge intents
        //
        if (!removeIntentIds.isEmpty()) {
            lock.lock(); // TODO optimize locking using smaller steps
            try {
                highLevelIntents.purge(removeIntentIds);
            } finally {
                lock.unlock();
            }
        }

        executeIntentOperations(operations);

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntentOperationList executeIntentOperations(IntentOperationList list) {

        if (list == null || list.size() == 0) {
            return null;
        }

        lock.lock(); // TODO optimize locking using smaller steps
        try {
            log.trace("lock executeIntentOperations, lock obj is already locked? {}", lock.isLocked());
            // update the map of high-level intents

            highLevelIntents.executeOperations(list);

            // change states of high-level intents
            IntentStateList states = new IntentStateList();
            for (IntentOperation op : list) {
                switch (op.operator) {
                    case ADD:
                        switch (op.intent.getState()) {
                            case CREATED:
                                states.put(op.intent.getId(), IntentState.INST_REQ);
                                break;
                            case INST_ACK:
                                states.put(op.intent.getId(), IntentState.REROUTE_REQ);
                                break;
                            default:
                                break;
                        }
                        break;
                    case REMOVE:
                        switch (op.intent.getState()) {
                            case CREATED:
                                states.put(op.intent.getId(), IntentState.DEL_REQ);
                                break;
                            default:
                                break;
                        }
                        break;
                    default:
                        break;
                }
            }
            highLevelIntents.changeStates(states);

            // calculate path-intents (low-level operations)
            IntentOperationList pathIntentOperations = runtime.calcPathIntents(list, highLevelIntents, pathIntents);

            // persist calculated low-level operations into data store
            long key = persistIntent.getKey();
            persistIntent.persistIfLeader(key, pathIntentOperations);

            // remove error-intents and reflect them to high-level intents
            states.clear();
            Iterator<IntentOperation> i = pathIntentOperations.iterator();
            while (i.hasNext()) {
                IntentOperation op = i.next();
                if (op.operator.equals(Operator.ERROR)) {
                    states.put(op.intent.getId(), IntentState.INST_NACK);
                    i.remove();
                }
            }
            highLevelIntents.changeStates(states);

            // update the map of path intents and publish the path operations
            pathIntents.executeOperations(pathIntentOperations);

            // send notification
            // XXX: Send notifications using the same key every time
            // and receive them by entryAdded() and entryUpdated()
            opEventChannel.addEntry(0L, pathIntentOperations);
            //opEventChannel.removeEntry(key);
            return pathIntentOperations;
        } finally {
            lock.unlock();
            log.trace("unlock executeIntentOperations");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntentMap getHighLevelIntents() {
        return highLevelIntents;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntentMap getPathIntents() {
        return pathIntents;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void purgeIntents() {
        highLevelIntents.purge();
        pathIntents.purge();
    }

    // ================================================================================
    // ITopologyListener implementations
    // ================================================================================
    /**
     * {@inheritDoc}
     */
    @Override
    public void topologyEvents(TopologyEvents topologyEvents) {
        PerfLogger p = new PerfLogger("networkGraphEvents");
        HashSet<Intent> affectedPaths = new HashSet<>();

        boolean rerouteAll = false;
        for (LinkEvent le : topologyEvents.getAddedLinkEvents()) {
            final LinkTuple rev = new LinkTuple(le.getDst(), le.getSrc());
            if (unmatchedLinkEvents.contains(rev)) {
                rerouteAll = true;
                unmatchedLinkEvents.remove(rev);
                log.debug("Found matched LinkEvent: {} {}", rev, le);
            } else {
                unmatchedLinkEvents.add(le.getLinkTuple());
                log.debug("Adding unmatched LinkEvent: {}", le);
            }
        }
        for (LinkEvent le : topologyEvents.getRemovedLinkEvents()) {
            if (unmatchedLinkEvents.contains(le.getLinkTuple())) {
                unmatchedLinkEvents.remove(le.getLinkTuple());
                log.debug("Removing LinkEvent: {}", le);
            }
        }
        if (unmatchedLinkEvents.size() > 0) {
            log.debug("Unmatched link events: {} events", unmatchedLinkEvents.size());
        }

        if (rerouteAll) {
            //
            // (topologyEvents.getAddedLinkEvents().size() > 0) ||
            // (topologyEvents.getAddedPortEvents().size() > 0) ||
            // (topologyEvents.getAddedSwitchEvents.size() > 0)
            //
            p.log("begin_getAllIntents");
            affectedPaths.addAll(getPathIntents().getAllIntents());
            p.log("end_getAllIntents");
        } else if (topologyEvents.getRemovedSwitchEvents().size() > 0 ||
                   topologyEvents.getRemovedLinkEvents().size() > 0 ||
                   topologyEvents.getRemovedPortEvents().size() > 0) {
            p.log("begin_getIntentsByLink");
            for (LinkEvent linkEvent : topologyEvents.getRemovedLinkEvents()) {
                affectedPaths.addAll(pathIntents.getIntentsByLink(linkEvent.getLinkTuple()));
            }
            p.log("end_getIntentsByLink");

            p.log("begin_getIntentsByPort");
            for (PortEvent portEvent : topologyEvents.getRemovedPortEvents()) {
                affectedPaths.addAll(pathIntents.getIntentsByPort(
                        portEvent.getDpid(),
                        portEvent.getPortNumber()));
            }
            p.log("end_getIntentsByPort");

            p.log("begin_getIntentsByDpid");
            for (SwitchEvent switchEvent : topologyEvents.getRemovedSwitchEvents()) {
                affectedPaths.addAll(pathIntents.getIntentsByDpid(switchEvent.getDpid()));
            }
            p.log("end_getIntentsByDpid");
        }
        p.log("begin_reroutePaths");
        reroutePaths(affectedPaths);
        p.log("end_reroutePaths");
        p.flushLog();
    }

    // ================================================================================
    // IEventChannelListener implementations
    // ================================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void entryAdded(IntentStateList value) {
        entryUpdated(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void entryRemoved(IntentStateList value) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("fallthrough")
    @Override
    public void entryUpdated(IntentStateList value) {
        // TODO draw state transition diagram in multiple ONOS instances and update this method

        IntentOperationList opList = new IntentOperationList();
        lock.lock(); // TODO optimize locking using smaller steps
        try {
            log.trace("lock entryUpdated, lock obj is already locked? {}", lock.isLocked());
            // reflect state changes of path-level intent into application-level intents
            IntentStateList highLevelIntentStates = new IntentStateList();
            IntentStateList pathIntentStates = new IntentStateList();
            for (Entry<String, IntentState> entry : value.entrySet()) {
                String pathIntentId = entry.getKey();
                IntentState nextPathIntentState = entry.getValue();
                PathIntent pathIntent = (PathIntent) pathIntents.getIntent(pathIntentId);
                if (pathIntent == null) {
                    continue;
                }

                Intent parentIntent = pathIntent.getParentIntent();
                if (parentIntent == null ||
                        !(parentIntent instanceof ShortestPathIntent)) {
                    continue;
                }
                String parentIntentId = parentIntent.getId();

                boolean isChildIntent = ((ShortestPathIntent) parentIntent).getPathIntentId().equals(pathIntentId);

                // Check necessity for retrying the intent execution.
                // When the PathIntent(=isChildIntent) transitioned to INST_{ACK/NACK}
                // but was marked as stale (e.g., has been requested to reroute by Topology event),
                // then immediately enqueue the re-computation of parent intent.
                if (isChildIntent && staleIntents.containsKey(parentIntentId) && (
                        nextPathIntentState.equals(IntentState.INST_ACK) ||
                        nextPathIntentState.equals(IntentState.INST_NACK))) {
                    opList.add(Operator.ADD, parentIntent);
                    staleIntents.remove(parentIntentId);
                    log.debug("retrying intent execution for intent ID:{}", parentIntentId);
                }

                switch (nextPathIntentState) {
                    case INST_ACK:
                        Set<Long> installedDpids = calcInstalledDpids(pathIntent, value.domainSwitchDpids);
                        if (!isFlowInstalled(pathIntent, installedDpids)) {
                            break;
                        }
                        // FALLTHROUGH
                    case INST_NACK:
                        // FALLTHROUGH
                    case DEL_PENDING:
                        if (isChildIntent) {
                            log.debug("put the state highLevelIntentStates ID {}, state {}",
                                    parentIntentId, nextPathIntentState);
                            highLevelIntentStates.put(parentIntentId, nextPathIntentState);
                        }
                        log.debug("put the state pathIntentStates ID {}, state {}",
                                pathIntentId, nextPathIntentState);
                        pathIntentStates.put(pathIntentId, nextPathIntentState);
                        break;
                    case DEL_ACK:
                        if (isChildIntent) {
                            if (intentInstalledMap.containsKey(pathIntentId)) {
                                 intentInstalledMap.remove(pathIntentId);
                            }
                            log.debug("put the state highLevelIntentStates ID {}, state {}",
                                    parentIntentId, nextPathIntentState);
                            highLevelIntentStates.put(parentIntentId, nextPathIntentState);
                        }
                        log.debug("put the state pathIntentStates ID {}, state {}",
                                pathIntentId, nextPathIntentState);
                        pathIntentStates.put(pathIntentId, nextPathIntentState);
                        break;
                    case CREATED:
                        break;
                    case DEL_REQ:
                        break;
                    case INST_REQ:
                        break;
                    case REROUTE_REQ:
                        break;
                    default:
                        break;
                }
            }
            highLevelIntents.changeStates(highLevelIntentStates);
            pathIntents.changeStates(pathIntentStates);
        } finally {
            lock.unlock();
            log.trace("unlock entryUpdated");
        }
        executeIntentOperations(opList);
    }
}
