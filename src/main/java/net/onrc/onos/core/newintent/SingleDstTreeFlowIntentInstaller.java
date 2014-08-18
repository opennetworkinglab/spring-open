package net.onrc.onos.core.newintent;

import net.onrc.onos.api.flowmanager.FlowManagerService;

/**
 * An intent installer for {@link SingleDstTreeFlowIntent}.
 */
public class SingleDstTreeFlowIntentInstaller
        extends AbstractIntentInstaller<SingleDstTreeFlowIntent> {

    /**
     * Constructs an intent installer for {@link SingleDstTreeFlowIntent} with
     * the specified Flow Manager service, which is used in this installer.
     *
     * @param flowManager Flow Manager service, which is used
     *                    to install/remove an intent
     */
    public SingleDstTreeFlowIntentInstaller(FlowManagerService flowManager) {
        super(flowManager);
    }

    @Override
    public void install(SingleDstTreeFlowIntent intent) {
        installFlow(intent, intent.getTree());
    }

    @Override
    public void remove(SingleDstTreeFlowIntent intent) {
        removeFlow(intent, intent.getTree());
    }
}
