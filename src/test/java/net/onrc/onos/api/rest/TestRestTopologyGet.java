package net.onrc.onos.api.rest;

import net.onrc.onos.core.intent.runtime.PathCalcRuntimeModule;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;

import java.util.HashSet;
import java.util.Set;

import static net.onrc.onos.api.rest.ClientResourceStatusMatcher.hasStatusOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for topology REST get operations.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PathCalcRuntimeModule.class)
public class TestRestTopologyGet extends TestRestTopology {

    /**
     * Create the web server and mocks required for
     * all of the tests.
     */
    @Before
    @SuppressWarnings("ununsed")
    public void beforeTest() {
        setRestPort(generateRandomPort());
        setUp();
    }


    /**
     * Remove anything that will interfere with the next test running correctly.
     * Shuts down the test REST web server and removes the mocks.
     */
    @After
    @SuppressWarnings("unused")
    public void afterTest() {
        tearDown();
    }

    /**
     * Check that the JSON array returned for the switches element matches
     * the data in the mocked topology.
     *
     * @param switches JSON array of switches
     * @throws JSONException if the JSON is not properly specified
     */
    private void checkSwitches(final JSONArray switches) throws JSONException {
        assertThat(switches.length(), is(equalTo(4)));

        // Check that the first switch has the proper data
        final JSONObject switch0 = switches.getJSONObject(0);
        assertThat(switch0.length(), is(equalTo(3)));
        assertThat(switch0.getString("dpid"), is(equalTo("00:00:00:00:00:00:00:02")));
        assertThat(switch0.getString("state"), is(equalTo("ACTIVE")));

        // Check that the ports array for the switch is correct
        final JSONArray switch0Ports = switch0.getJSONArray("ports");

        // check the length of the port array
        assertThat(switch0Ports.length(), equalTo(4));

        // check the contents of the ports array.  All of the ports should be
        // active and refer to this switch.
        for (int portIndex = 0; portIndex < switch0Ports.length(); portIndex++) {
            final JSONObject switchPort = switch0Ports.getJSONObject(portIndex);
            assertThat(switchPort.getString("dpid"), is(equalTo("00:00:00:00:00:00:00:02")));
            assertThat(switchPort.getString("state"), is(equalTo("ACTIVE")));
        }
    }

    /**
     * Check that the JSON array returned for the links element matches
     * the data in the mocked topology.
     *
     * @param links JSON array of links
     * @throws JSONException if the JSON is not properly specified
     */
    private void checkLinks(final JSONArray links) throws JSONException {
        // Check the length of the links array
        assertThat(links.length(), is(equalTo(10)));

        final Set<String> fromLinks = new HashSet<>();
        final Set<String> toLinks = new HashSet<>();

        // Check that the source and destination of links to switch 0 are
        // correct
        for (int linkIndex = 0; linkIndex < links.length(); linkIndex++) {
            final JSONObject link = links.getJSONObject(linkIndex);
            final JSONObject src = link.getJSONObject("src");
            assertThat(src, is(notNullValue()));
            final JSONObject dst = link.getJSONObject("dst");
            assertThat(dst, is(notNullValue()));
            final String srcDpid = src.getString("dpid");
            final String dstDpid = dst.getString("dpid");
            assertThat(srcDpid, is(notNullValue()));
            assertThat(dstDpid, is(notNullValue()));

            if (srcDpid.equals("00:00:00:00:00:00:00:02")) {
                toLinks.add(dstDpid);
            }

            if (dstDpid.equals("00:00:00:00:00:00:00:02")) {
                fromLinks.add(srcDpid);
            }
        }

        assertThat(toLinks, hasItems("00:00:00:00:00:00:00:01",
                                     "00:00:00:00:00:00:00:03",
                                     "00:00:00:00:00:00:00:04"));

        assertThat(fromLinks, hasItems("00:00:00:00:00:00:00:01",
                                       "00:00:00:00:00:00:00:03",
                                       "00:00:00:00:00:00:00:04"));
    }

    /**
     * Check that the JSON array returned for the hosts element matches
     * the data in the mocked topology.
     *
     * @param hosts JSON array of hosts
     */
    private void checkHosts(final JSONArray hosts) {
        // hosts array should be empty
        assertThat(hosts.length(), is(equalTo(0)));
    }

    /**
     * Test that the GET of all Topology REST call returns the proper result.
     * The call to get all Topology should return 3 items (switches, links,
     * and hosts), an HTTP status of OK, and the proper topology data.
     */
    @Test
    public void testFetchOfAllTopology() throws Exception {
        final ClientResource client = new ClientResource(getBaseRestTopologyUrl());
        final JSONObject topology = getJSONObject(client);

        // HTTP status should be OK
        assertThat(client, hasStatusOf(Status.SUCCESS_OK));

        // Check the number of top level members in the topology object
        assertThat(topology.length(), is(equalTo(3)));

        //  Check the switches element
        final JSONArray switches = topology.getJSONArray("switches");
        checkSwitches(switches);

        // Check the values in the links array
        final JSONArray links = topology.getJSONArray("links");
        checkLinks(links);

        // Check the hosts array
        final JSONArray hosts = topology.getJSONArray("hosts");
        checkHosts(hosts);
    }

    /**
     * Test that the GET of all switches REST call returns the proper result.
     * The call to get all switches should return the correct switch data.
     */
    @Test
    public void testFetchOfAllSwitches() throws Exception {
        final ClientResource client = new ClientResource(getBaseRestTopologyUrl() + "/switches");
        final JSONArray switches = getJSONArray(client);

        // HTTP status should be OK
        assertThat(client, hasStatusOf(Status.SUCCESS_OK));

        checkSwitches(switches);
    }

    /**
     * Test that the GET of all links REST call returns the proper result.
     * The call to get all links should return the proper link data.
     */
    @Test
    public void testFetchOfAllLinks() throws Exception {
        final ClientResource client = new ClientResource(getBaseRestTopologyUrl() + "/links");
        final JSONArray links = getJSONArray(client);

        // HTTP status should be OK
        assertThat(client, hasStatusOf(Status.SUCCESS_OK));

        checkLinks(links);
    }

    /**
     * Test that the GET of all hosts REST call returns the proper result.
     * The call to get all hosts should return no hosts.
     */
    @Test
    public void testFetchOfAllHosts() throws Exception {
        final ClientResource client = new ClientResource(getBaseRestTopologyUrl() + "/hosts");
        final JSONArray hosts = getJSONArray(client);

        // HTTP status should be OK
        assertThat(client, hasStatusOf(Status.SUCCESS_OK));

        checkHosts(hosts);
    }
}
