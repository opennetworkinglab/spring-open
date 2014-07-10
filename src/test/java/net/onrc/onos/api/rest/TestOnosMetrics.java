package net.onrc.onos.api.rest;

import net.onrc.onos.core.metrics.OnosMetrics;
import org.junit.Test;

import static net.onrc.onos.core.util.UtilityClassChecker.assertThatClassIsUtility;


//  TODO - move this class to someplace more appropriate

/**
 * Unit tests for the OnosMetrics class.
 */
public class TestOnosMetrics {

    /**
     * Make sure that the Onos Metrics class is a utility class.
     */
    @Test
    public void assureThatOnosMetricsIsUtility() {
        assertThatClassIsUtility(OnosMetrics.class);
    }
}
