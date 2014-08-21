package net.onrc.onos.core.newintent;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import net.onrc.onos.api.newintent.Intent;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.core.datastore.hazelcast.DummySharedCollectionsService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

/**
 * Suites of test of {@link IntentMap}.
 */
public class IntentMapTest {

    private final IntentId id1 = new IntentId(1);
    private DummySharedCollectionsService service;
    private IntentMap<Intent> sut;

    @Before
    public void setUp() {
        service = new DummySharedCollectionsService();
        sut = new IntentMap<>("test", Intent.class, service);
    }

    @After
    public void tearDown() {
        sut.destroy();
    }

    /**
     * Tests if listener is invoked when add/remove/update occurs.
     *
     * @throws InterruptedException if interrupt occurs
     */
    @Test(timeout = 1000)
    public void testListener() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(3);

        sut.addListener(new EntryListener<IntentId, Intent>() {
            @Override
            public void entryAdded(EntryEvent<IntentId, Intent> event) {
                latch.countDown();
            }

            @Override
            public void entryRemoved(EntryEvent<IntentId, Intent> event) {
                latch.countDown();
            }

            @Override
            public void entryUpdated(EntryEvent<IntentId, Intent> event) {
                latch.countDown();
            }

            @Override
            public void entryEvicted(EntryEvent<IntentId, Intent> event) {
            }
        });

        sut.put(id1, new TestIntent(id1));
        sut.put(id1, new TestIntent(id1));
        sut.remove(id1);

        latch.await();
    }
}
