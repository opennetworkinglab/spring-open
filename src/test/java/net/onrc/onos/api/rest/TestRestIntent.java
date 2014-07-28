package net.onrc.onos.api.rest;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.onrc.onos.core.intent.Intent;
import net.onrc.onos.core.intent.runtime.IPathCalcRuntimeService;
import net.onrc.onos.core.intent.runtime.IntentStateList;
import net.onrc.onos.core.intent.runtime.IntentTestMocks;
import net.onrc.onos.core.intent.runtime.PathCalcRuntimeModule;
import net.onrc.onos.core.intent.runtime.web.IntentWebRoutable;
import org.junit.After;
import org.restlet.resource.ClientResource;

import java.util.Collection;
import java.util.Map;

/**
 * Base class for Intents REST API tests.
 * Maintains the web server and restlets used for the REST calls
 * Maintains whatever mocks are required to interact with the Intents framework
 * Provides JSON related utility routines for processing Intents APIs return values
 */
public class TestRestIntent extends TestRest {

    private PathCalcRuntimeModule runtime;
    private IntentTestMocks mocks;

    /**
     * Fetch the Path Calc Runtime.
     *
     * @return path calc runtime.
     */
    PathCalcRuntimeModule getRuntime() {
        return runtime;
    }

    /**
     * Fetch the Intent mocking object.
     *
     * @return intent mocking object
     */
    IntentTestMocks getMocks() {
        return mocks;
    }

    /**
     * Create the web server, PathCalcRuntime, and mocks required for
     * all of the tests.
     */
    @Override
    public void setUp() {
        mocks = new IntentTestMocks();
        mocks.setUpIntentMocks();

        addRestlet(new IntentWebRoutable());
        super.setUp();

        runtime = new PathCalcRuntimeModule();
        final FloodlightModuleContext moduleContext = getMocks().getModuleContext();
        try {
            runtime.init(moduleContext);
        } catch (FloodlightModuleException floodlightEx) {
            throw new IllegalArgumentException(floodlightEx);
        }
        runtime.startUp(moduleContext);

        getRestApiServer().addAttribute(IPathCalcRuntimeService.class.getCanonicalName(),
                                        runtime);
    }

    /**
     * Remove anything that will interfere with the next test running correctly.
     * Shuts down the test REST web server and removes the mocks.
     */
    @After
    public void tearDownMocks() {
        getMocks().tearDownIntentMocks();
    }

    /**
     * Fetch the base URL for Intents REST APIs.
     *
     * @return base URL
     */
    String getBaseRestIntentUrl() {
        return getBaseRestUrl() + "/intent";
    }

    /**
     * Fetch the URL to use for High Level Intents REST API calls.
     *
     * @return high level intents REST API URL
     */
    String getHighRestIntentUrl() {
        return getBaseRestIntentUrl() + "/high";
    }

    /**
     * Fetch the URL to use for Low Level Intents REST API calls.
     *
     * @return low level intents REST API URL
     */
    String getLowRestIntentUrl() {
        return getBaseRestIntentUrl() + "/low";
    }

    /**
     * Utility function to locate an intent in a JSON collection
     * that has the given id.
     * The JSON collection of intents looks like:
     *  {@code
     *      MAP =
     *        [0] =
     *          MAP =
     *            id = "1"
     *            ...
     *        [1]
     *          MAP =
     *            id = "2"
     *            ...
     *        [2]
     *          MAP =
     *            id = "3"
     *            ...
     *        ...
     *  }
     *
     * @param intents collection map to search
     * @param id id of the intent to look for
     * @return map for the intent if one was found, null otherwise
     */
    Map<String, String> findIntentWithId(final Collection<Map<String, String>> intents,
                                         final String id) {
        for (final Map<String, String> intentMap : intents) {
            if (id.equals(intentMap.get("id"))) {
                return intentMap;
            }
        }
        return null;
    }


    /**
     * Convenience function to fetch a collection of Intents from the JSON
     * result of a REST call.  Hides the ugliness of the unchecked conversion
     * to the proper Collection of Map type.
     *
     * @param client ClientResource that was used to make the REST call
     * @return Collection of Maps that hold the Intent data
     */
    @SuppressWarnings("unchecked")
    Collection<Map<String, String>> getIntentsCollection(final ClientResource client) {
        return client.get(Collection.class);
    }

    /**
     * Convenience function to fetch a single Intent from the JSON
     * result of a REST call.  Hides the ugliness of the unchecked conversion
     * to the proper Map type.
     *
     * @param client ClientResource that was used to make the REST call
     * @return Map that hold the Intent data
     */
    @SuppressWarnings("unchecked")
    Map<String, String> getIntent(final ClientResource client) {
        return client.get(Map.class);
    }

    /**
     * Modify the state of an intent by directly calling the Intent state
     * machine.  Needed in unit tests because of mocking of back end
     * components.
     *
     * @param intentId id of the intent to modify
     * @param newState assign this state to the intent
     */
    public void modifyIntentState(final String intentId,
                                  final Intent.IntentState newState) {
        final IntentStateList intentStateList = new IntentStateList();
        intentStateList.put(intentId, newState);
        getRuntime().entryUpdated(intentStateList);
    }
}
