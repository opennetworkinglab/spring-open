package net.onrc.onos.core.topology;

import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.datagrid.IDatagridService;
import net.onrc.onos.core.datagrid.IEventChannel;
import net.onrc.onos.core.datagrid.IEventChannelListener;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.registry.RegistryException;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.EventEntry;
import static net.onrc.onos.core.util.ImmutableClassChecker.assertThatClassIsImmutable;
import net.onrc.onos.core.util.OnosInstanceId;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;
import net.onrc.onos.core.util.TestUtils;
import net.onrc.onos.core.util.UnitTest;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the TopologyManager class in the Topology module.
 * These test cases only check the sanity of functions in the TopologyManager.
 * Note that we do not test the eventHandler functions in the TopologyManager
 * class.
 * DatagridService, DataStoreService, eventChannel, and
 * controllerRegistryService are mocked out.
 */
public class TopologyManagerTest extends UnitTest {
    private TopologyManager theTopologyManager;
    private TopologyManager.EventHandler theEventHandler;
    private TopologyListenerTest theTopologyListener =
        new TopologyListenerTest();
    private final String eventChannelName = "onos.topology";
    private IEventChannel<byte[], TopologyEvent> eventChannel;
    private IDatagridService datagridService;
    private TopologyDatastore dataStoreService;
    private IControllerRegistryService registryService;
    private Collection<TopologyEvent> allTopologyEvents;
    private static final OnosInstanceId ONOS_INSTANCE_ID_1 =
        new OnosInstanceId("ONOS-Instance-ID-1");
    private static final OnosInstanceId ONOS_INSTANCE_ID_2 =
        new OnosInstanceId("ONOS-Instance-ID-2");
    private static final Dpid DPID_1 = new Dpid(1);
    private static final Dpid DPID_2 = new Dpid(2);

    /**
     * Topology events listener.
     */
    private class TopologyListenerTest implements ITopologyListener {
        private TopologyEvents topologyEvents;

        @Override
        public void topologyEvents(TopologyEvents events) {
            this.topologyEvents = events;
        }

        /**
         * Clears the Topology Listener state.
         */
        public void clear() {
            this.topologyEvents = null;
        }
    }

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        // Mock objects for testing
        datagridService = createNiceMock(IDatagridService.class);
        dataStoreService = createNiceMock(TopologyDatastore.class);
        registryService = createMock(IControllerRegistryService.class);
        eventChannel = createNiceMock(IEventChannel.class);

        expect(datagridService.createChannel(
                eq(eventChannelName),
                eq(byte[].class),
                eq(TopologyEvent.class)))
                .andReturn(eventChannel).once();

        expect(datagridService.addListener(
                eq(eventChannelName),
                anyObject(IEventChannelListener.class),
                eq(byte[].class),
                eq(TopologyEvent.class)))
                .andReturn(eventChannel).once();

        expect(dataStoreService.addSwitch(
                anyObject(SwitchEvent.class),
                anyObject(Collection.class)))
                .andReturn(true).anyTimes();

        expect(dataStoreService.deactivateSwitch(
                anyObject(SwitchEvent.class),
                anyObject(Collection.class)))
                .andReturn(true).anyTimes();

        expect(dataStoreService.addPort(
                anyObject(PortEvent.class)))
                .andReturn(true).anyTimes();

        expect(dataStoreService.deactivatePort(
                anyObject(PortEvent.class)))
                .andReturn(true).anyTimes();

        expect(dataStoreService.addHost(
                anyObject(HostEvent.class)))
                .andReturn(true).anyTimes();

        expect(dataStoreService.removeHost(
                anyObject(HostEvent.class)))
                .andReturn(true).anyTimes();

        expect(dataStoreService.addLink(
                anyObject(LinkEvent.class)))
                .andReturn(true).anyTimes();

        expect(dataStoreService.removeLink(
                anyObject(LinkEvent.class)))
                .andReturn(true).anyTimes();

        // Setup the Registry Service
        expect(registryService.getOnosInstanceId()).andReturn(ONOS_INSTANCE_ID_1).anyTimes();
        expect(registryService.getControllerForSwitch(DPID_1.value()))
            .andReturn(ONOS_INSTANCE_ID_1.toString()).anyTimes();
        expect(registryService.getControllerForSwitch(DPID_2.value()))
            .andReturn(ONOS_INSTANCE_ID_2.toString()).anyTimes();

        allTopologyEvents = new CopyOnWriteArrayList<>();
        expect(eventChannel.getAllEntries())
            .andReturn(allTopologyEvents).anyTimes();

        replay(datagridService);
        replay(registryService);
        replay(dataStoreService);
        // replay(eventChannel);
    }

    /**
     * Setup the Topology Manager.
     */
    private void setupTopologyManager() {
        // Create a TopologyManager object for testing
        theTopologyManager = new TopologyManager(registryService);

        // Replace the eventHandler to prevent the thread from starting
        TestUtils.setField(theTopologyManager, "eventHandler",
            createNiceMock(TopologyManager.EventHandler.class));
        theTopologyManager.startup(datagridService);

        // Replace the data store with a mocked object
        TestUtils.setField(theTopologyManager, "datastore", dataStoreService);
    }

    /**
     * Setup the Topology Manager with the Event Handler.
     */
    private void setupTopologyManagerWithEventHandler() {
        // Create a TopologyManager object for testing
        theTopologyManager = new TopologyManager(registryService);
        theTopologyManager.addListener(theTopologyListener, true);

        // Allocate the Event Handler, so we can have direct access to it
        theEventHandler = theTopologyManager.new EventHandler();
        TestUtils.setField(theTopologyManager, "eventHandler",
                           theEventHandler);

        // Replace the data store with a mocked object
        TestUtils.setField(theTopologyManager, "datastore", dataStoreService);

        replay(eventChannel);
        //
        // NOTE: Uncomment-out the line below if the startup() method needs
        // to be called for some of the unit tests. For now it is commented-out
        // to avoid any side effects of starting the eventHandler thread.
        //
        // theTopologyManager.startup(datagridService);
    }

    /**
     * Tests the immutability of {@link TopologyEvents}.
     */
    @Test
    public void testImmutableTopologyEvents() {
        assertThatClassIsImmutable(TopologyEvents.class);
    }

    /**
     * Test the Switch Mastership Updated Event.
     */
    @Test
    public void testPutSwitchMastershipEvent() {
        // Mock the eventChannel functions first
        eventChannel.addEntry(anyObject(byte[].class),
                              anyObject(TopologyEvent.class));
        expectLastCall().times(1, 1);          // 1 event
        replay(eventChannel);

        setupTopologyManager();

        // Generate a new Switch Mastership event
        Role role = Role.MASTER;
        MastershipEvent mastershipEvent =
            new MastershipEvent(DPID_1, ONOS_INSTANCE_ID_1, role);

        // Call the topologyManager function for adding the event
        theTopologyManager.putSwitchMastershipEvent(mastershipEvent);

        // Verify the function calls
        verify(eventChannel);
    }

    /**
     * Test the Switch Mastership Removed Event.
     */
    @Test
    public void testRemoveSwitchMastershipEvent() {
        // Mock the eventChannel functions first
        eventChannel.removeEntry(anyObject(byte[].class));
        expectLastCall().times(1, 1);          // 1 event
        replay(eventChannel);

        setupTopologyManager();

        // Generate a new Switch Mastership Event
        Role role = Role.MASTER;
        MastershipEvent mastershipEvent =
            new MastershipEvent(DPID_1, ONOS_INSTANCE_ID_1, role);

        // Call the topologyManager function for removing the event
        theTopologyManager.removeSwitchMastershipEvent(mastershipEvent);

        // Verify the function calls
        verify(eventChannel);
    }

    /**
     * Test the Switch discovered and Port discovered functions.
     */
    @Test
    public void testPutSwitchAndPortDiscoveryEvent() {
        // Mock the eventChannel functions first
        eventChannel.addEntry(anyObject(byte[].class),
                              anyObject(TopologyEvent.class));
        expectLastCall().times(3, 3);  // (1 Switch + 1 Port), 1 Port
        replay(eventChannel);

        setupTopologyManager();

        // Mock Switch has one Port
        PortNumber portNumber = PortNumber.uint32(1);

        // Generate a new Switch Event along with a Port Event
        SwitchEvent switchEvent = new SwitchEvent(DPID_1);

        Collection<PortEvent> portEvents = new ArrayList<PortEvent>();
        portEvents.add(new PortEvent(DPID_1, portNumber));

        // Call the topologyManager function for adding a Switch
        theTopologyManager.putSwitchDiscoveryEvent(switchEvent, portEvents);

        for (PortEvent portEvent : portEvents) {
            // Call the topologyManager function for adding a Port
            theTopologyManager.putPortDiscoveryEvent(portEvent);
        }

        // Verify the function calls
        verify(eventChannel);

    }

    /**
     * Test the Switch and Port removed functions.
     */
    @Test
    public void testRemoveSwitchAndPortDiscoveryEvent() {
        // Mock the eventChannel functions first
        eventChannel.removeEntry(anyObject(byte[].class));
        expectLastCall().times(2, 2);          // 1 Switch, 1 Port
        replay(eventChannel);

        setupTopologyManager();

        PortNumber portNumber = PortNumber.uint32(1);

        // Generate a Port Event
        Collection<PortEvent> portEvents = new ArrayList<PortEvent>();
        portEvents.add(new PortEvent(DPID_1, portNumber));

        // Call the topologyManager function for removing a Port
        for (PortEvent portEvent : portEvents) {
            theTopologyManager.removePortDiscoveryEvent(portEvent);
        }

        // Call the topologyManager function for removing a Switch
        SwitchEvent switchEvent = new SwitchEvent(DPID_1);
        theTopologyManager.removeSwitchDiscoveryEvent(switchEvent);

        // Verify the function calls
        verify(eventChannel);

    }

    /**
     * Test the Link discovered function.
     */
    @Test
    public void testPutLinkDiscoveryEvent() {
        // Mock the eventChannel functions first
        eventChannel.addEntry(anyObject(byte[].class),
                              anyObject(TopologyEvent.class));
        expectLastCall().times(5, 5);  // (2 Switch + 2 Port + 1 Link)
        replay(eventChannel);

        setupTopologyManager();

        // Generate the Switch and Port Events
        PortNumber portNumber1 = PortNumber.uint32(1);
        SwitchEvent switchEvent1 = new SwitchEvent(DPID_1);
        Collection<PortEvent> portEvents1 = new ArrayList<PortEvent>();
        portEvents1.add(new PortEvent(DPID_1, portNumber1));

        // Call the topologyManager function for adding a Switch
        theTopologyManager.putSwitchDiscoveryEvent(switchEvent1, portEvents1);

        // Generate the Switch and Port Events
        PortNumber portNumber2 = PortNumber.uint32(2);
        SwitchEvent switchEvent2 = new SwitchEvent(DPID_2);
        Collection<PortEvent> portEvents2 = new ArrayList<PortEvent>();
        portEvents2.add(new PortEvent(DPID_2, portNumber2));

        // Call the topologyManager function for adding a Switch
        theTopologyManager.putSwitchDiscoveryEvent(switchEvent2, portEvents2);

        // Create the Link Event
        LinkEvent linkEvent =
            new LinkEvent(new SwitchPort(DPID_1, portNumber1),
                          new SwitchPort(DPID_2, portNumber2));
        theTopologyManager.putLinkDiscoveryEvent(linkEvent);

        // Verify the function calls
        verify(eventChannel);
    }

    /**
     * Test the Link removed function.
     */
    @Test
    public void testRemoveLinkDiscoveryEvent() {
        // Mock the eventChannel functions first
        eventChannel.removeEntry(anyObject(byte[].class));
        expectLastCall().times(1, 1);          // (1 Link)
        replay(eventChannel);

        setupTopologyManager();

        // Generate the Switch and Port Events
        PortNumber portNumber1 = PortNumber.uint32(1);
        SwitchEvent switchEvent1 = new SwitchEvent(DPID_1);
        Collection<PortEvent> portEvents1 = new ArrayList<PortEvent>();
        portEvents1.add(new PortEvent(DPID_1, portNumber1));

        // Call the topologyManager function for adding a Switch
        theTopologyManager.putSwitchDiscoveryEvent(switchEvent1, portEvents1);

        // Generate the Switch and Port Events
        PortNumber portNumber2 = PortNumber.uint32(2);
        SwitchEvent switchEvent2 = new SwitchEvent(DPID_2);
        Collection<PortEvent> portEvents2 = new ArrayList<PortEvent>();
        portEvents2.add(new PortEvent(DPID_2, portNumber2));

        // Call the topologyManager function for adding a Switch
        theTopologyManager.putSwitchDiscoveryEvent(switchEvent2, portEvents2);

        // Remove the Link
        LinkEvent linkEventRemove =
            new LinkEvent(new SwitchPort(DPID_1, portNumber1),
                          new SwitchPort(DPID_2, portNumber2));
        theTopologyManager.removeLinkDiscoveryEvent(linkEventRemove);

        // Verify the function calls
        verify(eventChannel);
    }

    /**
     * Test the Host discovered function.
     */
    @Test
    public void testPutHostDiscoveryEvent() {
        // Mock the eventChannel functions first
        eventChannel.addEntry(anyObject(byte[].class),
                              anyObject(TopologyEvent.class));
        expectLastCall().times(1, 1);          // 1 Host
        replay(eventChannel);

        setupTopologyManager();

        // Generate a new Host Event
        PortNumber portNumber = PortNumber.uint32(1);
        MACAddress hostMac = MACAddress.valueOf("00:AA:11:BB:33:CC");
        SwitchPort sp = new SwitchPort(DPID_1, portNumber);
        List<SwitchPort> spLists = new ArrayList<SwitchPort>();
        spLists.add(sp);
        HostEvent hostEvent = new HostEvent(hostMac);
        hostEvent.setAttachmentPoints(spLists);

        // Call the topologyManager function for adding a Host
        theTopologyManager.putHostDiscoveryEvent(hostEvent);

        // Verify the function calls
        verify(eventChannel);
    }

    /**
     * Test the Host removed function.
     */
    @Test
    public void testRemoveHostDiscoveryEvent() {
        // Mock the eventChannel functions first
        eventChannel.removeEntry(anyObject(byte[].class));
        expectLastCall().times(1, 1);          // 1 Host
        replay(eventChannel);

        setupTopologyManager();

        // Generate a new Host Event
        PortNumber portNumber = PortNumber.uint32(1);
        MACAddress hostMac = MACAddress.valueOf("00:AA:11:BB:33:CC");
        SwitchPort sp = new SwitchPort(DPID_1, portNumber);
        List<SwitchPort> spLists = new ArrayList<SwitchPort>();
        spLists.add(sp);
        HostEvent hostEvent = new HostEvent(hostMac);
        hostEvent.setAttachmentPoints(spLists);

        // Call the topologyManager function for removing a Host
        theTopologyManager.removeHostDiscoveryEvent(hostEvent);

        // Verify the function calls
        verify(eventChannel);
    }

    /**
     * Tests adding of a Switch Mastership event and the topology replica
     * transformation.
     */
    @Test
    public void testAddMastershipEvent() {
        setupTopologyManager();

        // Prepare the event
        Role role = Role.MASTER;
        MastershipEvent mastershipEvent =
            new MastershipEvent(DPID_1, ONOS_INSTANCE_ID_1, role);
        // Add the event
        TestUtils.callMethod(theTopologyManager, "addMastershipEvent",
                             MastershipEvent.class, mastershipEvent);

        //
        // NOTE: The topology itself doesn't contain the Mastership Events,
        // hence we don't check the topology.
        //

        // Check the events to be fired
        List<MastershipEvent> apiAddedMastershipEvents
            = TestUtils.getField(theTopologyManager,
                                 "apiAddedMastershipEvents");
        assertThat(apiAddedMastershipEvents, hasItem(mastershipEvent));
    }

    /**
     * Tests removing of a Switch Mastership event and the topology replica
     * transformation.
     */
    @Test
    public void testRemoveMastershipEvent() {
        setupTopologyManager();

        // Prepare the event
        Role role = Role.MASTER;
        MastershipEvent mastershipEvent =
            new MastershipEvent(DPID_1, ONOS_INSTANCE_ID_1, role);
        // Add the event
        TestUtils.callMethod(theTopologyManager, "addMastershipEvent",
                             MastershipEvent.class, mastershipEvent);

        // Check the events to be fired
        List<MastershipEvent> apiAddedMastershipEvents
            = TestUtils.getField(theTopologyManager,
                                 "apiAddedMastershipEvents");
        assertThat(apiAddedMastershipEvents, hasItem(mastershipEvent));

        // Remove the event
        TestUtils.callMethod(theTopologyManager, "removeMastershipEvent",
                             MastershipEvent.class,
                             new MastershipEvent(mastershipEvent));

        // Check the events to be fired
        List<MastershipEvent> apiRemovedMastershipEvents
            = TestUtils.getField(theTopologyManager,
                                 "apiRemovedMastershipEvents");
        assertThat(apiRemovedMastershipEvents, hasItem(mastershipEvent));
    }

    /**
     * Tests adding of a Switch and the topology replica transformation.
     */
    @Test
    public void testAddSwitch() {
        setupTopologyManager();

        SwitchEvent sw = new SwitchEvent(DPID_1);
        sw.createStringAttribute("foo", "bar");

        TestUtils.callMethod(theTopologyManager, "addSwitch",
                             SwitchEvent.class, sw);

        // Check the topology structure
        TopologyInternal topology =
            (TopologyInternal) theTopologyManager.getTopology();
        SwitchEvent swInTopo = topology.getSwitchEvent(DPID_1);
        assertEquals(sw, swInTopo);
        assertTrue(swInTopo.isFrozen());
        assertEquals("bar", swInTopo.getStringAttribute("foo"));

        // Check the events to be fired
        List<SwitchEvent> apiAddedSwitchEvents
            = TestUtils.getField(theTopologyManager, "apiAddedSwitchEvents");
        assertThat(apiAddedSwitchEvents, hasItem(sw));
    }

    /**
     * Tests adding of a Port and the topology replica transformation.
     */
    @Test
    public void testAddPort() {
        setupTopologyManager();

        SwitchEvent sw = new SwitchEvent(DPID_1);
        sw.createStringAttribute("foo", "bar");

        final PortNumber portNumber = PortNumber.uint32(2);
        PortEvent port = new PortEvent(DPID_1, portNumber);
        port.createStringAttribute("fuzz", "buzz");

        TestUtils.callMethod(theTopologyManager, "addSwitch",
                             SwitchEvent.class, sw);
        TestUtils.callMethod(theTopologyManager, "addPort",
                             PortEvent.class, port);

        // Check the topology structure
        TopologyInternal topology =
            (TopologyInternal) theTopologyManager.getTopology();
        SwitchEvent swInTopo = topology.getSwitchEvent(DPID_1);
        assertEquals(sw, swInTopo);
        assertTrue(swInTopo.isFrozen());
        assertEquals("bar", swInTopo.getStringAttribute("foo"));

        final SwitchPort switchPort = new SwitchPort(DPID_1, portNumber);
        PortEvent portInTopo = topology.getPortEvent(switchPort);
        assertEquals(port, portInTopo);
        assertTrue(portInTopo.isFrozen());
        assertEquals("buzz", portInTopo.getStringAttribute("fuzz"));

        // Check the events to be fired
        List<PortEvent> apiAddedPortEvents
            = TestUtils.getField(theTopologyManager, "apiAddedPortEvents");
        assertThat(apiAddedPortEvents, hasItem(port));
    }

    /**
     * Tests removing of a Port followed by removing of a Switch,
     * and the topology replica transformation.
     */
    @Test
    public void testRemovePortThenSwitch() {
        setupTopologyManager();

        SwitchEvent sw = new SwitchEvent(DPID_1);
        sw.createStringAttribute("foo", "bar");

        final PortNumber portNumber = PortNumber.uint32(2);
        PortEvent port = new PortEvent(DPID_1, portNumber);
        port.createStringAttribute("fuzz", "buzz");

        TestUtils.callMethod(theTopologyManager, "addSwitch",
                             SwitchEvent.class, sw);
        TestUtils.callMethod(theTopologyManager, "addPort",
                             PortEvent.class, port);

        // Check the topology structure
        TopologyInternal topology =
            (TopologyInternal) theTopologyManager.getTopology();
        SwitchEvent swInTopo = topology.getSwitchEvent(DPID_1);
        assertEquals(sw, swInTopo);
        assertTrue(swInTopo.isFrozen());
        assertEquals("bar", swInTopo.getStringAttribute("foo"));

        final SwitchPort switchPort = new SwitchPort(DPID_1, portNumber);
        PortEvent portInTopo = topology.getPortEvent(switchPort);
        assertEquals(port, portInTopo);
        assertTrue(portInTopo.isFrozen());
        assertEquals("buzz", portInTopo.getStringAttribute("fuzz"));

        // Remove in proper order
        TestUtils.callMethod(theTopologyManager, "removePort",
                            PortEvent.class, new PortEvent(port));
        TestUtils.callMethod(theTopologyManager, "removeSwitch",
                            SwitchEvent.class, new SwitchEvent(sw));


        // Check the events to be fired
        List<PortEvent> apiRemovedPortEvents
            = TestUtils.getField(theTopologyManager, "apiRemovedPortEvents");
        assertThat(apiRemovedPortEvents, hasItem(port));
        List<SwitchEvent> apiRemovedSwitchEvents
            = TestUtils.getField(theTopologyManager, "apiRemovedSwitchEvents");
        assertThat(apiRemovedSwitchEvents, hasItem(sw));
    }

    /**
     * Tests removing of a Switch without removing of a Port,
     * and the topology replica transformation.
     */
    @Test
    public void testRemoveSwitchWithoutPortRemoval() {
        setupTopologyManager();

        SwitchEvent sw = new SwitchEvent(DPID_1);
        sw.createStringAttribute("foo", "bar");

        final PortNumber portNumber = PortNumber.uint32(2);
        PortEvent port = new PortEvent(DPID_1, portNumber);
        port.createStringAttribute("fuzz", "buzz");

        TestUtils.callMethod(theTopologyManager, "addSwitch",
                             SwitchEvent.class, sw);
        TestUtils.callMethod(theTopologyManager, "addPort",
                             PortEvent.class, port);

        // Check the topology structure
        TopologyInternal topology =
            (TopologyInternal) theTopologyManager.getTopology();
        SwitchEvent swInTopo = topology.getSwitchEvent(DPID_1);
        assertEquals(sw, swInTopo);
        assertTrue(swInTopo.isFrozen());
        assertEquals("bar", swInTopo.getStringAttribute("foo"));

        final SwitchPort switchPort = new SwitchPort(DPID_1, portNumber);
        PortEvent portInTopo = topology.getPortEvent(switchPort);
        assertEquals(port, portInTopo);
        assertTrue(portInTopo.isFrozen());
        assertEquals("buzz", portInTopo.getStringAttribute("fuzz"));

        // Remove in in-proper order
//        TestUtils.callMethod(theTopologyManager, "removePort",
//                            PortEvent.class, new PortEvent(port));
        TestUtils.callMethod(theTopologyManager, "removeSwitch",
                            SwitchEvent.class, new SwitchEvent(sw));


        // Check the events to be fired
        // The outcome should be the same as #testRemovePortThenSwitch
        List<PortEvent> apiRemovedPortEvents
            = TestUtils.getField(theTopologyManager, "apiRemovedPortEvents");
        assertThat(apiRemovedPortEvents, hasItem(port));
        List<SwitchEvent> apiRemovedSwitchEvents
            = TestUtils.getField(theTopologyManager, "apiRemovedSwitchEvents");
        assertThat(apiRemovedSwitchEvents, hasItem(sw));
    }

    /**
     * Tests adding of a Link and the topology replica transformation.
     */
    @Test
    public void testAddLink() {
        setupTopologyManager();

        SwitchEvent sw = new SwitchEvent(DPID_1);
        sw.createStringAttribute("foo", "bar");

        final PortNumber portNumberA = PortNumber.uint32(2);
        PortEvent portA = new PortEvent(DPID_1, portNumberA);
        portA.createStringAttribute("fuzz", "buzz");

        final PortNumber portNumberB = PortNumber.uint32(3);
        PortEvent portB = new PortEvent(DPID_1, portNumberB);
        portB.createStringAttribute("fizz", "buz");

        LinkEvent linkA = new LinkEvent(portA.getSwitchPort(),
                                        portB.getSwitchPort());
        linkA.createStringAttribute(TopologyElement.TYPE,
                                    TopologyElement.TYPE_OPTICAL_LAYER);
        LinkEvent linkB = new LinkEvent(portB.getSwitchPort(),
                                        portA.getSwitchPort());
        linkB.createStringAttribute(TopologyElement.TYPE,
                                    TopologyElement.TYPE_OPTICAL_LAYER);

        TestUtils.callMethod(theTopologyManager, "addSwitch",
                             SwitchEvent.class, sw);
        TestUtils.callMethod(theTopologyManager, "addPort",
                             PortEvent.class, portA);
        TestUtils.callMethod(theTopologyManager, "addPort",
                             PortEvent.class, portB);
        TestUtils.callMethod(theTopologyManager, "addLink",
                             LinkEvent.class, linkA);
        TestUtils.callMethod(theTopologyManager, "addLink",
                             LinkEvent.class, linkB);

        // Check the topology structure
        TopologyInternal topology =
            (TopologyInternal) theTopologyManager.getTopology();
        SwitchEvent swInTopo = topology.getSwitchEvent(DPID_1);
        assertEquals(sw, swInTopo);
        assertTrue(swInTopo.isFrozen());
        assertEquals("bar", swInTopo.getStringAttribute("foo"));

        final SwitchPort switchPortA = new SwitchPort(DPID_1, portNumberA);
        PortEvent portAInTopo = topology.getPortEvent(switchPortA);
        assertEquals(portA, portAInTopo);
        assertTrue(portAInTopo.isFrozen());
        assertEquals("buzz", portAInTopo.getStringAttribute("fuzz"));

        final SwitchPort switchPortB = new SwitchPort(DPID_1, portNumberB);
        PortEvent portBInTopo = topology.getPortEvent(switchPortB);
        assertEquals(portB, portBInTopo);
        assertTrue(portBInTopo.isFrozen());
        assertEquals("buz", portBInTopo.getStringAttribute("fizz"));

        LinkEvent linkAInTopo = topology.getLinkEvent(linkA.getLinkTuple());
        assertEquals(linkA, linkAInTopo);
        assertTrue(linkAInTopo.isFrozen());
        assertEquals(TopologyElement.TYPE_OPTICAL_LAYER,
                     linkAInTopo.getType());

        LinkEvent linkBInTopo = topology.getLinkEvent(linkB.getLinkTuple());
        assertEquals(linkB, linkBInTopo);
        assertTrue(linkBInTopo.isFrozen());
        assertEquals(TopologyElement.TYPE_OPTICAL_LAYER,
                     linkBInTopo.getType());

        // Check the events to be fired
        List<LinkEvent> apiAddedLinkEvents
            = TestUtils.getField(theTopologyManager, "apiAddedLinkEvents");
        assertThat(apiAddedLinkEvents, containsInAnyOrder(linkA, linkB));
    }

    /**
     * Tests removing of a Link without removing of a Host, and the topology
     * replica transformation.
     */
    @Test
    public void testAddLinkKickingOffHost() {
        setupTopologyManager();

        SwitchEvent sw = new SwitchEvent(DPID_1);
        sw.createStringAttribute("foo", "bar");

        final PortNumber portNumberA = PortNumber.uint32(2);
        PortEvent portA = new PortEvent(DPID_1, portNumberA);
        portA.createStringAttribute("fuzz", "buzz");

        final PortNumber portNumberB = PortNumber.uint32(3);
        PortEvent portB = new PortEvent(DPID_1, portNumberB);
        portB.createStringAttribute("fizz", "buz");

        final PortNumber portNumberC = PortNumber.uint32(4);
        PortEvent portC = new PortEvent(DPID_1, portNumberC);
        portC.createStringAttribute("fizz", "buz");

        final MACAddress macA = MACAddress.valueOf(666L);
        HostEvent hostA = new HostEvent(macA);
        hostA.addAttachmentPoint(portA.getSwitchPort());
        final long timestampA = 392893200L;
        hostA.setLastSeenTime(timestampA);

        final MACAddress macB = MACAddress.valueOf(999L);
        HostEvent hostB = new HostEvent(macB);
        hostB.addAttachmentPoint(portB.getSwitchPort());
        hostB.addAttachmentPoint(portC.getSwitchPort());
        final long timestampB = 392893201L;
        hostB.setLastSeenTime(timestampB);


        LinkEvent linkA = new LinkEvent(portA.getSwitchPort(),
                                        portB.getSwitchPort());
        linkA.createStringAttribute(TopologyElement.TYPE,
                                    TopologyElement.TYPE_OPTICAL_LAYER);
        LinkEvent linkB = new LinkEvent(portB.getSwitchPort(),
                                        portA.getSwitchPort());
        linkB.createStringAttribute(TopologyElement.TYPE,
                                    TopologyElement.TYPE_OPTICAL_LAYER);

        TestUtils.callMethod(theTopologyManager, "addSwitch",
                             SwitchEvent.class, sw);
        TestUtils.callMethod(theTopologyManager, "addPort",
                             PortEvent.class, portA);
        TestUtils.callMethod(theTopologyManager, "addPort",
                             PortEvent.class, portB);
        TestUtils.callMethod(theTopologyManager, "addPort",
                             PortEvent.class, portC);
        TestUtils.callMethod(theTopologyManager, "addHost",
                             HostEvent.class, hostA);
        TestUtils.callMethod(theTopologyManager, "addHost",
                             HostEvent.class, hostB);

        TestUtils.callMethod(theTopologyManager, "addLink",
                             LinkEvent.class, linkA);
        TestUtils.callMethod(theTopologyManager, "addLink",
                             LinkEvent.class, linkB);

        // Check the topology structure
        TopologyInternal topology =
            (TopologyInternal) theTopologyManager.getTopology();
        SwitchEvent swInTopo = topology.getSwitchEvent(DPID_1);
        assertEquals(sw, swInTopo);
        assertTrue(swInTopo.isFrozen());
        assertEquals("bar", swInTopo.getStringAttribute("foo"));

        final SwitchPort switchPortA = new SwitchPort(DPID_1, portNumberA);
        PortEvent portAInTopo = topology.getPortEvent(switchPortA);
        assertEquals(portA, portAInTopo);
        assertTrue(portAInTopo.isFrozen());
        assertEquals("buzz", portAInTopo.getStringAttribute("fuzz"));

        final SwitchPort switchPortB = new SwitchPort(DPID_1, portNumberB);
        PortEvent portBInTopo = topology.getPortEvent(switchPortB);
        assertEquals(portB, portBInTopo);
        assertTrue(portBInTopo.isFrozen());
        assertEquals("buz", portBInTopo.getStringAttribute("fizz"));

        // hostA expected to be removed
        assertNull(topology.getHostEvent(macA));
        // hostB expected to be there with reduced attachment point
        HostEvent hostBrev = new HostEvent(macB);
        hostBrev.addAttachmentPoint(portC.getSwitchPort());
        hostBrev.setLastSeenTime(timestampB);
        hostBrev.freeze();
        assertEquals(hostBrev, topology.getHostEvent(macB));


        LinkEvent linkAInTopo = topology.getLinkEvent(linkA.getLinkTuple());
        assertEquals(linkA, linkAInTopo);
        assertTrue(linkAInTopo.isFrozen());
        assertEquals(TopologyElement.TYPE_OPTICAL_LAYER,
                     linkAInTopo.getType());

        LinkEvent linkBInTopo = topology.getLinkEvent(linkB.getLinkTuple());
        assertEquals(linkB, linkBInTopo);
        assertTrue(linkBInTopo.isFrozen());
        assertEquals(TopologyElement.TYPE_OPTICAL_LAYER,
                     linkBInTopo.getType());

        // Check the events to be fired
        List<HostEvent> apiAddedHostEvents
            = TestUtils.getField(theTopologyManager, "apiAddedHostEvents");
        assertThat(apiAddedHostEvents, hasItem(hostBrev));

        List<HostEvent> apiRemovedHostEvents
            = TestUtils.getField(theTopologyManager, "apiRemovedHostEvents");
        assertThat(apiRemovedHostEvents, hasItem(hostA));
        List<LinkEvent> apiAddedLinkEvents
            = TestUtils.getField(theTopologyManager, "apiAddedLinkEvents");
        assertThat(apiAddedLinkEvents, containsInAnyOrder(linkA, linkB));
    }

    /**
     * Tests removing of a Link and the topology replica transformation.
     */
    @Test
    public void testRemoveLink() {
        setupTopologyManager();

        SwitchEvent sw = new SwitchEvent(DPID_1);
        sw.createStringAttribute("foo", "bar");

        final PortNumber portNumberA = PortNumber.uint32(2);
        PortEvent portA = new PortEvent(DPID_1, portNumberA);
        portA.createStringAttribute("fuzz", "buzz");

        final PortNumber portNumberB = PortNumber.uint32(3);
        PortEvent portB = new PortEvent(DPID_1, portNumberB);
        portB.createStringAttribute("fizz", "buz");

        LinkEvent linkA = new LinkEvent(portA.getSwitchPort(),
                                        portB.getSwitchPort());
        linkA.createStringAttribute(TopologyElement.TYPE,
                                    TopologyElement.TYPE_OPTICAL_LAYER);
        LinkEvent linkB = new LinkEvent(portB.getSwitchPort(),
                                        portA.getSwitchPort());
        linkB.createStringAttribute(TopologyElement.TYPE,
                                    TopologyElement.TYPE_OPTICAL_LAYER);

        TestUtils.callMethod(theTopologyManager, "addSwitch",
                             SwitchEvent.class, sw);
        TestUtils.callMethod(theTopologyManager, "addPort",
                             PortEvent.class, portA);
        TestUtils.callMethod(theTopologyManager, "addPort",
                             PortEvent.class, portB);
        TestUtils.callMethod(theTopologyManager, "addLink",
                             LinkEvent.class, linkA);
        TestUtils.callMethod(theTopologyManager, "addLink",
                             LinkEvent.class, linkB);

        // Check the topology structure
        TopologyInternal topology =
            (TopologyInternal) theTopologyManager.getTopology();
        SwitchEvent swInTopo = topology.getSwitchEvent(DPID_1);
        assertEquals(sw, swInTopo);
        assertTrue(swInTopo.isFrozen());
        assertEquals("bar", swInTopo.getStringAttribute("foo"));

        final SwitchPort switchPortA = new SwitchPort(DPID_1, portNumberA);
        PortEvent portAInTopo = topology.getPortEvent(switchPortA);
        assertEquals(portA, portAInTopo);
        assertTrue(portAInTopo.isFrozen());
        assertEquals("buzz", portAInTopo.getStringAttribute("fuzz"));

        final SwitchPort switchPortB = new SwitchPort(DPID_1, portNumberB);
        PortEvent portBInTopo = topology.getPortEvent(switchPortB);
        assertEquals(portB, portBInTopo);
        assertTrue(portBInTopo.isFrozen());
        assertEquals("buz", portBInTopo.getStringAttribute("fizz"));

        LinkEvent linkAInTopo = topology.getLinkEvent(linkA.getLinkTuple());
        assertEquals(linkA, linkAInTopo);
        assertTrue(linkAInTopo.isFrozen());
        assertEquals(TopologyElement.TYPE_OPTICAL_LAYER,
                     linkAInTopo.getType());


        LinkEvent linkBInTopo = topology.getLinkEvent(linkB.getLinkTuple());
        assertEquals(linkB, linkBInTopo);
        assertTrue(linkBInTopo.isFrozen());
        assertEquals(TopologyElement.TYPE_OPTICAL_LAYER,
                     linkBInTopo.getType());

        // Check the events to be fired
        // FIXME if link flapped (linkA in this scenario),
        //  linkA appears in both removed and added is this expected behavior?
        List<LinkEvent> apiAddedLinkEvents
            = TestUtils.getField(theTopologyManager, "apiAddedLinkEvents");
        assertThat(apiAddedLinkEvents, containsInAnyOrder(linkA, linkB));

        // Clear the events before removing the link
        apiAddedLinkEvents.clear();

        // Remove the link
        TestUtils.callMethod(theTopologyManager, "removeLink",
                             LinkEvent.class, new LinkEvent(linkA));

        LinkEvent linkANotInTopo = topology.getLinkEvent(linkA.getLinkTuple());
        assertNull(linkANotInTopo);

        List<LinkEvent> apiRemovedLinkEvents
            = TestUtils.getField(theTopologyManager, "apiRemovedLinkEvents");
        assertThat(apiRemovedLinkEvents, hasItem(linkA));
    }

    /**
     * Tests adding of a Host without adding of a Link, and the topology
     * replica transformation.
     */
    @Test
    public void testAddHostIgnoredByLink() {
        setupTopologyManager();

        SwitchEvent sw = new SwitchEvent(DPID_1);
        sw.createStringAttribute("foo", "bar");

        final PortNumber portNumberA = PortNumber.uint32(2);
        PortEvent portA = new PortEvent(DPID_1, portNumberA);
        portA.createStringAttribute("fuzz", "buzz");

        final PortNumber portNumberB = PortNumber.uint32(3);
        PortEvent portB = new PortEvent(DPID_1, portNumberB);
        portB.createStringAttribute("fizz", "buz");

        final PortNumber portNumberC = PortNumber.uint32(4);
        PortEvent portC = new PortEvent(DPID_1, portNumberC);
        portC.createStringAttribute("fizz", "buz");

        LinkEvent linkA = new LinkEvent(portA.getSwitchPort(),
                                        portB.getSwitchPort());
        linkA.createStringAttribute(TopologyElement.TYPE,
                                    TopologyElement.TYPE_OPTICAL_LAYER);
        LinkEvent linkB = new LinkEvent(portB.getSwitchPort(),
                                        portA.getSwitchPort());
        linkB.createStringAttribute(TopologyElement.TYPE,
                                    TopologyElement.TYPE_OPTICAL_LAYER);

        TestUtils.callMethod(theTopologyManager, "addSwitch",
                             SwitchEvent.class, sw);
        TestUtils.callMethod(theTopologyManager, "addPort",
                             PortEvent.class, portA);
        TestUtils.callMethod(theTopologyManager, "addPort",
                             PortEvent.class, portB);
        TestUtils.callMethod(theTopologyManager, "addPort",
                             PortEvent.class, portC);
        TestUtils.callMethod(theTopologyManager, "addLink",
                             LinkEvent.class, linkA);
        TestUtils.callMethod(theTopologyManager, "addLink",
                             LinkEvent.class, linkB);

        // Add hostA attached to a port which already has a link
        final MACAddress macA = MACAddress.valueOf(666L);
        HostEvent hostA = new HostEvent(macA);
        hostA.addAttachmentPoint(portA.getSwitchPort());
        final long timestampA = 392893200L;
        hostA.setLastSeenTime(timestampA);

        TestUtils.callMethod(theTopologyManager, "addHost",
                             HostEvent.class, hostA);

        // Add hostB attached to multiple ports,
        // some of them which already has a link
        final MACAddress macB = MACAddress.valueOf(999L);
        HostEvent hostB = new HostEvent(macB);
        hostB.addAttachmentPoint(portB.getSwitchPort());
        hostB.addAttachmentPoint(portC.getSwitchPort());
        final long timestampB = 392893201L;
        hostB.setLastSeenTime(timestampB);

        TestUtils.callMethod(theTopologyManager, "addHost",
                             HostEvent.class, hostB);

        // Check the topology structure
        TopologyInternal topology =
            (TopologyInternal) theTopologyManager.getTopology();
        SwitchEvent swInTopo = topology.getSwitchEvent(DPID_1);
        assertEquals(sw, swInTopo);
        assertTrue(swInTopo.isFrozen());
        assertEquals("bar", swInTopo.getStringAttribute("foo"));

        final SwitchPort switchPortA = new SwitchPort(DPID_1, portNumberA);
        PortEvent portAInTopo = topology.getPortEvent(switchPortA);
        assertEquals(portA, portAInTopo);
        assertTrue(portAInTopo.isFrozen());
        assertEquals("buzz", portAInTopo.getStringAttribute("fuzz"));

        final SwitchPort switchPortB = new SwitchPort(DPID_1, portNumberB);
        PortEvent portBInTopo = topology.getPortEvent(switchPortB);
        assertEquals(portB, portBInTopo);
        assertTrue(portBInTopo.isFrozen());
        assertEquals("buz", portBInTopo.getStringAttribute("fizz"));

        // hostA expected to be completely ignored
        assertNull(topology.getHostEvent(macA));
        // hostB expected to be there with reduced attachment point
        HostEvent hostBrev = new HostEvent(macB);
        hostBrev.addAttachmentPoint(portC.getSwitchPort());
        hostBrev.setLastSeenTime(timestampB);
        hostBrev.freeze();
        assertEquals(hostBrev, topology.getHostEvent(macB));


        LinkEvent linkAInTopo = topology.getLinkEvent(linkA.getLinkTuple());
        assertEquals(linkA, linkAInTopo);
        assertTrue(linkAInTopo.isFrozen());
        assertEquals(TopologyElement.TYPE_OPTICAL_LAYER,
                     linkAInTopo.getType());

        LinkEvent linkBInTopo = topology.getLinkEvent(linkB.getLinkTuple());
        assertEquals(linkB, linkBInTopo);
        assertTrue(linkBInTopo.isFrozen());
        assertEquals(TopologyElement.TYPE_OPTICAL_LAYER,
                     linkBInTopo.getType());

        // Check the events to be fired
        // hostB should be added with reduced attachment points
        List<HostEvent> apiAddedHostEvents
            = TestUtils.getField(theTopologyManager, "apiAddedHostEvents");
        assertThat(apiAddedHostEvents, hasItem(hostBrev));

        // hostA should not be ignored
        List<HostEvent> apiRemovedHostEvents
            = TestUtils.getField(theTopologyManager, "apiRemovedHostEvents");
        assertThat(apiRemovedHostEvents, not(hasItem(hostA)));

        List<LinkEvent> apiAddedLinkEvents
            = TestUtils.getField(theTopologyManager, "apiAddedLinkEvents");
        assertThat(apiAddedLinkEvents, containsInAnyOrder(linkA, linkB));
    }

    /**
     * Tests adding and moving of a Host, and the topology replica
     * transformation.
     */
    @Test
    public void testAddHostMove() {
        setupTopologyManager();

        SwitchEvent sw = new SwitchEvent(DPID_1);
        sw.createStringAttribute("foo", "bar");

        final PortNumber portNumberA = PortNumber.uint32(2);
        PortEvent portA = new PortEvent(DPID_1, portNumberA);
        portA.createStringAttribute("fuzz", "buzz");

        final PortNumber portNumberB = PortNumber.uint32(3);
        PortEvent portB = new PortEvent(DPID_1, portNumberB);
        portB.createStringAttribute("fizz", "buz");

        final PortNumber portNumberC = PortNumber.uint32(4);
        PortEvent portC = new PortEvent(DPID_1, portNumberC);
        portC.createStringAttribute("fizz", "buz");

        TestUtils.callMethod(theTopologyManager, "addSwitch",
                             SwitchEvent.class, sw);
        TestUtils.callMethod(theTopologyManager, "addPort",
                             PortEvent.class, portA);
        TestUtils.callMethod(theTopologyManager, "addPort",
                             PortEvent.class, portB);
        TestUtils.callMethod(theTopologyManager, "addPort",
                             PortEvent.class, portC);

        // Add hostA attached to a Port which already has a Link
        final MACAddress macA = MACAddress.valueOf(666L);
        HostEvent hostA = new HostEvent(macA);
        hostA.addAttachmentPoint(portA.getSwitchPort());
        final long timestampA = 392893200L;
        hostA.setLastSeenTime(timestampA);

        TestUtils.callMethod(theTopologyManager, "addHost",
                             HostEvent.class, hostA);


        // Check the topology structure
        TopologyInternal topology =
            (TopologyInternal) theTopologyManager.getTopology();
        SwitchEvent swInTopo = topology.getSwitchEvent(DPID_1);
        assertEquals(sw, swInTopo);
        assertTrue(swInTopo.isFrozen());
        assertEquals("bar", swInTopo.getStringAttribute("foo"));

        final SwitchPort switchPortA = new SwitchPort(DPID_1, portNumberA);
        PortEvent portAInTopo = topology.getPortEvent(switchPortA);
        assertEquals(portA, portAInTopo);
        assertTrue(portAInTopo.isFrozen());
        assertEquals("buzz", portAInTopo.getStringAttribute("fuzz"));

        final SwitchPort switchPortB = new SwitchPort(DPID_1, portNumberB);
        PortEvent portBInTopo = topology.getPortEvent(switchPortB);
        assertEquals(portB, portBInTopo);
        assertTrue(portBInTopo.isFrozen());
        assertEquals("buz", portBInTopo.getStringAttribute("fizz"));

        // hostA expected to be there
        assertEquals(hostA, topology.getHostEvent(macA));
        assertEquals(timestampA,
                     topology.getHostEvent(macA).getLastSeenTime());

        // Check the events to be fired
        // hostA should be added
        List<HostEvent> apiAddedHostEvents
            = TestUtils.getField(theTopologyManager, "apiAddedHostEvents");
        assertThat(apiAddedHostEvents, hasItem(hostA));


        // Clear the events before moving the Host
        apiAddedHostEvents.clear();

        HostEvent hostAmoved = new HostEvent(macA);
        hostAmoved.addAttachmentPoint(portB.getSwitchPort());
        final long timestampAmoved = 392893201L;
        hostAmoved.setLastSeenTime(timestampAmoved);

        TestUtils.callMethod(theTopologyManager, "addHost",
                             HostEvent.class, hostAmoved);

        assertEquals(hostAmoved, topology.getHostEvent(macA));
        assertEquals(timestampAmoved,
                     topology.getHostEvent(macA).getLastSeenTime());

        // hostA expected to be there with new attachment point
        apiAddedHostEvents
            = TestUtils.getField(theTopologyManager, "apiAddedHostEvents");
        assertThat(apiAddedHostEvents, hasItem(hostAmoved));

        // hostA is updated not removed
        List<HostEvent> apiRemovedHostEvents
            = TestUtils.getField(theTopologyManager, "apiRemovedHostEvents");
        assertThat(apiRemovedHostEvents, not(hasItem(hostA)));
    }

    /**
     * Tests processing of a Switch Mastership Event and the delivery of the
     * topology events.
     */
    @Test
    public void testProcessMastershipEvent() {
        List<EventEntry<TopologyEvent>> events = new LinkedList<>();
        EventEntry<TopologyEvent> eventEntry;
        TopologyEvent topologyEvent;

        setupTopologyManagerWithEventHandler();

        // Prepare the Mastership Event
        Role role = Role.MASTER;
        MastershipEvent mastershipEvent =
            new MastershipEvent(DPID_1, ONOS_INSTANCE_ID_1, role);
        topologyEvent = new TopologyEvent(mastershipEvent, ONOS_INSTANCE_ID_1);

        // Add the Mastership Event
        eventEntry = new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                                                   topologyEvent);
        events.add(eventEntry);

        // Process the events
        TestUtils.callMethod(theEventHandler, "processEvents",
                             List.class, events);

        // Check the fired events
        TopologyEvents topologyEvents = theTopologyListener.topologyEvents;
        assertNotNull(topologyEvents);
        assertThat(topologyEvents.getAddedMastershipEvents(),
                   hasItem(mastershipEvent));
        theTopologyListener.clear();
    }

    /**
     * Tests processing of a Switch Event, and the delivery of the topology
     * events.
     *
     * We test the following scenario:
     * - Switch Mastership Event is processed along with a Switch Event - both
     *   events should be delivered.
     */
    @Test
    public void testProcessSwitchEvent() {
        TopologyEvents topologyEvents;
        List<EventEntry<TopologyEvent>> events = new LinkedList<>();
        EventEntry<TopologyEvent> eventEntry;
        TopologyEvent topologyMastershipEvent;
        TopologyEvent topologySwitchEvent;

        setupTopologyManagerWithEventHandler();

        // Prepare the Mastership Event
        Role role = Role.MASTER;
        MastershipEvent mastershipEvent =
            new MastershipEvent(DPID_1, ONOS_INSTANCE_ID_1, role);
        topologyMastershipEvent = new TopologyEvent(mastershipEvent,
                                                    ONOS_INSTANCE_ID_1);

        // Prepare the Switch Event
        SwitchEvent switchEvent = new SwitchEvent(DPID_1);
        topologySwitchEvent = new TopologyEvent(switchEvent,
                                                ONOS_INSTANCE_ID_1);

        // Add the Mastership Event
        eventEntry = new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                                                   topologyMastershipEvent);
        events.add(eventEntry);

        // Add the Switch Event
        eventEntry = new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                                                   topologySwitchEvent);
        events.add(eventEntry);

        // Process the events
        TestUtils.callMethod(theEventHandler, "processEvents",
                             List.class, events);

        // Check the fired events
        topologyEvents = theTopologyListener.topologyEvents;
        assertNotNull(topologyEvents);
        assertThat(topologyEvents.getAddedMastershipEvents(),
                   hasItem(mastershipEvent));
        assertThat(topologyEvents.getAddedSwitchEvents(),
                   hasItem(switchEvent));
        theTopologyListener.clear();
    }

    /**
     * Tests processing of a misordered Switch Event, and the delivery of the
     * topology events.
     *
     * We test the following scenario:
     * - Only a Switch Event is processed first, later followed by a Switch
     *   Mastership Event - the Switch Event should be delivered after the
     *   Switch Mastership Event is processed.
     */
    @Test
    public void testProcessMisorderedSwitchEvent() {
        TopologyEvents topologyEvents;
        List<EventEntry<TopologyEvent>> events = new LinkedList<>();
        EventEntry<TopologyEvent> eventEntry;
        TopologyEvent topologyMastershipEvent;
        TopologyEvent topologySwitchEvent;

        setupTopologyManagerWithEventHandler();

        // Prepare the Mastership Event
        Role role = Role.MASTER;
        MastershipEvent mastershipEvent =
            new MastershipEvent(DPID_1, ONOS_INSTANCE_ID_1, role);
        topologyMastershipEvent = new TopologyEvent(mastershipEvent,
                                                    ONOS_INSTANCE_ID_1);

        // Prepare the Switch Event
        SwitchEvent switchEvent = new SwitchEvent(DPID_1);
        topologySwitchEvent = new TopologyEvent(switchEvent,
                                                ONOS_INSTANCE_ID_1);

        // Add the Switch Event
        eventEntry = new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                                                   topologySwitchEvent);
        events.add(eventEntry);

        // Process the events
        TestUtils.callMethod(theEventHandler, "processEvents",
                             List.class, events);

        // Check the fired events: no events should be fired
        topologyEvents = theTopologyListener.topologyEvents;
        assertNull(topologyEvents);
        theTopologyListener.clear();
        events.clear();

        // Add the Mastership Event
        eventEntry = new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                                                   topologyMastershipEvent);
        events.add(eventEntry);

        // Process the events
        TestUtils.callMethod(theEventHandler, "processEvents",
                             List.class, events);

        // Check the fired events: both events should be fired
        topologyEvents = theTopologyListener.topologyEvents;
        assertNotNull(topologyEvents);
        assertThat(topologyEvents.getAddedMastershipEvents(),
                   hasItem(mastershipEvent));
        assertThat(topologyEvents.getAddedSwitchEvents(),
                   hasItem(switchEvent));
        theTopologyListener.clear();
    }

    /**
     * Tests processing of a Switch Event with Mastership Event from
     * another ONOS instance, and the delivery of the topology events.
     *
     * We test the following scenario:
     * - Only a Switch Event is processed first, later followed by a Switch
     *   Mastership Event from another ONOS instance - only the Switch
     *   Mastership Event should be delivered.
     */
    @Test
    public void testProcessSwitchEventNoMastership() {
        TopologyEvents topologyEvents;
        List<EventEntry<TopologyEvent>> events = new LinkedList<>();
        EventEntry<TopologyEvent> eventEntry;
        TopologyEvent topologyMastershipEvent;
        TopologyEvent topologySwitchEvent;

        setupTopologyManagerWithEventHandler();

        // Prepare the Mastership Event
        Role role = Role.MASTER;
        MastershipEvent mastershipEvent =
            new MastershipEvent(DPID_2, ONOS_INSTANCE_ID_2, role);
        topologyMastershipEvent = new TopologyEvent(mastershipEvent,
                                                    ONOS_INSTANCE_ID_2);

        // Prepare the Switch Event
        // NOTE: The originator (ONOS_INSTANCE_ID_1) is NOT the Master
        SwitchEvent switchEvent = new SwitchEvent(DPID_2);
        topologySwitchEvent = new TopologyEvent(switchEvent,
                                                ONOS_INSTANCE_ID_1);

        // Add the Switch Event
        eventEntry = new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                                                   topologySwitchEvent);
        events.add(eventEntry);

        // Process the events
        TestUtils.callMethod(theEventHandler, "processEvents",
                             List.class, events);

        // Check the fired events: no events should be fired
        topologyEvents = theTopologyListener.topologyEvents;
        assertNull(topologyEvents);
        theTopologyListener.clear();
        events.clear();

        // Add the Mastership Event
        eventEntry = new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                                                   topologyMastershipEvent);
        events.add(eventEntry);

        // Process the events
        TestUtils.callMethod(theEventHandler, "processEvents",
                             List.class, events);

        // Check the fired events: only the Mastership event should be fired
        topologyEvents = theTopologyListener.topologyEvents;
        assertNotNull(topologyEvents);
        assertThat(topologyEvents.getAddedMastershipEvents(),
                   hasItem(mastershipEvent));
        assertThat(topologyEvents.getAddedSwitchEvents(), is(empty()));
        theTopologyListener.clear();
    }

    /**
     * Tests processing of Switch Events with Mastership switchover between
     * two ONOS instance, and the delivery of the topology events.
     *
     * We test the following scenario:
     * - Initially, a Mastership Event and a Switch Event from one ONOS
     *   instance are processed - both events should be delivered.
     * - Later, a Mastership Event and a Switch event from another ONOS
     *   instances are processed - both events should be delivered.
     * - Finally, a REMOVE Switch Event is received from the first ONOS
     *   instance - no event should be delivered.
     *
     * @throws RegistryException
     */
    @Test
    public void testProcessSwitchMastershipSwitchover()
                        throws RegistryException {
        TopologyEvents topologyEvents;
        List<EventEntry<TopologyEvent>> events = new LinkedList<>();
        EventEntry<TopologyEvent> eventEntry;
        TopologyEvent topologyMastershipEvent;
        TopologyEvent topologySwitchEvent;

        setupTopologyManagerWithEventHandler();

        // Prepare the Mastership Event from the first ONOS instance
        Role role = Role.MASTER;
        MastershipEvent mastershipEvent =
            new MastershipEvent(DPID_1, ONOS_INSTANCE_ID_1, role);
        topologyMastershipEvent = new TopologyEvent(mastershipEvent,
                                                    ONOS_INSTANCE_ID_1);

        // Prepare the Switch Event from the first ONOS instance
        SwitchEvent switchEvent = new SwitchEvent(DPID_1);
        topologySwitchEvent = new TopologyEvent(switchEvent,
                                                ONOS_INSTANCE_ID_1);

        // Add the Mastership Event
        eventEntry = new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                                                   topologyMastershipEvent);
        events.add(eventEntry);

        // Add the Switch Event
        eventEntry = new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                                                   topologySwitchEvent);
        events.add(eventEntry);

        // Process the events
        TestUtils.callMethod(theEventHandler, "processEvents",
                             List.class, events);

        // Check the fired events: both events should be fired
        topologyEvents = theTopologyListener.topologyEvents;
        assertNotNull(topologyEvents);
        assertThat(topologyEvents.getAddedMastershipEvents(),
                   hasItem(mastershipEvent));
        assertThat(topologyEvents.getAddedSwitchEvents(),
                   hasItem(switchEvent));
        theTopologyListener.clear();
        events.clear();

        //
        // Update the Registry Service, so the second ONOS instance is the
        // Master.
        //
        reset(registryService);
        expect(registryService.getControllerForSwitch(DPID_1.value()))
            .andReturn(ONOS_INSTANCE_ID_2.toString()).anyTimes();
        replay(registryService);

        // Prepare the Mastership Event from the second ONOS instance
        role = Role.MASTER;
        mastershipEvent = new MastershipEvent(DPID_1,
                                              ONOS_INSTANCE_ID_2, role);
        topologyMastershipEvent = new TopologyEvent(mastershipEvent,
                                                    ONOS_INSTANCE_ID_2);

        // Prepare the Switch Event from second ONOS instance
        switchEvent = new SwitchEvent(DPID_1);
        topologySwitchEvent = new TopologyEvent(switchEvent,
                                                ONOS_INSTANCE_ID_2);

        // Add the Mastership Event
        eventEntry = new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                                                   topologyMastershipEvent);
        events.add(eventEntry);

        // Add the Switch Event
        eventEntry = new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                                                   topologySwitchEvent);
        events.add(eventEntry);

        // Process the events
        TestUtils.callMethod(theEventHandler, "processEvents",
                             List.class, events);

        // Check the fired events: both events should be fired
        topologyEvents = theTopologyListener.topologyEvents;
        assertNotNull(topologyEvents);
        assertThat(topologyEvents.getAddedMastershipEvents(),
                   hasItem(mastershipEvent));
        assertThat(topologyEvents.getAddedSwitchEvents(),
                   hasItem(switchEvent));
        theTopologyListener.clear();
        events.clear();

        // Prepare the REMOVE Switch Event from first ONOS instance
        switchEvent = new SwitchEvent(DPID_1);
        topologySwitchEvent = new TopologyEvent(switchEvent,
                                                ONOS_INSTANCE_ID_1);
        // Add the Switch Event
        eventEntry = new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_REMOVE,
                                                   topologySwitchEvent);
        events.add(eventEntry);

        // Process the events
        TestUtils.callMethod(theEventHandler, "processEvents",
                             List.class, events);

        // Check the fired events: no events should be fired
        topologyEvents = theTopologyListener.topologyEvents;
        assertNull(topologyEvents);
        theTopologyListener.clear();
        events.clear();
    }

    /**
     * Tests processing of Configured Switch Events with Mastership switchover
     * between two ONOS instance, and the delivery of the topology events.
     * <p/>
     * NOTE: This test is similar to testProcessSwitchMastershipSwitchover()
     * except that the topology and all events are considered as statically
     * configured.
     * <p/>
     * We test the following scenario:
     * - Initially, a Mastership Event and a Switch Event from one ONOS
     *   instance are processed - both events should be delivered.
     * - Later, a Mastership Event and a Switch event from another ONOS
     *   instances are processed - both events should be delivered.
     */
    @Test
    public void testProcessConfiguredSwitchMastershipSwitchover() {
        TopologyEvents topologyEvents;
        List<EventEntry<TopologyEvent>> events = new LinkedList<>();
        EventEntry<TopologyEvent> eventEntry;
        TopologyEvent topologyMastershipEvent;
        TopologyEvent topologySwitchEvent;

        setupTopologyManagerWithEventHandler();

        // Reset the Registry Service so it is not used
        reset(registryService);

        // Prepare the Mastership Event from the first ONOS instance
        Role role = Role.MASTER;
        MastershipEvent mastershipEvent =
            new MastershipEvent(DPID_1, ONOS_INSTANCE_ID_1, role);
        mastershipEvent.createStringAttribute(
                TopologyElement.ELEMENT_CONFIG_STATE,
                ConfigState.CONFIGURED.toString());
        topologyMastershipEvent = new TopologyEvent(mastershipEvent,
                                                    ONOS_INSTANCE_ID_1);

        // Prepare the Switch Event from the first ONOS instance
        SwitchEvent switchEvent = new SwitchEvent(DPID_1);
        switchEvent.createStringAttribute(
                TopologyElement.ELEMENT_CONFIG_STATE,
                ConfigState.CONFIGURED.toString());
        topologySwitchEvent = new TopologyEvent(switchEvent,
                                                ONOS_INSTANCE_ID_1);

        // Add the Mastership Event
        eventEntry = new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                                                   topologyMastershipEvent);
        events.add(eventEntry);

        // Add the Switch Event
        eventEntry = new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                                                   topologySwitchEvent);
        events.add(eventEntry);

        // Process the events
        TestUtils.callMethod(theEventHandler, "processEvents",
                             List.class, events);

        // Check the fired events: both events should be fired
        topologyEvents = theTopologyListener.topologyEvents;
        assertNotNull(topologyEvents);
        assertThat(topologyEvents.getAddedMastershipEvents(),
                   hasItem(mastershipEvent));
        assertThat(topologyEvents.getAddedSwitchEvents(),
                   hasItem(switchEvent));
        theTopologyListener.clear();
        events.clear();

        // Prepare the Mastership Event from the second ONOS instance
        role = Role.MASTER;
        mastershipEvent = new MastershipEvent(DPID_1,
                                              ONOS_INSTANCE_ID_2, role);
        mastershipEvent.createStringAttribute(
                TopologyElement.ELEMENT_CONFIG_STATE,
                ConfigState.CONFIGURED.toString());
        topologyMastershipEvent = new TopologyEvent(mastershipEvent,
                                                    ONOS_INSTANCE_ID_2);

        // Prepare the Switch Event from second ONOS instance
        switchEvent = new SwitchEvent(DPID_1);
        switchEvent.createStringAttribute(
                TopologyElement.ELEMENT_CONFIG_STATE,
                ConfigState.CONFIGURED.toString());
        topologySwitchEvent = new TopologyEvent(switchEvent,
                                                ONOS_INSTANCE_ID_2);

        // Add the Mastership Event
        eventEntry = new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                                                   topologyMastershipEvent);
        events.add(eventEntry);

        // Add the Switch Event
        eventEntry = new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_ADD,
                                                   topologySwitchEvent);
        events.add(eventEntry);

        // Process the events
        TestUtils.callMethod(theEventHandler, "processEvents",
                             List.class, events);

        // Check the fired events: both events should be fired
        topologyEvents = theTopologyListener.topologyEvents;
        assertNotNull(topologyEvents);
        assertThat(topologyEvents.getAddedMastershipEvents(),
                   hasItem(mastershipEvent));
        assertThat(topologyEvents.getAddedSwitchEvents(),
                   hasItem(switchEvent));
        theTopologyListener.clear();
        events.clear();

        // Prepare the REMOVE Switch Event from first ONOS instance
        //
        // NOTE: This event only is explicitly marked as NOT_CONFIGURED,
        // otherwise it will override the previous configuration events.
        //
        switchEvent = new SwitchEvent(DPID_1);
        switchEvent.createStringAttribute(
                TopologyElement.ELEMENT_CONFIG_STATE,
                ConfigState.NOT_CONFIGURED.toString());
        topologySwitchEvent = new TopologyEvent(switchEvent,
                                                ONOS_INSTANCE_ID_1);
        // Add the Switch Event
        eventEntry = new EventEntry<TopologyEvent>(EventEntry.Type.ENTRY_REMOVE,
                                                   topologySwitchEvent);
        events.add(eventEntry);

        // Process the events
        TestUtils.callMethod(theEventHandler, "processEvents",
                             List.class, events);

        // Check the fired events: no events should be fired
        topologyEvents = theTopologyListener.topologyEvents;
        assertNull(topologyEvents);
        theTopologyListener.clear();
        events.clear();
    }
}
