package net.onrc.onos.core.metrics.web;


import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Histogram;
import net.onrc.onos.core.metrics.web.serializers.MetricsObjectSerializer;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.List;

/**
 * Resource class to hold Metrics information.  Timers, Gauges, Counters,
 * Meters and Historgrams are currently implemented.
 */
@JsonSerialize(using = MetricsObjectSerializer.class)
@SuppressWarnings("rawtypes")
public class MetricsObjectResource {

    /**
     * Base Metric object that all metrics inherit from.  Defines common
     * attributes.
     */
    public static class BaseMetricObject {
        private final String name;

        /**
         * Constructor for the base object.  Sets the name attribute.
         *
         * @param newName name of the Metric
         */
        protected BaseMetricObject(final String newName) {
            name = newName;
        }

        /**
         * Get the name of the Metric.
         *
         * @return metric name
         */
        public String getName() {
            return name;
        }
    }

    /**
     * Metric object that represents a Timer.
     */
    public static class TimerObjectResource extends BaseMetricObject {
        private final Timer timer;

        /**
         * Construct a new Timer resource object.
         *
         * @param newName name to use for the timer
         * @param newTimer Metrics Timer object
         */
        public TimerObjectResource(final String newName,
                                   final Timer newTimer) {
            super(newName);
            timer = newTimer;
        }

        /**
         * Get the Metrics Timer object for this resource.
         *
         * @return Metrics Timer object.
         */
        public Timer getTimer() {
            return timer;
        }
    }

    /**
     * Metric object that represents a Gauge.
     */
    public static class GaugeObjectResource extends BaseMetricObject {
        private final Gauge gauge;

        /**
         * Constructs a new Gauge resource object.
         *
         * @param newName name to use for the Gauge object
         * @param newGauge Metrics Gauge object
         */
        public GaugeObjectResource(final String newName,
                                   final Gauge newGauge) {
            super(newName);
            gauge = newGauge;
        }

        /**
         * Gets the Metrics Gauge object for this resource.
         *
         * @return Metrics Gauge object.
         */
        public Gauge getGauge() {
            return gauge;
        }
    }

    /**
     * Metric object that represents a Counter.
     */
    public static class CounterObjectResource extends BaseMetricObject {
        private final Counter counter;

        /**
         * Constructs a new Counter resource object.
         *
         * @param newName name to use for the Counter object
         * @param newCounter Metrics Counter object
         */
        public CounterObjectResource(final String newName,
                                     final Counter newCounter) {
            super(newName);
            counter = newCounter;
        }

        /**
         * Gets the Metrics Counter object for this resource.
         *
         * @return Metrics Counter object.
         */
        public Counter getCounter() {
            return counter;
        }
    }

    /**
     * Metric object that represents a Meter.
     */
    public static class MeterObjectResource extends BaseMetricObject {
        private final Meter meter;

        /**
         * Constructs a new Meter resource object.
         *
         * @param newName name to use for the Meter object
         * @param newMeter Metrics Meter object
         */
        public MeterObjectResource(final String newName,
                                   final Meter newMeter) {
            super(newName);
            meter = newMeter;
        }

        /**
         * Gets the Metrics Meter object for this resource.
         *
         * @return Metrics Meter object.
         */
        public Meter getMeter() {
            return meter;
        }
    }

    /**
     * Metric objerct that represents a Histogram.
     */
    public static class HistogramObjectResource extends BaseMetricObject {
        private final Histogram histogram;

        /**
         * Constructs a new Histogram resource object.
         *
         * @param newName name to use for Histogram object.
         * @param newHistogram Metrics Histogram object.
         */
        public HistogramObjectResource(final String newName,
                                       final Histogram newHistogram) {
            super(newName);
            histogram = newHistogram;
        }

        /**
         * Gets the Metrics Histogram object for this resource.
         *
         * @return Metrics Histogram Object
         */
        public Histogram getHistogram() {
            return histogram;
        }
    }


    private List<TimerObjectResource> timers;
    private List<GaugeObjectResource> gauges;
    private List<CounterObjectResource> counters;
    private List<MeterObjectResource> meters;
    private List<HistogramObjectResource> histograms;

    /**
     * Gets the list of Gauge objects.
     *
     * @return list of gauges
     */
    public List<GaugeObjectResource> getGauges() {
        return gauges;
    }

    /**
     * Defines the list of Gauge objects.
     *
     * @param gauges list of gauges
     */
    public void setGauges(List<GaugeObjectResource> gauges) {
        this.gauges = gauges;
    }

    /**
     * Gets the list of Timer objects.
     *
     * @return list of Timers
     */
    public List<TimerObjectResource> getTimers() {
        return timers;
    }

    /**
     * Defines the list of Timer objects.
     *
     * @param newTimers list of Timers
     */
    public void setTimers(List<TimerObjectResource> newTimers) {
        timers = newTimers;
    }

    /**
     * Gets the list of Counter objects.
     *
     * @return list of Counters
     */
    public List<CounterObjectResource> getCounters() {
        return counters;
    }

    /**
     * Defines the list of Counter objects.
     *
     * @param counters list of Counters
     */
    public void setCounters(List<CounterObjectResource> counters) {
        this.counters = counters;
    }

    /**
     * Gets the list of Meter objects.
     *
     * @return list of Meters
     */
    public List<MeterObjectResource> getMeters() {
        return meters;
    }

    /**
     * Defines the list of Meter objects.
     *
     * @param meters list of Meters
     */
    public void setMeters(List<MeterObjectResource> meters) {
        this.meters = meters;
    }

    /**
     * Gets the list of Histogram objects.
     *
     * @return list of Histograms
     */
    public List<HistogramObjectResource> getHistograms() {
        return histograms;
    }

    /**
     * Defines the list of Histogram objects.
     *
     * @param histograms list of Histograms.
     */
    public void setHistograms(List<HistogramObjectResource> histograms) {
        this.histograms = histograms;
    }
}
