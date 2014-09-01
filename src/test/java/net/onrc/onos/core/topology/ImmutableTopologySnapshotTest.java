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
                builder.getCurrentInternal().getAllSwitchDataEntries().isEmpty());
        assertTrue("Topology should be empty",
                builder.build().getAllSwitchDataEntries().isEmpty());
    }

    /**
     * Builder should create a copy of the parent topology.
     */
    @Test
    public void testBuilder() {
        final Builder builder = ss.builder();
        assertEquals("Number of switch", NUM_SWITCH,
                builder.getCurrentInternal().getAllSwitchDataEntries().size());
        assertEquals("Number of ports", NUM_PORTS * NUM_SWITCH,
                builder.getCurrentInternal().getAllPortDataEntries().size());
        assertEquals("Number of links", NUM_LINKS,
                builder.getCurrentInternal().getAllLinkDataEntries().size());
        assertEquals("Number of hosts", NUM_HOSTS,
                builder.getCurrentInternal().getAllHostDataEntries().size());

        final ImmutableTopologySnapshot clone = builder.build();
        assertEquals("Number of switch", NUM_SWITCH,
                clone.getAllSwitchDataEntries().size());
        assertEquals("Number of ports", NUM_PORTS * NUM_SWITCH,
                clone.getAllPortDataEntries().size());
        assertEquals("Number of links", NUM_LINKS,
                clone.getAllLinkDataEntries().size());
        assertEquals("Number of hosts", NUM_HOSTS,
                clone.getAllHostDataEntries().size());
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getSwitchData(Dpid)}.
     */
    @Test
    public void testGetSwitchData() {
        final Dpid dpid1 = new Dpid(1);
        assertNotNull("Switch 1 should exist",
                ss.getSwitchData(dpid1));

        final Dpid dpidNa = new Dpid(NUM_SWITCH + 1);
        assertNull("Switch NUM_SWITCH + 1 should not exist",
                ss.getSwitchData(dpidNa));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getAllSwitchDataEntries()}.
     */
    @Test
    public void testGetAllSwitchDataEntries() {
        assertEquals("Number of switch", NUM_SWITCH,
                ss.getAllSwitchDataEntries().size());
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getPortData(SwitchPort)}.
     */
    @Test
    public void testGetPortDataSwitchPort() {
        final Dpid dpid1 = new Dpid(1);
        final PortNumber port1 = PortNumber.uint32(1);
        assertNotNull("Switch 1 Port 1 should exist",
                ss.getPortData(new SwitchPort(dpid1, port1)));

        final PortNumber portNa = PortNumber.uint32(PORT_NUM_NA);
        assertNull("Switch 1 Port PORT_NUM_NA should not exist",
                ss.getPortData(new SwitchPort(dpid1, portNa)));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getPortData(Dpid, PortNumber)}.
     */
    @Test
    public void testGetPortDataDpidPortNumber() {
        final Dpid dpid1 = new Dpid(1);
        final PortNumber port1 = PortNumber.uint32(1);
        assertNotNull("Switch 1 Port 1 should exist",
                ss.getPortData(dpid1, port1));

        final PortNumber portNa = PortNumber.uint32(PORT_NUM_NA);
        assertNull("Switch 1 Port PORT_NUM_NA should not exist",
                ss.getPortData(dpid1, portNa));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getPortDataEntries(Dpid)}.
     */
    @Test
    public void testGetPortDataEntries() {
        final Dpid dpid1 = new Dpid(1);
        assertEquals("Number of ports", NUM_PORTS,
                ss.getPortDataEntries(dpid1).size());
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getAllPortDataEntries()}.
     */
    @Test
    public void testGetAllPortDataEntries() {
        assertEquals("Number of ports", NUM_PORTS * NUM_SWITCH,
                ss.getAllPortDataEntries().size());
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getLinkData(LinkTuple)}.
     */
    @Test
    public void testGetLinkDataLinkTuple() {
        assertNotNull("Link (1:1 -> 2:1) should exist",
                ss.getLinkData(LINK_IN_TOPOLOGY));

        final Dpid dpid1 = new Dpid(1);
        final Dpid dpid2 = new Dpid(2);
        final PortNumber port1 = PortNumber.uint32(1);
        final PortNumber portNa = PortNumber.uint32(PORT_NUM_NA);
        assertNull("Link (1:1 -> 2:PORT_NUM_NA) should not exist",
                ss.getLinkData(new LinkTuple(dpid1, port1, dpid2, portNa)));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getLinkData(LinkTuple, String)}.
     */
    @Test
    public void testGetLinkDataLinkTupleString() {
        assertNotNull("Link (1:1 -> 2:1) should exist",
                ss.getLinkData(LINK_IN_TOPOLOGY,
                        TopologyElement.TYPE_PACKET_LAYER));

        final Dpid dpid1 = new Dpid(1);
        final Dpid dpid2 = new Dpid(2);
        final PortNumber port1 = PortNumber.uint32(1);
        final PortNumber portNa = PortNumber.uint32(PORT_NUM_NA);
        assertNull("Link (1:1 -> 2:PORT_NUM_NA) should not exist",
                ss.getLinkData(new LinkTuple(dpid1, port1, dpid2, portNa),
                        TopologyElement.TYPE_PACKET_LAYER));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getLinkDataEntriesFrom(SwitchPort)}.
     */
    @Test
    public void testGetLinkDataEntriesFrom() {
        assertThat("Port 1:1 should at least have 1 outgoing link",
                ss.getLinkDataEntriesFrom(LINK_IN_TOPOLOGY.getSrc()).size(),
                is(greaterThanOrEqualTo(1)));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getLinkDataEntriesTo(SwitchPort)}.
     */
    @Test
    public void testGetLinkDataEntriesTo() {
        assertThat("Port 2:1 should at least have 1 incoming link",
                ss.getLinkDataEntriesTo(LINK_IN_TOPOLOGY.getDst()).size(),
                is(greaterThanOrEqualTo(1)));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getLinkDataEntries(LinkTuple)}.
     */
    @Test
    public void testGetLinkDataEntries() {
        assertFalse("Link (1:1 -> 2:1) should exist",
                ss.getLinkDataEntries(LINK_IN_TOPOLOGY).isEmpty());

        final Dpid dpid1 = new Dpid(1);
        final Dpid dpid2 = new Dpid(2);
        final PortNumber port1 = PortNumber.uint32(1);
        final PortNumber portNa = PortNumber.uint32(PORT_NUM_NA);
        assertTrue("Link (1:1 -> 2:PORT_NUM_NA) should not exist",
                ss.getLinkDataEntries(new LinkTuple(dpid1, port1, dpid2, portNa)).isEmpty());
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getAllLinkDataEntries()}.
     */
    @Test
    public void testGetAllLinkDataEntries() {
        assertEquals("Number of links", NUM_LINKS,
                ss.getAllLinkDataEntries().size());
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getHostData(MACAddress)}.
     */
    @Test
    public void testGetHostData() {
        assertNotNull("Host 0:..:0 should exist",
                ss.getHostData(MACAddress.valueOf(0L)));

        assertNull("Host MAC_NA should exist",
                ss.getHostData(MACAddress.valueOf(MAC_NA)));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getHostDataEntries(SwitchPort)}.
     */
    @Test
    public void testGetHostDataEntries() {
        final HostData host = ss.getHostData(MACAddress.valueOf(0L));
        final SwitchPort attachment = host.getAttachmentPoints().get(0);

        assertThat("should at least have 1 host attached",
                ss.getHostDataEntries(attachment).size(),
                is(greaterThanOrEqualTo(1)));
    }

    /**
     * Test for {@link ImmutableTopologySnapshot#getAllHostDataEntries()}.
     */
    @Test
    public void testGetAllHostDataEntries() {
        assertEquals("Number of hosts", NUM_HOSTS,
                ss.getAllHostDataEntries().size());
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
        final HostData host = ss.getHostData(MACAddress.valueOf(0L));
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
            builder.putSwitch(new SwitchData(dpid));
            for (int j = 0; j < ports; ++j) {
                builder.putPort(new PortData(dpid, PortNumber.uint32(j + 1)));
            }
        }


        final RandomDataGenerator rand = new RandomDataGenerator();
        int l = 0;
        // Always add Link (1:1 -> 2:1)
        builder.putLink(new LinkData(LINK_IN_TOPOLOGY));
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
            if (!current.getLinkDataEntriesFrom(linkId.getSrc()).isEmpty() ||
                !current.getLinkDataEntriesTo(linkId.getSrc()).isEmpty()) {
                // src port already has a link
                continue;
            }
            if (!current.getLinkDataEntriesFrom(linkId.getDst()).isEmpty() ||
                !current.getLinkDataEntriesTo(linkId.getDst()).isEmpty()) {
                // dst port already has a link
                continue;
            }

            // add only if both port doesn't have any link
            builder.putLink(new LinkData(linkId));
            ++l;
        }

        // Add host with mac 0 -> hosts
        int h = 0;
        while (h < hosts) {
            HostData host = new HostData(MACAddress.valueOf(h));
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
