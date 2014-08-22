package net.onrc.onos.apps.sdnip;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reportMatcher;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.api.newintent.IntentService;
import net.onrc.onos.api.newintent.MultiPointToSinglePointIntent;
import net.onrc.onos.apps.proxyarp.IProxyArpService;
import net.onrc.onos.apps.sdnip.RibUpdate.Operation;
import net.onrc.onos.core.matchaction.action.ModifyDstMacAction;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.util.IPv4;
import net.onrc.onos.core.util.IdBlock;
import net.onrc.onos.core.util.IntegrationTest;
import net.onrc.onos.core.util.SwitchPort;
import net.onrc.onos.core.util.TestUtils;

import org.easymock.IAnswer;
import org.easymock.IArgumentMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.common.net.InetAddresses;

/**
 * Integration tests for the SDN-IP application.
 * <p/>
 * The tests are very coarse-grained. They feed route updates in to SDN-IP
 * (simulating routes learnt from BGPd), then they check that the correct
 * intents are created and submitted to the intent service. The entire route
 * processing logic of SDN-IP is tested.
 */
@Category(IntegrationTest.class)
public class SdnIpTest {
    private static final int MAC_ADDRESS_LENGTH = 6;
    private static final InetAddress ROUTER_ID =
            InetAddresses.forString("192.168.10.101");

    private static final int MIN_PREFIX_LENGTH = 1;
    private static final int MAX_PREFIX_LENGTH = 32;

    private SdnIp sdnip;
    private IProxyArpService proxyArp;
    private IntentService intentService;

    private Map<String, Interface> interfaces;
    private Map<InetAddress, BgpPeer> peers;

    private Random random;

    @Before
    public void setUp() throws Exception {
        interfaces = setUpInterfaces();
        peers = setUpPeers();
        random = new Random();
        initSdnIp();
    }

    private Map<String, Interface> setUpInterfaces() {
        Map<String, Interface> configuredInterfaces = new HashMap<>();

        String name1 = "s1-eth1";
        configuredInterfaces.put(name1, new Interface(name1, "00:00:00:00:00:00:00:01",
                (short) 1, "192.168.10.101", 24));
        String name2 = "s2-eth1";
        configuredInterfaces.put(name2, new Interface(name2, "00:00:00:00:00:00:00:02",
                (short) 1, "192.168.20.101", 24));
        String name3 = "s3-eth1";
        configuredInterfaces.put(name3, new Interface(name3, "00:00:00:00:00:00:00:03",
                (short) 1, "192.168.30.101", 24));

        return configuredInterfaces;
    }

    private Map<InetAddress, BgpPeer> setUpPeers() {
        Map<InetAddress, BgpPeer> configuredPeers = new LinkedHashMap<>();

        String peer1 = "192.168.10.1";
        configuredPeers.put(InetAddresses.forString(peer1),
                new BgpPeer("s1-eth1", peer1));

        String peer2 = "192.168.20.1";
        configuredPeers.put(InetAddresses.forString(peer2),
                new BgpPeer("s2-eth1", peer2));

        String peer3 = "192.168.30.1";
        configuredPeers.put(InetAddresses.forString(peer3),
                new BgpPeer("s3-eth1", peer3));

        return configuredPeers;
    }

    private void initSdnIp() throws FloodlightModuleException {
        sdnip = new SdnIp();

        FloodlightModuleContext context = new FloodlightModuleContext();
        context.addConfigParam(sdnip, "BgpdRestIp", "1.1.1.1");
        context.addConfigParam(sdnip, "RouterId", "192.168.10.101");

        intentService = createMock(IntentService.class);
        replay(intentService);

        proxyArp = new TestProxyArpService();
        context.addService(IProxyArpService.class, proxyArp);

        IControllerRegistryService registry =
                createMock(IControllerRegistryService.class);
        expect(registry.allocateUniqueIdBlock()).
                andReturn(new IdBlock(0, Long.MAX_VALUE));
        replay(registry);
        context.addService(IControllerRegistryService.class, registry);

        TestUtils.setField(sdnip, "intentService", intentService);
        sdnip.init(context);
        TestUtils.setField(sdnip, "interfaces", interfaces);
        TestUtils.setField(sdnip, "bgpPeers", peers);

    }

    /**
     * EasyMock matcher that matches {@link MultiPointToSinglePointIntent}s but
     * ignores the {@link IntentId} when matching.
     * <p/>
     * The normal intent equals method tests that the intent IDs are equal,
     * however in these tests I can't know what the intent IDs will be in
     * advance, so I can't set up expected intents with the correct IDs. Even
     * though I can set up an ID generator that generates a sequential stream
     * of IDs, I don't know in which order the IDs will be assigned to intents
     * (because intents can be processed out of order due to timing randomness
     * introduced by the test ARP module).
     * <p/>
     * The solution is to use an EasyMock matcher that verifies that all the
     * value properties of the provided intent match the expected values, but
     * ignores the intent ID when testing equality.
     */
    private static final class IdAgnosticIntentMatcher implements IArgumentMatcher {
        private final MultiPointToSinglePointIntent intent;
        private String providedIntentString;

        /**
         * Constructor taking the expected intent to match against.
         *
         * @param intent the expected intent
         */
        public IdAgnosticIntentMatcher(MultiPointToSinglePointIntent intent) {
            this.intent = intent;
        }

        @Override
        public void appendTo(StringBuffer strBuffer) {
            strBuffer.append("IntentMatcher unable to match: " + providedIntentString);
        }

        @Override
        public boolean matches(Object object) {
            if (!(object instanceof MultiPointToSinglePointIntent)) {
                return false;
            }

            MultiPointToSinglePointIntent providedIntent =
                    (MultiPointToSinglePointIntent) object;
            providedIntentString = providedIntent.toString();

            MultiPointToSinglePointIntent matchIntent =
                    new MultiPointToSinglePointIntent(providedIntent.getId(),
                    intent.getMatch(), intent.getAction(), intent.getIngressPorts(),
                    intent.getEgressPort());

            return matchIntent.equals(providedIntent);
        }
    }

    /**
     * Matcher method to set an expected intent to match against (ignoring the
     * the intent ID).
     *
     * @param intent the expected intent
     * @return something of type MultiPointToSinglePointIntent
     */
    private static MultiPointToSinglePointIntent eqExceptId(
            MultiPointToSinglePointIntent intent) {
        reportMatcher(new IdAgnosticIntentMatcher(intent));
        return null;
    }

    /**
     * Tests adding a set of routes into SDN-IP.
     * <p/>
     * Random routes are generated and fed in to the SDN-IP route processing
     * logic (via processRibAdd). We check that the correct intents are
     * generated and submitted to our mock intent service.
     *
     * @throws InterruptedException if interrupted while waiting on a latch
     */
    @Test
    public void testAddRoutes() throws InterruptedException {
        int numRoutes = 100;

        final CountDownLatch latch = new CountDownLatch(numRoutes);

        List<RibUpdate> routeUpdates = generateRouteUpdateIntents(numRoutes);

        reset(intentService);

        // Set up expectations
        for (RibUpdate update : routeUpdates) {
            InetAddress nextHopPeer = update.getRibEntry().getNextHop();
            MultiPointToSinglePointIntent intent = getIntentForUpdate(update,
                    generateMacAddress(nextHopPeer),
                    interfaces.get(peers.get(nextHopPeer).getInterfaceName()));
            intentService.submit(eqExceptId(intent));
            expectLastCall().andAnswer(new IAnswer<Object>() {
                @Override
                public Object answer() throws Throwable {
                    latch.countDown();
                    return null;
                }
            }).once();
        }

        replay(intentService);

        // Add route updates
        for (RibUpdate update : routeUpdates) {
            sdnip.processRibAdd(update);
        }

        latch.await(5000, TimeUnit.MILLISECONDS);

        assertEquals(sdnip.getPtree().size(), numRoutes);

        verify(intentService);
    }

    /**
     * Tests adding then deleting a set of routes from SDN-IP.
     * <p/>
     * Random routes are generated and fed in to the SDN-IP route processing
     * logic (via processRibAdd), and we check that the correct intents are
     * generated. We then delete the entire set of routes (by feeding updates
     * to processRibDelete), and check that the correct intents are withdrawn
     * from the intent service.
     *
     * @throws InterruptedException if interrupted while waiting on a latch
     */
    @Test
    public void testDeleteRoutes() throws InterruptedException {
        int numRoutes = 100;
        List<RibUpdate> routeUpdates = generateRouteUpdateIntents(numRoutes);

        final CountDownLatch installCount = new CountDownLatch(numRoutes);
        final CountDownLatch deleteCount = new CountDownLatch(numRoutes);

        reset(intentService);

        for (RibUpdate update : routeUpdates) {
            InetAddress nextHopPeer = update.getRibEntry().getNextHop();
            MultiPointToSinglePointIntent intent = getIntentForUpdate(update,
                    generateMacAddress(nextHopPeer),
                    interfaces.get(peers.get(nextHopPeer).getInterfaceName()));
            intentService.submit(eqExceptId(intent));
            expectLastCall().andAnswer(new IAnswer<Object>() {
                @Override
                public Object answer() throws Throwable {
                    installCount.countDown();
                    return null;
                }
            }).once();
            intentService.withdraw(eqExceptId(intent));
            expectLastCall().andAnswer(new IAnswer<Object>() {
                @Override
                public Object answer() throws Throwable {
                    deleteCount.countDown();
                    return null;
                }
            }).once();
        }

        replay(intentService);


        // Send the add updates first
        for (RibUpdate update : routeUpdates) {
            sdnip.processRibAdd(update);
        }

        // Give some time to let the intents be submitted
        installCount.await(5000, TimeUnit.MILLISECONDS);

        // Send the DELETE updates
        for (RibUpdate update : routeUpdates) {
            RibUpdate deleteUpdate = new RibUpdate(Operation.DELETE,
                    update.getPrefix(), update.getRibEntry());
            sdnip.processRibDelete(deleteUpdate);
        }

        deleteCount.await(5000, TimeUnit.MILLISECONDS);

        assertEquals(0, sdnip.getPtree().size());

        verify(intentService);
    }

    /**
     * Generates a set of route updates. The prefix for each route is randomly
     * generated, and the next hop is selected from the set of BGP peers that
     * was generated during {@link #setUp()}. All have the UPDATE operation.
     * The generated prefixes are unique within the batch generated by each
     * call of this method.
     *
     * @param numRoutes the number of route updates to generate
     * @return a list of generated route updates
     */
    private List<RibUpdate> generateRouteUpdateIntents(int numRoutes) {
        List<RibUpdate> routeUpdates = new ArrayList<>(numRoutes);

        Set<Prefix> prefixes = new HashSet<>();

        for (int i = 0; i < numRoutes; i++) {
            Prefix prefix;
            do {
                InetAddress prefixAddress = InetAddresses.fromInteger(random.nextInt());
                // Generate a random prefix length between MIN_PREFIX_LENGTH and
                // MAX_PREFIX_LENGTH
                int prefixLength = random.nextInt(
                        (MAX_PREFIX_LENGTH - MIN_PREFIX_LENGTH) + 1) + MIN_PREFIX_LENGTH;
                prefix = new Prefix(prefixAddress.getAddress(), prefixLength);
                // We have to ensure we don't generate the same prefix twice
                // (this is quite easy to do with small prefix lengths)
            } while (prefixes.contains(prefix));

            prefixes.add(prefix);

            // Randomly select a peer to use as the next hop
            BgpPeer nextHop = null;
            int peerNumber = random.nextInt(peers.size());
            int j = 0;
            for (BgpPeer peer : peers.values()) {
                if (j++ == peerNumber) {
                    nextHop = peer;
                    break;
                }
            }

            assertNotNull(nextHop);

            RibUpdate update = new RibUpdate(Operation.UPDATE, prefix,
                    new RibEntry(ROUTER_ID, nextHop.getIpAddress()));

            routeUpdates.add(update);
        }

        return routeUpdates;
    }

    /**
     * Generates the MultiPointToSinglePointIntent that should be
     * submitted/withdrawn for a particular RibUpdate.
     *
     * @param update the RibUpdate to generate an intent for
     * @param nextHopMac a MAC address to use as the dst-mac for the intent
     * @param egressInterface the outgoing interface for the intent
     * @return the generated intent
     */
    private MultiPointToSinglePointIntent getIntentForUpdate(RibUpdate update,
            MACAddress nextHopMac, Interface egressInterface) {
        Prefix prefix = update.getPrefix();

        PacketMatchBuilder builder = new PacketMatchBuilder();
        builder.setDstIp(new IPv4(
                InetAddresses.coerceToInteger(prefix.getInetAddress())),
                (short) prefix.getPrefixLength());
        PacketMatch match = builder.build();

        ModifyDstMacAction action = new ModifyDstMacAction(nextHopMac);

        Set<SwitchPort> ingressPorts = new HashSet<>();
        for (Interface intf : interfaces.values()) {
            if (!intf.equals(egressInterface)) {
                ingressPorts.add(intf.getSwitchPort());
            }
        }

        // Create the intent. The intent ID is arbitrary because we don't consider
        // it when matching against the intent passed to the mock
        MultiPointToSinglePointIntent intent = new MultiPointToSinglePointIntent(
                new IntentId(0), match, action, ingressPorts,
                egressInterface.getSwitchPort());

        return intent;
    }

    /**
     * Generates a MAC address based on an IP address.
     * For the test we need MAC addresses but the actual values don't have any
     * meaning, so we'll just generate them based on the IP address. This means
     * we have a deterministic mapping from IP address to MAC address.
     *
     * @param ipAddress IP address used to generate a MAC address
     * @return generated MAC address
     */
    public static MACAddress generateMacAddress(InetAddress ipAddress) {
        byte[] macAddress = new byte[MAC_ADDRESS_LENGTH];
        ByteBuffer bb = ByteBuffer.wrap(macAddress);

        // Put the IP address bytes into the lower four bytes of the MAC address.
        // Leave the first two bytes set to 0.
        bb.position(2);
        bb.put(ipAddress.getAddress());

        return MACAddress.valueOf(bb.array());
    }

}
