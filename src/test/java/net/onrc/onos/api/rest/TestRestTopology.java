package net.onrc.onos.api.rest;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.onrc.onos.core.intent.runtime.IntentTestMocks;
import net.onrc.onos.core.intent.runtime.PathCalcRuntimeModule;
import net.onrc.onos.core.intent.runtime.web.IntentWebRoutable;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.web.TopologyWebRoutable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.resource.ClientResource;

/**
 * Test harness for Topology based REST API tests.  This class maintains the
 * web server and mocks required for testing topology APIs.  REST API tests
 * for topology should inherit from this class.
 */
public class TestRestTopology extends TestRest {

    private IntentTestMocks mocks;

    /**
     * Fetch the Intent mocking object.
     *
     * @return intent mocking object
     */
    IntentTestMocks getMocks() {
        return mocks;
    }

    /**
     * Create the web server and mocks required for the topology tests.
     */
    @Override
    public void setUp() {
        mocks = new IntentTestMocks();
        mocks.setUpIntentMocks();

        addRestlet(new TopologyWebRoutable());
        super.setUp();

        final PathCalcRuntimeModule runtime = new PathCalcRuntimeModule();
        final FloodlightModuleContext moduleContext = getMocks().getModuleContext();
        try {
            runtime.init(moduleContext);
        } catch (FloodlightModuleException floodlightEx) {
            throw new IllegalArgumentException(floodlightEx);
        }
        runtime.startUp(moduleContext);

        getRestApiServer().addAttribute(ITopologyService.class.getCanonicalName(),
                                        mocks.getTopologyService());
    }

    /**
     * Remove anything that will interfere with the next test running correctly.
     * Shuts down the test REST web server and removes the mocks.
     */
    @Override
    public void tearDown() {
        getMocks().tearDownIntentMocks();
        super.tearDown();
    }

    /**
     * Fetch the base URL for Topology REST APIs.
     *
     * @return base URL
     */
    String getBaseRestTopologyUrl() {
        return getBaseRestUrl() + "/topology";
    }

    /**
     * Get the JSON object representation for the top level object referred
     * to by the given client.
     *
     * @param client the ClientResource that references the JSON object
     * @return JSONObject that represents the object, null if it can't be
     *         fetched
     */
    JSONObject getJSONObject(final ClientResource client) {
        try {
            final String topologyJSONString = client.get(String.class);
            return new JSONObject(topologyJSONString);
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
    JSONArray getJSONArray(final ClientResource client) {
        try {
            final String topologyJSONString = client.get(String.class);
            return new JSONArray(topologyJSONString);
        } catch (JSONException jsonException) {
            return null;
        }
    }
}
