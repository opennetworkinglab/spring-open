package net.onrc.onos.core.topology;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.LinkTuple;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

/**
 * A mock class of Topology.
 * This class should be used only by test code.
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
        SwitchEvent sw = new SwitchEvent(new Dpid(switchId));
        this.putSwitch(sw);
        return this.getSwitch(sw.getDpid());
    }

    public Port addPort(Switch sw, Long portNumber) {
        PortEvent port = new PortEvent(sw.getDpid(),
                                new PortNumber(portNumber.shortValue()));
        ((TopologyImpl) this).putPort(port);
        return this.getPort(port.getSwitchPort());
    }

    public void addBidirectionalLinks(Long srcDpid, Long srcPortNo,
                                      Long dstDpid, Long dstPortNo) {
        addBidirectionalLinks(srcDpid, srcPortNo, dstDpid, dstPortNo, null);
    }

    public void addBidirectionalLinks(Long srcDpid, Long srcPortNo,
                                      Long dstDpid, Long dstPortNo,
                                      Double capacity) {
        LinkEvent[] links = new LinkEvent[2];
        final SwitchPort src = new SwitchPort(srcDpid, srcPortNo);
        final SwitchPort dst = new SwitchPort(dstDpid, dstPortNo);
        links[0] = new LinkEvent(src, dst);
        links[1] = new LinkEvent(dst, src);
        if (capacity != null) {
            links[0].setCapacity(capacity);
            links[1].setCapacity(capacity);
        }

        putLink(links[0]);
        putLink(links[1]);
    }

    /**
     * Creates a sample topology of 4 switches and 5 bidirectional links.
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

        addBidirectionalLinks(1L, 12L, 2L, 21L, 1000.0);
        addBidirectionalLinks(2L, 23L, 3L, 32L, 1000.0);
        addBidirectionalLinks(3L, 34L, 4L, 43L, 1000.0);
        addBidirectionalLinks(4L, 41L, 1L, 14L, 1000.0);
        addBidirectionalLinks(2L, 24L, 4L, 42L, 1000.0);
    }

    /**
     * Creates a sample topology of 4 switches and 5 bidirectional links.
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
        HostEvent host1 = new HostEvent(mac1);
        host1.addAttachmentPoint(port15.getSwitchPort());
        host1.setLastSeenTime(1L);
        this.putHost(host1);

        MACAddress mac3 = MACAddress.valueOf("00:11:22:33:44:55");
        HostEvent host3 = new HostEvent(mac3);
        host3.addAttachmentPoint(port35.getSwitchPort());
        host3.setLastSeenTime(1L);
        this.putHost(host3);

        addBidirectionalLinks(1L, 12L, 2L, 21L, 1000.0);
        addBidirectionalLinks(2L, 23L, 3L, 32L, 1000.0);
        addBidirectionalLinks(3L, 34L, 4L, 43L, 1000.0);
        addBidirectionalLinks(4L, 41L, 1L, 14L, 1000.0);
        addBidirectionalLinks(2L, 24L, 4L, 42L, 1000.0);
    }

    public void removeLink(Long srcDpid, Long srcPortNo, Long dstDpid,
                           Long dstPortNo) {
        this.removeLink(new Dpid(srcDpid),
                           new PortNumber(srcPortNo.shortValue()),
                           new Dpid(dstDpid),
                           new PortNumber(dstPortNo.shortValue()));
    }

    public void removeLink(Dpid srcDpid, PortNumber srcPortNo,
                           Dpid dstDpid, PortNumber dstPortNo) {
        this.removeLink(srcDpid, srcPortNo, dstDpid, dstPortNo,
                TopologyElement.TYPE_PACKET_LAYER);
    }
    public void removeLink(Dpid srcDpid, PortNumber srcPortNo,
                           Dpid dstDpid, PortNumber dstPortNo, String type) {
        super.removeLink(new LinkTuple(srcDpid, srcPortNo, dstDpid, dstPortNo),
                        type);
    }
}
