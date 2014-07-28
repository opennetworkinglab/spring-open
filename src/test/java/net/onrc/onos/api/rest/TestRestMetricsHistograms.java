package net.onrc.onos.api.rest;

import com.codahale.metrics.Histogram;
import net.onrc.onos.core.metrics.OnosMetrics;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.restlet.resource.ClientResource;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for REST APIs for Histogram Metrics.
 */
public class TestRestMetricsHistograms extends TestRestMetrics {

    // Test data for Histograms

    private static final OnosMetrics.MetricsComponent COMPONENT =
            OnosMetrics.registerComponent("MetricsUnitTests");
    private static final OnosMetrics.MetricsFeature FEATURE =
            COMPONENT.registerFeature("Histograms");

    private static final String HISTOGRAM1_NAME = "HISTOGRAM1";
    private static final String HISTOGRAM1_FULL_NAME =
            OnosMetrics.generateName(COMPONENT, FEATURE, HISTOGRAM1_NAME);

    private static final int[] HISTOGRAM1_VALUES =
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    private static final String HISTOGRAM2_NAME = "HISTOGRAM2";
    private static final String HISTOGRAM2_FULL_NAME =
            OnosMetrics.generateName(COMPONENT, FEATURE, HISTOGRAM2_NAME);
    private static final int[] HISTOGRAM2_VALUES =
            {100, 100, 100, 100, 100, 100, 100};

    private static final String HISTOGRAM3_NAME = "HISTOGRAM3";
    private static final String HISTOGRAM3_FULL_NAME =
            OnosMetrics.generateName(COMPONENT, FEATURE, HISTOGRAM3_NAME);
    private static final int[] HISTOGRAM3_VALUES =
            {555};

    private final Histogram histogram1 =
            OnosMetrics.createHistogram(
                    COMPONENT,
                    FEATURE,
                    HISTOGRAM1_NAME);

    private final Histogram histogram2 =
            OnosMetrics.createHistogram(
                    COMPONENT,
                    FEATURE,
                    HISTOGRAM2_NAME);

    private final Histogram histogram3 =
            OnosMetrics.createHistogram(
                    COMPONENT,
                    FEATURE,
                    HISTOGRAM3_NAME);

    /**
     * Add each int in an array to a Histogram.
     *
     * @param histogram Histogram object to update
     * @param values list of values to add to the Histogram
     */
    private void updateHistogramFromArray(final Histogram histogram,
                                          final int[] values) {
        for (final int value : values) {
            histogram.update(value);
        }
    }

    /**
     * Initialize all the Histograms.
     */
    private void fillHistograms() {
        updateHistogramFromArray(histogram1, HISTOGRAM1_VALUES);
        updateHistogramFromArray(histogram2, HISTOGRAM2_VALUES);
        updateHistogramFromArray(histogram3, HISTOGRAM3_VALUES);
    }

    /**
     * Check that a JSON object representing a histogram contains the correct
     * data.
     *
     * @param histogramContainer JSON object for the Histogram
     * @param name the name of the Histogram
     * @param values the array of expected values in the histogram
     * @throws JSONException if any of the JSON processing generates an error
     */
    private void checkHistogram(final JSONObject histogramContainer,
                              final String name,
                              final int[] values) throws JSONException {

        // Check that the name is correct
        final String histogramName = histogramContainer.getString("name");
        assertThat(histogramName, is(notNullValue()));
        assertThat(histogramName, is(equalTo(name)));

        //  Make sure a histogram is present
        final JSONObject histogramObject = histogramContainer.getJSONObject("histogram");
        assertThat(histogramObject, is(notNullValue()));

        // The histogram count should equal the length of the array used to
        // initialize it.
        final int histogramCount = histogramObject.getInt("count");
        assertThat(histogramCount, is(equalTo(values.length)));

        final int[] sortedValues = Arrays.copyOf(values, values.length);
        Arrays.sort(sortedValues);

        // max should be the largest value from the array
        final int max = histogramObject.getInt("max");
        assertThat(max, is(equalTo(sortedValues[sortedValues.length - 1])));

        // min should be the smallest value from the array
        final int min = histogramObject.getInt("min");
        assertThat(min, is(equalTo(sortedValues[0])));

        // Each of the probability values should be between the min and the max
        // value
        final double p999 = histogramObject.getDouble("p999");
        assertThat((int) p999,
                is(both(greaterThanOrEqualTo(min)).and(lessThanOrEqualTo(max))));

        final double p99 = histogramObject.getDouble("p99");
        assertThat((int) p99,
                is(both(greaterThanOrEqualTo(min)).and(lessThanOrEqualTo(max))));

        final double p98 = histogramObject.getDouble("p98");
        assertThat((int) p98,
                is(both(greaterThanOrEqualTo(min)).and(lessThanOrEqualTo(max))));

        final double p95 = histogramObject.getDouble("p95");
        assertThat((int) p95,
                is(both(greaterThanOrEqualTo(min)).and(lessThanOrEqualTo(max))));

        final double p75 = histogramObject.getDouble("p75");
        assertThat((int) p75,
                is(both(greaterThanOrEqualTo(min)).and(lessThanOrEqualTo(max))));

        final double p50 = histogramObject.getDouble("p50");
        assertThat((int) p50,
                is(both(greaterThanOrEqualTo(min)).and(lessThanOrEqualTo(max))));

    }

    /**
     * Unit test for REST APIs for Histogram Metrics.
     *
     * @throws JSONException if any JSON processing causes an error
     */
    @Test
    public void testHistograms() throws JSONException {

        fillHistograms();

        //  Read the metrics from the REST API for the test data
        final ClientResource client = new ClientResource(getBaseRestMetricsUrl());

        final JSONObject metrics = getJSONObject(client);
        assertThat(metrics.length(), is(equalTo(5)));

        //  There should be 3 histograms
        final JSONArray histograms = metrics.getJSONArray("histograms");
        assertThat(histograms, is(notNullValue()));
        assertThat(histograms.length(), is(3));

        //  There should be no timers, gauges, meters or counters
        checkEmptyLists(metrics, "timers", "gauges", "meters", "counters");

        //  Check the values for histogram 1
        final JSONObject histogram1Container = histograms.getJSONObject(0);
        checkHistogram(histogram1Container, HISTOGRAM1_FULL_NAME, HISTOGRAM1_VALUES);

        //  Check the values for histogram 2
        final JSONObject histogram2Container = histograms.getJSONObject(1);
        checkHistogram(histogram2Container, HISTOGRAM2_FULL_NAME, HISTOGRAM2_VALUES);

        //  Check the values for histogram 3
        final JSONObject histogram3Container = histograms.getJSONObject(2);
        checkHistogram(histogram3Container, HISTOGRAM3_FULL_NAME, HISTOGRAM3_VALUES);

    }

}

