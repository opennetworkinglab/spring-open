package net.onrc.onos.core.intent.runtime;

import java.util.Collection;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.onrc.onos.api.intent.ApplicationIntent;
import net.onrc.onos.core.intent.IntentMap;
import net.onrc.onos.core.intent.IntentOperationList;

/**
 * Interface class used by PathCalcRuntimeModule class to operate intents.
 */
public interface IPathCalcRuntimeService extends IFloodlightService {
    /**
     * Adds Application Intents.
     *
     * @param appId the Application ID to use.
     * @param appIntents the Application Intents to add.
     * @return true on success, otherwise false.
     */
    public boolean addApplicationIntents(
                final String appId,
                Collection<ApplicationIntent> appIntents);

    /**
     * Removes Application Intents.
     *
     * @param appId the Application ID to use.
     * @param intentIds the Application Intent IDs to remove.
     * @return true on success, otherwise false.
     */
    public boolean removeApplicationIntents(final String appId,
                                            Collection<String> intentIds);

    /**
     * Removes all Application Intents.
     *
     * @param appId the Application ID to use.
     * @return true on success, otherwise false.
     */
    public boolean removeAllApplicationIntents(final String appId);

    /**
     * Executes Application-level Intent operations.
     * <p>
     * IntentOperationList accepts ADD and REMOVE operations at the same time
     * in order to update intents in one shot. It converts application-level
     * intent operations into path-level intent operations, and send them to
     * PlanCalcModule.
     *
     * @param list a list of intent operations
     * @return the converted path-level intent operations
     */
    public IntentOperationList executeIntentOperations(IntentOperationList list);

    /**
     * Retrieves application-level intents.
     * <p>
     * It returns IntentMap object. This object has listener to listen the
     * additions, the removals and the status changes of intents.
     *
     * @return application-level intents.
     */
    public IntentMap getHighLevelIntents();

    /**
     * Retrieves path-level intents.
     * <p>
     * It returns IntentMap object. This object has listener to listen the
     * additions, the removals and the status changes of intents.
     *
     * @return path-level intents.
     */
    public IntentMap getPathIntents();

    /**
     * Purges invalid intents.
     * <p>
     * It removes all uninstalled or failed application/path level intents.
     */
    public void purgeIntents();
}
