package net.onrc.onos.api.rest;

import com.codahale.metrics.MetricFilter;
import net.floodlightcontroller.restserver.RestletRoutable;
import net.onrc.onos.core.metrics.OnosMetrics;
import net.onrc.onos.core.util.UnitTest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.restlet.resource.ClientResource;

import java.util.LinkedList;
import java.util.List;

/**
 * Base class for REST API tests.  This class exposes common code for setting
 * up REST tests.
 *
 * This class maintains the web server and restlets that allow a test to call
 * REST APIs.  It is intended to be used as a base class for a class that
 * allows testing of a specific REST API resource (an example is Intents).
 * See TestRestIntent as an example of an implementation which uses the
 * TestRest framework.
 */
public class TestRest extends UnitTest {

    private final List<RestletRoutable> restlets = new LinkedList<>();
    private TestRestApiServer restApiServer;

    /**
     * Add a restlet to the web server.  Tests call this to add the specific
     * REST APIs they are testing.  Call this before starting the server via
     * setUp().
     *
     * @param newRestlet restlet to add to the web server
     */
    void addRestlet(final RestletRoutable newRestlet) {
        restlets.add(newRestlet);
    }

    /**
     * Fetch the REST API Web Server object.
     *
     * @return REST API web server
     */
    TestRestApiServer getRestApiServer() {
        return restApiServer;
    }

    /**
     * Set up the REST API web server and start it.
     */
    @Before
    public void setUp() {
        restApiServer = new TestRestApiServer();
        restApiServer.startServer(restlets);
    }

    /**
     * Remove anything that will interfere with the next test running correctly.
     * Shuts down the test REST web server.
     */
    @After
    public void tearDownRest() {
        getRestApiServer().stopServer();
        OnosMetrics.removeMatching(MetricFilter.ALL);
    }

    /**
     * Get the base URL to use for REST requests.
     *
     * @return base URL
     */
    String getBaseRestUrl() {
        return "http://localhost:" + Integer.toString(restApiServer.getRestPort()) + "/wm/onos";
    }

    /**
     * Get the JSON object representation for the top level object referred
     * to by the given client.
     *
     * @param client the ClientResource that references the JSON object
     * @return JSONObject that represents the object, null if it can't be
     *         fetched
     */
    protected static JSONObject getJSONObject(final ClientResource client) {
        try {
            final String responseJSONString = client.get(String.class);
            return new JSONObject(responseJSONString);
        } catch (JSONException jsonException) {
            return null;
        }
    }

    /**
     * Get the JSON array representation for the array referred to by
     * the given client.
     *
     * @param client the ClientResource that references the JSON array
     * @return JSONArray that represents the array, null if it can't be
     *         fetched.
     */
    protected static JSONArray getJSONArray(final ClientResource client) {
        try {
            final String responseJSONString = client.get(String.class);
            return new JSONArray(responseJSONString);
        } catch (JSONException jsonException) {
            return null;
        }
    }
}
