package net.onrc.onos.core.datastore.hazelcast;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.onrc.onos.core.datastore.ObjectDoesntExistException;
import net.onrc.onos.core.util.distributed.DistributedAtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for HZDistributedAtomicLong.
 */
public class HZDistributedAtomicLongTest {
    static {
        // configuration to quickly fall back to instance mode for faster test run
        System.setProperty("net.onrc.onos.core.datastore.hazelcast.client.attemptLimit", "0");
    }

    static final String TEST_COUNTER_NAME = "Counter" + UUID.randomUUID();

    DistributedAtomicLong counter;

    @Before
    public void setUp() throws Exception {
        counter = new HZDistributedAtomicLong(
                    HZClient.getClient(), TEST_COUNTER_NAME);
        counter.set(0L);
    }

    @Test
    public void testGet() {
        assertEquals(0L, counter.get());
    }

    @Test
    public void testAddAndGet() {
        assertEquals(0L, counter.get());
        assertEquals(1L, counter.addAndGet(1L));
        assertEquals(3L, counter.addAndGet(2L));
        assertEquals(7L, counter.addAndGet(4L));
    }

    @Test
    public void testSet() {
        counter.set(42L);
        assertEquals(42L, counter.get());
    }

    @Test
    public void testIncrementAndGet() {
        assertEquals(1L, counter.incrementAndGet());
        assertEquals(2L, counter.incrementAndGet());
        assertEquals(3L, counter.incrementAndGet());
    }

    /**
     * Callable task incrementing atomicLong.
     */
    private static final class AdderTask implements Callable<Long> {
        // using Map as Set
        private final ConcurrentMap<Long, Long> uniquenessTestSet;
        // Queue given here should be Thread-safe
        private final Queue<Long> incrementTimes;
        private final HZDistributedAtomicLong counter;
        private final int numIncrements;

        /**
         * Constructor.
         *
         * @param numIncrements number of increments to execute
         * @param uniquenessTestSet ThreadSafe Map to store increment result
         * @param incrementTimes ThreadSafe Queue to store time it
         *      took on each increment
         */
        public AdderTask(int numIncrements,
                ConcurrentMap<Long, Long> uniquenessTestSet,
                Queue<Long> incrementTimes) {

            super();
            this.uniquenessTestSet = uniquenessTestSet;
            this.incrementTimes = incrementTimes;
            this.counter = new HZDistributedAtomicLong(
                    HZClient.getClient(), TEST_COUNTER_NAME);
            this.numIncrements = numIncrements;
        }

        @Override
        public Long call() throws ObjectDoesntExistException {
            for (int i = 0; i < numIncrements; ++i) {
                final long start = System.nanoTime();
                final long incremented = counter.addAndGet(1L);
                incrementTimes.add(System.nanoTime() - start);
                final Long expectNull = uniquenessTestSet.putIfAbsent(
                                                    incremented, incremented);
                assertNull(expectNull);
            }
            return null;
        }
    }

    private static final int NUM_THREADS = Integer.parseInt(
                              System.getProperty(
                                      "HZDistributedAtomicLongTest.NUM_THREADS",
                                        System.getProperty("NUM_THREADS",
                                                           "10")));

    private static final int NUM_INCREMENTS = Integer.parseInt(
                              System.getProperty(
                                      "HZDistributedAtomicLongTest.NUM_INCREMENTS",
                                        System.getProperty("NUM_INCREMENTS",
                                                           "100")));

    /**
     * Increment using multiple threads to test addition is atomic.
     */
    @Test
    public void testConcurrentAddAndGet() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        // using Map as Set
        ConcurrentMap<Long, Long> uniquenessTestSet = new ConcurrentSkipListMap<>();
        Queue<Long> incrementTimes = new ConcurrentLinkedQueue<>();

        // Start NUM_THREADS threads and increment NUM_INCREMENTS times each
        List<Callable<Long>> tasks = new ArrayList<>(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; ++i) {
            tasks.add(new AdderTask(NUM_INCREMENTS,
                                      uniquenessTestSet,
                                      incrementTimes));
        }
        List<Future<Long>> futures = executor.invokeAll(tasks);

        // wait for all tasks to end
        for (Future<Long> future : futures) {
            future.get();
        }


        assertEquals(NUM_THREADS * NUM_INCREMENTS , uniquenessTestSet.size());

        // check uniqueness of result
        long prevValue = 0;
        for (Long value : uniquenessTestSet.keySet()) {
            assertEquals((prevValue + 1), value.longValue());
            prevValue = value;
        }

        long max = 0L;
        long min = Long.MAX_VALUE;
        long sum = 0L;
        for (Long time : incrementTimes) {
            sum += time;
            max = Math.max(max, time);
            min = Math.min(min, time);
        }
        System.err.printf("incrementCounter: th, incs, tot_incs,"
                        + " avg(ns), min(ns), max(ns), T-put(1s/avg)\n");
        System.err.printf("incrementCounter: %d, %d, %d,"
                        + " %f, %d, %d, %f\n",
                          NUM_THREADS, NUM_INCREMENTS, incrementTimes.size(),
                          sum / (double) incrementTimes.size(), min, max,
                          Math.pow(10, 9) * incrementTimes.size() / sum);

        executor.shutdown();
    }

}
