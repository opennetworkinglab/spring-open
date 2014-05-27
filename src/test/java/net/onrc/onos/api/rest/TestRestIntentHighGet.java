package net.onrc.onos.api.rest;


import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.restserver.RestletRoutable;
import net.onrc.onos.core.intent.IntentOperation;
import net.onrc.onos.core.intent.IntentOperationList;
import net.onrc.onos.core.intent.ShortestPathIntent;
import net.onrc.onos.core.intent.runtime.IPathCalcRuntimeService;
import net.onrc.onos.core.intent.runtime.IntentTestMocks;
import net.onrc.onos.core.intent.runtime.PathCalcRuntimeModule;
import net.onrc.onos.core.intent.runtime.web.IntentWebRoutable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests to test the Intents REST APIs.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PathCalcRuntimeModule.class)
public class TestRestIntentHighGet {
    private static final Long LOCAL_PORT = 0xFFFEL;

    private static int REST_PORT = 7777;
    private static String HOST_BASE_URL = "http://localhost:" +
                                          Integer.toString(REST_PORT);
    private static final String BASE_URL = HOST_BASE_URL + "/wm/onos/intent";
    private static final String HIGH_URL = BASE_URL + "/high";

    private PathCalcRuntimeModule runtime;
    private TestRestApiServer restApiServer;
    private IntentTestMocks mocks;


    /**
     * Create the web server, PathCalcRuntime, and mocks required for
     * all of the tests.
     * @throws Exception if the mocks or webserver cannot be started.
     */
    @Before
    public void setUp() throws Exception {
        mocks = new IntentTestMocks();
        mocks.setUpIntentMocks();

        runtime = new PathCalcRuntimeModule();
        final FloodlightModuleContext moduleContext = mocks.getModuleContext();
        runtime.init(moduleContext);
        runtime.startUp(moduleContext);

        final List<RestletRoutable> restlets = new LinkedList<>();
        restlets.add(new IntentWebRoutable());

        restApiServer = new TestRestApiServer(REST_PORT);
        restApiServer.startServer(restlets);
        restApiServer.addAttribute(IPathCalcRuntimeService.class.getCanonicalName(),
                                   runtime);
    }


    /**
     * Remove anything that will interfere with the next test running correctly.
     * Shuts down the test REST web server and removes the mocks.
     * @throws Exception if the mocks can't be removed or the web server can't
     *         shut down correctly.
     */
    @After
    public void tearDown() throws Exception {
        restApiServer.stopServer();
        mocks.tearDownIntentMocks();
    }


    /**
     * Make a set of Intents that can be used as test data.
     */
    private void makeDefaultIntents() {
        final String BAD_SWITCH_INTENT_NAME = "No Such Switch Intent";

        // create shortest path intents
        final IntentOperationList opList = new IntentOperationList();
        opList.add(IntentOperation.Operator.ADD,
                new ShortestPathIntent(BAD_SWITCH_INTENT_NAME, 111L, 12L,
                        LOCAL_PORT, 2L, 21L, LOCAL_PORT));
        opList.add(IntentOperation.Operator.ADD,
                new ShortestPathIntent("1:2", 1L, 14L, LOCAL_PORT, 4L, 41L,
                        LOCAL_PORT));
        opList.add(IntentOperation.Operator.ADD,
                new ShortestPathIntent("1:3", 2L, 23L, LOCAL_PORT, 3L, 32L,
                        LOCAL_PORT));

        // compile high-level intent operations into low-level intent
        // operations (calculate paths)

        final IntentOperationList pathIntentOpList =
                runtime.executeIntentOperations(opList);
        assertThat(pathIntentOpList, notNullValue());

    }

    /**
     * Utility function to locate an intent in a JSON collection
     * that has the given id.
     * The JSON collection of intents looks like:
     *  <code>
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
     *  </code>
     *
     * @param intents collection map to search
     * @param id id of the intent to look for
     * @return map for the intent if one was found, null otherwise
     */
    private Map<String, String> findIntentWithId(final Collection<Map<String, String>> intents,
                                                 final String id) {
        for (final Map<String, String>intentMap : intents) {
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
    private Collection<Map<String, String>> getIntentsCollection (final ClientResource client) {
        return (Collection<Map<String, String>>)client.get(Collection.class);
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
    private Map<String, String> getIntent (final ClientResource client) {
        return (Map<String, String>)client.get(Map.class);
    }

    /**
     * Test that the GET of all Intents REST call returns the proper result.
     * The call to get all Intents should return 3 items, an HTTP status of OK,
     * and the proper Intent data.
     *
     * @throws Exception if any of the set up or tear down operations fail
     */
    @Test
    public void testFetchOfAllIntents() throws Exception {

        makeDefaultIntents();

        final ClientResource client = new ClientResource(HIGH_URL);
        final Collection<Map<String, String>> intents = getIntentsCollection(client);

        // HTTP status should be OK
        assertThat(client.getStatus(), is(equalTo(Status.SUCCESS_OK)));

        // 3 intents should have been fetched
        assertThat(intents, hasSize(3));

        // check that the Intent with id "3" is present, and has the right data
        final Map<String, String> mapForIntent3 = findIntentWithId(intents, "1:3");
        // Intent 3 must exist
        assertThat(mapForIntent3, notNullValue());
        //  Data must be correct
        assertThat(mapForIntent3, hasKey("state"));
        final String state = mapForIntent3.get("state");
        assertThat(state, is(equalTo("INST_REQ")));
    }

    /**
     * Test that the GET of a single Intent REST call returns the proper result
     * when given a bad Intent id. The call to get the Intent should return a
     * status of NOT_FOUND.
     *
     * @throws Exception if any of the set up or tear down operations fail
     */
    @Test
    public void testFetchOfBadIntent() throws Exception {

        makeDefaultIntents();

        final ClientResource client = new ClientResource(HIGH_URL + "/2334");

        try {
            getIntent(client);
            // The get operation should have thrown a ResourceException.
            // Fail because the Exception was not seen.
            Assert.fail("Invalid intent fetch did not cause an exception");
        } catch (ResourceException resourceError) {
            // The HTTP status should be NOT FOUND
            assertThat(client.getStatus(), is(equalTo(Status.CLIENT_ERROR_NOT_FOUND)));
        }
    }

    /**
     * Test that the GET of a single Intent REST call returns the proper result
     * for an existing Intent. The call to get the Intent should return a
     * status of OK, and the data for the Intent should be correct.
     *
     * @throws Exception if any of the set up or tear down operations fail
     */
    @Test
    public void testFetchOfGoodIntent() throws Exception {

        makeDefaultIntents();

        final ClientResource client = new ClientResource(HIGH_URL + "/2");
        final Map<String, String> intent;
        intent = getIntent(client);

        // HTTP status should be OK
        assertThat(client.getStatus(), is(equalTo(Status.SUCCESS_OK)));

        //  Intent data should be correct
        assertThat(intent, is(notNullValue()));
        assertThat(intent, hasKey("id"));
        assertThat(intent.get("id"), is(equalTo("1:2")));
        assertThat(intent, hasKey("state"));
        assertThat(intent.get("state"), is(equalTo("INST_REQ")));
    }
}
