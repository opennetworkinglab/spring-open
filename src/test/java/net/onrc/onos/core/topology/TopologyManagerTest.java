package net.onrc.onos.core.topology;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.datagrid.IDatagridService;
import net.onrc.onos.core.datagrid.IEventChannel;
import net.onrc.onos.core.datagrid.IEventChannelListener;
import net.onrc.onos.core.metrics.OnosMetrics;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;
import net.onrc.onos.core.util.TestUtils;

import com.codahale.metrics.MetricFilter;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the TopologyManager class in the Topology module.
 * These test cases only check the sanity of functions in the TopologyManager.
 * Note that we do not test the eventHandler functions in the TopologyManager class.
 * DatagridService, DataStoreService, eventChannel, and controllerRegistryService are mocked out.
 */
public class TopologyManagerTest {
    private TopologyManager theTopologyManager;
    private final String eventChannelName = "onos.topology";
    private IEventChannel<byte[], TopologyEvent> eventChannel;
    private IDatagridService datagridService;
    private TopologyDatastore dataStoreService;
    private IControllerRegistryService registryService;
    private CopyOnWriteArrayList<ITopologyListener> topologyListeners;
    private Collection<TopologyEvent> allTopologyEvents;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        // Mock objects for testing
        datagridService = EasyMock.createNiceMock(IDatagridService.class);
        dataStoreService = EasyMock.createNiceMock(TopologyDatastore.class);
        registryService = createMock(IControllerRegistryService.class);
        eventChannel = EasyMock.createNiceMock(IEventChannel.class);

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

        replay(datagridService);
        replay(registryService);
        replay(dataStoreService);

        allTopologyEvents = new CopyOnWriteArrayList<>();
        expect(eventChannel.getAllEntries())
            .andReturn(allTopologyEvents).anyTimes();
    }

    private void setupTopologyManager() {
        // Create a topologyManager object for testing
        topologyListeners = new CopyOnWriteArrayList<>();
        theTopologyManager = new TopologyManager(registryService, topologyListeners);

        // replace EventHandler to avoid thread from starting
        TestUtils.setField(theTopologyManager, "eventHandler",
            EasyMock.createNiceMock(TopologyManager.EventHandler.class));
        theTopologyManager.startup(datagridService);

        // replace data store with Mocked object
        TestUtils.setField(theTopologyManager, "datastore", dataStoreService);
    }

    @After
    public void tearDown() throws Exception {
        OnosMetrics.removeMatching(MetricFilter.ALL);
    }

    /**
     * Test the Switch discovered and Port discovered functions.
     */
    @Test
    public void testPutSwitchAndPortDiscoveryEvent() {
        // Mock the eventChannel functions first
        eventChannel.addEntry(anyObject(byte[].class),
                anyObject(TopologyEvent.class));
        EasyMock.expectLastCall().times(3, 3); // (1 switch + 1 port), 1 port
        replay(eventChannel);

        setupTopologyManager();

        // mockSwitch has one port
        Dpid swDPId = new Dpid(100L);
        PortNumber portId = new PortNumber((short) 1);

        // Generate a new switch event along with a port event
        SwitchEvent switchEvent = new SwitchEvent(swDPId);

        Collection<PortEvent> portEvents = new ArrayList<PortEvent>();
        portEvents.add(new PortEvent(swDPId, portId));

        // Call the topologyManager function for adding a switch
        theTopologyManager.putSwitchDiscoveryEvent(switchEvent, portEvents);

        for (PortEvent portEvent : portEvents) {
            // Call the topologyManager function for adding a port
            theTopologyManager.putPortDiscoveryEvent(portEvent);
        }

        // Verify the function calls
        verify(eventChannel);

    }

    /**
     * Test the switch and port removed functions.
     */
    @Test
    public void testRemoveSwitchAndPortDiscoveryEvent() {
        // Mock the eventChannel functions first
        eventChannel.removeEntry(anyObject(byte[].class));
        EasyMock.expectLastCall().times(2, 2); //1 switch, 1 port
        replay(eventChannel);

        setupTopologyManager();

        Dpid swDPId = new Dpid(100L);
        PortNumber portId = new PortNumber((short) 1);

        // Generate a port event
        Collection<PortEvent> portEvents = new ArrayList<PortEvent>();
        portEvents.add(new PortEvent(swDPId, portId));

        // Call the topologyManager function for removing a port
        for (PortEvent portEvent : portEvents) {
            theTopologyManager.removePortDiscoveryEvent(portEvent);
        }

        // Call the topologyManager function for removing a switch
        SwitchEvent switchEvent = new SwitchEvent(swDPId);
        theTopologyManager.removeSwitchDiscoveryEvent(switchEvent);

        // Verify the function calls
        verify(eventChannel);

    }

    /**
     * Test the device discovered function.
     */
    @Test
    public void testPutDeviceDiscoveryEvent() {
        // Mock the eventChannel functions first
        eventChannel.addEntry(anyObject(byte[].class),
                anyObject(TopologyEvent.class));
        EasyMock.expectLastCall().times(1, 1); // 1 device
        replay(eventChannel);

        setupTopologyManager();

        long swDPId = 100L;
        long portId = 1L;

        // Generate a new device event
        MACAddress devMac = MACAddress.valueOf("00:AA:11:BB:33:CC");
        SwitchPort sp = new SwitchPort(swDPId, portId);
        List<SwitchPort> spLists = new ArrayList<SwitchPort>();
        spLists.add(sp);
        HostEvent hostEvent = new HostEvent(devMac);
        hostEvent.setAttachmentPoints(spLists);

        // Call the topologyManager function for adding a device
        theTopologyManager.putHostDiscoveryEvent(hostEvent);

        // Verify the function calls
        verify(eventChannel);
    }

    /**
     * Test the device removed function.
     */
    @Test
    public void testRemoveDeviceDiscoveryEvent() {
        // Mock the eventChannel functions first
        eventChannel.removeEntry(anyObject(byte[].class));
        EasyMock.expectLastCall().times(1, 1); // 1 device
        replay(eventChannel);

        setupTopologyManager();

        long swDPId = 100L;
        long portId = 1L;

        // Generate a new device event
        MACAddress devMac = MACAddress.valueOf("00:AA:11:BB:33:CC");
        SwitchPort sp = new SwitchPort(swDPId, portId);
        List<SwitchPort> spLists = new ArrayList<SwitchPort>();
        spLists.add(sp);
        HostEvent hostEvent = new HostEvent(devMac);
        hostEvent.setAttachmentPoints(spLists);

        // Call the topologyManager function for removing a device
        theTopologyManager.removeHostDiscoveryEvent(hostEvent);

        // Verify the function calls
        verify(eventChannel);
    }

    /**
     * Test the Switch Mastership updated event.
     */
    @Test
    public void testPutSwitchMastershipEvent() {
        // Mock the eventChannel functions first
        eventChannel.addEntry(anyObject(byte[].class),
                anyObject(TopologyEvent.class));
        EasyMock.expectLastCall().times(1, 1); // 1 event
        replay(eventChannel);

        setupTopologyManager();

        // Generate a new Switch Mastership event
        Dpid dpid = new Dpid(100L);
        String onosInstanceId = "ONOS-Test-Instance-ID";
        Role role = Role.MASTER;
        MastershipEvent mastershipEvent =
            new MastershipEvent(dpid, onosInstanceId, role);

        // Call the topologyManager function for adding the event
        theTopologyManager.putSwitchMastershipEvent(mastershipEvent);

        // Verify the function calls
        verify(eventChannel);
    }

    /**
     * Test the Switch Mastership removed event.
     */
    @Test
    public void testRemoveSwitchMastershipEvent() {
        // Mock the eventChannel functions first
        eventChannel.removeEntry(anyObject(byte[].class));
        EasyMock.expectLastCall().times(1, 1); // 1 event
        replay(eventChannel);

        setupTopologyManager();

        // Generate a new Switch Mastership event
        Dpid dpid = new Dpid(100L);
        String onosInstanceId = "ONOS-Test-Instance-ID";
        Role role = Role.MASTER;
        MastershipEvent mastershipEvent =
            new MastershipEvent(dpid, onosInstanceId, role);

        // Call the topologyManager function for removing the event
        theTopologyManager.removeSwitchMastershipEvent(mastershipEvent);

        // Verify the function calls
        verify(eventChannel);
    }

    /**
     * Test the link discovered function.
     */
    @Test
    public void testPutLinkDiscoveryEvent() {
        // Mock the eventChannel functions first
        eventChannel.addEntry(anyObject(byte[].class),
                anyObject(TopologyEvent.class));
        EasyMock.expectLastCall().times(5, 5); // (2 switch + 2 port + 1 link)
        replay(eventChannel);

        setupTopologyManager();

        // Assign the switch and port IDs
        Dpid sw1DPId = new Dpid(100L);
        PortNumber port1Id = new PortNumber((short) 1);
        Dpid sw2DPId = new Dpid(200L);
        PortNumber port2Id = new PortNumber((short) 2);

        // Generate the switch and port events
        SwitchEvent switchEvent1 = new SwitchEvent(sw1DPId);
        Collection<PortEvent> portEvents1 = new ArrayList<PortEvent>();
        portEvents1.add(new PortEvent(sw1DPId, port1Id));

        // Call the topologyManager function for adding a switch
        theTopologyManager.putSwitchDiscoveryEvent(switchEvent1, portEvents1);

        // Generate the switch and port events
        SwitchEvent switchEvent2 = new SwitchEvent(sw2DPId);
        Collection<PortEvent> portEvents2 = new ArrayList<PortEvent>();
        portEvents2.add(new PortEvent(sw2DPId, port2Id));

        // Call the topologyManager function for adding a switch
        theTopologyManager.putSwitchDiscoveryEvent(switchEvent2, portEvents2);

        // Create the link
        LinkEvent linkEvent = new LinkEvent(sw1DPId, port1Id, sw2DPId, port2Id);
        theTopologyManager.putLinkDiscoveryEvent(linkEvent);

        // Verify the function calls
        verify(eventChannel);
    }

    /**
     * Test the link removed function.
     */
    @Test
    public void testRemoveLinkDiscoveryEvent() {
        // Mock the eventChannel functions first
        eventChannel.removeEntry(anyObject(byte[].class));
        EasyMock.expectLastCall().times(1, 1); // (1 link)
        replay(eventChannel);

        setupTopologyManager();

        // Assign the switch and port IDs
        Dpid sw1DPId = new Dpid(100L);
        PortNumber port1Id = new PortNumber((short) 1);
        Dpid sw2DPId = new Dpid(200L);
        PortNumber port2Id = new PortNumber((short) 2);

        // Generate the switch and port events
        SwitchEvent switchEvent1 = new SwitchEvent(sw1DPId);
        Collection<PortEvent> portEvents1 = new ArrayList<PortEvent>();
        portEvents1.add(new PortEvent(sw1DPId, port1Id));

        // Call the topologyManager function for adding a switch
        theTopologyManager.putSwitchDiscoveryEvent(switchEvent1, portEvents1);

        // Generate the switch and port events
        SwitchEvent switchEvent2 = new SwitchEvent(sw2DPId);
        Collection<PortEvent> portEvents2 = new ArrayList<PortEvent>();
        portEvents2.add(new PortEvent(sw2DPId, port2Id));

        // Call the topologyManager function for adding a switch
        theTopologyManager.putSwitchDiscoveryEvent(switchEvent2, portEvents2);

        // Remove the link
        LinkEvent linkEventRemove = new LinkEvent(sw1DPId, port1Id, sw2DPId, port2Id);
        theTopologyManager.removeLinkDiscoveryEvent(linkEventRemove);

        // Verify the function calls
        verify(eventChannel);
    }

    /**
     * Test to confirm topology replica transformation.
     */
    @Test
    public void testAddSwitch() {
        setupTopologyManager();

        final Dpid dpid = new Dpid(1);
        SwitchEvent sw = new SwitchEvent(dpid);
        sw.createStringAttribute("foo", "bar");

        TestUtils.callMethod(theTopologyManager, "addSwitch", SwitchEvent.class, sw);

        // check topology structure
        TopologyInternal topology = (TopologyInternal) theTopologyManager.getTopology();
        SwitchEvent swInTopo = topology.getSwitchEvent(dpid);
        assertEquals(sw, swInTopo);
        assertTrue(swInTopo.isFrozen());
        assertEquals("bar", swInTopo.getStringAttribute("foo"));

        // check events to be fired
        List<SwitchEvent> apiAddedSwitchEvents
            = TestUtils.getField(theTopologyManager, "apiAddedSwitchEvents");
        assertThat(apiAddedSwitchEvents, hasItem(sw));
    }

    /**
     * Test to confirm topology replica transformation.
     */
    @Test
    public void testAddPort() {
        setupTopologyManager();

        final Dpid dpid = new Dpid(1);
        SwitchEvent sw = new SwitchEvent(dpid);
        sw.createStringAttribute("foo", "bar");

        final PortNumber portNumber = new PortNumber((short) 2);
        PortEvent port = new PortEvent(dpid, portNumber);
        port.createStringAttribute("fuzz", "buzz");

        TestUtils.callMethod(theTopologyManager, "addSwitch", SwitchEvent.class, sw);
        TestUtils.callMethod(theTopologyManager, "addPort", PortEvent.class, port);

        // check topology structure
        TopologyInternal topology = (TopologyInternal) theTopologyManager.getTopology();
        SwitchEvent swInTopo = topology.getSwitchEvent(dpid);
        assertEquals(sw, swInTopo);
        assertTrue(swInTopo.isFrozen());
        assertEquals("bar", swInTopo.getStringAttribute("foo"));

        final SwitchPort portId = new SwitchPort(dpid, portNumber);
        PortEvent portInTopo = topology.getPortEvent(portId);
        assertEquals(port, portInTopo);
        assertTrue(portInTopo.isFrozen());
        assertEquals("buzz", portInTopo.getStringAttribute("fuzz"));

        // check events to be fired
        List<PortEvent> apiAddedPortEvents
            = TestUtils.getField(theTopologyManager, "apiAddedPortEvents");
        assertThat(apiAddedPortEvents, hasItem(port));
    }

    /**
     * Test to confirm topology replica transformation.
     */
    @Test
    public void testRemovePortThenSwitch() {
        setupTopologyManager();

        final Dpid dpid = new Dpid(1);
        SwitchEvent sw = new SwitchEvent(dpid);
        sw.createStringAttribute("foo", "bar");

        final PortNumber portNumber = new PortNumber((short) 2);
        PortEvent port = new PortEvent(dpid, portNumber);
        port.createStringAttribute("fuzz", "buzz");

        TestUtils.callMethod(theTopologyManager, "addSwitch", SwitchEvent.class, sw);
        TestUtils.callMethod(theTopologyManager, "addPort", PortEvent.class, port);

        // check topology structure
        TopologyInternal topology = (TopologyInternal) theTopologyManager.getTopology();
        SwitchEvent swInTopo = topology.getSwitchEvent(dpid);
        assertEquals(sw, swInTopo);
        assertTrue(swInTopo.isFrozen());
        assertEquals("bar", swInTopo.getStringAttribute("foo"));

        final SwitchPort portId = new SwitchPort(dpid, portNumber);
        PortEvent portInTopo = topology.getPortEvent(portId);
        assertEquals(port, portInTopo);
        assertTrue(portInTopo.isFrozen());
        assertEquals("buzz", portInTopo.getStringAttribute("fuzz"));

        // remove in proper order
        TestUtils.callMethod(theTopologyManager, "removePort",
                            PortEvent.class, new PortEvent(port));
        TestUtils.callMethod(theTopologyManager, "removeSwitch",
                            SwitchEvent.class, new SwitchEvent(sw));


        // check events to be fired
        List<PortEvent> apiRemovedPortEvents
            = TestUtils.getField(theTopologyManager, "apiRemovedPortEvents");
        assertThat(apiRemovedPortEvents, hasItem(port));
        List<SwitchEvent> apiRemovedSwitchEvents
            = TestUtils.getField(theTopologyManager, "apiRemovedSwitchEvents");
        assertThat(apiRemovedSwitchEvents, hasItem(sw));
    }

    /**
     * Test to confirm topology replica transformation.
     */
    @Test
    public void testRemoveSwitch() {
        setupTopologyManager();

        final Dpid dpid = new Dpid(1);
        SwitchEvent sw = new SwitchEvent(dpid);
        sw.createStringAttribute("foo", "bar");

        final PortNumber portNumber = new PortNumber((short) 2);
        PortEvent port = new PortEvent(dpid, portNumber);
        port.createStringAttribute("fuzz", "buzz");

        TestUtils.callMethod(theTopologyManager, "addSwitch", SwitchEvent.class, sw);
        TestUtils.callMethod(theTopologyManager, "addPort", PortEvent.class, port);

        // check topology structure
        TopologyInternal topology = (TopologyInternal) theTopologyManager.getTopology();
        SwitchEvent swInTopo = topology.getSwitchEvent(dpid);
        assertEquals(sw, swInTopo);
        assertTrue(swInTopo.isFrozen());
        assertEquals("bar", swInTopo.getStringAttribute("foo"));

        final SwitchPort portId = new SwitchPort(dpid, portNumber);
        PortEvent portInTopo = topology.getPortEvent(portId);
        assertEquals(port, portInTopo);
        assertTrue(portInTopo.isFrozen());
        assertEquals("buzz", portInTopo.getStringAttribute("fuzz"));

        // remove in in-proper order
//        TestUtils.callMethod(theTopologyManager, "removePort",
//                            PortEvent.class, new PortEvent(port));
        TestUtils.callMethod(theTopologyManager, "removeSwitch",
                            SwitchEvent.class, new SwitchEvent(sw));


        // check events to be fired
        // outcome should be the same as #testRemovePortThenSwitch
        List<PortEvent> apiRemovedPortEvents
            = TestUtils.getField(theTopologyManager, "apiRemovedPortEvents");
        assertThat(apiRemovedPortEvents, hasItem(port));
        List<SwitchEvent> apiRemovedSwitchEvents
            = TestUtils.getField(theTopologyManager, "apiRemovedSwitchEvents");
        assertThat(apiRemovedSwitchEvents, hasItem(sw));
    }

    /**
     * Test to confirm topology replica transformation.
     */
    @Test
    public void testAddLink() {
        setupTopologyManager();

        final Dpid dpid = new Dpid(1);
        SwitchEvent sw = new SwitchEvent(dpid);
        sw.createStringAttribute("foo", "bar");

        final PortNumber portNumberA = new PortNumber((short) 2);
        PortEvent portA = new PortEvent(dpid, portNumberA);
        portA.createStringAttribute("fuzz", "buzz");

        final PortNumber portNumberB = new PortNumber((short) 3);
        PortEvent portB = new PortEvent(dpid, portNumberB);
        portB.createStringAttribute("fizz", "buz");

        LinkEvent linkA = new LinkEvent(portA.getSwitchPort(), portB.getSwitchPort());
        linkA.createStringAttribute(TopologyElement.TYPE,
                                    TopologyElement.TYPE_OPTICAL_LAYER);
        LinkEvent linkB = new LinkEvent(portB.getSwitchPort(), portA.getSwitchPort());
        linkB.createStringAttribute(TopologyElement.TYPE,
                                    TopologyElement.TYPE_OPTICAL_LAYER);

        TestUtils.callMethod(theTopologyManager, "addSwitch", SwitchEvent.class, sw);
        TestUtils.callMethod(theTopologyManager, "addPort", PortEvent.class, portA);
        TestUtils.callMethod(theTopologyManager, "addPort", PortEvent.class, portB);
        TestUtils.callMethod(theTopologyManager, "addLink", LinkEvent.class, linkA);
        TestUtils.callMethod(theTopologyManager, "addLink", LinkEvent.class, linkB);

        // check topology structure
        TopologyInternal topology = (TopologyInternal) theTopologyManager.getTopology();
        SwitchEvent swInTopo = topology.getSwitchEvent(dpid);
        assertEquals(sw, swInTopo);
        assertTrue(swInTopo.isFrozen());
        assertEquals("bar", swInTopo.getStringAttribute("foo"));

        final SwitchPort portIdA = new SwitchPort(dpid, portNumberA);
        PortEvent portAInTopo = topology.getPortEvent(portIdA);
        assertEquals(portA, portAInTopo);
        assertTrue(portAInTopo.isFrozen());
        assertEquals("buzz", portAInTopo.getStringAttribute("fuzz"));

        final SwitchPort portIdB = new SwitchPort(dpid, portNumberB);
        PortEvent portBInTopo = topology.getPortEvent(portIdB);
        assertEquals(portB, portBInTopo);
        assertTrue(portBInTopo.isFrozen());
        assertEquals("buz", portBInTopo.getStringAttribute("fizz"));

        LinkEvent linkAInTopo = topology.getLinkEvent(linkA.getLinkTuple());
        assertEquals(linkA, linkAInTopo);
        assertTrue(linkAInTopo.isFrozen());
        assertEquals(TopologyElement.TYPE_OPTICAL_LAYER, linkAInTopo.getType());

        LinkEvent linkBInTopo = topology.getLinkEvent(linkB.getLinkTuple());
        assertEquals(linkB, linkBInTopo);
        assertTrue(linkBInTopo.isFrozen());
        assertEquals(TopologyElement.TYPE_OPTICAL_LAYER, linkBInTopo.getType());

        // check events to be fired
        List<LinkEvent> apiAddedLinkEvents
            = TestUtils.getField(theTopologyManager, "apiAddedLinkEvents");
        assertThat(apiAddedLinkEvents, containsInAnyOrder(linkA, linkB));
    }

    /**
     * Test to confirm topology replica transformation.
     */
    @Test
    public void testAddLinkKickingOffHost() {
        setupTopologyManager();

        final Dpid dpid = new Dpid(1);
        SwitchEvent sw = new SwitchEvent(dpid);
        sw.createStringAttribute("foo", "bar");

        final PortNumber portNumberA = new PortNumber((short) 2);
        PortEvent portA = new PortEvent(dpid, portNumberA);
        portA.createStringAttribute("fuzz", "buzz");

        final PortNumber portNumberB = new PortNumber((short) 3);
        PortEvent portB = new PortEvent(dpid, portNumberB);
        portB.createStringAttribute("fizz", "buz");

        final PortNumber portNumberC = new PortNumber((short) 4);
        PortEvent portC = new PortEvent(dpid, portNumberC);
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


        LinkEvent linkA = new LinkEvent(portA.getSwitchPort(), portB.getSwitchPort());
        linkA.createStringAttribute(TopologyElement.TYPE,
                                    TopologyElement.TYPE_OPTICAL_LAYER);
        LinkEvent linkB = new LinkEvent(portB.getSwitchPort(), portA.getSwitchPort());
        linkB.createStringAttribute(TopologyElement.TYPE,
                                    TopologyElement.TYPE_OPTICAL_LAYER);

        TestUtils.callMethod(theTopologyManager, "addSwitch", SwitchEvent.class, sw);
        TestUtils.callMethod(theTopologyManager, "addPort", PortEvent.class, portA);
        TestUtils.callMethod(theTopologyManager, "addPort", PortEvent.class, portB);
        TestUtils.callMethod(theTopologyManager, "addPort", PortEvent.class, portC);
        TestUtils.callMethod(theTopologyManager, "addHost", HostEvent.class, hostA);
        TestUtils.callMethod(theTopologyManager, "addHost", HostEvent.class, hostB);

        TestUtils.callMethod(theTopologyManager, "addLink", LinkEvent.class, linkA);
        TestUtils.callMethod(theTopologyManager, "addLink", LinkEvent.class, linkB);

        // check topology structure
        TopologyInternal topology = (TopologyInternal) theTopologyManager.getTopology();
        SwitchEvent swInTopo = topology.getSwitchEvent(dpid);
        assertEquals(sw, swInTopo);
        assertTrue(swInTopo.isFrozen());
        assertEquals("bar", swInTopo.getStringAttribute("foo"));

        final SwitchPort portIdA = new SwitchPort(dpid, portNumberA);
        PortEvent portAInTopo = topology.getPortEvent(portIdA);
        assertEquals(portA, portAInTopo);
        assertTrue(portAInTopo.isFrozen());
        assertEquals("buzz", portAInTopo.getStringAttribute("fuzz"));

        final SwitchPort portIdB = new SwitchPort(dpid, portNumberB);
        PortEvent portBInTopo = topology.getPortEvent(portIdB);
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
        assertEquals(TopologyElement.TYPE_OPTICAL_LAYER, linkAInTopo.getType());

        LinkEvent linkBInTopo = topology.getLinkEvent(linkB.getLinkTuple());
        assertEquals(linkB, linkBInTopo);
        assertTrue(linkBInTopo.isFrozen());
        assertEquals(TopologyElement.TYPE_OPTICAL_LAYER, linkBInTopo.getType());

        // check events to be fired
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
     * Test to confirm topology replica transformation.
     */
    @Test
    public void testRemoveLink() {
        setupTopologyManager();

        final Dpid dpid = new Dpid(1);
        SwitchEvent sw = new SwitchEvent(dpid);
        sw.createStringAttribute("foo", "bar");

        final PortNumber portNumberA = new PortNumber((short) 2);
        PortEvent portA = new PortEvent(dpid, portNumberA);
        portA.createStringAttribute("fuzz", "buzz");

        final PortNumber portNumberB = new PortNumber((short) 3);
        PortEvent portB = new PortEvent(dpid, portNumberB);
        portB.createStringAttribute("fizz", "buz");

        LinkEvent linkA = new LinkEvent(portA.getSwitchPort(), portB.getSwitchPort());
        linkA.createStringAttribute(TopologyElement.TYPE,
                                    TopologyElement.TYPE_OPTICAL_LAYER);
        LinkEvent linkB = new LinkEvent(portB.getSwitchPort(), portA.getSwitchPort());
        linkB.createStringAttribute(TopologyElement.TYPE,
                                    TopologyElement.TYPE_OPTICAL_LAYER);

        TestUtils.callMethod(theTopologyManager, "addSwitch", SwitchEvent.class, sw);
        TestUtils.callMethod(theTopologyManager, "addPort", PortEvent.class, portA);
        TestUtils.callMethod(theTopologyManager, "addPort", PortEvent.class, portB);
        TestUtils.callMethod(theTopologyManager, "addLink", LinkEvent.class, linkA);
        TestUtils.callMethod(theTopologyManager, "addLink", LinkEvent.class, linkB);

        // check topology structure
        TopologyInternal topology = (TopologyInternal) theTopologyManager.getTopology();
        SwitchEvent swInTopo = topology.getSwitchEvent(dpid);
        assertEquals(sw, swInTopo);
        assertTrue(swInTopo.isFrozen());
        assertEquals("bar", swInTopo.getStringAttribute("foo"));

        final SwitchPort portIdA = new SwitchPort(dpid, portNumberA);
        PortEvent portAInTopo = topology.getPortEvent(portIdA);
        assertEquals(portA, portAInTopo);
        assertTrue(portAInTopo.isFrozen());
        assertEquals("buzz", portAInTopo.getStringAttribute("fuzz"));

        final SwitchPort portIdB = new SwitchPort(dpid, portNumberB);
        PortEvent portBInTopo = topology.getPortEvent(portIdB);
        assertEquals(portB, portBInTopo);
        assertTrue(portBInTopo.isFrozen());
        assertEquals("buz", portBInTopo.getStringAttribute("fizz"));

        LinkEvent linkAInTopo = topology.getLinkEvent(linkA.getLinkTuple());
        assertEquals(linkA, linkAInTopo);
        assertTrue(linkAInTopo.isFrozen());
        assertEquals(TopologyElement.TYPE_OPTICAL_LAYER, linkAInTopo.getType());


        LinkEvent linkBInTopo = topology.getLinkEvent(linkB.getLinkTuple());
        assertEquals(linkB, linkBInTopo);
        assertTrue(linkBInTopo.isFrozen());
        assertEquals(TopologyElement.TYPE_OPTICAL_LAYER, linkBInTopo.getType());

        // check events to be fired
        // FIXME if link flapped (linkA in this scenario),
        //  linkA appears in both removed and added is this expected behavior?
        List<LinkEvent> apiAddedLinkEvents
            = TestUtils.getField(theTopologyManager, "apiAddedLinkEvents");
        assertThat(apiAddedLinkEvents, containsInAnyOrder(linkA, linkB));

        // clear event before removing Link
        apiAddedLinkEvents.clear();

        // remove link
        TestUtils.callMethod(theTopologyManager, "removeLink", LinkEvent.class, new LinkEvent(linkA));

        LinkEvent linkANotInTopo = topology.getLinkEvent(linkA.getLinkTuple());
        assertNull(linkANotInTopo);

        List<LinkEvent> apiRemovedLinkEvents
            = TestUtils.getField(theTopologyManager, "apiRemovedLinkEvents");
        assertThat(apiRemovedLinkEvents, hasItem(linkA));
    }

    /**
     * Test to confirm topology replica transformation.
     */
    @Test
    public void testAddHostIgnoredByLink() {
        setupTopologyManager();

        final Dpid dpid = new Dpid(1);
        SwitchEvent sw = new SwitchEvent(dpid);
        sw.createStringAttribute("foo", "bar");

        final PortNumber portNumberA = new PortNumber((short) 2);
        PortEvent portA = new PortEvent(dpid, portNumberA);
        portA.createStringAttribute("fuzz", "buzz");

        final PortNumber portNumberB = new PortNumber((short) 3);
        PortEvent portB = new PortEvent(dpid, portNumberB);
        portB.createStringAttribute("fizz", "buz");

        final PortNumber portNumberC = new PortNumber((short) 4);
        PortEvent portC = new PortEvent(dpid, portNumberC);
        portC.createStringAttribute("fizz", "buz");

        LinkEvent linkA = new LinkEvent(portA.getSwitchPort(), portB.getSwitchPort());
        linkA.createStringAttribute(TopologyElement.TYPE,
                                    TopologyElement.TYPE_OPTICAL_LAYER);
        LinkEvent linkB = new LinkEvent(portB.getSwitchPort(), portA.getSwitchPort());
        linkB.createStringAttribute(TopologyElement.TYPE,
                                    TopologyElement.TYPE_OPTICAL_LAYER);

        TestUtils.callMethod(theTopologyManager, "addSwitch", SwitchEvent.class, sw);
        TestUtils.callMethod(theTopologyManager, "addPort", PortEvent.class, portA);
        TestUtils.callMethod(theTopologyManager, "addPort", PortEvent.class, portB);
        TestUtils.callMethod(theTopologyManager, "addPort", PortEvent.class, portC);
        TestUtils.callMethod(theTopologyManager, "addLink", LinkEvent.class, linkA);
        TestUtils.callMethod(theTopologyManager, "addLink", LinkEvent.class, linkB);

        // Add hostA attached to a port which already has a link
        final MACAddress macA = MACAddress.valueOf(666L);
        HostEvent hostA = new HostEvent(macA);
        hostA.addAttachmentPoint(portA.getSwitchPort());
        final long timestampA = 392893200L;
        hostA.setLastSeenTime(timestampA);

        TestUtils.callMethod(theTopologyManager, "addHost", HostEvent.class, hostA);

        // Add hostB attached to multiple ports,
        // some of them which already has a link
        final MACAddress macB = MACAddress.valueOf(999L);
        HostEvent hostB = new HostEvent(macB);
        hostB.addAttachmentPoint(portB.getSwitchPort());
        hostB.addAttachmentPoint(portC.getSwitchPort());
        final long timestampB = 392893201L;
        hostB.setLastSeenTime(timestampB);

        TestUtils.callMethod(theTopologyManager, "addHost", HostEvent.class, hostB);

        // check topology structure
        TopologyInternal topology = (TopologyInternal) theTopologyManager.getTopology();
        SwitchEvent swInTopo = topology.getSwitchEvent(dpid);
        assertEquals(sw, swInTopo);
        assertTrue(swInTopo.isFrozen());
        assertEquals("bar", swInTopo.getStringAttribute("foo"));

        final SwitchPort portIdA = new SwitchPort(dpid, portNumberA);
        PortEvent portAInTopo = topology.getPortEvent(portIdA);
        assertEquals(portA, portAInTopo);
        assertTrue(portAInTopo.isFrozen());
        assertEquals("buzz", portAInTopo.getStringAttribute("fuzz"));

        final SwitchPort portIdB = new SwitchPort(dpid, portNumberB);
        PortEvent portBInTopo = topology.getPortEvent(portIdB);
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
        assertEquals(TopologyElement.TYPE_OPTICAL_LAYER, linkAInTopo.getType());

        LinkEvent linkBInTopo = topology.getLinkEvent(linkB.getLinkTuple());
        assertEquals(linkB, linkBInTopo);
        assertTrue(linkBInTopo.isFrozen());
        assertEquals(TopologyElement.TYPE_OPTICAL_LAYER, linkBInTopo.getType());

        // check events to be fired
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
     * Test to confirm topology replica transformation.
     */
    @Test
    public void testAddHostMove() {
        setupTopologyManager();

        final Dpid dpid = new Dpid(1);
        SwitchEvent sw = new SwitchEvent(dpid);
        sw.createStringAttribute("foo", "bar");

        final PortNumber portNumberA = new PortNumber((short) 2);
        PortEvent portA = new PortEvent(dpid, portNumberA);
        portA.createStringAttribute("fuzz", "buzz");

        final PortNumber portNumberB = new PortNumber((short) 3);
        PortEvent portB = new PortEvent(dpid, portNumberB);
        portB.createStringAttribute("fizz", "buz");

        final PortNumber portNumberC = new PortNumber((short) 4);
        PortEvent portC = new PortEvent(dpid, portNumberC);
        portC.createStringAttribute("fizz", "buz");

        TestUtils.callMethod(theTopologyManager, "addSwitch", SwitchEvent.class, sw);
        TestUtils.callMethod(theTopologyManager, "addPort", PortEvent.class, portA);
        TestUtils.callMethod(theTopologyManager, "addPort", PortEvent.class, portB);
        TestUtils.callMethod(theTopologyManager, "addPort", PortEvent.class, portC);

        // Add hostA attached to a port which already has a link
        final MACAddress macA = MACAddress.valueOf(666L);
        HostEvent hostA = new HostEvent(macA);
        hostA.addAttachmentPoint(portA.getSwitchPort());
        final long timestampA = 392893200L;
        hostA.setLastSeenTime(timestampA);

        TestUtils.callMethod(theTopologyManager, "addHost", HostEvent.class, hostA);


        // check topology structure
        TopologyInternal topology = (TopologyInternal) theTopologyManager.getTopology();
        SwitchEvent swInTopo = topology.getSwitchEvent(dpid);
        assertEquals(sw, swInTopo);
        assertTrue(swInTopo.isFrozen());
        assertEquals("bar", swInTopo.getStringAttribute("foo"));

        final SwitchPort portIdA = new SwitchPort(dpid, portNumberA);
        PortEvent portAInTopo = topology.getPortEvent(portIdA);
        assertEquals(portA, portAInTopo);
        assertTrue(portAInTopo.isFrozen());
        assertEquals("buzz", portAInTopo.getStringAttribute("fuzz"));

        final SwitchPort portIdB = new SwitchPort(dpid, portNumberB);
        PortEvent portBInTopo = topology.getPortEvent(portIdB);
        assertEquals(portB, portBInTopo);
        assertTrue(portBInTopo.isFrozen());
        assertEquals("buz", portBInTopo.getStringAttribute("fizz"));

        // hostA expected to be there
        assertEquals(hostA, topology.getHostEvent(macA));
        assertEquals(timestampA, topology.getHostEvent(macA).getLastSeenTime());

        // check events to be fired
        // hostA should be added
        List<HostEvent> apiAddedHostEvents
            = TestUtils.getField(theTopologyManager, "apiAddedHostEvents");
        assertThat(apiAddedHostEvents, hasItem(hostA));


        // clear event before moving host
        apiAddedHostEvents.clear();

        HostEvent hostAmoved = new HostEvent(macA);
        hostAmoved.addAttachmentPoint(portB.getSwitchPort());
        final long timestampAmoved = 392893201L;
        hostAmoved.setLastSeenTime(timestampAmoved);

        TestUtils.callMethod(theTopologyManager, "addHost", HostEvent.class, hostAmoved);

        assertEquals(hostAmoved, topology.getHostEvent(macA));
        assertEquals(timestampAmoved, topology.getHostEvent(macA).getLastSeenTime());

        // hostA expected to be there with new attachment point
        apiAddedHostEvents
            = TestUtils.getField(theTopologyManager, "apiAddedHostEvents");
        assertThat(apiAddedHostEvents, hasItem(hostAmoved));

        // hostA is updated not removed
        List<HostEvent> apiRemovedHostEvents
            = TestUtils.getField(theTopologyManager, "apiRemovedHostEvents");
        assertThat(apiRemovedHostEvents, not(hasItem(hostA)));
    }
}
