package net.onrc.onos.core.intent.runtime;


import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.restserver.IRestApiService;
import net.onrc.onos.core.datagrid.IDatagridService;
import net.onrc.onos.core.datagrid.IEventChannel;
import net.onrc.onos.core.datagrid.IEventChannelListener;
import net.onrc.onos.core.intent.IntentOperationList;
import net.onrc.onos.core.intent.MockTopology;
import net.onrc.onos.core.intent.runtime.web.IntentWebRoutable;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.topology.ITopologyListener;
import net.onrc.onos.core.topology.ITopologyService;
import org.powermock.api.easymock.PowerMock;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * This class contains all of the mocked code required to run a test that uses
 * the Intent framework.  The normal lifecycle of an object of this class is to
 * create the object and call setUpIntentMocks() in the @Before (setUp()) part of
 * the test, then call tearDownIntentMocks() in the @After (tearDown()) part
 * of the test.  The intention of this class is to hide the mocking mechanics
 * and the unchecked suppressions in one place, and to make the Intent testing
 * reusable.
 *
 * This code is largely refactored from
 * net.onrc.onos.core.intent.runtime.UseCaseTest
 */
public class IntentTestMocks {

    private FloodlightModuleContext moduleContext;
    private IDatagridService datagridService;
    private ITopologyService topologyService;
    private IControllerRegistryService controllerRegistryService;
    private PersistIntent persistIntent;
    private IRestApiService restApi;

    /**
     * Default constructor.  Doesn't do anything interesting.
     */
    public IntentTestMocks() { }

    /**
     * Create whatever mocks are required to access the Intents framework.
     * This method is intended to be called during @Before processing (setUp())
     * of the JUnit test.
     *
     * @throws Exception if any of the mocks cannot be created.
     */
    @SuppressWarnings("unchecked")
    public void setUpIntentMocks() throws Exception {
        final MockTopology topology = new MockTopology();
        topology.createSampleTopology1();

        datagridService = createMock(IDatagridService.class);
        topologyService = createMock(ITopologyService.class);
        controllerRegistryService = createMock(IControllerRegistryService.class);
        moduleContext = createMock(FloodlightModuleContext.class);
        final IEventChannel<Long, IntentOperationList> intentOperationChannel =
                createMock(IEventChannel.class);
        final IEventChannel<Long, IntentStateList>intentStateChannel =
                createMock(IEventChannel.class);
        persistIntent = PowerMock.createMock(PersistIntent.class);
        restApi = createMock(IRestApiService.class);

        PowerMock.expectNew(PersistIntent.class,
                anyObject(IControllerRegistryService.class)).andReturn(persistIntent);

        expect(moduleContext.getServiceImpl(IDatagridService.class))
                .andReturn(datagridService).once();
        expect(moduleContext.getServiceImpl(ITopologyService.class))
                .andReturn(topologyService).once();
        expect(moduleContext.getServiceImpl(IControllerRegistryService.class))
                .andReturn(controllerRegistryService).once();
        expect(persistIntent.getKey()).andReturn(1L).anyTimes();
        expect(persistIntent.persistIfLeader(eq(1L),
                anyObject(IntentOperationList.class))).andReturn(true)
                .anyTimes();
        expect(moduleContext.getServiceImpl(IRestApiService.class))
                .andReturn(restApi).once();

        expect(topologyService.getTopology()).andReturn(topology)
                .anyTimes();
        topologyService.registerTopologyListener(
                anyObject(ITopologyListener.class));
        expectLastCall();

        expect(datagridService.createChannel("onos.pathintent",
                Long.class, IntentOperationList.class))
                .andReturn(intentOperationChannel).once();

        expect(datagridService.addListener(
                eq("onos.pathintent_state"),
                anyObject(IEventChannelListener.class),
                eq(Long.class),
                eq(IntentStateList.class)))
                .andReturn(intentStateChannel).once();
        restApi.addRestletRoutable(anyObject(IntentWebRoutable.class));

        replay(datagridService);
        replay(topologyService);
        replay(moduleContext);
        replay(controllerRegistryService);
        PowerMock.replay(persistIntent, PersistIntent.class);
        replay(restApi);
    }

    /**
     * Remove whatever mocks were put in place.  This method is intended to be
     * called as part of @After processing (tearDown()) of the JUnit test.
     */
    public void tearDownIntentMocks() {
        verify(datagridService);
        verify(topologyService);
        verify(moduleContext);
        verify(controllerRegistryService);
        PowerMock.verify(persistIntent, PersistIntent.class);
        verify(restApi);
    }

    /**
     * Fetch the Floodligh module context being used by the mock.  Some tests
     * will need to add items to the Context to allow communications with
     * downstream classes.
     *
     * @return the FloodlightModuleCOntext used by the mock.
     */
    public FloodlightModuleContext getModuleContext() {
        return moduleContext;
    }
}
