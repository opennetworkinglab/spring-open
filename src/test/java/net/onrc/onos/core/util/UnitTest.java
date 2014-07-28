package net.onrc.onos.core.util;

import com.codahale.metrics.MetricFilter;
import net.onrc.onos.core.metrics.OnosMetrics;
import org.junit.After;
import org.junit.Before;

/**
 * Base class that Unit Tests should inherit from.
 */
public class UnitTest {

    /**
     * Performs initialization operations requred by all unit tests.
     */
    @Before
    public void setUpBase() {}

    /**
     * Performs clean up operations required by all unit tests.
     */
    @After
    public void tearDownBase() {
        // Destroy any metrics created during the execution of the test.
        OnosMetrics.removeMatching(MetricFilter.ALL);
    }

}
