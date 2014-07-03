package net.onrc.onos.core.topology;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;

/**
 * A mock class of Topology.
 * This class should be used only by test codes.
 */
public class MockTopology extends TopologyImpl {
    // TODO this class doesn't seem like it should extend TopologyImpl. It
    // isn't a Topology, it's more of a TopologyBuilder - methods to
    // create an populate a fake topology that's not based on discovery
    // data from the driver modules.
    // We may well need a MockTopology, but that's not what this class is
    // doing.

    public static final Long LOCAL_PORT = 0xFFFEL;
    public SwitchImpl sw1, sw2, sw3, sw4;

    public Switch addSwitch(Long switchId) {
        SwitchImpl sw = new SwitchImpl(this, switchId);
        this.putSwitch(sw);
        return sw;
    }

    public Port addPort(Switch sw, Long portNumber) {
        PortImpl port = new PortImpl(this, sw.getDpid(),
                                new PortNumber(portNumber.shortValue()));
        ((TopologyImpl) this).putPort(port);
        return port;
    }

    public Link[] addBidirectionalLinks(Long srcDpid, Long srcPortNo, Long dstDpid, Long dstPortNo) {
        Link[] links = new Link[2];
        final Dpid srcDpidObj = new Dpid(srcDpid);
        final Dpid dstDpidObj = new Dpid(dstDpid);
        final PortNumber srcPortNum = new PortNumber(srcPortNo.shortValue());
        final PortNumber dstPortNum = new PortNumber(dstPortNo.shortValue());
        links[0] = new LinkImpl(this,
                getPort(srcDpidObj, srcPortNum),
                getPort(dstDpidObj, dstPortNum));
        links[1] = new LinkImpl(this,
                getPort(dstDpidObj, dstPortNum),
                getPort(srcDpidObj, srcPortNum));

        putLink(links[0]);
        putLink(links[1]);

        return links;
    }

    /**
     * create sample topology of 4 switches and 5 bidirectional links.
     * <pre>
     * [1] --- [2]
     *  |    /  |
     *  |  /    |
     * [4] --- [3]
     * </pre>
     */
    public void createSampleTopology1() {
        sw1 = (SwitchImpl) addSwitch(1L);
        addPort(sw1, LOCAL_PORT);
        sw2 = (SwitchImpl) addSwitch(2L);
        addPort(sw2, LOCAL_PORT);
        sw3 = (SwitchImpl) addSwitch(3L);
        addPort(sw3, LOCAL_PORT);
        sw4 = (SwitchImpl) addSwitch(4L);
        addPort(sw4, LOCAL_PORT);

        addPort(sw1, 12L); // sw1 -> sw2
        addPort(sw1, 14L); // sw1 -> sw4
        addPort(sw2, 21L); // sw2 -> sw1
        addPort(sw2, 23L); // sw2 -> sw3
        addPort(sw2, 24L); // sw2 -> sw4
        addPort(sw3, 32L); // sw3 -> sw2
        addPort(sw3, 34L); // sw3 -> sw4
        addPort(sw4, 41L); // sw4 -> sw1
        addPort(sw4, 42L); // sw4 -> sw2
        addPort(sw4, 43L); // sw4 -> sw3

        addBidirectionalLinks(1L, 12L, 2L, 21L);
        addBidirectionalLinks(2L, 23L, 3L, 32L);
        addBidirectionalLinks(3L, 34L, 4L, 43L);
        addBidirectionalLinks(4L, 41L, 1L, 14L);
        addBidirectionalLinks(2L, 24L, 4L, 42L);

        // set capacity of all links to 1000Mbps
        for (Link link : getLinks()) {
            ((LinkImpl) link).setCapacity(1000.0);
        }
    }

    /**
     * create sample topology of 4 switches and 5 bidirectional links.
     * <pre>
     *
     *
     * [H1]-[1] --- [2]
     *       |    /  |
     *       |  /    |
     *      [4] --- [3]-[H3]
     * </pre>
     */
    public void createSampleTopology2() {
        sw1 = (SwitchImpl) addSwitch(1L);
        addPort(sw1, LOCAL_PORT);
        sw2 = (SwitchImpl) addSwitch(2L);
        addPort(sw2, LOCAL_PORT);
        sw3 = (SwitchImpl) addSwitch(3L);
        addPort(sw3, LOCAL_PORT);
        sw4 = (SwitchImpl) addSwitch(4L);
        addPort(sw4, LOCAL_PORT);

        Port port12 = addPort(sw1, 12L); // sw1 -> sw2
        Port port14 = addPort(sw1, 14L); // sw1 -> sw4
        Port port15 = addPort(sw1, 15L); // sw1 -> h1
        Port port21 = addPort(sw2, 21L); // sw2 -> sw1
        Port port23 = addPort(sw2, 23L); // sw2 -> sw3
        Port port24 = addPort(sw2, 24L); // sw2 -> sw4
        Port port32 = addPort(sw3, 32L); // sw3 -> sw2
        Port port34 = addPort(sw3, 34L); // sw3 -> sw4
        Port port35 = addPort(sw3, 35L); // sw3 -> h3
        Port port41 = addPort(sw4, 41L); // sw4 -> sw1
        Port port42 = addPort(sw4, 42L); // sw4 -> sw2
        Port port43 = addPort(sw4, 43L); // sw4 -> sw3

        MACAddress mac1 = MACAddress.valueOf("00:44:33:22:11:00");
        DeviceImpl dev1 = new DeviceImpl(this, mac1);
        dev1.addAttachmentPoint(port15);
        dev1.setLastSeenTime(1L);
        this.putDevice(dev1);

        MACAddress mac3 = MACAddress.valueOf("00:11:22:33:44:55");
        DeviceImpl dev3 = new DeviceImpl(this, mac3);
        dev3.addAttachmentPoint(port35);
        dev3.setLastSeenTime(1L);
        this.putDevice(dev3);

        addBidirectionalLinks(1L, 12L, 2L, 21L);
        addBidirectionalLinks(2L, 23L, 3L, 32L);
        addBidirectionalLinks(3L, 34L, 4L, 43L);
        addBidirectionalLinks(4L, 41L, 1L, 14L);
        addBidirectionalLinks(2L, 24L, 4L, 42L);

        // set capacity of all links to 1000Mbps
        for (Link link : getLinks()) {
            ((LinkImpl) link).setCapacity(1000.0);
        }
    }

    public void removeLink(Long srcDpid, Long srcPortNo, Long dstDpid, Long dstPortNo) {
        removeLink(getLink(new Dpid(srcDpid),
                           new PortNumber(srcPortNo.shortValue()),
                           new Dpid(dstDpid),
                           new PortNumber(dstPortNo.shortValue())));
    }

    public void removeLink(Dpid srcDpid, PortNumber srcPortNo,
                           Dpid dstDpid, PortNumber dstPortNo) {
        super.removeLink(getLink(srcDpid, srcPortNo, dstDpid, dstPortNo));
    }
}
