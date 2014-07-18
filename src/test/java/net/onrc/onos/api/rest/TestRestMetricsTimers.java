package net.onrc.onos.api.rest;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.SlidingWindowReservoir;
import com.codahale.metrics.Timer;
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
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for REST APIs for Timer Metrics.
 */
@RunWith(PowerMockRunner.class)
public class TestRestMetricsTimers extends TestRestMetrics {

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

    //  Test data objects for Timers

    private static final OnosMetrics.MetricsComponent COMPONENT =
            OnosMetrics.registerComponent("MetricsUnitTests");
    private static final OnosMetrics.MetricsFeature FEATURE =
            COMPONENT.registerFeature("Timers");

    //  timer1 will be called 3 times
    private static final String TIMER1_NAME = "timer1";
    private static final String TIMER1_FULL_NAME =
            OnosMetrics.generateName(COMPONENT,
                                     FEATURE,
                                     TIMER1_NAME);
    private static final int TIMER1_COUNT = 3;

    //  timer2 will be called 10 times
    private static final String TIMER2_NAME = "timer2";
    private static final String TIMER2_FULL_NAME =
            OnosMetrics.generateName(COMPONENT,
                                     FEATURE,
                                     TIMER2_NAME);
    private static final int TIMER2_COUNT = 10;

    private static final int RESERVOIR_SIZE = 100;
    private Reservoir reservoir = new SlidingWindowReservoir(RESERVOIR_SIZE);

    private final Timer timer1 = new Timer(reservoir, getMockClock());
    private final Timer timer2 = new Timer(reservoir, getMockClock());

    /**
     * Fill in test data in the Timer objects.
     */
    private void fillTimers() {
        // The mock clock will simulate ticks at MOCK_CLOCK_MILISECONDS_PER_TICK
        // Each timer.time(), context.stop() pair will elapse
        // MOCK_CLOCK_MILISECONDS_PER_TICK (currently 50 milliseconds)

        // Initialize timer1 - 3 ticks, 50 milliseconds (simulted) apart
        for (int i = 0; i < TIMER1_COUNT; i++) {
            final Timer.Context context = timer1.time();
            context.stop();
        }

        // Initialize timer2 - 10 ticks, 50 milliseconds (simulated) apart
        for (int i = 0; i < TIMER2_COUNT; i++) {
            final Timer.Context context = timer2.time();
            context.stop();
        }

        // add the two timers to the registry so the REST APIs will pick them
        // up

        OnosMetrics.registerMetric(COMPONENT,
                                   FEATURE,
                                   TIMER1_NAME,
                                   timer1);

        OnosMetrics.registerMetric(COMPONENT,
                                   FEATURE,
                                   TIMER2_NAME,
                                   timer2);
    }

    /**
     * Check that the time values for a JSON Timer object are within the
     * allowed range.
     *
     * @param timer JSON Timer object
     * @param timeValue expected time value
     * @throws JSONException if any of the JSON operations fail
     */
    private void assertThatTimesAreInRange(JSONObject timer, int timeValue)
                 throws JSONException {

        final double timerMax = timer.getDouble("max");
        assertThat((int) timerMax,
                is(both(greaterThanOrEqualTo(timeValue)).
                        and(lessThan(timeValue + 5))));

        final double timerMin = timer.getDouble("min");
        assertThat((int) timerMin,
                is(both(greaterThanOrEqualTo(timeValue)).
                        and(lessThan(timeValue + 5))));

        final double timerP99 = timer.getDouble("p99");
        assertThat((int) timerP99,
                is(both(greaterThanOrEqualTo(timeValue)).
                        and(lessThan(timeValue + 5))));

        final double timerP999 = timer.getDouble("p999");
        assertThat((int) timerP999,
                is(both(greaterThanOrEqualTo(timeValue)).
                        and(lessThan(timeValue + 5))));
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
     * Unit test for the REST APIs for Metrics Timers.
     *
     * @throws JSONException if any of the JSON processing fails.
     */
    @Test
    public void testTimers() throws JSONException {
        //  Make some test data
        fillTimers();

        //  Read the metrics from the REST API for the test data
        final ClientResource client = new ClientResource(getBaseRestMetricsUrl());

        final JSONObject metrics = getJSONObject(client);
        assertThat(metrics.length(), is(equalTo(5)));

        //  There should be 2 timers
        final JSONArray timers = metrics.getJSONArray("timers");
        assertThat(timers, is(notNullValue()));
        assertThat(timers.length(), is(2));


        //  There should be no historgramss, gauges, meters or counters
        checkEmptyLists(metrics, "histograms", "gauges", "meters", "counters");

        //  Check the values for timer 1
        final JSONObject timer1Values = timers.getJSONObject(0);
        assertThat(timer1Values, is(notNullValue()));

        final String timer1Name = timer1Values.getString("name");
        assertThat(timer1Name, is(notNullValue()));
        assertThat(timer1Name, is(equalTo(TIMER1_FULL_NAME)));

        final JSONObject timer1TimerObject = timer1Values.getJSONObject("timer");
        assertThat(timer1TimerObject, is(notNullValue()));

        final int timer1Count = timer1TimerObject.getInt("count");
        assertThat(timer1Count, is(equalTo(TIMER1_COUNT)));

        final String timer1DurationUnits = timer1TimerObject.getString("duration_units");
        assertThat(timer1DurationUnits, is(equalTo("milliseconds")));

        assertThatTimesAreInRange(timer1TimerObject,
                                  MOCK_CLOCK_MILISECONDS_PER_TICK);

        //  Check the values for timer 2
        final JSONObject timer2Values = timers.getJSONObject(1);
        assertThat(timer2Values, is(notNullValue()));

        final String timer2Name = timer2Values.getString("name");
        assertThat(timer2Name, is(notNullValue()));
        assertThat(timer2Name, is(equalTo(TIMER2_FULL_NAME)));

        final JSONObject timer2TimerObject = timer2Values.getJSONObject("timer");
        assertThat(timer2TimerObject, is(notNullValue()));

        final int timer2Count = timer2TimerObject.getInt("count");
        assertThat(timer2Count, is(equalTo(TIMER2_COUNT)));

        final String timer2DurationUnits = timer2TimerObject.getString("duration_units");
        assertThat(timer2DurationUnits, is(equalTo("milliseconds")));

        assertThatTimesAreInRange(timer2TimerObject,
                                  MOCK_CLOCK_MILISECONDS_PER_TICK);
    }

}
