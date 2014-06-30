package net.onrc.onos.api.rest;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
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
 * Unit tests for REST APIs for Gauges Metrics.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PathCalcRuntimeModule.class)
public class TestRestMetricsGauges extends TestRestMetrics {

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

    // Test data for Gauges
    private static final String GAUGE1_NAME = "gauge1";
    private static final int GAUGE1_VALUE = 0;

    private static final String GAUGE2_NAME = "gauge2";
    private static final int GAUGE2_VALUE = -1;

    private static final String GAUGE3_NAME = "gauge3";
    private static final int GAUGE3_VALUE = 123456789;

    private final Gauge<Integer> gauge1 = OnosMetrics.getMetricsRegistry().
            register(MetricRegistry.name(GAUGE1_NAME),
                    new Gauge<Integer>() {
                        @Override
                        public Integer getValue() {
                            return GAUGE1_VALUE;
                        }
                    });

    private final Gauge<Integer> gauge2 = OnosMetrics.getMetricsRegistry().
            register(MetricRegistry.name(GAUGE2_NAME),
                    new Gauge<Integer>() {
                        @Override
                        public Integer getValue() {
                            return GAUGE2_VALUE;
                        }
                    });

    private final Gauge<Integer> gauge3 = OnosMetrics.getMetricsRegistry().
            register(MetricRegistry.name(GAUGE3_NAME),
                    new Gauge<Integer>() {
                        @Override
                        public Integer getValue() {
                            return GAUGE3_VALUE;
                        }
                    });

    /**
     * Check that the JSON for a Gauge obect has the correct data values.
     *
     * @param gaugeContainer JSON object for the Gauge
     * @param name expected name of the gauge
     * @param gauge Metrics Gauge object that hold the expected value
     * @throws JSONException if any JSON operation fails
     */
    private void checkGauge(final JSONObject gaugeContainer,
                            final String name,
                            final Gauge<Integer> gauge)
                 throws JSONException {
        assertThat(gaugeContainer, is(notNullValue()));

        final String gaugeName = gaugeContainer.getString("name");
        assertThat(gaugeName, is(notNullValue()));
        assertThat(gaugeName, is(equalTo(name)));

        final JSONObject gaugeObject = gaugeContainer.getJSONObject("gauge");
        assertThat(gaugeObject, is(notNullValue()));

        final int gaugeValue = gaugeObject.getInt("value");
        assertThat(gaugeValue, is(equalTo(gauge.getValue())));
    }

    /**
     * Unit test for the Gauges portion of the Metrics REST API.
     *
     * @throws JSONException if any JSON operation fails
     */
    @Test
    public void testGauges() throws Exception {

        //  Read the metrics from the REST API for the test data
        final ClientResource client = new ClientResource(getBaseRestMetricsUrl());

        final JSONObject metrics = getJSONObject(client);
        assertThat(metrics.length(), is(equalTo(5)));

        //  There should be 3 gauges
        final JSONArray gauges = metrics.getJSONArray("gauges");
        assertThat(gauges, is(notNullValue()));
        assertThat(gauges.length(), is(3));

        //  There should be no timers, meters, histograms or counters
        checkEmptyLists(metrics, "timers", "meters", "histograms", "counters");

        //  Check the values for gauge 1
        final JSONObject gauge1Container = gauges.getJSONObject(0);
        checkGauge(gauge1Container, GAUGE1_NAME, gauge1);

        //  Check the values for gauge 2
        final JSONObject gauge2Container = gauges.getJSONObject(1);
        checkGauge(gauge2Container, GAUGE2_NAME, gauge2);

        //  Check the values for gauge 3
        final JSONObject gauge3Container = gauges.getJSONObject(2);
        checkGauge(gauge3Container, GAUGE3_NAME, gauge3);
    }

}
