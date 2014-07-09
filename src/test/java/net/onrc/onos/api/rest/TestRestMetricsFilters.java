package net.onrc.onos.api.rest;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricFilter;
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

import static com.codahale.metrics.MetricRegistry.name;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for filtering REST APIs for Timer Metrics.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(PathCalcRuntimeModule.class)
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
    private static final String TIMER1_NAME = name("timer1");
    private static final String TIMER2_NAME = name("timer2");
    private static final String TIMER3_NAME = name("timer3");

    private static final String GAUGE1_NAME = name("gauge1");
    private static final String GAUGE2_NAME = name("gauge2");
    private static final String GAUGE3_NAME = name("gauge3");

    private static final String COUNTER1_NAME = name("counter1");
    private static final String COUNTER2_NAME = name("counter2");
    private static final String COUNTER3_NAME = name("counter3");

    private static final String METER1_NAME = name("meter1");
    private static final String METER2_NAME = name("meter2");
    private static final String METER3_NAME = name("meter3");

    private static final String HISTOGRAM1_NAME = name("histogram1");
    private static final String HISTOGRAM2_NAME = name("histogram2");
    private static final String HISTOGRAM3_NAME = name("histogram3");

    final Gauge<Integer> testGauge = new Gauge<Integer>() {
        @Override
        public Integer getValue() {
            return 1;
        }
    };

    /**
     * Creates Metrics objects for test.
     */
    private void createMetrics() {
        OnosMetrics.getMetricsRegistry().timer(TIMER1_NAME);
        OnosMetrics.getMetricsRegistry().timer(TIMER2_NAME);
        OnosMetrics.getMetricsRegistry().timer(TIMER3_NAME);

        OnosMetrics.getMetricsRegistry().counter(COUNTER1_NAME);
        OnosMetrics.getMetricsRegistry().counter(COUNTER2_NAME);
        OnosMetrics.getMetricsRegistry().counter(COUNTER3_NAME);

        OnosMetrics.getMetricsRegistry().meter(METER1_NAME);
        OnosMetrics.getMetricsRegistry().meter(METER2_NAME);
        OnosMetrics.getMetricsRegistry().meter(METER3_NAME);

        OnosMetrics.getMetricsRegistry().histogram(HISTOGRAM1_NAME);
        OnosMetrics.getMetricsRegistry().histogram(HISTOGRAM2_NAME);
        OnosMetrics.getMetricsRegistry().histogram(HISTOGRAM3_NAME);

        OnosMetrics.getMetricsRegistry().register(GAUGE1_NAME, testGauge);
        OnosMetrics.getMetricsRegistry().register(GAUGE2_NAME, testGauge);
        OnosMetrics.getMetricsRegistry().register(GAUGE3_NAME, testGauge);
    }

    /**
     * Removes the Metrics to clean up for the next test run.
     */
    private void destroyMetrics() {
        OnosMetrics.getMetricsRegistry().removeMatching(MetricFilter.ALL);
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
        client.addQueryParameter("ids", "timer1,timer2");

        final JSONObject metrics = getJSONObject(client);
        assertThat(metrics.length(), is(equalTo(5)));

        //  There should be 2 timer that match the filter
        final JSONArray timers = metrics.getJSONArray("timers");
        assertThat(timers, is(notNullValue()));
        assertThat(timers.length(), is(2));

        final JSONObject jsonTimer1 = timers.getJSONObject(0);
        assertThat(jsonTimer1.getString("name"), is(equalTo("timer1")));

        final JSONObject jsonTimer2 = timers.getJSONObject(1);
        assertThat(jsonTimer2.getString("name"), is(equalTo("timer2")));

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
        client.addQueryParameter("ids", "timer1");

        final JSONObject metrics = getJSONObject(client);
        assertThat(metrics.length(), is(equalTo(5)));

        //  There should be 1 timer that matches the filter
        final JSONArray timers = metrics.getJSONArray("timers");
        assertThat(timers, is(notNullValue()));
        assertThat(timers.length(), is(1));

        final JSONObject jsonTimer1 = timers.getJSONObject(0);
        assertThat(jsonTimer1.getString("name"), is(equalTo("timer1")));


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
        client.addQueryParameter("ids", "timer1,gauge2,histogram3");

        final JSONObject metrics = getJSONObject(client);
        assertThat(metrics.length(), is(equalTo(5)));

        //  There should be 1 timer that matches the filter
        final JSONArray timers = metrics.getJSONArray("timers");
        assertThat(timers, is(notNullValue()));
        assertThat(timers.length(), is(1));

        final JSONObject jsonTimer1 = timers.getJSONObject(0);
        assertThat(jsonTimer1.getString("name"), is(equalTo("timer1")));

        //  There should be 1 gauge that matches the filter
        final JSONArray gauges = metrics.getJSONArray("gauges");
        assertThat(gauges, is(notNullValue()));
        assertThat(gauges.length(), is(1));

        final JSONObject jsonGauge1 = gauges.getJSONObject(0);
        assertThat(jsonGauge1.getString("name"), is(equalTo("gauge2")));

        //  There should be 1 histogram that matches the filter
        final JSONArray histograms = metrics.getJSONArray("histograms");
        assertThat(histograms, is(notNullValue()));
        assertThat(histograms.length(), is(1));

        final JSONObject jsonHistogram1 = histograms.getJSONObject(0);
        assertThat(jsonHistogram1.getString("name"), is(equalTo("histogram3")));

        //  There should be no meters or counters
        checkEmptyLists(metrics, "meters", "counters");
    }


}
