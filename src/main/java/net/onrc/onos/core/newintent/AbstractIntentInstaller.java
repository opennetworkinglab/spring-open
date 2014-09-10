package net.onrc.onos.core.newintent;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.onrc.onos.api.flowmanager.FlowState.FAILED;
import static net.onrc.onos.api.flowmanager.FlowState.INSTALLED;
import static net.onrc.onos.api.flowmanager.FlowState.WITHDRAWN;

import java.util.concurrent.CountDownLatch;

import net.onrc.onos.api.flowmanager.Flow;
import net.onrc.onos.api.flowmanager.FlowBatchHandle;
import net.onrc.onos.api.flowmanager.FlowBatchStateChangedEvent;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowManagerListener;
import net.onrc.onos.api.flowmanager.FlowManagerService;
import net.onrc.onos.api.flowmanager.FlowState;
import net.onrc.onos.api.flowmanager.FlowStateChange;
import net.onrc.onos.api.flowmanager.FlowStatesChangedEvent;
import net.onrc.onos.api.newintent.InstallableIntent;
import net.onrc.onos.api.newintent.Intent;
import net.onrc.onos.api.newintent.IntentInstaller;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

// TODO: consider naming because to call Flow manager's API will be removed
// in long-term refactoring
/**
 * Base class for implementing an intent installer, which use Flow Manager's API.
 *
 * @param <T> the type of intent
 */
public abstract class AbstractIntentInstaller<T extends InstallableIntent>
        implements IntentInstaller<T> {
    protected final FlowManagerService flowManager;

    /**
     * Constructs a base class with the specified Flow Manager service.
     *
     * @param flowManager Flow manager service, which is used to install/remove
     *                    an intent
     */
    protected AbstractIntentInstaller(FlowManagerService flowManager) {
        this.flowManager = flowManager;
    }

    protected void installFlow(Intent intent, Flow flow) {
        InstallationListener listener = new InstallationListener(flow.getId());
        flowManager.addListener(listener);

        FlowBatchHandle handle = flowManager.addFlow(flow);
        if (handle == null) {
            throw new IntentInstallationException("intent installation failed: " + intent);
        }

        // TODO (BOC) this blocks a Hazelcast thread, commenting out for now
        // try {
        // listener.await();
        // if (listener.getFinalState() == FAILED) {
        // throw new IntentInstallationException("intent installation failed: "
        // + intent);
        // }
        // } catch (InterruptedException e) {
        // throw new IntentInstallationException("intent installation failed: "
        // + intent, e);
        // } finally {
        // flowManager.removeListener(listener);
        // }
    }

    protected void removeFlow(Intent intent, Flow flow) {
        RemovalListener listener = new RemovalListener(flow.getId());
        flowManager.addListener(listener);

        FlowBatchHandle handle = flowManager.removeFlow(flow.getId());
        if (handle == null) {
            throw new IntentRemovalException("intent removal failed: " + intent);
        }


        try {
            listener.await();
            if (listener.getFinalState() == FAILED) {
                throw new IntentInstallationException("intent removal failed: " + intent);
            }
        } catch (InterruptedException e) {
            throw new IntentInstallationException("intent removal failed: " + intent, e);
        } finally {
            flowManager.removeListener(listener);
        }
    }

    protected abstract static class SyncListener implements FlowManagerListener {
        protected final FlowId target;
        protected final CountDownLatch latch = new CountDownLatch(1);
        protected FlowState finalState;

        protected SyncListener(FlowId target) {
            this.target = checkNotNull(target);
        }

        protected Optional<FlowStateChange> findTargetFlow(FlowStatesChangedEvent event) {
            return Iterables.tryFind(event.getStateChanges(), new Predicate<FlowStateChange>() {
                @Override
                public boolean apply(FlowStateChange stateChange) {
                    return stateChange.getFlowId().equals(target);
                }
            });
        }

        public FlowState getFinalState() {
            return finalState;
        }

        public void await() throws InterruptedException {
            latch.await();
        }
    }

    protected static class InstallationListener extends SyncListener {
        public InstallationListener(FlowId target) {
            super(target);
        }

        @Override
        public void flowStatesChanged(FlowStatesChangedEvent event) {
            Optional<FlowStateChange> optional = findTargetFlow(event);

            if (!optional.isPresent()) {
                return;
            }

            FlowStateChange stateChange = optional.get();
            switch (stateChange.getCurrentState()) {
                case INSTALLED:
                    latch.countDown();
                    finalState = INSTALLED;
                    break;
                case FAILED:
                    latch.countDown();
                    finalState = FAILED;
                    break;
                default:
                    break;
            }
        }

        @Override
        public void flowBatchStateChanged(FlowBatchStateChangedEvent event) {
            // nop
        }
    }

    protected static class RemovalListener extends SyncListener {
        public RemovalListener(FlowId target) {
            super(target);
        }

        @Override
        public void flowStatesChanged(FlowStatesChangedEvent event) {
            Optional<FlowStateChange> optional = findTargetFlow(event);

            if (!optional.isPresent()) {
                return;
            }

            FlowStateChange stateChange = optional.get();
            switch (stateChange.getCurrentState()) {
                case WITHDRAWN:
                    latch.countDown();
                    finalState = WITHDRAWN;
                    break;
                case FAILED:
                    latch.countDown();
                    finalState = FAILED;
                    break;
                default:
                    break;
            }
        }

        @Override
        public void flowBatchStateChanged(FlowBatchStateChangedEvent event) {
            // nop
        }
    }
}
