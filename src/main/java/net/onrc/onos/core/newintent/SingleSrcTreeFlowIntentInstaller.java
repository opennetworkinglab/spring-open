package net.onrc.onos.core.newintent;

import net.onrc.onos.api.flowmanager.FlowManagerService;

/**
 * An intent installer for {@link SingleSrcTreeFlowIntent}.
 */
public class SingleSrcTreeFlowIntentInstaller
        extends AbstractIntentInstaller<SingleSrcTreeFlowIntent> {

    /**
     * Constructs an intent installer for {@link SingleSrcTreeFlowIntent}
     * with the specified Flow Manager service, which is used in this class.
     *
     * @param flowManager Flow Manager service, which is used
     *                    to install/remove an intent
     */
    public SingleSrcTreeFlowIntentInstaller(FlowManagerService flowManager) {
        super(flowManager);
    }

    @Override
    public void install(SingleSrcTreeFlowIntent intent) {
        installFlow(intent, intent.getTree());
    }

    @Override
    public void remove(SingleSrcTreeFlowIntent intent) {
        removeFlow(intent, intent.getTree());
    }
}
