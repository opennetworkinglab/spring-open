package net.onrc.onos.core.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Histogram;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST APIs for Metrics objects.
 */
public class MetricsResource extends ServerResource {

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

        for (final Map.Entry<String, Timer> timer :
             registry.getTimers().entrySet()) {
            timers.add(new MetricsObjectResource.TimerObjectResource(
                    timer.getKey(), timer.getValue()));
        }
        result.setTimers(timers);

        for (final Map.Entry<String, Gauge> gauge :
             registry.getGauges().entrySet()) {
            gauges.add(new MetricsObjectResource.GaugeObjectResource(
                    gauge.getKey(), gauge.getValue()));
        }
        result.setGauges(gauges);

        for (final Map.Entry<String, Counter> counter :
             registry.getCounters().entrySet()) {
            counters.add(new MetricsObjectResource.CounterObjectResource(
                    counter.getKey(), counter.getValue()));
        }
        result.setCounters(counters);

        for (final Map.Entry<String, Meter> meter :
             registry.getMeters().entrySet()) {
            meters.add(new MetricsObjectResource.MeterObjectResource(
                    meter.getKey(), meter.getValue()));
        }
        result.setMeters(meters);

        for (final Map.Entry<String, Histogram> histogram :
             registry.getHistograms().entrySet()) {
            histograms.add(new MetricsObjectResource.HistogramObjectResource(
                    histogram.getKey(), histogram.getValue()));
        }
        result.setHistograms(histograms);

        return toRepresentation(result, null);
    }

}
