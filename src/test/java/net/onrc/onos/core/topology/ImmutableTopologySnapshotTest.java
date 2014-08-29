package net.onrc.onos.core.topology;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.topology.ImmutableTopologySnapshot.Builder;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.LinkTuple;
import net.onrc.onos.core.util.OnosInstanceId;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Iterators;

/**
 * Tests for {@link ImmutableTopologySnapshot}.
 */
public class ImmutableTopologySnapshotTest {

    // randomly generated topology
    private static ImmutableTopologySnapshot ss;

    // randomly generated topology size
    private static final int NUM_HOSTS = 12;
    private static final int NUM_LINKS = 11;
    private static final int NUM_PORTS = 48;
    private static final int NUM_SWITCH = 10;

    // a link that is assured to be in topology
    private static final LinkTuple LINK_IN_TOPOLOGY = new LinkTuple(
            new Dpid(1), PortNumber.uint32(1),
            new Dpid(2), PortNumber.uint32(1));

    // invalid port number, etc. which should not appear in topology
    private static final int PORT_NUM_NA = NUM_PORTS + 2;
    private static final long MAC_NA = NUM_HOSTS + 1L;

    // master instance of all the switches in ramdomly generated topology
    private static final OnosInstanceId INSTANCE_ID
                            = new OnosInstanceId("TheInstance");

    /**
     * Generate topology to test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        ss = createTopology(NUM_SWITCH, NUM_PORTS, NUM_LINKS, NUM_HOSTS);
    }

    /**
     * InitialBuilder should contains empty topology.
     */
    @Test
    public void testInitialBuilder() {
        final Builder builder = ImmutableTopologySnapshot.initialBuilder();
        assertTrue("Topology should be empty",
                builder.getCurrentInternal().getAllSwitchEvents().isEmpty());
        assertTrue("Topology should be empty",
                builder.build().getAllSwitchEvents().isEmpty());
    }

    /**
     * Builder should create a copy of the parent topology.
     */
    @Test
    public void testBuilder() {
        final Builder builder = ss.builder();
        assertEquals("Number of switch", NUM_SWITCH,
                builder.getCurrentInternal().getAllSwitchEvents().size());
        assertEquals("Number of ports", NUM_PORTS * NUM_SWITCH,
                builder.getCurrentInternal().getAllPortEvents().size());
        assertEquals("Number of links", NUM_LINKS,
                builder.getCurrentInternal().getAllLinkEvents().size());
        assertEquals("Number of hosts", NUM_HOSTS,
                builder.getCurrentInternal().getAllHostEvents().size());

        final ImmutableTopologySnapshot clone = builder.build();
        assertEquals("Number of switch", NUM_SWITCH,
                clone.getAllSwitchEvents().size());
        assertEquals("Number of ports", NUM_PORTS * NUM_SWITCH,
                clone.getAllPortEvents().size());
        assertEquals("Number of links", NUM_LINKS,
                clone.getAllLinkEvents().size());
        assertEquals("Number of hosts", NUM_HOSTS,
                clone.getAllHostEvents().size());
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getSwitchEvent(Dpid)}.
     */
    @Test
    public void testGetSwitchEvent() {
        final Dpid dpid1 = new Dpid(1);
        assertNotNull("Switch 1 should exist",
                ss.getSwitchEvent(dpid1));

        final Dpid dpidNa = new Dpid(NUM_SWITCH + 1);
        assertNull("Switch NUM_SWITCH + 1 should not exist",
                ss.getSwitchEvent(dpidNa));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getAllSwitchEvents()}.
     */
    @Test
    public void testGetAllSwitchEvents() {
        assertEquals("Number of switch", NUM_SWITCH,
                ss.getAllSwitchEvents().size());
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getPortEvent(SwitchPort)}.
     */
    @Test
    public void testGetPortEventSwitchPort() {
        final Dpid dpid1 = new Dpid(1);
        final PortNumber port1 = PortNumber.uint32(1);
        assertNotNull("Switch 1 Port 1 should exist",
                ss.getPortEvent(new SwitchPort(dpid1, port1)));

        final PortNumber portNa = PortNumber.uint32(PORT_NUM_NA);
        assertNull("Switch 1 Port PORT_NUM_NA should not exist",
                ss.getPortEvent(new SwitchPort(dpid1, portNa)));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getPortEvent(Dpid, PortNumber)}.
     */
    @Test
    public void testGetPortEventDpidPortNumber() {
        final Dpid dpid1 = new Dpid(1);
        final PortNumber port1 = PortNumber.uint32(1);
        assertNotNull("Switch 1 Port 1 should exist",
                ss.getPortEvent(dpid1, port1));

        final PortNumber portNa = PortNumber.uint32(PORT_NUM_NA);
        assertNull("Switch 1 Port PORT_NUM_NA should not exist",
                ss.getPortEvent(dpid1, portNa));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getPortEvents(Dpid)}.
     */
    @Test
    public void testGetPortEvents() {
        final Dpid dpid1 = new Dpid(1);
        assertEquals("Number of ports", NUM_PORTS,
                ss.getPortEvents(dpid1).size());
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getAllPortEvents()}.
     */
    @Test
    public void testGetAllPortEvents() {
        assertEquals("Number of ports", NUM_PORTS * NUM_SWITCH,
                ss.getAllPortEvents().size());
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getLinkEvent(LinkTuple)}.
     */
    @Test
    public void testGetLinkEventLinkTuple() {
        assertNotNull("Link (1:1 -> 2:1) should exist",
                ss.getLinkEvent(LINK_IN_TOPOLOGY));

        final Dpid dpid1 = new Dpid(1);
        final Dpid dpid2 = new Dpid(2);
        final PortNumber port1 = PortNumber.uint32(1);
        final PortNumber portNa = PortNumber.uint32(PORT_NUM_NA);
        assertNull("Link (1:1 -> 2:PORT_NUM_NA) should not exist",
                ss.getLinkEvent(new LinkTuple(dpid1, port1, dpid2, portNa)));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getLinkEvent(LinkTuple, String)}.
     */
    @Test
    public void testGetLinkEventLinkTupleString() {
        assertNotNull("Link (1:1 -> 2:1) should exist",
                ss.getLinkEvent(LINK_IN_TOPOLOGY,
                        TopologyElement.TYPE_PACKET_LAYER));

        final Dpid dpid1 = new Dpid(1);
        final Dpid dpid2 = new Dpid(2);
        final PortNumber port1 = PortNumber.uint32(1);
        final PortNumber portNa = PortNumber.uint32(PORT_NUM_NA);
        assertNull("Link (1:1 -> 2:PORT_NUM_NA) should not exist",
                ss.getLinkEvent(new LinkTuple(dpid1, port1, dpid2, portNa),
                        TopologyElement.TYPE_PACKET_LAYER));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getLinkEventsFrom(SwitchPort)}.
     */
    @Test
    public void testGetLinkEventsFrom() {
        assertThat("Port 1:1 should at least have 1 outgoing link",
                ss.getLinkEventsFrom(LINK_IN_TOPOLOGY.getSrc()).size(),
                is(greaterThanOrEqualTo(1)));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getLinkEventsTo(SwitchPort)}.
     */
    @Test
    public void testGetLinkEventsTo() {
        assertThat("Port 2:1 should at least have 1 incoming link",
                ss.getLinkEventsTo(LINK_IN_TOPOLOGY.getDst()).size(),
                is(greaterThanOrEqualTo(1)));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getLinkEvents(LinkTuple)}.
     */
    @Test
    public void testGetLinkEvents() {
        assertFalse("Link (1:1 -> 2:1) should exist",
                ss.getLinkEvents(LINK_IN_TOPOLOGY).isEmpty());

        final Dpid dpid1 = new Dpid(1);
        final Dpid dpid2 = new Dpid(2);
        final PortNumber port1 = PortNumber.uint32(1);
        final PortNumber portNa = PortNumber.uint32(PORT_NUM_NA);
        assertTrue("Link (1:1 -> 2:PORT_NUM_NA) should not exist",
                ss.getLinkEvents(new LinkTuple(dpid1, port1, dpid2, portNa)).isEmpty());
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getAllLinkEvents()}.
     */
    @Test
    public void testGetAllLinkEvents() {
        assertEquals("Number of links", NUM_LINKS,
                ss.getAllLinkEvents().size());
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getHostEvent(MACAddress)}.
     */
    @Test
    public void testGetHostEvent() {
        assertNotNull("Host 0:..:0 should exist",
                ss.getHostEvent(MACAddress.valueOf(0L)));

        assertNull("Host MAC_NA should exist",
                ss.getHostEvent(MACAddress.valueOf(MAC_NA)));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getHostEvents(SwitchPort)}.
     */
    @Test
    public void testGetHostEvents() {
        final HostEvent host = ss.getHostEvent(MACAddress.valueOf(0L));
        final SwitchPort attachment = host.getAttachmentPoints().get(0);

        assertThat("should at least have 1 host attached",
                ss.getHostEvents(attachment).size(),
                is(greaterThanOrEqualTo(1)));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getAllHostEvents()}.
     */
    @Test
    public void testGetAllHostEvents() {
        assertEquals("Number of hosts", NUM_HOSTS,
                ss.getAllHostEvents().size());
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getSwitchMaster(Dpid)}.
     */
    @Test
    public void testGetSwitchMaster() {
        assertEquals("Master is stored", INSTANCE_ID, ss.getSwitchMaster(new Dpid(1)));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getSwitch(Dpid)}.
     */
    @Test
    public void testGetSwitch() {
        final Dpid dpid1 = new Dpid(1);
        assertNotNull("Switch 1 should exist",
                ss.getSwitch(dpid1));

        final Dpid dpidNa = new Dpid(NUM_SWITCH + 1);
        assertNull("Switch NUM_SWITCH + 1 should not exist",
                ss.getSwitch(dpidNa));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getSwitches()}.
     */
    @Test
    public void testGetSwitches() {
        assertEquals("Number of switch", NUM_SWITCH,
                Iterators.size(ss.getSwitches().iterator()));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getPort(Dpid, PortNumber)}.
     */
    @Test
    public void testGetPortDpidPortNumber() {
        final Dpid dpid1 = new Dpid(1);
        final PortNumber port1 = PortNumber.uint32(1);
        assertNotNull("Switch 1 Port 1 should exist",
                ss.getPort(dpid1, port1));

        final PortNumber portNa = PortNumber.uint32(PORT_NUM_NA);
        assertNull("Switch 1 Port PORT_NUM_NA should not exist",
                ss.getPort(dpid1, portNa));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getPort(SwitchPort)}.
     */
    @Test
    public void testGetPortSwitchPort() {
        final Dpid dpid1 = new Dpid(1);
        final PortNumber port1 = PortNumber.uint32(1);
        assertNotNull("Switch 1 Port 1 should exist",
                ss.getPort(new SwitchPort(dpid1, port1)));

        final PortNumber portNa = PortNumber.uint32(PORT_NUM_NA);
        assertNull("Switch 1 Port PORT_NUM_NA should not exist",
                ss.getPort(new SwitchPort(dpid1, portNa)));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getPorts(Dpid)}.
     */
    @Test
    public void testGetPorts() {
        final Dpid dpid1 = new Dpid(1);
        assertEquals("Number of ports", NUM_PORTS,
                ss.getPorts(dpid1).size());
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getOutgoingLink(Dpid, PortNumber)}.
     */
    @Test
    public void testGetOutgoingLinkDpidPortNumber() {
        final Dpid dpid1 = new Dpid(1);
        final PortNumber port1 = PortNumber.uint32(1);

        assertNotNull("Port 1:1 should at least have 1 outgoing link",
                ss.getOutgoingLink(dpid1, port1));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getOutgoingLink(SwitchPort)}.
     */
    @Test
    public void testGetOutgoingLinkSwitchPort() {
        final Dpid dpid1 = new Dpid(1);
        final PortNumber port1 = PortNumber.uint32(1);

        assertNotNull("Port 1:1 should at least have 1 outgoing link",
                ss.getOutgoingLink(new SwitchPort(dpid1, port1)));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getOutgoingLink(Dpid, PortNumber, String)}.
     */
    @Test
    public void testGetOutgoingLinkDpidPortNumberString() {
        final Dpid dpid1 = new Dpid(1);
        final PortNumber port1 = PortNumber.uint32(1);

        assertNotNull("Port 1:1 should at least have 1 outgoing link",
                ss.getOutgoingLink(dpid1, port1,
                        TopologyElement.TYPE_PACKET_LAYER));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getOutgoingLink(SwitchPort, String)}.
     */
    @Test
    public void testGetOutgoingLinkSwitchPortString() {
        final Dpid dpid1 = new Dpid(1);
        final PortNumber port1 = PortNumber.uint32(1);

        assertNotNull("Port 1:1 should at least have 1 outgoing link",
                ss.getOutgoingLink(new SwitchPort(dpid1, port1),
                        TopologyElement.TYPE_PACKET_LAYER));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getOutgoingLinks(SwitchPort)}.
     */
    @Test
    public void testGetOutgoingLinks() {
        final Dpid dpid1 = new Dpid(1);
        final PortNumber port1 = PortNumber.uint32(1);

        assertThat("Port 1:1 should at least have 1 outgoing link",
                ss.getOutgoingLinks(new SwitchPort(dpid1, port1)).size(),
                is(greaterThanOrEqualTo(1)));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getIncomingLink(Dpid, PortNumber)}.
     */
    @Test
    public void testGetIncomingLinkDpidPortNumber() {
        final Dpid dpid2 = new Dpid(2);
        final PortNumber port1 = PortNumber.uint32(1);

        assertNotNull("Port 2:1 should at least have 1 outgoing link",
                ss.getIncomingLink(dpid2, port1));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getIncomingLink(SwitchPort)}.
     */
    @Test
    public void testGetIncomingLinkSwitchPort() {
        final Dpid dpid2 = new Dpid(2);
        final PortNumber port1 = PortNumber.uint32(1);

        assertNotNull("Port 2:1 should at least have 1 outgoing link",
                ss.getIncomingLink(new SwitchPort(dpid2, port1)));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getIncomingLink(Dpid, PortNumber, String)}.
     */
    @Test
    public void testGetIncomingLinkDpidPortNumberString() {
        final Dpid dpid2 = new Dpid(2);
        final PortNumber port1 = PortNumber.uint32(1);

        assertNotNull("Port 2:1 should at least have 1 outgoing link",
                ss.getIncomingLink(dpid2, port1,
                        TopologyElement.TYPE_PACKET_LAYER));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getIncomingLink(SwitchPort, String)}.
     */
    @Test
    public void testGetIncomingLinkSwitchPortString() {
        final Dpid dpid2 = new Dpid(2);
        final PortNumber port1 = PortNumber.uint32(1);

        assertNotNull("Port 2:1 should at least have 1 outgoing link",
                ss.getIncomingLink(new SwitchPort(dpid2, port1),
                        TopologyElement.TYPE_PACKET_LAYER));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getIncomingLinks(SwitchPort)}.
     */
    @Test
    public void testGetIncomingLinks() {
        final Dpid dpid2 = new Dpid(2);
        final PortNumber port1 = PortNumber.uint32(1);

        assertThat("Port 2:1 should at least have 1 outgoing link",
                ss.getIncomingLinks(new SwitchPort(dpid2, port1)).size(),
                is(greaterThanOrEqualTo(1)));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getLink(Dpid, PortNumber, Dpid, PortNumber)}.
     */
    @Test
    public void testGetLinkDpidPortNumberDpidPortNumber() {
        final Dpid dpid1 = new Dpid(1);
        final Dpid dpid2 = new Dpid(2);
        final PortNumber port1 = PortNumber.uint32(1);
        assertNotNull("Link (1:1 -> 2:1) should exist",
                ss.getLink(dpid1, port1, dpid2, port1));

        final PortNumber portNa = PortNumber.uint32(PORT_NUM_NA);
        assertNull("Link (1:1 -> 2:PORT_NUM_NA) should not exist",
                ss.getLink(dpid1, port1, dpid2, portNa));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getLink(Dpid, PortNumber, Dpid, PortNumber, String)}.
     */
    @Test
    public void testGetLinkDpidPortNumberDpidPortNumberString() {
        final Dpid dpid1 = new Dpid(1);
        final Dpid dpid2 = new Dpid(2);
        final PortNumber port1 = PortNumber.uint32(1);
        assertNotNull("Link (1:1 -> 2:1) should exist",
                ss.getLink(dpid1, port1, dpid2, port1,
                        TopologyElement.TYPE_PACKET_LAYER));

        final PortNumber portNa = PortNumber.uint32(PORT_NUM_NA);
        assertNull("Link (1:1 -> 2:PORT_NUM_NA) should not exist",
                ss.getLink(dpid1, port1, dpid2, portNa,
                        TopologyElement.TYPE_PACKET_LAYER));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getLinks()}.
     */
    @Test
    public void testGetLinks() {
        assertEquals("Number of links", NUM_LINKS,
                Iterators.size(ss.getLinks().iterator()));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getHostByMac(MACAddress)}.
     */
    @Test
    public void testGetHostByMac() {
        assertNotNull("Host 0:..:0 should exist",
                ss.getHostByMac(MACAddress.valueOf(0L)));

        assertNull("Host MAC_NA should exist",
                ss.getHostByMac(MACAddress.valueOf(MAC_NA)));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getHosts()}.
     */
    @Test
    public void testGetHosts() {
        assertEquals("Number of host", NUM_HOSTS,
                Iterators.size(ss.getHosts().iterator()));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getHosts(SwitchPort)}.
     */
    @Test
    public void testGetHostsSwitchPort() {
        final HostEvent host = ss.getHostEvent(MACAddress.valueOf(0L));
        final SwitchPort attachment = host.getAttachmentPoints().get(0);

        assertThat("should at least have 1 host attached",
                ss.getHosts(attachment).size(),
                is(greaterThanOrEqualTo(1)));
    }

    /**
     * Create random topology with specified number of elements.
     *
     * @param switches number of switches
     * @param ports number of ports
     * @param links number of links
     * @param hosts number of hosts
     * @return created topology
     */
    private static ImmutableTopologySnapshot createTopology(
                                                    final int switches,
                                                    final int ports,
                                                    final int links,
                                                    final int hosts) {

        Builder builder = ImmutableTopologySnapshot.initialBuilder();
        for (int i = 0; i < switches; ++i) {
            final Dpid dpid = new Dpid(i + 1);
            builder.putSwitchMastershipEvent(new MastershipEvent(dpid,
                                INSTANCE_ID,
                                Role.MASTER));
            builder.putSwitch(new SwitchEvent(dpid));
            for (int j = 0; j < ports; ++j) {
                builder.putPort(new PortEvent(dpid, PortNumber.uint32(j + 1)));
            }
        }


        final RandomDataGenerator rand = new RandomDataGenerator();
        int l = 0;
        // Always add Link (1:1 -> 2:1)
        builder.putLink(new LinkEvent(LINK_IN_TOPOLOGY));
        ++l;

        while (l < links) {
            int sw1 = rand.nextInt(1, switches);
            int port1 = rand.nextInt(1, ports);
            int sw2 = rand.nextInt(1, switches);
            int port2 = rand.nextInt(1, ports);
            if (sw1 == sw2 && port1 == port2) {
                continue;
            }
            LinkTuple linkId = new LinkTuple(
                    new Dpid(sw1), PortNumber.uint32(port1),
                    new Dpid(sw2), PortNumber.uint32(port2));

            final BaseInternalTopology current = builder.getCurrentInternal();
            if (!current.getLinkEventsFrom(linkId.getSrc()).isEmpty() ||
                !current.getLinkEventsTo(linkId.getSrc()).isEmpty()) {
                // src port already has a link
                continue;
            }
            if (!current.getLinkEventsFrom(linkId.getDst()).isEmpty() ||
                !current.getLinkEventsTo(linkId.getDst()).isEmpty()) {
                // dst port already has a link
                continue;
            }

            // add only if both port doesn't have any link
            builder.putLink(new LinkEvent(linkId));
            ++l;
        }

        // Add host with mac 0 -> hosts
        int h = 0;
        while (h < hosts) {
            HostEvent host = new HostEvent(MACAddress.valueOf(h));
            SwitchPort swp = new SwitchPort(
                                new Dpid(rand.nextInt(1, switches)),
                                PortNumber.uint32(rand.nextInt(1, ports)));

            if (builder.getCurrent().getIncomingLinks(swp).isEmpty() &&
                builder.getCurrent().getOutgoingLinks(swp).isEmpty()) {
                // add only if link doesn't exist
                host.addAttachmentPoint(swp);
                builder.putHost(host);
                ++h;
            }
        }

        return builder.build();
    }
}
