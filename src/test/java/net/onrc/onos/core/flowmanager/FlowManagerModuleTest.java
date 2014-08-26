package net.onrc.onos.core.flowmanager;

import static org.junit.Assert.assertEquals;
import net.onrc.onos.api.flowmanager.ConflictDetectionPolicy;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.registry.StandaloneRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for {@link FlowManagerModule}.
 */
public class FlowManagerModuleTest {
    IControllerRegistryService idBlockAllocator;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        idBlockAllocator = new StandaloneRegistry();
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Checks the default conflict detection policy is the FREE policy, and the
     * other policies are not supported.
     */
    @Test
    public void testConflictDetectionPolicy() {
        FlowManagerModule flowManager = new FlowManagerModule();
        assertEquals(ConflictDetectionPolicy.FREE,
                flowManager.getConflictDetectionPolicy());

        flowManager.setConflictDetectionPolicy(ConflictDetectionPolicy.FREE);
        assertEquals(ConflictDetectionPolicy.FREE,
                flowManager.getConflictDetectionPolicy());

        thrown.expect(UnsupportedOperationException.class);
        thrown.expectMessage("LOOSE is not supported.");
        flowManager.setConflictDetectionPolicy(ConflictDetectionPolicy.LOOSE);
        assertEquals(ConflictDetectionPolicy.FREE,
                flowManager.getConflictDetectionPolicy());

        thrown.expect(UnsupportedOperationException.class);
        thrown.expectMessage("STRICT is not supported.");
        flowManager.setConflictDetectionPolicy(ConflictDetectionPolicy.STRICT);
        assertEquals(ConflictDetectionPolicy.FREE,
                flowManager.getConflictDetectionPolicy());
    }

}
