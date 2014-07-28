package net.onrc.onos.api.rest;

import net.onrc.onos.core.intent.runtime.PathCalcRuntimeModule;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;

import static net.onrc.onos.api.rest.ClientResourceStatusMatcher.hasStatusOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests to test the Intents POST REST APIs.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PathCalcRuntimeModule.class)
public class TestRestIntentHighPost extends TestRestIntent {
    private static final String JSON_STRING_FOR_NEW_INTENT =
    "[" +
    "{" +
    "        \"dstSwitchDpid\": \"00:00:00:00:00:00:04:02\"," +
    "        \"staticPath\": true," +
    "        \"dstSwitchPort\": 1," +
    "        \"intentType\": \"SHORTEST_PATH\"," +
    "        \"matchDstMac\": \"00:00:00:02:02:02\"," +
    "        \"srcSwitchPort\": 1," +
    "        \"srcSwitchDpid\": \"00:00:00:00:00:00:02:02\"," +
    "        \"intentId\": 1," +
    "        \"matchSrcMac\": \"00:00:00:01:01:01\"" +
    "}" +
    "]";

    /**
     * Test that a POST operation to create a high level Intent
     * creates an object correctly.
     * The HTTP status of the POST call should be CREATED and
     * the POST operation should return the new object.
     * Once the Intent is created, a fetch operation should return the
     * new Intent.
     *
     * @throws Exception if any of the JSON conversions fail
     */
    @Test
    public void testPostHighIntent() throws Exception {

        final ClientResource postClient = new ClientResource(getHighRestIntentUrl());
        final Representation postResource =
                new StringRepresentation(JSON_STRING_FOR_NEW_INTENT,
                                         MediaType.APPLICATION_JSON);
        postClient.post(postResource);

        // HTTP status should be CREATED
        assertThat(postClient, hasStatusOf(Status.SUCCESS_CREATED));

        //  Check that the returned entity is correct
        final String responseString = postClient.getResponse().getEntityAsText();
        final JSONArray responseIntents = new JSONArray(responseString);
        assertThat(responseIntents.length(), is(equalTo(1)));

        final JSONObject responseIntent = responseIntents.getJSONObject(0);
        assertThat(responseIntent, notNullValue());
        assertThat(responseIntent.getBoolean("staticPath"), is(true));
        assertThat(responseIntent.getString("dstSwitchDpid"),
                   is(equalTo("00:00:00:00:00:00:04:02")));
        assertThat(responseIntent.getInt("dstSwitchPort"),
                   is(equalTo(1)));
        assertThat(responseIntent.getString("intentType"),
                   is(equalTo("SHORTEST_PATH")));
        assertThat(responseIntent.getString("matchDstMac"),
                   is(equalTo("00:00:00:02:02:02")));
        assertThat(responseIntent.getInt("srcSwitchPort"),
                   is(equalTo(1)));
        assertThat(responseIntent.getString("srcSwitchDpid"),
                   is(equalTo("00:00:00:00:00:00:02:02")));
        assertThat(responseIntent.getInt("intentId"),
                   is(equalTo(1)));
        assertThat(responseIntent.getString("matchSrcMac"),
                   is(equalTo("00:00:00:01:01:01")));

        //  Now query the intent to make sure it was created properly
        final ClientResource client = new ClientResource(getHighRestIntentUrl());
        final JSONArray intents = getJSONArray(client);

        // HTTP status should be OK
        assertThat(client, hasStatusOf(Status.SUCCESS_OK));

        // There should be 1 intent
        assertThat(intents.length(), is(equalTo(1)));

        final JSONObject queriedIntent = intents.getJSONObject(0);
        assertThat(queriedIntent, notNullValue());

        // Check the attributes of the intent
        assertThat(queriedIntent.getString("id"),
                   is(equalTo("1:1")));
        assertThat(queriedIntent.getString("srcPortNumber"),
                is(equalTo("1")));
        assertThat(queriedIntent.getString("srcMac"),
                   is(equalTo("00:00:00:01:01:01")));
        assertThat(queriedIntent.getString("srcSwitchDpid"),
                   is(equalTo("00:00:00:00:00:00:02:02")));
        assertThat(queriedIntent.getString("dstPortNumber"),
                is(equalTo("1")));
        assertThat(queriedIntent.getString("dstMac"),
                is(equalTo("00:00:00:02:02:02")));
        assertThat(queriedIntent.getString("dstSwitchDpid"),
                is(equalTo("00:00:00:00:00:00:04:02")));
    }
}
