package net.onrc.onos.core.metrics;

import com.codahale.metrics.MetricRegistry;

/**
 * This class acts a singleton to hold the Metrics registry for ONOS.
 */
public final class OnosMetrics {

    /**
     * Hide constructor.  The only way to get the registry is through the
     * singleton getter.
     */
    private OnosMetrics() {}

    private static final MetricRegistry METRICS_REGISTRY = new MetricRegistry();

    /**
     * Get the singleton Metrics registry.  A single instance of
     * the registry is statically allocated and then used by all callers.
     *
     * @return Metrics registry
     */
    public static MetricRegistry getMetricsRegistry() {
        return METRICS_REGISTRY;
    }
}

