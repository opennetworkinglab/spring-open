package net.onrc.onos.api.rest;

import com.codahale.metrics.Meter;
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
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for REST APIs for Meter Metrics.
 */
@RunWith(PowerMockRunner.class)
public class TestRestMetricsMeters extends TestRestMetrics {

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

    //  Test data for Meters


    private static final OnosMetrics.MetricsComponent COMPONENT =
            OnosMetrics.registerComponent("MetricsUnitTest");
    private static final OnosMetrics.MetricsFeature FEATURE =
            COMPONENT.registerFeature("Meters");

    private static final String METER1_NAME = "METER1";
    private static final String METER1_FULL_NAME =
            OnosMetrics.generateName(COMPONENT, FEATURE, METER1_NAME);
    private static final int METER1_ITERATIONS = 1;

    private static final String METER2_NAME = "METER2";
    private static final String METER2_FULL_NAME =
            OnosMetrics.generateName(COMPONENT, FEATURE, METER2_NAME);
    private static final int METER2_ITERATIONS = 10;

    private static final String METER3_NAME = "METER3";
    private static final String METER3_FULL_NAME =
            OnosMetrics.generateName(COMPONENT, FEATURE, METER3_NAME);
    private static final int METER3_ITERATIONS = 100;

    private final Meter meter1 = new Meter(getMockClock());
    private final Meter meter2 = new Meter(getMockClock());
    private final Meter meter3 = new Meter(getMockClock());

    /**
     * Fill in test data for a given Meter.
     *
     * @param meter Meter object to fill in
     * @param iterations How many times to mark the meter
     */
    private void fillMeter(final Meter meter,
                           final long iterations) {
        for (int i = 0; i < iterations; i++) {
            meter.mark();
        }
    }

    /**
     * Fill in test data in the test Meters.
     */
    private void fillMeters() {
        fillMeter(meter1, METER1_ITERATIONS);
        fillMeter(meter2, METER2_ITERATIONS);
        fillMeter(meter3, METER3_ITERATIONS);

        OnosMetrics.registerMetric(COMPONENT,
                                   FEATURE,
                                   METER1_NAME,
                                   meter1);

        OnosMetrics.registerMetric(COMPONENT,
                                   FEATURE,
                                   METER2_NAME,
                                   meter2);

        OnosMetrics.registerMetric(COMPONENT,
                                   FEATURE,
                                   METER3_NAME,
                                   meter3);
    }

    /**
     * Check if the data in a Meter matches the expected values.
     *
     * @param meter the meter object that this JSON representation should match
     * @param meterContainer JSON object for the Meter
     * @param name name of the Meter
     * @param iterations count of marks that should be in the Meter
     * @throws JSONException if any of the JSON processing fails
     */
    private void checkMeter(final Meter meter,
                            final JSONObject meterContainer,
                            final String name,
                            final int iterations)
                 throws JSONException {
        final String meterName = meterContainer.getString("name");
        assertThat(meterName, is(notNullValue()));
        assertThat(meterName, is(equalTo(name)));

        final JSONObject meterObject = meterContainer.getJSONObject("meter");
        assertThat(meterObject, is(notNullValue()));

        final int meterCount = meterObject.getInt("count");
        assertThat(meterCount, is(equalTo(iterations)));

        final double m15Rate = meterObject.getDouble("m15_rate");
        assertThat(m15Rate, is(equalTo(meter.getFifteenMinuteRate())));

        final double m5Rate = meterObject.getDouble("m5_rate");
        assertThat(m5Rate, is(equalTo(meter.getFiveMinuteRate())));

        final double m1Rate = meterObject.getDouble("m1_rate");
        assertThat(m1Rate, is(equalTo(meter.getOneMinuteRate())));

        // mean should be between 0.0 and the longest rate.  Since all the rates
        // are the same because of the mocked clock, use the 15 minute rate as
        // the max - all the rates should be the same.
        final double meanRate = meterObject.getDouble("mean_rate");
        assertThat(meanRate,
                is(both(greaterThanOrEqualTo(0.0)).
                        and(lessThanOrEqualTo(m15Rate))));

        final String units = meterObject.getString("units");
        assertThat(units, is(equalTo("events/second")));
    }

    /**
     * UNIT test for the Metrics REST API for Meters.
     *
     * @throws JSONException if any JSON processing fails
     */
    @Test
    public void testMeters() throws JSONException {

        fillMeters();

        //  Read the metrics from the REST API for the test data
        final ClientResource client = new ClientResource(getBaseRestMetricsUrl());

        final JSONObject metrics = getJSONObject(client);
        assertThat(metrics.length(), is(equalTo(5)));

        //  There should be 3 meters
        final JSONArray meters = metrics.getJSONArray("meters");
        assertThat(meters, is(notNullValue()));
        assertThat(meters.length(), is(3));

        //  There should be no timers, gauges, histograms or counters
        checkEmptyLists(metrics, "timers", "gauges", "histograms", "counters");

        //  Check the values for meter 1
        final JSONObject meter1Container = meters.getJSONObject(0);
        checkMeter(meter1, meter1Container, METER1_FULL_NAME, METER1_ITERATIONS);

        //  Check the values for meter 2
        final JSONObject meter2Container = meters.getJSONObject(1);
        checkMeter(meter2, meter2Container, METER2_FULL_NAME, METER2_ITERATIONS);

        //  Check the values for meter 3
        final JSONObject meter3Container = meters.getJSONObject(2);
        checkMeter(meter3, meter3Container, METER3_FULL_NAME, METER3_ITERATIONS);

    }

}

