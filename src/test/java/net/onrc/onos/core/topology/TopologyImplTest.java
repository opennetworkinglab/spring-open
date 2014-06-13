package net.onrc.onos.core.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import net.floodlightcontroller.util.MACAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the TopologyImpl Class in the Topology module.
 * These test cases check the sanity of getSwitch/removeSwitch,
 * getLink/removeLink, getDevice/removeDevice, and getPort functions and
 * verify the data objects inside the global graphDB through a injected network.
 * The injected network has a ring topology with a large number of switches (configurable), and
 * each switch is associated with one device and connected to two other switches.
 */
public class TopologyImplTest {
    private TopologyImpl testTopology;
    private static final Long SWITCH_HOST_PORT = 1L;
    private static final Long SWITCH_PORT_1 = 2L;
    private static final Long SWITCH_PORT_2 = 3L;

    // Set the test network size, it should be larger than 3
    private static final long TEST_SWITCH_NUM = 100L;

    @Before
    public void setUp() throws Exception {
        // Create the injected network first
        testTopology = new TopologyImpl();

        // Create a number of switches and install two ports for each switch
        for (long switchID = 1; switchID <= TEST_SWITCH_NUM; switchID++) {
            SwitchImpl testSwitch = new SwitchImpl(testTopology, switchID);
            testSwitch.addPort(SWITCH_PORT_1);
            testSwitch.addPort(SWITCH_PORT_2);
            testTopology.putSwitch(testSwitch);

            // Create a host for each switch
            MACAddress devMac = MACAddress.valueOf(switchID);
            DeviceImpl testHost = new DeviceImpl(testTopology, devMac);
            testHost.addAttachmentPoint(testSwitch.addPort(SWITCH_HOST_PORT));
            testTopology.putDevice(testHost);
        }

        // Create one bidirectional link b/w two switches to construct a ring topology
        for (long switchID = 1; switchID <= TEST_SWITCH_NUM; switchID++) {
            LinkImpl testLinkEast = new LinkImpl(testTopology,
                    testTopology.getPort(switchID, SWITCH_PORT_2),
                    testTopology.getPort(switchID % TEST_SWITCH_NUM + 1, SWITCH_PORT_1)
                    );
            LinkImpl testLinkWest = new LinkImpl(testTopology,
                    testTopology.getPort(switchID % TEST_SWITCH_NUM + 1, SWITCH_PORT_1),
                    testTopology.getPort(switchID, SWITCH_PORT_2)
                    );
            testTopology.putLink(testLinkEast);
            testTopology.putLink(testLinkWest);
        }
    }

    @After
    public void tearDown() throws Exception {

    }

    /**
     * Test the result of getSwitch function.
     */
    @Test
    public void testGetSwitch() {
        // Verify the switch is in the graphDB
        assertNotNull(testTopology.getSwitch(TEST_SWITCH_NUM - 1));

        // Verify there is no such switch in the graphDB
        assertNull(testTopology.getSwitch(TEST_SWITCH_NUM + 1));
        long swID = 0;
        long index = 0;
        Iterator<Switch> itr =  testTopology.getSwitches().iterator();
        while (itr.hasNext()) {
            index++;
            swID = itr.next().getDpid();
            assertTrue(swID >= 1 && swID <= TEST_SWITCH_NUM);
        }

        // Verify the total number of switches
        assertEquals(TEST_SWITCH_NUM, index);
    }

    /**
     * Test the result of getPort function.
     */
    @Test
    public void testGetPort() {
        for (long switchID = 1; switchID <= TEST_SWITCH_NUM; switchID++) {
            // Verify ports are in the graphDB
            assertNotNull(testTopology.getSwitch(switchID).getPort(SWITCH_PORT_1));
            assertNotNull(testTopology.getSwitch(switchID).getPort(SWITCH_PORT_2));

            // Verify there is no such port in the graphDB
            assertNull(testTopology.getSwitch(switchID).getPort(SWITCH_PORT_2 + 1));
        }
    }

    /**
     * Test the result of getLink function.
     */
    @Test
    public void testGetLink() {
        long sw1ID = 1L;
        long sw2ID = 3L;

        // Verify there is no such link b/w these two switches
        assertNull((testTopology.getSwitch(sw1ID)).getLinkToNeighbor(sw2ID));
        long index = 0;
        Iterator<Link> itr = testTopology.getLinks().iterator();
        while (itr.hasNext()) {
            index++;
            Link objectLink = itr.next();
            Switch srcSw = (objectLink.getSrcSwitch());
            Switch dstSw = (objectLink.getDstSwitch());
            if (srcSw.getDpid() < TEST_SWITCH_NUM && dstSw.getDpid() < TEST_SWITCH_NUM) {
                // Verify the link relationship
                assertTrue((srcSw.getDpid() == dstSw.getDpid() - 1
                        || (srcSw.getDpid() == dstSw.getDpid() + 1)));
            }
        }

        // Verify the total number of links
        assertEquals(TEST_SWITCH_NUM * 2, index);
    }

    /**
     * Test the result of getOutgoingLink function.
     */
    @Test
    public void testGetOutgoingLink() {
        for (long switchID = 1; switchID <= TEST_SWITCH_NUM; switchID++) {
            assertNotNull(testTopology.getOutgoingLink(switchID, SWITCH_PORT_1));
            assertNotNull(testTopology.getOutgoingLink(switchID, SWITCH_PORT_2));

            // Verify there is no such link in the graphDB
            assertNull(testTopology.getOutgoingLink(switchID, SWITCH_PORT_1 + 2));
        }
    }

    /**
     * Test the result of getIncomingLink function.
     */
    @Test
    public void testGetIncomingLink() {
        for (long switchID = 1; switchID <= TEST_SWITCH_NUM; switchID++) {
            // Verify the links are in the graphDB
            assertNotNull(testTopology.getIncomingLink(switchID, SWITCH_PORT_1));
            assertNotNull(testTopology.getIncomingLink(switchID, SWITCH_PORT_2));

            // Verify there is no such link in the graphDB
            assertNull(testTopology.getIncomingLink(switchID, SWITCH_PORT_1 + 2));
        }
    }

    /**
     * Test the result of getDeviceByMac function.
     */
    @Test
    public void testGetDeviceByMac() {
        for (long switchID = 1; switchID <= TEST_SWITCH_NUM; switchID++) {
            MACAddress devMac = MACAddress.valueOf(switchID);

            // Verify the device is in the graphDB
            assertNotNull(testTopology.getDeviceByMac(devMac));
        }
    }

    /**
     * Test the result of removeDevice function.
     */
    @Test
    public void testRemoveDevice() {
        int devCount = 0;
        Iterator<Device> itr = testTopology.getDevices().iterator();
        while (itr.hasNext()) {
            Device currDev = itr.next();
            testTopology.removeDevice(currDev);
            testTopology.getDeviceByMac(currDev.getMacAddress());
            devCount++;
        }

        // Verify all hosts have been removed successfully
        assertEquals(TEST_SWITCH_NUM, devCount);
    }

    /**
     * Test the result of removeLink function.
     */
    @Test
    public void testRemoveLink() {
        long index = 0;
        Iterator<Link> itr = testTopology.getLinks().iterator();
        while (itr.hasNext()) {
            index++;
            Link objectLink = itr.next();
            Switch srcSw = (objectLink.getSrcSwitch());
            Port srcPort = objectLink.getSrcPort();
            Switch dstSw = (objectLink.getDstSwitch());
            Port dstPort = objectLink.getDstPort();
            testTopology.removeLink(objectLink);

            // Verify the link was removed successfully
            assertNull(testTopology.getLink(srcSw.getDpid(), srcPort.getNumber(),
                    dstSw.getDpid(), dstPort.getNumber()));
        }

        // Verify all links have been removed successfully
        assertEquals(TEST_SWITCH_NUM * 2, index);
    }

    /**
     * Test the result of removeSwitch function.
     */
    @Test
    public void testRemoveSwitch() {
        for (long switchID = 1; switchID <= TEST_SWITCH_NUM; switchID++) {
            Iterator<Device> itr = testTopology.getSwitch(switchID).getDevices().iterator();
            while (itr.hasNext()) {
                testTopology.removeDevice((Device) itr);
            }
            testTopology.removeSwitch(switchID);

            // Verify the switch has been removed from the graphDB successfully
            assertNull(testTopology.getSwitch(switchID));
        }

        // Verify all switches have been removed successfully
        Iterator<Switch> itr = testTopology.getSwitches().iterator();
        assertFalse(itr.hasNext());
    }
}
