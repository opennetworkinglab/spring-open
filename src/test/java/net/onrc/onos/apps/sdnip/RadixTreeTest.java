package net.onrc.onos.apps.sdnip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

import com.google.common.net.InetAddresses;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;

/**
 * Sanity tests for the InvertedRadixTree.
 * <p/>
 * These tests are used to verify that the InvertedRadixTree provides the
 * functionality we need to fit our use case.
 */
public class RadixTreeTest {

    private Map<String, Interface> interfaces;
    private InvertedRadixTree<Interface> interfaceRoutes;

    private Interface longestInterfacePrefixMatch(InetAddress address) {
        Prefix prefixToSearchFor = new Prefix(address.getAddress(),
                Prefix.MAX_PREFIX_LENGTH);
        Iterator<Interface> it =
                interfaceRoutes.getValuesForKeysPrefixing(
                        prefixToSearchFor.toBinaryString()).iterator();
        Interface intf = null;
        // Find the last prefix, which will be the longest prefix
        while (it.hasNext()) {
            intf = it.next();
        }

        return intf;
    }

    /**
     * This is just a test of the InvertedRadixTree, rather than an actual unit
     * test of SdnIp. It tests that the algorithm used to retrieve the
     * longest prefix match from the tree actually does retrieve the longest
     * prefix, and not just any matching prefix.
     */
    @Test
    public void getOutgoingInterfaceTest() {
        interfaces = new HashMap<>();
        interfaceRoutes = new ConcurrentInvertedRadixTree<>(
                new DefaultByteArrayNodeFactory());

        Interface interface1 = new Interface("sw3-eth1", "00:00:00:00:00:00:00:a3",
                (short) 1, "192.168.10.101", 24);
        interfaces.put(interface1.getName(), interface1);
        Interface interface2 = new Interface("sw5-eth1", "00:00:00:00:00:00:00:a5",
                (short) 1, "192.168.20.101", 16);
        interfaces.put(interface2.getName(), interface2);
        Interface interface3 = new Interface("sw2-eth1", "00:00:00:00:00:00:00:a2",
                (short) 1, "192.168.60.101", 16);
        interfaces.put(interface3.getName(), interface3);
        Interface interface4 = new Interface("sw6-eth1", "00:00:00:00:00:00:00:a6",
                (short) 1, "192.168.60.101", 30);
        interfaces.put(interface4.getName(), interface4);
        Interface interface5 = new Interface("sw4-eth4", "00:00:00:00:00:00:00:a4",
                (short) 4, "192.168.60.101", 24);
        interfaces.put(interface5.getName(), interface5);

        for (Interface intf : interfaces.values()) {
            Prefix prefix = new Prefix(intf.getIpAddress().getAddress(),
                    intf.getPrefixLength());
            interfaceRoutes.put(prefix.toBinaryString(), intf);
        }

        // Check whether the prefix length takes effect
        InetAddress nextHopAddress = InetAddresses.forString("192.0.0.1");
        assertNotNull(nextHopAddress);
        assertNull(longestInterfacePrefixMatch(nextHopAddress));

        // Check whether it returns the longest matchable address
        nextHopAddress = InetAddresses.forString("192.168.60.101");
        assertEquals("sw6-eth1", longestInterfacePrefixMatch(nextHopAddress).getName());
    }
}
