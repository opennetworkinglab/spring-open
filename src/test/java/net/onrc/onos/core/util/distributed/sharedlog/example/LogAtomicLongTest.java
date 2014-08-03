package net.onrc.onos.core.util.distributed.sharedlog.example;

import static org.junit.Assert.*;

import java.util.UUID;

import net.onrc.onos.core.datastore.hazelcast.HZClient;
import net.onrc.onos.core.util.IntegrationTest;
import net.onrc.onos.core.util.TestUtils;
import net.onrc.onos.core.util.distributed.sharedlog.hazelcast.HazelcastRuntime;
import net.onrc.onos.core.util.distributed.sharedlog.hazelcast.HazelcastSequencerRuntime;
import net.onrc.onos.core.util.distributed.sharedlog.runtime.LogBasedRuntime;
import net.onrc.onos.core.util.distributed.sharedlog.runtime.SequencerRuntime;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.hazelcast.core.HazelcastInstance;

/**
 * Unit test to run LogAtomicLong example.
 */
public class LogAtomicLongTest {

    static {
        // configuration to quickly fall back to instance mode for faster test run
        System.setProperty("net.onrc.onos.core.datastore.hazelcast.client.attemptLimit", "0");
    }


    private LogAtomicLong along;

    /**
     * Create LogAtomicLong instance.
     */
    @Before
    public void setUp() {
        final HZClient cl = HZClient.getClient();
        HazelcastInstance hz = TestUtils.callMethod(cl, "getHZInstance", null);

        SequencerRuntime sequencerRuntime = new HazelcastSequencerRuntime(hz);
        LogBasedRuntime runtime = new HazelcastRuntime(hz, sequencerRuntime);

        String counterName = UUID.randomUUID().toString();
        along = new LogAtomicLong(runtime, counterName);
    }

    /**
     * Test unconditional write.
     */
    @Test
    public void testSet() {
        along.set(42);
        assertEquals(42, along.get());
        along.set(0);
        assertEquals(0, along.get());
    }

    /**
     * Test conditional write.
     */
    @Test
    public void testCompareAndSet() {
        along.set(42);
        assertEquals(42, along.get());

        along.compareAndSet(42, 43);
        assertEquals(43, along.get());

        // should remain unchanged if expectation not met
        along.compareAndSet(42, 45);
        assertEquals(43, along.get());
    }

    /**
     * Confirm initial value is 0.
     */
    @Test
    public void testGet() {
        assertEquals(0, along.get());
    }

    /**
     * Confirm another instance with same ID observes the same value.
     */
    @Test
    public void testOtherInstance() {
        along.set(42);
        assertEquals(42, along.get());

        final HZClient cl = HZClient.getClient();
        HazelcastInstance hz = TestUtils.callMethod(cl, "getHZInstance", null);

        SequencerRuntime sequencerRuntime = new HazelcastSequencerRuntime(hz);
        LogBasedRuntime runtime = new HazelcastRuntime(hz, sequencerRuntime);

        LogAtomicLong anotherInstance = new LogAtomicLong(runtime,
                                                along.getObjectID());
        assertEquals(42, anotherInstance.get());
    }

    /**
     * Confirm another instance with same ID initializes using snapshot.
     */
    @Category(IntegrationTest.class)
    @Test
    public void testOtherInstanceFromSnapshot() {
        along.set(42);
        assertEquals(42, along.get());

        final HZClient cl = HZClient.getClient();
        HazelcastInstance hz = TestUtils.callMethod(cl, "getHZInstance", null);

        SequencerRuntime sequencerRuntime = new HazelcastSequencerRuntime(hz);
        LogBasedRuntime runtime = new HazelcastRuntime(hz, sequencerRuntime);

        // FIXME SNAPSHOT_INTERVAL should be customized to smaller values
        // write multiple times to trigger snapshot
        for (int i = 0; i < HazelcastRuntime.SNAPSHOT_INTERVAL + 2; ++i) {
            along.set(i);
        }
        along.set(99);
        Thread.yield();

        // this instance might start from latest snap shot then replay
        LogAtomicLong anotherInstance2 = new LogAtomicLong(runtime,
                along.getObjectID());
        assertEquals(99, anotherInstance2.get());

    }
}
