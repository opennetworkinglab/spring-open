package net.onrc.onos.api.rest;

import net.floodlightcontroller.restserver.RestletRoutable;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

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
public class TestRest {

    private final List<RestletRoutable> restlets = new LinkedList<>();
    private TestRestApiServer restApiServer;
    private int restPort;

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
     * Assign the TCP port for the web server.
     *
     * @param newPort port number the web server will use
     */
    void setRestPort(int newPort) {
        restPort = newPort;
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
    public void setUp() {
        restApiServer = new TestRestApiServer(restPort);
        restApiServer.startServer(restlets);
    }

    /**
     * Remove anything that will interfere with the next test running correctly.
     * Shuts down the test REST web server.
     */

    public void tearDown() {
        getRestApiServer().stopServer();
    }

    /**
     * Get the base URL to use for REST requests.
     *
     * @return base URL
     */
    String getBaseRestUrl() {
        return "http://localhost:" + Integer.toString(restPort) + "/wm/onos";
    }

    /**
     * Generate a random port number for the REST API web server to use.  For
     * now, a random port between 50000 and 55000 is selected
     *
     * @return a port number that the web server can use
     */
    int generateRandomPort() {
        final int portStartRange = 50000;
        final int portEndRange = 55000;

        final Random random = new Random();

        return portStartRange + (random.nextInt(portEndRange - portStartRange));
    }
}
