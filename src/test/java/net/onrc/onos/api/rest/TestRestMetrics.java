package net.onrc.onos.api.rest;

import com.codahale.metrics.Clock;
import net.onrc.onos.core.metrics.web.MetricsWebRoutable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Test harness for Metrics based REST API tests.  This class maintains the
 * web server and mocks required for testing metrics APIs.  REST API tests
 * for metrics should inherit from this class.
 */
public class TestRestMetrics extends TestRest {
    /**
     * Creates the web server and mocks required for the metrics tests.
     */
    @Override
    public void setUp() {
        addRestlet(new MetricsWebRoutable());
        super.setUp();
    }

    /**
     * Removes anything that will interfere with the next test running
     * correctly.
     * Shuts down the test REST web server and removes the mocks.
     */
    @Override
    public void tearDown() {
        super.tearDown();
    }

    /**
     * Fetches the base URL for Metrics REST APIs.
     *
     * @return base URL
     */
    String getBaseRestMetricsUrl() {
        return getBaseRestUrl() + "/metrics";
    }

    /**
     * Checks that the given list of elements in a JSON object are all 0 length
     * arrays.
     *
     * @param elementNames names of top level elements to check
     * @param jsonObject top level JSON object
     * @throws JSONException if JSON fetching throws an error
     */
    public void checkEmptyLists(final JSONObject jsonObject,
                                final String ... elementNames)
                throws JSONException {
        for (final String elementName : elementNames) {
            final JSONArray element = jsonObject.getJSONArray(elementName);
            assertThat(element, is(notNullValue()));
            assertThat(element.length(), is(0));
        }
    }

    /**
     * Tick time of the mock clock, in milliseconds.
     */
    public static final int MOCK_CLOCK_MILISECONDS_PER_TICK = 50;

    /**
     * Mock clock used for Timer and Meter tests to give known time values for
     * test data.  Each simulated tick increments the time by
     * MOCK_CLOCK_MILISECONDS_PER_TICK which is currently defined for 50
     * millisecond ticks.
     */
    private final Clock mockClock = new Clock() {
        private long currentTime = 0;

        @Override
        public long getTick() {
            final long tickInNanoseconds =
                    TimeUnit.NANOSECONDS.convert(MOCK_CLOCK_MILISECONDS_PER_TICK,
                                                 TimeUnit.MILLISECONDS);
            currentTime = currentTime + tickInNanoseconds;
            return currentTime;
        }
    };

    /**
     * Fetches a mock clock that can be used for Timers and Meters.
     *
     * @return Mock clock base on 50 msec ticks
     */
    public Clock getMockClock() {
        return mockClock;
    }
}
