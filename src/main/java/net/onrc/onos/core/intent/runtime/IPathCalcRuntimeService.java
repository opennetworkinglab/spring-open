package net.onrc.onos.core.intent.runtime;

import java.util.Collection;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.onrc.onos.api.intent.ApplicationIntent;
import net.onrc.onos.core.intent.IntentMap;
import net.onrc.onos.core.intent.IntentOperationList;

/**
 * @author Toshio Koide (t-koide@onlab.us)
 */
public interface IPathCalcRuntimeService extends IFloodlightService {
    /**
     * Add Application Intents.
     *
     * @param appId the Application ID to use.
     * @param appIntents the Application Intents to add.
     * @return true on success, otherwise false.
     */
    public boolean addApplicationIntents(
                final String appId,
                Collection<ApplicationIntent> appIntents);

    /**
     * Remove Application Intents.
     *
     * @param appId the Application ID to use.
     * @param intentIds the Application Intent IDs to remove.
     * @return true on success, otherwise false.
     */
    public boolean removeApplicationIntents(final String appId,
                                            Collection<String> intentIds);

    /**
     * Remove all Application Intents.
     *
     * @param appId the Application ID to use.
     * @return true on success, otherwise false.
     */
    public boolean removeAllApplicationIntents(final String appId);

    public IntentOperationList executeIntentOperations(IntentOperationList list);

    public IntentMap getHighLevelIntents();

    public IntentMap getPathIntents();

    public void purgeIntents();
}
