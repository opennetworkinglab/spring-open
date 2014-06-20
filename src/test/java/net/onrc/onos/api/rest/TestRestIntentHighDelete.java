package net.onrc.onos.api.rest;

import net.onrc.onos.core.intent.Intent;
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

import java.util.Collection;
import java.util.Map;

import static net.onrc.onos.api.rest.ClientResourceStatusMatcher.hasStatusOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests to test the Intents DELETE REST APIs.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PathCalcRuntimeModule.class)
public class TestRestIntentHighDelete extends TestRestIntent {
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
     * Test that the DELETE of all high level Intents REST call returns the
     * proper result.
     * The HTTP status of the delete call should be NO CONTENT
     * Once the Intents are removed, an empty list should be
     * returned by the fetch of all high level Intents.
     */
    @Test
    public void testDeleteOfAllIntents() {

        makeDefaultIntents();

        final ClientResource deleteClient = new ClientResource(getHighRestIntentUrl());
        deleteClient.delete();

        // HTTP status should be NO CONTENT
        assertThat(deleteClient, hasStatusOf(Status.SUCCESS_NO_CONTENT));

        // trigger the back end deletion of the Intents
        modifyIntentState("1:2___0", Intent.IntentState.DEL_ACK);
        modifyIntentState("1:3___0", Intent.IntentState.DEL_ACK);

        //  Now query the intents to make sure they all got deleted
        final ClientResource client = new ClientResource(getHighRestIntentUrl());
        final Collection<Map<String, String>> intents = getIntentsCollection(client);

        // HTTP status should be OK
        assertThat(client, hasStatusOf(Status.SUCCESS_OK));

        // There should be no intents left
        assertThat(intents, hasSize(0));
    }

    /**
     * Test that the DELETE of an existing high level Intents REST call returns the
     * proper result.
     * The HTTP status of the delete call should be NO CONTENT
     * Once the Intent is removed, an empty list should be
     * returned by the fetch of the Intent.
     */
    @Test
    public void testDeleteOfSingleExistingIntent() {

        makeDefaultIntents();

        final String intentUrl = getHighRestIntentUrl() + "/2";
        final ClientResource deleteClient = new ClientResource(intentUrl);
        deleteClient.delete();

        // HTTP status should be NO CONTENT
        assertThat(deleteClient, hasStatusOf(Status.SUCCESS_NO_CONTENT));

        // trigger the back end deletion of the Intent.
        modifyIntentState("1:2___0", Intent.IntentState.DEL_ACK);

        ClientResource client = new ClientResource(intentUrl);
        try {
            //  Now query the intent to make sure it got deleted
            getIntent(client);
            Assert.fail("Fetch of deleted intent did not throw an exception");
        } catch (Exception ex) {
            // HTTP status should be NOT FOUND
            assertThat(client, hasStatusOf(Status.CLIENT_ERROR_NOT_FOUND));
        }
    }

    /**
     * Test that the DELETE of an existing high level Intents REST call returns the
     * proper result.
     * The HTTP status of the delete call should be NO CONTENT
     * Once the Intent remove API is called, an empty list should be
     * returned by the fetch of the Intent.
     */
    @Test
    public void testDeleteOfSingleNonExistingIntent() {

        makeDefaultIntents();

        final String intentUrl = getHighRestIntentUrl() + "/2345678";
        final ClientResource deleteClient = new ClientResource(intentUrl);
        deleteClient.delete();

        // HTTP status should be NO CONTENT
        assertThat(deleteClient, hasStatusOf(Status.SUCCESS_NO_CONTENT));

        ClientResource client = new ClientResource(intentUrl);
        try {
            //  Now query the intent to make sure its not there
            getIntent(client);
            Assert.fail("Fetch of deleted intent did not throw an exception");
        } catch (Exception ex) {
            // HTTP status should be NOT FOUND
            assertThat(client, hasStatusOf(Status.CLIENT_ERROR_NOT_FOUND));
        }
    }
}
