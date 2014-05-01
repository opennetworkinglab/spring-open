package net.onrc.onos.core.intent;

import net.onrc.onos.core.topology.Link;
import net.onrc.onos.core.topology.LinkImpl;
import net.onrc.onos.core.topology.NetworkGraphImpl;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.topology.SwitchImpl;

/**
 * A mock class of NetworkGraph.
 * This class should be used only by test codes.
 *
 * @author Toshio Koide (t-koide@onlab.us)
 */
public class MockNetworkGraph extends NetworkGraphImpl {
    // TODO this class doesn't seem like it should extend NetworkGraphImpl. It
    // isn't a NetworkGraph, it's more of a NetworkGraphBuilder - methods to
    // create an populate a fake network graph that's not based on discovery
    // data from the driver modules.
    // We may well need a MockNetworkGraph, but that's not what this class is
    // doing.

    public static Long LOCAL_PORT = 0xFFFEL;
    public SwitchImpl sw1, sw2, sw3, sw4;

    public Switch addSwitch(Long switchId) {
        SwitchImpl sw = new SwitchImpl(this, switchId);
        this.putSwitch(sw);
        return sw;
    }

    public Link[] addBidirectionalLinks(Long srcDpid, Long srcPortNo, Long dstDpid, Long dstPortNo) {
        Link[] links = new Link[2];
        links[0] = new LinkImpl(this, getPort(srcDpid, srcPortNo), getPort(dstDpid, dstPortNo));
        links[1] = new LinkImpl(this, getPort(dstDpid, dstPortNo), getPort(srcDpid, srcPortNo));

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
        sw1.addPort(LOCAL_PORT);
        sw2 = (SwitchImpl) addSwitch(2L);
        sw2.addPort(LOCAL_PORT);
        sw3 = (SwitchImpl) addSwitch(3L);
        sw3.addPort(LOCAL_PORT);
        sw4 = (SwitchImpl) addSwitch(4L);
        sw4.addPort(LOCAL_PORT);

        sw1.addPort(12L); // sw1 -> sw2
        sw1.addPort(14L); // sw1 -> sw4
        sw2.addPort(21L); // sw2 -> sw1
        sw2.addPort(23L); // sw2 -> sw3
        sw2.addPort(24L); // sw2 -> sw4
        sw3.addPort(32L); // sw3 -> sw2
        sw3.addPort(34L); // sw3 -> sw4
        sw4.addPort(41L); // sw4 -> sw1
        sw4.addPort(42L); // sw4 -> sw2
        sw4.addPort(43L); // sw4 -> sw3

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
        removeLink(getLink(srcDpid, srcPortNo, dstDpid, dstPortNo));
    }
}
