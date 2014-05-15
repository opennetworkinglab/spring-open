package net.onrc.onos.core.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AtomicCounterTest {

    static {
        // configuration to quickly fall back to instance mode for faster test run
        System.setProperty("net.onrc.onos.core.datastore.hazelcast.client.attemptLimit", "0");
    }

    private static final String TEST_COUNTER = "TestCounter" + UUID.randomUUID();
    private static final byte[] LONG_ZERO = {0, 0, 0, 0, 0, 0, 0, 0}; // 0L
    private IKVTableID counterID;

    @Before
    @After
    public void resetCounter() {
        IKVClient client = DataStoreClient.getClient();
        counterID = client.getTable(TEST_COUNTER).getTableId();
        client.setCounter(counterID, LONG_ZERO, 0L);
        client.destroyCounter(counterID, LONG_ZERO);
        counterID = client.getTable(TEST_COUNTER).getTableId();
    }

    @Test
    public void testSetCounter() throws ObjectExistsException, ObjectDoesntExistException {
        IKVClient client = DataStoreClient.getClient();

        final long five = 5;
        client.createCounter(counterID, LONG_ZERO, five);

        final long three = 3;
        client.setCounter(counterID, LONG_ZERO, three);

        final long four = client.incrementCounter(counterID, LONG_ZERO, 1);
        assertEquals(4, four);
    }

    @Test
    public void testIncrementCounter() throws ObjectExistsException, ObjectDoesntExistException {

        IKVClient client = DataStoreClient.getClient();

        final long five = 5;
        client.createCounter(counterID, LONG_ZERO, five);

        final long six = client.incrementCounter(counterID, LONG_ZERO, 1);
        assertEquals(6, six);


        final long nine = client.incrementCounter(counterID, LONG_ZERO, 3);
        assertEquals(9, nine);
    }


    private static final int NUM_INCREMENTS = Math.max(1, Integer
            .valueOf(System.getProperty("AtomicCounterTest.NUM_INCREMENTS",
                    "500")));
    private static final int NUM_THREADS = Math.max(1, Integer.valueOf(System
            .getProperty("AtomicCounterTest.NUM_THREADS", "3")));

    class Incrementor implements Callable<Long> {
        private final ConcurrentMap<Long, Long> uniquenessTestSet;
        private final ConcurrentLinkedQueue<Long> incrementTimes;

        public Incrementor(ConcurrentMap<Long, Long> uniquenessTestSet, ConcurrentLinkedQueue<Long> incrementTimes) {
            super();
            this.uniquenessTestSet = uniquenessTestSet;
            this.incrementTimes = incrementTimes;
        }

        @Override
        public Long call() throws ObjectDoesntExistException {
            IKVClient client = DataStoreClient.getClient();
            for (int i = 0; i < NUM_INCREMENTS; ++i) {
                final long start = System.nanoTime();
                final long incremented = client.incrementCounter(counterID, LONG_ZERO, 1);
                incrementTimes.add(System.nanoTime() - start);
                final Long expectNull = uniquenessTestSet.putIfAbsent(incremented, incremented);
                assertNull(expectNull);
            }
            return null;
        }
    }

    @Test
    public void testParallelIncrementCounter() throws ObjectExistsException,
                                    InterruptedException, ExecutionException {

        IKVClient client = DataStoreClient.getClient();

        client.createCounter(counterID, LONG_ZERO, 0L);

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        final int initThreads = Math.max(1, Integer.valueOf(System
                .getProperty("AtomicCounterTest.initThreads",
                        String.valueOf(NUM_THREADS))));
        for (int numThreads = initThreads; numThreads <= NUM_THREADS; ++numThreads) {
            client.setCounter(counterID, LONG_ZERO, 0L);
            parallelIncrementCounter(executor, numThreads);
        }

        executor.shutdown();
    }

    private void parallelIncrementCounter(final ExecutorService executor,
            final int numThreads) throws InterruptedException, ExecutionException {

        ConcurrentNavigableMap<Long, Long> uniquenessTestSet = new ConcurrentSkipListMap<>();
        ConcurrentLinkedQueue<Long> incrementTimes = new ConcurrentLinkedQueue<Long>();

        List<Callable<Long>> tasks = new ArrayList<>(numThreads);
        for (int i = 0; i < numThreads; ++i) {
            tasks.add(new Incrementor(uniquenessTestSet, incrementTimes));
        }
        List<Future<Long>> futures = executor.invokeAll(tasks);

        // wait for all tasks to end
        for (Future<Long> future : futures) {
            future.get();
        }

        assertEquals(numThreads * NUM_INCREMENTS , uniquenessTestSet.size());
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
        System.err.printf("incrementCounter: th, incs, N, avg(ns), min(ns), max(ns)\n");
        System.err.printf("incrementCounter: %d, %d, %d, %f, %d, %d\n",
                numThreads, NUM_INCREMENTS, incrementTimes.size(),
                sum / (double) incrementTimes.size(), min, max);
    }

}
