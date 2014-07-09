package net.onrc.onos.core.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * REST APIs for Metrics objects.
 */
public class MetricsResource extends ServerResource {

    /**
     * Metric filter to allow selecting metrics by name.
     */
    private static class MetricNameFilter implements MetricFilter {
        final HashSet<String> names;

        /**
         * Hide default constructor.
         */
        @SuppressWarnings("unused")
        private MetricNameFilter() {
            names = null;
        }

        /**
         * Initializes a filter for the given name list.
         *
         * @param nameListString comma separated list of strings of the
         *                       names of metrics to query
         */
        public MetricNameFilter(final String nameListString) {

            if (nameListString == null) {
                names = null;
            } else {
                List<String> nameList = Arrays.asList(nameListString.split(","));
                names = new HashSet<>();
                names.addAll(nameList);
            }
        }

        @Override
        public boolean matches(String s, Metric metric) {
            return names == null || names.contains(s);
        }
    }
    /**
     * REST API to get all of the system's metrics.
     *
     * @return a Representation object containing the metrics
     */
    @Get("json")
    @SuppressWarnings("rawtypes")
    public Representation retrieve() throws Exception {
        final MetricRegistry registry = OnosMetrics.getMetricsRegistry();
        final MetricsObjectResource result = new MetricsObjectResource();

        final List<MetricsObjectResource.TimerObjectResource> timers =
                new ArrayList<>();
        final List<MetricsObjectResource.GaugeObjectResource> gauges =
                new ArrayList<>();
        final List<MetricsObjectResource.CounterObjectResource> counters =
                new ArrayList<>();
        final List<MetricsObjectResource.MeterObjectResource> meters =
                new ArrayList<>();
        final List<MetricsObjectResource.HistogramObjectResource> histograms =
                new ArrayList<>();

        final String metricIdsString = getQuery().getValues("ids");

        final MetricFilter filter = new MetricNameFilter(metricIdsString);

        for (final Map.Entry<String, Timer> timer :
             registry.getTimers(filter).entrySet()) {
            timers.add(new MetricsObjectResource.TimerObjectResource(
                    timer.getKey(), timer.getValue()));
        }
        result.setTimers(timers);

        for (final Map.Entry<String, Gauge> gauge :
             registry.getGauges(filter).entrySet()) {
            gauges.add(new MetricsObjectResource.GaugeObjectResource(
                    gauge.getKey(), gauge.getValue()));
        }
        result.setGauges(gauges);

        for (final Map.Entry<String, Counter> counter :
             registry.getCounters(filter).entrySet()) {
            counters.add(new MetricsObjectResource.CounterObjectResource(
                    counter.getKey(), counter.getValue()));
        }
        result.setCounters(counters);

        for (final Map.Entry<String, Meter> meter :
             registry.getMeters(filter).entrySet()) {
            meters.add(new MetricsObjectResource.MeterObjectResource(
                    meter.getKey(), meter.getValue()));
        }
        result.setMeters(meters);

        for (final Map.Entry<String, Histogram> histogram :
             registry.getHistograms(filter).entrySet()) {
            histograms.add(new MetricsObjectResource.HistogramObjectResource(
                    histogram.getKey(), histogram.getValue()));
        }
        result.setHistograms(histograms);

        return toRepresentation(result, null);
    }

}
