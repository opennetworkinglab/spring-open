package net.onrc.onos.core.matchaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.internal.OFMessageFuture;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.onrc.onos.api.flowmanager.ConflictDetectionPolicy;
import net.onrc.onos.core.datagrid.IDatagridService;
import net.onrc.onos.core.datagrid.IEventChannel;
import net.onrc.onos.core.datagrid.IEventChannelListener;
import net.onrc.onos.core.flowprogrammer.IFlowPusherService;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.IdGenerator;
import net.onrc.onos.core.util.SwitchPort;

import org.apache.commons.lang3.tuple.Pair;
import org.projectfloodlight.openflow.protocol.OFBarrierReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Manages Match-Action entries.
 * <p>
 * TODO: Make all methods thread-safe
 */
public class MatchActionComponent implements MatchActionService, IFloodlightService {

    private static final Logger log = LoggerFactory.getLogger(MatchActionService.class);
    IFlowPusherService pusher;
    IFloodlightProviderService provider;

    private ConcurrentMap<MatchActionId, MatchAction> matchActionMap = new ConcurrentHashMap<>();
    private ConcurrentMap<MatchActionOperationsId, MatchActionOperations> matchSetMap =
            new ConcurrentHashMap<>();
    //  TODO - want something better here for the resolved Queue
    private BlockingQueue<MatchActionOperationsId> resolvedQueue = new ArrayBlockingQueue<>(100);
    private BlockingQueue<MatchActionOperations> installationWorkQueue = new ArrayBlockingQueue<>(100);

    private IEventChannel<String, MatchActionOperations> installSetChannel;
    private IEventChannel<String, SwitchResultList> installSetReplyChannel;

    // TODO Single instance for now, should be a work queue of some sort eventually
    private Thread coordinator;
    private Thread installer;
    private final IDatagridService datagrid;

    public MatchActionComponent(final IDatagridService newDatagrid,
                                final IFlowPusherService newPusher,
                                final IFloodlightProviderService newProvider) {
        datagrid = newDatagrid;
        pusher = newPusher;
        provider = newProvider;
    }

    public void start() {
        installSetChannel = datagrid.createChannel("onos.matchaction.installSetChannel",
                String.class,
                MatchActionOperations.class);

        installSetReplyChannel = datagrid.createChannel("onos.matchaction.installSetReplyChannel",
                String.class,
                SwitchResultList.class);

        coordinator = new Coordinator();
        coordinator.start();

        installer = new InstallerWorker();
        installer.start();
    }

    public MatchActionOperationsId installMatchActionOperations(MatchActionOperations matchSet) {
        if (checkResolved(matchSet)) {
            matchSet.setState(MatchActionOperationsState.RESOLVED);
        } else {
            matchSet.setState(MatchActionOperationsState.INIT);
        }
        matchSetMap.put(matchSet.getOperationsId(), matchSet);
        if (matchSet.getState() == MatchActionOperationsState.RESOLVED) {
            resolvedQueue.add(matchSet.getOperationsId());
        }
        return matchSet.getOperationsId();
    }

    public MatchActionOperationsState getMatchActionOperationsState(MatchActionOperationsId matchSetId) {
        MatchActionOperations set = matchSetMap.get(matchSetId);
        return (set == null) ? null : set.getState();
    }

    protected boolean checkResolved(MatchActionOperations matchSet) {
        boolean resolved = true;
        for (MatchActionOperationsId setId : matchSet.getDependencies()) {
            MatchActionOperations set = matchSetMap.get(setId);
            if (set == null || set.getState() != MatchActionOperationsState.RESOLVED) {
                resolved = false;
                break;
            }
        }
        return resolved;
    }

    class Coordinator extends Thread
            implements IEventChannelListener<String, SwitchResultList> {

        private Map<MatchActionOperationsId, Map<Dpid, SwitchResult>> pendingMatchActionOperationss = new HashMap<>();

        protected Coordinator() {
            installSetReplyChannel.addListener(this);
        }

        @Override
        public void run() {
            while (true) {
                // 1. Remove MatchActionOperations(s) from the Global Resolved Queue
                try {
                    MatchActionOperationsId setId = resolvedQueue.take();
                    processSet(setId);
                } catch (InterruptedException e) {
                    log.warn("Error taking from resolved queue: {}", e.getMessage());
                }
            }
        }

        private void processSet(MatchActionOperationsId setId) {
            MatchActionOperations matchSet = matchSetMap.get(setId);
            matchSet.setState(MatchActionOperationsState.PENDING);
            matchSetMap.put(setId, matchSet);

            // TODO apply updates to in-memory flow table and resolve conflicts
            // TODO generate apply and undo sets, using MatchActionOperations for now...

            // build pending switches set for coordinator tracking
            Map<Dpid, SwitchResult> switches = new HashMap<>();
            for (MatchActionOperationEntry matchActionOp : matchSet.getOperations()) {
                MatchAction matchAction = matchActionOp.getTarget();
                SwitchPort sw = matchAction.getSwitchPort();
                switches.put(sw.getDpid(), new SwitchResult(setId, sw.getDpid()));
                switch(matchActionOp.getOperator()) {
                case ADD:
                    matchActionMap.put(matchAction.getId(), matchAction);
                    break;
                case REMOVE:
                    //TODO
                default:
                    throw new UnsupportedOperationException(
                            "Unsupported MatchAction operation" +
                                    matchActionOp.getOperator().toString());
                }
            }
            pendingMatchActionOperationss.put(setId, switches);

            // distribute apply/undo sets to cluster
            //installSetChannel.addTransientEntry(setId.toString(), matchSet);
        }

        @Override
        public void entryAdded(SwitchResultList value) {
            updateSwitchResults(value);
        }

        @Override
        public void entryRemoved(SwitchResultList value) {
            // noop
        }

        @Override
        public void entryUpdated(SwitchResultList value) {
            updateSwitchResults(value);
        }

        private void updateSwitchResults(SwitchResultList results) {
            if (results == null || results.size() == 0) {
                return;
            }
            MatchActionOperationsId matchSetId = results.get(0).getMatchActionOperationsId();

            // apply updates from results list
            Map<Dpid, SwitchResult> resultMap = pendingMatchActionOperationss.get(matchSetId);
            for (SwitchResult result : results) {
                SwitchResult resultToUpdate = resultMap.get(result.getSwitch());
                if (resultToUpdate != null) {
                    resultToUpdate.setStatus(result.getStatus());
                }
                // else {
                // TODO error!
                // }
            }

            // check to see the overall outcome of the install operation
            SwitchResult.Status setResult = SwitchResult.Status.SUCCESS;
            for (SwitchResult result : resultMap.values()) {
                if (result.getStatus().equals(SwitchResult.Status.FAILURE)) {
                    setResult = SwitchResult.Status.FAILURE;
                    // if any switch fails, we fail the installation
                    break;
                } else if (!setResult.equals(SwitchResult.Status.FAILURE)
                        && result.getStatus().equals(SwitchResult.Status.UNKNOWN)) {
                    setResult = SwitchResult.Status.UNKNOWN;
                }
            }
            switch (setResult) {
                case SUCCESS:
                    // mark MatchActionOperations as INSTALLED
                    MatchActionOperations matchSet = matchSetMap.get(matchSetId);
                    matchSet.setState(MatchActionOperationsState.INSTALLED);
                    matchSetMap.replace(matchSetId, matchSet);
                    pendingMatchActionOperationss.remove(matchSetId);

                    // TODO update dependent sets as needed
                    break;
                case FAILURE:
                    // mark MatchActionOperations as FAILED
                    matchSet = matchSetMap.get(matchSetId);
                    matchSet.setState(MatchActionOperationsState.FAILED);
                    matchSetMap.replace(matchSetId, matchSet);

                    // TODO instruct installers to install Undo set
                    break;
                case UNKNOWN:
                default:
                    // noop, still waiting for results
                    // TODO: check to see if installers are dead after timeout
            }
        }
    }


    class InstallerWorker extends Thread {

        // Note: we should consider using an alternative representation for
        // apply sets
        protected void install(MatchActionOperations matchSet) {
            Set<Long> masterDpids = provider.getAllMasterSwitchDpids();

            Set<MatchActionOperationEntry> installSet = new HashSet<>();
            Set<Dpid> modifiedSwitches = new HashSet<>();

            for (MatchActionOperationEntry matchActionOp : matchSet.getOperations()) {
                MatchAction matchAction = matchActionOp.getTarget();
                Dpid dpid = matchAction.getSwitchPort().getDpid();
                if (masterDpids.contains(dpid.value())) {
                    // only install if we are the master
                    // TODO this optimization will introduce some nice race
                    // conditions on failure requiring mastership change
                    installSet.add(matchActionOp);
                    modifiedSwitches.add(dpid);
                }
            }

            // push flow entries to switches
            pusher.pushMatchActions(installSet);

            // insert a barrier after each phase on each modifiedSwitch
            // wait for confirmation messages before proceeding
            List<Pair<Dpid, OFMessageFuture<OFBarrierReply>>> barriers = new ArrayList<>();
            for (Dpid dpid : modifiedSwitches) {
                barriers.add(Pair.of(dpid, pusher.barrierAsync(dpid)));
            }
            List<SwitchResult> switchResults = new ArrayList<>();
            for (Pair<Dpid, OFMessageFuture<OFBarrierReply>> pair : barriers) {
                Dpid dpid = pair.getLeft();
                OFMessageFuture<OFBarrierReply> future = pair.getRight();
                SwitchResult switchResult = new SwitchResult(matchSet.getOperationsId(),
                        dpid);
                try {
                    future.get();
                    switchResult.setStatus(SwitchResult.Status.SUCCESS);
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Barrier message not received for sw: {}", dpid);
                    switchResult.setStatus(SwitchResult.Status.FAILURE);
                }
                switchResults.add(switchResult);
            }

            // send update message to coordinator
            // TODO: we might want to use another ID here, i.e. GUID, to avoid
            // overlap
            final SwitchResultList switchResultList = new SwitchResultList();
            switchResultList.addAll(switchResults);
            installSetReplyChannel.addTransientEntry(matchSet.getOperationsId().toString(),
                    switchResultList);
        }

        @Override
        public void run() {
            while (true) {
                // 1. Remove MatchActionOperations(s) from the Global Resolved Queue
                try {
                    MatchActionOperations operations = installationWorkQueue.take();
                    install(operations);
                } catch (InterruptedException e) {
                    log.warn("Error taking from installation queue: {}", e.getMessage());
                }
            }
        }
    }

    class Installer
            implements IEventChannelListener<String, MatchActionOperations> {

        protected Installer() {
            installSetChannel.addListener(this);
        }


        @Override
        public void entryAdded(MatchActionOperations value) {
            installationWorkQueue.add(value);
        }

        @Override
        public void entryRemoved(MatchActionOperations value) {
            // noop
        }

        @Override
        public void entryUpdated(MatchActionOperations value) {
            installationWorkQueue.add(value);
        }
    }

    private final HashSet<MatchAction> currentOperations = new HashSet<>();

    private boolean processMatchActionEntries(
            final List<MatchActionOperationEntry> entries) {
        int successfulOperations = 0;
        for (final MatchActionOperationEntry entry : entries) {
            if (currentOperations.add(entry.getTarget())) {
                successfulOperations++;
            }
        }
        return entries.size() == successfulOperations;
    }

    @Override
    public boolean addMatchAction(MatchAction matchAction) {
        return false;
    }

    @Override
    public Set<MatchAction> getMatchActions() {
        return Collections.unmodifiableSet(currentOperations);
    }

    @Override
    public boolean executeOperations(final MatchActionOperations operations) {
        installMatchActionOperations(operations);
        return processMatchActionEntries(operations.getOperations());
    }

    @Override
    public void setConflictDetectionPolicy(ConflictDetectionPolicy policy) {
        // TODO Auto-generated method stub

    }

    @Override
    public ConflictDetectionPolicy getConflictDetectionPolicy() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addEventListener(EventListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeEventListener(EventListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public IdGenerator<MatchActionId> getMatchActionIdGenerator() {
        return null;
    }

    @Override
    public IdGenerator<MatchActionOperationsId> getMatchActionOperationsIdGenerator() {
        return null;
    }

}
