package net.onrc.onos.api.rest;


import com.google.common.net.InetAddresses;
import net.onrc.onos.core.intent.IntentOperation;
import net.onrc.onos.core.intent.IntentOperationList;
import net.onrc.onos.core.intent.ShortestPathIntent;
import net.onrc.onos.core.intent.runtime.PathCalcRuntimeModule;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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

import static net.onrc.onos.api.rest.ClientResourceStatusMatcher.hasStatusOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests to test the Intents GET REST APIs.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PathCalcRuntimeModule.class)
public class TestRestIntentLowGet extends TestRestIntent {
    private static final Long LOCAL_PORT = 0xFFFEL;
    private static final String BAD_SWITCH_INTENT_NAME = "No Such Switch Intent";
    private static final String IP_ADDRESS_1 = "127.0.0.1";
    private static final String IP_ADDRESS_2 = "127.0.0.2";
    private static final String IP_ADDRESS_3 = "127.0.0.3";

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
        final int ipAddress1AsInt = InetAddresses.coerceToInteger(
                InetAddresses.forString(IP_ADDRESS_1));
        final int ipAddress2AsInt = InetAddresses.coerceToInteger(
                InetAddresses.forString(IP_ADDRESS_2));
        final int ipAddress3AsInt = InetAddresses.coerceToInteger(
                InetAddresses.forString(IP_ADDRESS_3));

        // create shortest path intents
        final IntentOperationList opList = new IntentOperationList();
        opList.add(IntentOperation.Operator.ADD,
                new ShortestPathIntent(BAD_SWITCH_INTENT_NAME, 111L, 12L,
                        LOCAL_PORT, 2L, 21L, LOCAL_PORT));
        opList.add(IntentOperation.Operator.ADD,
                new ShortestPathIntent("1:2", 1L, 14L, LOCAL_PORT, ipAddress1AsInt,
                        4L, 41L, LOCAL_PORT, ipAddress2AsInt));
        opList.add(IntentOperation.Operator.ADD,
                new ShortestPathIntent("1:3", 2L, 23L, LOCAL_PORT, ipAddress2AsInt,
                        3L, 32L, LOCAL_PORT, ipAddress3AsInt));

        // compile high-level intent operations into low-level intent
        // operations (calculate paths)

        final IntentOperationList pathIntentOpList =
                getRuntime().executeIntentOperations(opList);
        assertThat(pathIntentOpList, notNullValue());

    }

    /**
     * Find the intent with the given ID in the intent array.
     *
     * @param intents array of intents
     * @param id this is the id too look up
     * @return JSONObject for the intent if found, null otherwise
     * @throws JSONException if the intent object marshalling fails
     */
    private JSONObject findIntent(final JSONArray intents, final String id)
                       throws JSONException {

        if (id == null) {
            return null;
        }

        JSONObject result = null;
        for (int intentIndex = 0; intentIndex < intents.length(); intentIndex++) {
            final JSONObject thisIntent = intents.getJSONObject(intentIndex);
            if (id.equals(thisIntent.getString("id"))) {
                result = thisIntent;
            }
        }

        return result;
    }

    /**
     * Test that the GET of all Intents REST call returns the proper result.
     * The call to get all Intents should return 3 items, an HTTP status of OK,
     * and the proper Intent data.
     *
     * @throws Exception to fail the test if any unhandled errors occur
     */
    @Test
    public void testFetchOfAllLowLevelIntents() throws Exception {

        makeDefaultIntents();

        final ClientResource client = new ClientResource(getLowRestIntentUrl());
        final JSONArray intents = getJSONArray(client);
        assertThat(intents, is(notNullValue()));

        // HTTP status should be OK
        assertThat(client, hasStatusOf(Status.SUCCESS_OK));

        // 2 low level intents should have been fetched - one of the
        // high level intents is in error
        assertThat(intents.length(), is(equalTo(2)));

        //  Check that intent 0 is correct
        final JSONObject intent0 = findIntent(intents, "1:2___0");
        assertThat(intent0, is(notNullValue()));

        //  Check the values of the fields in low level intent 0
        assertThat(intent0.getString("id"), is(equalTo("1:2___0")));
        assertThat(intent0.getString("state"), is(equalTo("INST_REQ")));
        assertThat(intent0.getBoolean("pathFrozen"), is(equalTo(false)));

        // Check the path on intent 0
        final JSONArray path0 = intent0.getJSONArray("path");
        assertThat(path0, is(notNullValue()));
        final JSONObject ports0 = path0.getJSONObject(0);
        assertThat(ports0, is(notNullValue()));
        final JSONObject dst0 = ports0.getJSONObject("dst");
        assertThat(dst0, is(notNullValue()));
        final String dstDpid0 = dst0.getString("dpid");
        assertThat(dstDpid0, is(equalTo("00:00:00:00:00:00:00:04")));
        int dstPortNumber0 = dst0.getInt("portNumber");
        assertThat(dstPortNumber0, is(equalTo(41)));
        final JSONObject src0 = ports0.getJSONObject("src");
        assertThat(src0, is(notNullValue()));
        final String srcDpid0 = src0.getString("dpid");
        assertThat(srcDpid0, is(equalTo("00:00:00:00:00:00:00:01")));
        int srcPortNumber0 = src0.getInt("portNumber");
        assertThat(srcPortNumber0, is(equalTo(14)));

        //  Check that intent 1 is correct
        final JSONObject intent1 = findIntent(intents, "1:3___0");
        assertThat(intent1, is(notNullValue()));

        //  Check the values of the fields in low level intent 1
        assertThat(intent1.getString("id"), is(equalTo("1:3___0")));
        assertThat(intent1.getString("state"), is(equalTo("INST_REQ")));
        assertThat(intent1.getBoolean("pathFrozen"), is(equalTo(false)));

        // Check the path on intent 1
        final JSONArray path1 = intent1.getJSONArray("path");
        assertThat(path1, is(notNullValue()));
        final JSONObject ports1 = path1.getJSONObject(0);
        assertThat(ports1, is(notNullValue()));
        final JSONObject dst1 = ports1.getJSONObject("dst");
        assertThat(dst1, is(notNullValue()));
        final String dstDpid1 = dst1.getString("dpid");
        assertThat(dstDpid1, is(equalTo("00:00:00:00:00:00:00:03")));
        int dstPortNumber1 = dst1.getInt("portNumber");
        assertThat(dstPortNumber1, is(equalTo(32)));
        final JSONObject src1 = ports1.getJSONObject("src");
        assertThat(src1, is(notNullValue()));
        final String srcDpid1 = src1.getString("dpid");
        assertThat(srcDpid1, is(equalTo("00:00:00:00:00:00:00:02")));
        int srcPortNumber1 = src1.getInt("portNumber");
        assertThat(srcPortNumber1, is(equalTo(23)));
    }

    /**
     * Test that the GET of a single Intent REST call returns the proper result
     * when given a bad Intent id. The call to get the Intent should return a
     * status of NOT_FOUND.
     *
     * @throws JSONException if a bad JSON object is returned for the error
     */
    @Test
    public void testFetchOfBadLowLevelIntent() throws JSONException {

        makeDefaultIntents();

        final ClientResource client = new ClientResource(getLowRestIntentUrl() + "/2334");

        try {
            client.get();
            // The get operation should have thrown a ResourceException.
            // Fail because the Exception was not seen.
            Assert.fail("Invalid intent fetch did not cause an exception");
        } catch (ResourceException resourceError) {
            // The HTTP status should be NOT FOUND
            assertThat(client, hasStatusOf(Status.CLIENT_ERROR_NOT_FOUND));

            //  Check that the error entity is correct
            final String responseErrorString = client.getResponse().getEntityAsText();
            final JSONObject responseError = new JSONObject(responseErrorString);
            assertThat(responseError.getString("code"),
                       is(equalTo("INTENT_NOT_FOUND")));
            assertThat(responseError.getString("summary"),
                       is(equalTo("Intent not found")));
            assertThat(responseError.getString("formattedDescription"),
                       containsString("An intent with the identifier"));
        }
    }

    /**
     * Test that the GET of a single Low Level Intent REST call returns the
     * proper result for an existing Intent. The call to get the Low Level
     * Intent should return a status of OK, and the data for the
     * Low Level Intent should be correct.
     *
     * @throws JSONException if the JSON cannot be marshalled into an object.
     */
    @Test
    public void testFetchOfGoodLowLevelIntent() throws JSONException {

        makeDefaultIntents();

        final ClientResource client = new ClientResource(getLowRestIntentUrl() + "/3___0");
        final JSONObject intent = getJSONObject(client);

        // HTTP status should be OK
        assertThat(client, hasStatusOf(Status.SUCCESS_OK));

        //  Intent data should be correct
        assertThat(intent, is(notNullValue()));

        assertThat(intent.getString("id"), is(equalTo("1:3___0")));
        assertThat(intent.getString("state"), is(equalTo("INST_REQ")));
        assertThat(intent.getBoolean("pathFrozen"), is(false));
    }
}

