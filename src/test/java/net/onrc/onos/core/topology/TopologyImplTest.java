package net.onrc.onos.core.topology;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.util.Collection;
import java.util.Iterator;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.LinkTuple;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

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
    private static final PortNumber PORT_NUMBER_1 = new PortNumber(SWITCH_PORT_1.shortValue());
    private static final Long SWITCH_PORT_2 = 3L;
    private static final PortNumber PORT_NUMBER_2 = new PortNumber(SWITCH_PORT_2.shortValue());

    // Set the test network size, it should be larger than 3
    private static final long TEST_SWITCH_NUM = 100L;
    private static final long TEST_HOST_NUM = TEST_SWITCH_NUM;

    @Before
    public void setUp() throws Exception {
        // Create the injected network first
        testTopology = new TopologyImpl();

        // Create a number of switches and install two ports for each switch
        for (long switchID = 1; switchID <= TEST_SWITCH_NUM; switchID++) {
            final Dpid dpid = new Dpid(switchID);
            SwitchEvent testSwitch = new SwitchEvent(dpid);
            testTopology.putSwitch(testSwitch);
            testTopology.putPort(new PortEvent(dpid, PORT_NUMBER_1));
            testTopology.putPort(new PortEvent(dpid, PORT_NUMBER_2));
            PortEvent hostPort = new PortEvent(dpid,
                    new PortNumber(SWITCH_HOST_PORT.shortValue()));
            testTopology.putPort(hostPort);

            // Create a host for each switch
            MACAddress devMac = MACAddress.valueOf(switchID);
            HostEvent testHost = new HostEvent(devMac);
            testHost.addAttachmentPoint(hostPort.getSwitchPort());
            testTopology.putHost(testHost);
        }

        // Create one bidirectional link b/w two switches to construct a ring topology
        for (long switchID = 1; switchID <= TEST_SWITCH_NUM; switchID++) {
            final Dpid dpidA = new Dpid(switchID);
            final Dpid dpidB = new Dpid(switchID % TEST_SWITCH_NUM + 1);
            LinkEvent testLinkEast = new LinkEvent(
                    testTopology.getPort(dpidA, PORT_NUMBER_2).asSwitchPort(),
                    testTopology.getPort(dpidB, PORT_NUMBER_1).asSwitchPort()
                    );
            LinkEvent testLinkWest = new LinkEvent(
                    testTopology.getPort(dpidB, PORT_NUMBER_1).asSwitchPort(),
                    testTopology.getPort(dpidA, PORT_NUMBER_2).asSwitchPort()
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
        assertNotNull(testTopology.getSwitch(new Dpid(TEST_SWITCH_NUM - 1)));

        // Verify there is no such switch in the graphDB
        assertNull(testTopology.getSwitch(new Dpid(TEST_SWITCH_NUM + 1)));
        long index = 0;
        Iterator<Switch> itr =  testTopology.getSwitches().iterator();
        while (itr.hasNext()) {
            index++;
            Dpid swID = itr.next().getDpid();
            assertThat(swID.value(),
                    is(both(greaterThanOrEqualTo(1L))
                       .and(lessThanOrEqualTo(TEST_SWITCH_NUM))));
        }

        // Verify the total number of switches
        assertEquals(TEST_SWITCH_NUM, index);
    }

    /**
     * Test the result of getPort function.
     */
    @Test
    public void testGetPort() {
        PortNumber bogusPortNum = new PortNumber((short) (SWITCH_PORT_2 + 1));
        for (long switchID = 1; switchID <= TEST_SWITCH_NUM; switchID++) {
            // Verify ports are in the graphDB
            final Dpid dpid = new Dpid(switchID);
            assertNotNull(testTopology.getSwitch(dpid).getPort(PORT_NUMBER_1));
            assertNotNull(testTopology.getSwitch(dpid).getPort(PORT_NUMBER_2));

            // Verify there is no such port in the graphDB
            assertNull(testTopology.getSwitch(dpid).getPort(bogusPortNum));
        }
    }

    /**
     * Test the result of getLink function.
     */
    @Test
    public void testGetLink() {
        Dpid sw1ID = new Dpid(1L);
        Dpid sw3ID = new Dpid(3L);

        // Verify there is no such link b/w these two switches
        assertNull((testTopology.getSwitch(sw1ID)).getLinkToNeighbor(sw3ID));
        long index = 0;
        Iterator<Link> itr = testTopology.getLinks().iterator();
        while (itr.hasNext()) {
            index++;
            Link objectLink = itr.next();
            Switch srcSw = (objectLink.getSrcSwitch());
            Switch dstSw = (objectLink.getDstSwitch());

            LinkTuple linkId = objectLink.getLinkTuple();
            // Verify the link through #getLink
            Link linkA = testTopology.getLink(linkId.getSrc().getDpid(),
                    linkId.getSrc().getPortNumber(),
                    linkId.getDst().getDpid(),
                    linkId.getDst().getPortNumber());
            assertEquals(linkId, linkA.getLinkTuple());

            Link linkB = testTopology.getLink(linkId.getSrc().getDpid(),
                    linkId.getSrc().getPortNumber(),
                    linkId.getDst().getDpid(),
                    linkId.getDst().getPortNumber(),
                    TopologyElement.TYPE_PACKET_LAYER);
            assertEquals(linkId, linkB.getLinkTuple());



            // confirm link is forming a link
            final long smallerDpid = Math.min(srcSw.getDpid().value(), dstSw.getDpid().value());
            final long largerDpid = Math.max(srcSw.getDpid().value(), dstSw.getDpid().value());
            assertThat(largerDpid - smallerDpid,
                is(either(equalTo(1L)).or(equalTo(TEST_SWITCH_NUM - 1))));
        }

        // Verify the total number of links
        assertEquals(TEST_SWITCH_NUM * 2, index);
    }

    /**
     * Test the result of getOutgoingLink function.
     */
    @Test
    public void testGetOutgoingLink() {
        PortNumber bogusPortNum = new PortNumber((short) (SWITCH_PORT_2 + 1));
        for (long switchID = 1; switchID <= TEST_SWITCH_NUM; switchID++) {
            final Dpid dpid = new Dpid(switchID);
            assertNotNull(testTopology.getOutgoingLink(dpid, PORT_NUMBER_1));
            assertNotNull(testTopology.getOutgoingLink(dpid, PORT_NUMBER_2));

            Link la = testTopology.getOutgoingLink(dpid, PORT_NUMBER_2,
                                TopologyElement.TYPE_PACKET_LAYER);
            Link lb = testTopology.getOutgoingLink(dpid, PORT_NUMBER_2);

            assertTrue(la.getLinkTuple().equals(lb.getLinkTuple()));

            Collection<Link> links = testTopology.getOutgoingLinks(
                                        new SwitchPort(dpid, PORT_NUMBER_1));
            assertEquals(1, links.size());

            // Verify there is no such link in the graphDB
            assertNull(testTopology.getOutgoingLink(dpid, bogusPortNum));
        }
    }

    /**
     * Test the result of getIncomingLink function.
     */
    @Test
    public void testGetIncomingLink() {
        PortNumber bogusPortNum = new PortNumber((short) (SWITCH_PORT_2 + 1));
        for (long switchID = 1; switchID <= TEST_SWITCH_NUM; switchID++) {
            // Verify the links are in the graphDB
            final Dpid dpid = new Dpid(switchID);
            assertNotNull(testTopology.getIncomingLink(
                                           dpid, PORT_NUMBER_1));
            assertNotNull(testTopology.getIncomingLink(
                                           dpid, PORT_NUMBER_2));

            Link la = testTopology.getIncomingLink(dpid, PORT_NUMBER_2,
                                        TopologyElement.TYPE_PACKET_LAYER);
            Link lb = testTopology.getIncomingLink(dpid, PORT_NUMBER_2);

            assertTrue(la.getLinkTuple().equals(lb.getLinkTuple()));

            Collection<Link> links = testTopology.getIncomingLinks(
                    new SwitchPort(dpid, PORT_NUMBER_1));
            assertEquals(1, links.size());

            // Verify there is no such link in the graphDB
            assertNull(testTopology.getIncomingLink(
                                        dpid, bogusPortNum));
        }
    }

    /**
     * Test the result of getHostByMac function.
     */
    @Test
    public void testGetHostByMac() {
        for (long switchID = 1; switchID <= TEST_SWITCH_NUM; switchID++) {
            MACAddress devMac = MACAddress.valueOf(switchID);

            // Verify the device is in the graphDB
            assertNotNull(testTopology.getHostByMac(devMac));
        }
    }

    /**
     * Test the result of removeHost function.
     */
    @Test
    public void testRemoveHost() {
        int devCount = 0;
        Iterator<Host> itr = testTopology.getHosts().iterator();
        while (itr.hasNext()) {
            Host currDev = itr.next();
            final MACAddress mac = currDev.getMacAddress();
            testTopology.removeHost(mac);
            assertNull(testTopology.getHostByMac(mac));
            devCount++;
        }
        for (Switch sw : testTopology.getSwitches()) {
            for (Port port : sw.getPorts()) {
                assertTrue(port.getHosts().isEmpty());
            }
        }

        // Verify all hosts have been removed successfully
        assertEquals(TEST_HOST_NUM, devCount);
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

            testTopology.removeLink(objectLink.getLinkTuple());

            // Verify the link was removed successfully
            assertNull(testTopology.getLink(
                    srcSw.getDpid(), srcPort.getNumber(),
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
            final Dpid dpid = new Dpid(switchID);
            Iterator<Host> itr = testTopology.getSwitch(dpid).getHosts().iterator();
            while (itr.hasNext()) {
                testTopology.removeHost(itr.next().getMacAddress());
            }
            for (Port port : testTopology.getSwitch(dpid).getPorts()) {
                testTopology.removePort(port.asSwitchPort());
            }
            testTopology.removeSwitch(dpid);

            // Verify the switch has been removed from the graphDB successfully
            assertNull(testTopology.getSwitch(dpid));
        }

        // Verify all switches have been removed successfully
        Iterator<Switch> itr = testTopology.getSwitches().iterator();
        assertFalse(itr.hasNext());
    }
}
