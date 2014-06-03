package net.onrc.onos.api.rest;


import net.onrc.onos.core.intent.IntentOperation;
import net.onrc.onos.core.intent.IntentOperationList;
import net.onrc.onos.core.intent.ShortestPathIntent;
import net.onrc.onos.core.intent.runtime.PathCalcRuntimeModule;
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
import java.util.Map;

import static net.onrc.onos.api.rest.ClientResourceStatusMatcher.hasStatusOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests to test the Intents GET REST APIs.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PathCalcRuntimeModule.class)
public class TestRestIntentHighGet extends TestRestIntent {
    private static final Long LOCAL_PORT = 0xFFFEL;
    private static final String BAD_SWITCH_INTENT_NAME = "No Such Switch Intent";


    /**
     * Create the web server, PathCalcRuntime, and mocks required for
     * all of the tests.
     */
    @Before
    public void beforeTest() {
        setRestPort(generateRandomPort());
        setUp();
    }


    /**
     * Remove anything that will interfere with the next test running correctly.
     * Shuts down the test REST web server and removes the mocks.
     */
    @After
    public void afterTest() {
        tearDown();
    }


    /**
     * Make a set of Intents that can be used as test data.
     */
    private void makeDefaultIntents() {
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
                getRuntime().executeIntentOperations(opList);
        assertThat(pathIntentOpList, notNullValue());

    }

    /**
     * Test that the GET of all Intents REST call returns the proper result.
     * The call to get all Intents should return 3 items, an HTTP status of OK,
     * and the proper Intent data.
     */
    @Test
    public void testFetchOfAllIntents() {

        makeDefaultIntents();

        final ClientResource client = new ClientResource(getHighRestIntentUrl());
        final Collection<Map<String, String>> intents = getIntentsCollection(client);

        // HTTP status should be OK
        assertThat(client, hasStatusOf(Status.SUCCESS_OK));

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

        // check that the Intent with the bad switch ID is present, and has the right data
        final Map<String, String> mapForIntentBadIntent =
                findIntentWithId(intents, BAD_SWITCH_INTENT_NAME);
        // Intent must exist
        assertThat(mapForIntentBadIntent, notNullValue());
        //  Data must be correct
        assertThat(mapForIntentBadIntent, hasKey("state"));
        final String badIntentState = mapForIntentBadIntent.get("state");
        assertThat(badIntentState, is(equalTo("INST_NACK")));

    }

    /**
     * Test that the GET of a single Intent REST call returns the proper result
     * when given a bad Intent id. The call to get the Intent should return a
     * status of NOT_FOUND.
     */
    @Test
    public void testFetchOfBadIntent() {

        makeDefaultIntents();

        final ClientResource client = new ClientResource(getHighRestIntentUrl() + "/2334");

        try {
            getIntent(client);
            // The get operation should have thrown a ResourceException.
            // Fail because the Exception was not seen.
            Assert.fail("Invalid intent fetch did not cause an exception");
        } catch (ResourceException resourceError) {
            // The HTTP status should be NOT FOUND
            assertThat(client, hasStatusOf(Status.CLIENT_ERROR_NOT_FOUND));
        }
    }

    /**
     * Test that the GET of a single Intent REST call returns the proper result
     * for an existing Intent. The call to get the Intent should return a
     * status of OK, and the data for the Intent should be correct.
     */
    @Test
    public void testFetchOfGoodIntent() {

        makeDefaultIntents();

        final ClientResource client = new ClientResource(getHighRestIntentUrl() + "/2");
        final Map<String, String> intent;
        intent = getIntent(client);

        // HTTP status should be OK
        assertThat(client, hasStatusOf(Status.SUCCESS_OK));

        //  Intent data should be correct
        assertThat(intent, is(notNullValue()));
        assertThat(intent, hasKey("id"));
        assertThat(intent.get("id"), is(equalTo("1:2")));
        assertThat(intent, hasKey("state"));
        assertThat(intent.get("state"), is(equalTo("INST_REQ")));
    }
}
