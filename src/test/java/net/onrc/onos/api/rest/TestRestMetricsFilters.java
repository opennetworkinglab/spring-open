package net.onrc.onos.api.rest;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricFilter;
import net.onrc.onos.core.metrics.OnosMetrics;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import org.restlet.resource.ClientResource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for filtering REST APIs for Timer Metrics.
 */

@RunWith(PowerMockRunner.class)
public class TestRestMetricsFilters extends TestRestMetrics {

    /**
     * Create the web server and mocks required for
     * all of the tests.
     */
    @Before
    @SuppressWarnings("ununsed")
    public void beforeTest() {
        setRestPort(generateRandomPort());
        setUp();
        //  Make some test data
        createMetrics();
    }

    /**
     * Remove anything that will interfere with the next test running correctly.
     * Shuts down the test REST web server and removes the mocks.
     */
    @After
    @SuppressWarnings("unused")
    public void afterTest() {
        destroyMetrics();
        tearDown();
    }

    //  Test data objects
    private static final OnosMetrics.MetricsComponent COMPONENT =
            OnosMetrics.registerComponent("MetricsUnitTests");
    private static final OnosMetrics.MetricsFeature FEATURE =
            COMPONENT.registerFeature("Filters");

    private static final String TIMER1_NAME = "timer1";
    private static final String TIMER1_FULL_NAME = OnosMetrics.generateName(COMPONENT,
            FEATURE,
            "timer1");
    private static final String TIMER2_NAME = "timer2";
    private static final String TIMER2_FULL_NAME = OnosMetrics.generateName(COMPONENT,
            FEATURE,
            "timer2");
    private static final String TIMER3_NAME = "timer3";
    private static final String TIMER3_FULL_NAME = OnosMetrics.generateName(COMPONENT,
            FEATURE,
            "timer3");

    private static final String GAUGE1_NAME = "gauge1";
    private static final String GAUGE1_FULL_NAME = OnosMetrics.generateName(COMPONENT,
            FEATURE,
            "gauge1");
    private static final String GAUGE2_NAME = "gauge2";
    private static final String GAUGE2_FULL_NAME = OnosMetrics.generateName(COMPONENT,
            FEATURE,
            "gauge2");
    private static final String GAUGE3_NAME = "gauge3";
    private static final String GAUGE3_FULL_NAME = OnosMetrics.generateName(COMPONENT,
            FEATURE,
            "gauge3");

    private static final String COUNTER1_NAME = "counter1";
    private static final String COUNTER1_FULL_NAME = OnosMetrics.generateName(COMPONENT,
            FEATURE,
            "counter1");
    private static final String COUNTER2_NAME = "counter2";
    private static final String COUNTER2_FULL_NAME = OnosMetrics.generateName(COMPONENT,
            FEATURE,
            "counter2");
    private static final String COUNTER3_NAME = "counter3";
    private static final String COUNTER3_FULL_NAME = OnosMetrics.generateName(COMPONENT,
            FEATURE,
            "counter3");

    private static final String METER1_NAME = "meter1";
    private static final String METER1_FULL_NAME = OnosMetrics.generateName(COMPONENT,
            FEATURE,
            "meter1");
    private static final String METER2_NAME = "meter2";
    private static final String METER2_FULL_NAME = OnosMetrics.generateName(COMPONENT,
            FEATURE,
            "meter2");
    private static final String METER3_NAME = "meter3";
    private static final String METER3_FULL_NAME = OnosMetrics.generateName(COMPONENT,
            FEATURE,
            "meter3");

    private static final String HISTOGRAM1_NAME = "histogram1";
    private static final String HISTOGRAM1_FULL_NAME = OnosMetrics.generateName(COMPONENT,
            FEATURE,
            "histogram1");
    private static final String HISTOGRAM2_NAME = "histogram2";
    private static final String HISTOGRAM2_FULL_NAME = OnosMetrics.generateName(COMPONENT,
            FEATURE,
            "histogram2");
    private static final String HISTOGRAM3_NAME = "histogram3";
    private static final String HISTOGRAM3_FULL_NAME = OnosMetrics.generateName(COMPONENT,
            FEATURE,
            "histogram3");

    private final Gauge<Integer> testGauge = new Gauge<Integer>() {
        @Override
        public Integer getValue() {
            return 1;
        }
    };

    /**
     * Creates Metrics objects for test.
     */
    private void createMetrics() {
        OnosMetrics.createTimer(COMPONENT,
                FEATURE,
                TIMER1_NAME);
        OnosMetrics.createTimer(COMPONENT,
                FEATURE,
                TIMER2_NAME);
        OnosMetrics.createTimer(COMPONENT,
                FEATURE,
                TIMER3_NAME);

        OnosMetrics.createCounter(COMPONENT,
                FEATURE,
                COUNTER1_NAME);
        OnosMetrics.createCounter(COMPONENT,
                FEATURE,
                COUNTER2_NAME);
        OnosMetrics.createCounter(COMPONENT,
                FEATURE,
                COUNTER3_NAME);

        OnosMetrics.createMeter(COMPONENT,
                FEATURE,
                METER1_NAME);
        OnosMetrics.createMeter(COMPONENT,
                FEATURE,
                METER2_NAME);
        OnosMetrics.createMeter(COMPONENT,
                FEATURE,
                METER3_NAME);

        OnosMetrics.createHistogram(COMPONENT,
                FEATURE,
                HISTOGRAM1_NAME);
        OnosMetrics.createHistogram(COMPONENT,
                FEATURE,
                HISTOGRAM2_NAME);
        OnosMetrics.createHistogram(COMPONENT,
                FEATURE,
                HISTOGRAM3_NAME);

        OnosMetrics.registerMetric(COMPONENT,
                FEATURE,
                GAUGE1_NAME,
                testGauge);
        OnosMetrics.registerMetric(COMPONENT,
                FEATURE,
                GAUGE2_NAME,
                testGauge);
        OnosMetrics.registerMetric(COMPONENT,
                FEATURE,
                GAUGE3_NAME,
                testGauge);
    }

    /**
     * Removes the Metrics to clean up for the next test run.
     */
    private void destroyMetrics() {
        OnosMetrics.removeMatching(MetricFilter.ALL);
    }

    /**
     * Tests that query of non existant name returns nothing.
     *
     * @throws JSONException if any of the JSON processing fails.
     */
    @Test
    public void testFilterMatchesNothing() throws JSONException {

        //  Read the metrics from the REST API for the test data
        final ClientResource client = new ClientResource(getBaseRestMetricsUrl());
        client.addQueryParameter("ids", "xyzzy");

        final JSONObject metrics = getJSONObject(client);
        assertThat(metrics.length(), is(equalTo(5)));

        //  There should be no timers, histograms, gauges, meters or counters
        checkEmptyLists(metrics, "timers", "histograms", "gauges", "meters", "counters");

    }

    /**
     * Tests that query of multiple metrics of a single Metric type
     * returns the proper data.
     *
     * @throws JSONException if any of the JSON processing fails.
     */
    @Test
    public void testMultipleFilterSingleType() throws JSONException {

        //  Read the metrics from the REST API for the test data
        final ClientResource client = new ClientResource(getBaseRestMetricsUrl());
        client.addQueryParameter("ids", TIMER1_FULL_NAME + "," + TIMER2_FULL_NAME);

        final JSONObject metrics = getJSONObject(client);
        assertThat(metrics.length(), is(equalTo(5)));

        //  There should be 2 timer that match the filter
        final JSONArray timers = metrics.getJSONArray("timers");
        assertThat(timers, is(notNullValue()));
        assertThat(timers.length(), is(2));

        final JSONObject jsonTimer1 = timers.getJSONObject(0);
        assertThat(jsonTimer1.getString("name"), is(equalTo(TIMER1_FULL_NAME)));

        final JSONObject jsonTimer2 = timers.getJSONObject(1);
        assertThat(jsonTimer2.getString("name"), is(equalTo(TIMER2_FULL_NAME)));

        //  There should be no histograms, gauges, meters or counters
        checkEmptyLists(metrics, "histograms", "gauges", "meters", "counters");
    }

    /**
     * Tests that query of a single metric retunrs just that metric.
     *
     * @throws JSONException if any of the JSON processing fails.
     */
    @Test
    public void testSingleFilter() throws JSONException {

        //  Read the metrics from the REST API for the test data
        final ClientResource client = new ClientResource(getBaseRestMetricsUrl());
        client.addQueryParameter("ids", TIMER1_FULL_NAME);

        final JSONObject metrics = getJSONObject(client);
        assertThat(metrics.length(), is(equalTo(5)));

        //  There should be 1 timer that matches the filter
        final JSONArray timers = metrics.getJSONArray("timers");
        assertThat(timers, is(notNullValue()));
        assertThat(timers.length(), is(1));

        final JSONObject jsonTimer1 = timers.getJSONObject(0);
        assertThat(jsonTimer1.getString("name"), is(equalTo(TIMER1_FULL_NAME)));


        //  There should be no histograms, gauges, meters or counters
        checkEmptyLists(metrics, "histograms", "gauges", "meters", "counters");
    }

    /**
     * Tests that query of multiple metrics of multiple metric types returns
     * the proper data.
     *
     * @throws JSONException if any of the JSON processing fails.
     */
    @Test
    public void testMultipleFiltersMultipleTypes() throws JSONException {

        //  Read the metrics from the REST API for the test data
        final ClientResource client = new ClientResource(getBaseRestMetricsUrl());
        client.addQueryParameter("ids",
                TIMER1_FULL_NAME + "," +
                GAUGE2_FULL_NAME + "," +
                HISTOGRAM3_FULL_NAME);

        final JSONObject metrics = getJSONObject(client);
        assertThat(metrics.length(), is(equalTo(5)));

        //  There should be 1 timer that matches the filter
        final JSONArray timers = metrics.getJSONArray("timers");
        assertThat(timers, is(notNullValue()));
        assertThat(timers.length(), is(1));

        final JSONObject jsonTimer1 = timers.getJSONObject(0);
        assertThat(jsonTimer1.getString("name"), is(equalTo(TIMER1_FULL_NAME)));

        //  There should be 1 gauge that matches the filter
        final JSONArray gauges = metrics.getJSONArray("gauges");
        assertThat(gauges, is(notNullValue()));
        assertThat(gauges.length(), is(1));

        final JSONObject jsonGauge1 = gauges.getJSONObject(0);
        assertThat(jsonGauge1.getString("name"), is(equalTo(GAUGE2_FULL_NAME)));

        //  There should be 1 histogram that matches the filter
        final JSONArray histograms = metrics.getJSONArray("histograms");
        assertThat(histograms, is(notNullValue()));
        assertThat(histograms.length(), is(1));

        final JSONObject jsonHistogram1 = histograms.getJSONObject(0);
        assertThat(jsonHistogram1.getString("name"), is(equalTo(HISTOGRAM3_FULL_NAME)));

        //  There should be no meters or counters
        checkEmptyLists(metrics, "meters", "counters");
    }


}
