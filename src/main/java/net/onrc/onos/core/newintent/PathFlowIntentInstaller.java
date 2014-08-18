package net.onrc.onos.core.newintent;

import net.onrc.onos.api.flowmanager.FlowManagerService;

/**
 * An intent installer for {@link PathFlowIntent}.
 */
public class PathFlowIntentInstaller
        extends AbstractIntentInstaller<PathFlowIntent> {
    /**
     * Constructs an intent installer for {@link PathFlowIntent} with the
     * specified Flow Manager service, which is used in this installer.
     *
     * @param flowManager Flow Manager service, which is used
     *                    to install/remove an intent
     */
    public PathFlowIntentInstaller(FlowManagerService flowManager) {
        super(flowManager);
    }

    @Override
    public void install(PathFlowIntent intent) {
        installFlow(intent, intent.getFlow());
    }

    @Override
    public void remove(PathFlowIntent intent) {
        removeFlow(intent, intent.getFlow());
    }
}
