package net.onrc.onos.core.newintent;

import net.onrc.onos.api.newintent.IntentException;
import net.onrc.onos.api.newintent.IntentServiceTest;
import net.onrc.onos.api.newintent.TestableIntentService;
import net.onrc.onos.core.datagrid.ISharedCollectionsService;
import net.onrc.onos.core.datastore.hazelcast.DummySharedCollectionsService;
import org.junit.After;

import java.util.Collections;
import java.util.List;

/**
 * Suites of test of {@link IntentManagerRuntime} inheriting from {@link IntentServiceTest}.
 */
public class IntentManagerRuntimeTest extends IntentServiceTest {

    private TestableIntentManagerRuntime sut;

    @Override
    protected TestableIntentService createIntentService() {
        DummySharedCollectionsService collectionsService = new DummySharedCollectionsService();
        sut = new TestableIntentManagerRuntime(collectionsService);
        return sut;
    }

    @After
    public void tearDown() {
        sut.destroy();
    }

    private static class TestableIntentManagerRuntime
            extends IntentManagerRuntime implements TestableIntentService {
        public TestableIntentManagerRuntime(ISharedCollectionsService service) {
            super(service);
        }

        @Override
        public List<IntentException> getExceptions() {
            return Collections.emptyList();
        }
    }
}
