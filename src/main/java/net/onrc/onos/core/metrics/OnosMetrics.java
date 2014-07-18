package net.onrc.onos.core.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This class acts a singleton to hold the Metrics registry for ONOS.
 * All metrics (Counter, Histogram, Timer, Meter, Gauge) use a hierarchical
 * string-based naming scheme: COMPONENT.FEATURE.NAME.
 * Example: "Topology.Counters.TopologyUpdates".
 * The COMPONENT and FEATURE names have to be registered in advance before
 * a metric can be created. Example:
 * <pre>
 *   <code>
 *     private static final OnosMetrics.MetricsComponent COMPONENT =
 *         OnosMetrics.registerComponent("Topology");
 *     private static final OnosMetrics.MetricsFeature FEATURE =
 *         COMPONENT.registerFeature("Counters");
 *     private final Counter counterTopologyUpdates =
 *         OnosMetrics.createCounter(COMPONENT, FEATURE, "TopologyUpdates");
 *   </code>
 * </pre>
 * Gauges are slightly different because they are not created directly in
 * this class, but are allocated by the caller and passed in for registration:
 * <pre>
 *   <code>
 *     private final Gauge<Long> gauge =
 *         new {@literal Gauge<Long>}() {
 *             {@literal @}Override
 *             public Long getValue() {
 *                 return gaugeValue;
 *             }
 *         };
 *     OnosMetrics.registerMetric(COMPONENT, FEATURE, GAUGE_NAME, gauge);
 *   </code>
 * </pre>
 */
public final class OnosMetrics {

    /**
     * Registry to hold the Components defined in the system.
     */
    private static ConcurrentMap<String, MetricsComponent> componentsRegistry =
            new ConcurrentHashMap<>();

    /**
     * Registry for the Metrics objects created in the system.
     */
    private static final MetricRegistry METRICS_REGISTRY = new MetricRegistry();

    /**
     * Hide constructor.  The only way to get the registry is through the
     * singleton getter.
     */
    private OnosMetrics() {}

    /**
     * Components that can hold Metrics.  This is used as the first part of
     * a Metric's name.
     */
    public interface MetricsComponent {
        /**
         * Fetches the name of the Component.
         *
         * @return name of the Component
         */
        public String getName();

        /**
         * Registers a Feature for this component.
         *
         * @param featureName name of the Feature to register
         * @return Feature object that can be used when creating Metrics
         */
        public MetricsFeature registerFeature(final String featureName);
    }

    /**
     * Features that can hold Metrics.  This is used as the second part of
     * a Metric's name.
     */
    public interface MetricsFeature {
        /**
         * Fetches the name of the Feature.
         *
         * @return name of the Feature
         */
        public String getName();
    }

    /**
     * Implementation of a class to represent the Component portion of a
     * Metric's name.
     */
    private static final class Component implements MetricsComponent {
        private final String name;

        /**
         * Registry to hold the Features defined in this Component.
         */
        private final ConcurrentMap<String, MetricsFeature> featuresRegistry =
                new ConcurrentHashMap<>();

        /**
         * Constructs a component from a name.
         *
         * @param newName name of the component
         */
        Component(final String newName) {
            name = newName;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public MetricsFeature registerFeature(final String featureName) {
            MetricsFeature feature = featuresRegistry.get(featureName);
            if (feature == null) {
                final MetricsFeature createdFeature = new Feature(featureName);
                feature = featuresRegistry.putIfAbsent(featureName, createdFeature);
                if (feature == null) {
                    feature = createdFeature;
                }
            }
            return feature;
        }
    }

    /**
     * Implementation of a class to represent the Feature portion of a Metric's
     * name.
     */
    private static final class Feature implements MetricsFeature {
        private final String name;

        /**
         * Constructs a Feature from a name.
         *
         * @param newName name of the Feature
         */
        Feature(final String newName) {
            name = newName;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /**
     * Registers a component.
     *
     * @param name name of the Component to register
     * @return MetricsComponent object that can be used to create Metrics.
     */
    public static MetricsComponent registerComponent(final String name) {
        MetricsComponent component = componentsRegistry.get(name);
        if (component == null) {
            final MetricsComponent createdComponent = new Component(name);
            component = componentsRegistry.putIfAbsent(name, createdComponent);
            if (component == null) {
                component = createdComponent;
            }
        }
        return component;
    }

    /**
     * Generates a name for a Metric from its component and feature.
     *
     * @param component component the metric is defined in
     * @param feature feature the metric is defined in
     * @param metricName local name of the metric
     *
     * @return full name of the metric
     */
    public static String generateName(final MetricsComponent component,
                                      final MetricsFeature feature,
                                      final String metricName) {
        return MetricRegistry.name(component.getName(),
                                   feature.getName(),
                                   metricName);
    }

    /**
     * Creates a Counter metric.
     *
     * @param component component the Counter is defined in
     * @param feature feature the Counter is defined in
     * @param metricName local name of the metric
     * @return the created Counter Meteric
     */
    public static Counter createCounter(final MetricsComponent component,
                                        final MetricsFeature feature,
                                        final String metricName) {
        final String name = generateName(component, feature, metricName);
        return METRICS_REGISTRY.counter(name);
    }

    /**
     * Creates a Histogram metric.
     *
     * @param component component the Histogram is defined in
     * @param feature feature the Histogram is defined in
     * @param metricName local name of the metric
     * @return the created Histogram Metric
     */
    public static Histogram createHistogram(final MetricsComponent component,
                                            final MetricsFeature feature,
                                            final String metricName) {
        final String name = generateName(component, feature, metricName);
        return METRICS_REGISTRY.histogram(name);
    }

    /**
     * Creates a Timer metric.
     *
     * @param component component the Timer is defined in
     * @param feature feature the Timeer is defined in
     * @param metricName local name of the metric
     * @return the created Timer Metric
     */
    public static Timer createTimer(final MetricsComponent component,
                                    final MetricsFeature feature,
                                    final String metricName) {
        final String name = generateName(component, feature, metricName);
        return METRICS_REGISTRY.timer(name);
    }

    /**
     * Creates a Meter metric.
     *
     * @param component component the Meter is defined in
     * @param feature feature the Meter is defined in
     * @param metricName local name of the metric
     * @return the created Meter Metric
     */
    public static Meter createMeter(final MetricsComponent component,
                                    final MetricsFeature feature,
                                    final String metricName) {
        final String name = generateName(component, feature, metricName);
        return METRICS_REGISTRY.meter(name);
    }

    /**
     * Registers an already created Metric.  This is used for situation where a
     * caller needs to allocate its own Metric, but still register it with the
     * system.
     *
     * @param component component the Metric is defined in
     * @param feature feature the Metric is defined in
     * @param metricName local name of the metric
     * @param metric Metric to register
     * @return the registered Metric
     */
    public static <T extends Metric> T registerMetric(
                                        final MetricsComponent component,
                                        final MetricsFeature feature,
                                        final String metricName,
                                        final T metric) {
        final String name = generateName(component, feature, metricName);
        METRICS_REGISTRY.register(name, metric);
        return metric;
    }

    /**
     * Fetches the existing Timers.
     *
     * @param filter filter to use to select Timers
     * @return a map of the Timers that match the filter, with the key as the
     *         name String to the Timer.
     */
    public static Map<String, Timer> getTimers(final MetricFilter filter) {
        return METRICS_REGISTRY.getTimers(filter);
    }

    /**
     * Fetches the existing Gauges.
     *
     * @param filter filter to use to select Gauges
     * @return a map of the Gauges that match the filter, with the key as the
     *         name String to the Gauge.
     */
    @SuppressWarnings("rawtypes")
    public static Map<String, Gauge> getGauges(final MetricFilter filter) {
        return METRICS_REGISTRY.getGauges(filter);
    }

    /**
     * Fetches the existing Counters.
     *
     * @param filter filter to use to select Counters
     * @return a map of the Counters that match the filter, with the key as the
     *         name String to the Counter.
     */
    public static Map<String, Counter> getCounters(final MetricFilter filter) {
        return METRICS_REGISTRY.getCounters(filter);
    }

    /**
     * Fetches the existing Meters.
     *
     * @param filter filter to use to select Meters
     * @return a map of the Meters that match the filter, with the key as the
     *         name String to the Meter.
     */
    public static Map<String, Meter> getMeters(final MetricFilter filter) {
        return METRICS_REGISTRY.getMeters(filter);
    }

    /**
     * Fetches the existing Histograms.
     *
     * @param filter filter to use to select Histograms
     * @return a map of the Histograms that match the filter, with the key as the
     *         name String to the Histogram.
     */
    public static Map<String, Histogram> getHistograms(final MetricFilter filter) {
        return METRICS_REGISTRY.getHistograms(filter);
    }

    /**
     * Removes all Metrics that match a given filter.
     *
     * @param filter filter to use to select the Metrics to remove.
     */
    public static void removeMatching(final MetricFilter filter) {
        METRICS_REGISTRY.removeMatching(filter);
    }
}

