package net.onrc.onos.api.rest;

import com.codahale.metrics.Counter;
import net.onrc.onos.core.intent.runtime.PathCalcRuntimeModule;
import net.onrc.onos.core.metrics.OnosMetrics;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.restlet.resource.ClientResource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for REST APIs for Counter Metrics.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PathCalcRuntimeModule.class)
public class TestRestMetricsCounters extends TestRestMetrics {

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

    //  Test Counter data objects
    private static final OnosMetrics.MetricsComponent COMPONENT =
            OnosMetrics.registerComponent("MetricsUnitTests");
    private static final OnosMetrics.MetricsFeature FEATURE =
            COMPONENT.registerFeature("Counters");

    private static final String COUNTER1_NAME = "COUNTER1";
    private static final String COUNTER1_FULL_NAME = OnosMetrics.generateName(COMPONENT,
                                                     FEATURE,
                                                     COUNTER1_NAME);
    private static final int COUNTER1_COUNT = 0;

    private static final String COUNTER2_NAME = "COUNTER2";
    private static final String COUNTER2_FULL_NAME = OnosMetrics.generateName(COMPONENT,
                                                     FEATURE,
                                                     COUNTER2_NAME);
    private static final int COUNTER2_COUNT = -1;

    private static final String COUNTER3_NAME = "COUNTER3";
    private static final String COUNTER3_FULL_NAME = OnosMetrics.generateName(COMPONENT,
                                                     FEATURE,
                                                     COUNTER3_NAME);
    private static final int COUNTER3_COUNT = 5;

    private final Counter counter1 =
            OnosMetrics.createCounter(COMPONENT,
                                      FEATURE,
                                      COUNTER1_NAME);
    private final Counter counter2 =
            OnosMetrics.createCounter(COMPONENT,
                                      FEATURE,
                                      COUNTER2_NAME);
    private final Counter counter3 =
            OnosMetrics.createCounter(COMPONENT,
                                      FEATURE,
                                      COUNTER3_NAME);

    /**
     * Create some test data for the tests.
     */
    private void fillCounters() {
        counter1.inc(COUNTER1_COUNT);
        counter2.inc(COUNTER2_COUNT);
        counter3.inc(COUNTER3_COUNT);
    }

    /**
     * Check that a Counter object has the right contents.
     *
     * @param counterContainer JSON for the Counter
     * @param name name of the Counter
     * @param count expected count for the Counter
     * @throws JSONException if any of the JSON fetches fail
     */
    private void checkCounter(final JSONObject counterContainer,
                              final String name,
                              final int count) throws JSONException {
        final String counterName = counterContainer.getString("name");
        assertThat(counterName, is(notNullValue()));
        assertThat(counterName, is(equalTo(name)));

        final JSONObject counterObject = counterContainer.getJSONObject("counter");
        assertThat(counterObject, is(notNullValue()));

        final int counterValue = counterObject.getInt("count");
        assertThat(counterValue, is(equalTo(count)));
    }

    /**
     * Test the REST APIs for Metrics Counter objects.
     *
     * @throws JSONException
     */
    @Test
    public void testCounters() throws JSONException {

        fillCounters();

        //  Read the metrics from the REST API for the test data
        final ClientResource client = new ClientResource(getBaseRestMetricsUrl());

        final JSONObject metrics = getJSONObject(client);
        assertThat(metrics.length(), is(equalTo(5)));

        //  There should be 3 counters
        final JSONArray counters = metrics.getJSONArray("counters");
        assertThat(counters, is(notNullValue()));
        assertThat(counters.length(), is(3));

        //  There should be no timers, gauges, meters or histograms
        checkEmptyLists(metrics, "timers", "gauges", "meters", "histograms");

        //  Check the values for counter 1
        final JSONObject counter1Container = counters.getJSONObject(0);
        checkCounter(counter1Container, COUNTER1_FULL_NAME, COUNTER1_COUNT);

        //  Check the values for counter 1
        final JSONObject counter2Container = counters.getJSONObject(1);
        checkCounter(counter2Container, COUNTER2_FULL_NAME, COUNTER2_COUNT);

        //  Check the values for counter 1
        final JSONObject counter3Container = counters.getJSONObject(2);
        checkCounter(counter3Container, COUNTER3_FULL_NAME, COUNTER3_COUNT);

    }

}
