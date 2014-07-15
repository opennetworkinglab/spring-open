package net.onrc.onos.core.topology;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.datagrid.IDatagridService;
import net.onrc.onos.core.datagrid.IEventChannel;
import net.onrc.onos.core.datagrid.IEventChannelListener;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

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
        theTopologyManager.startup(datagridService);
        theTopologyManager.debugReplaceDataStore(dataStoreService);
    }

    @After
    public void tearDown() throws Exception {

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

}
