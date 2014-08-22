package net.onrc.onos.apps.sdnip;

import java.net.InetAddress;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.apps.proxyarp.IArpRequester;
import net.onrc.onos.apps.proxyarp.IProxyArpService;

/**
 * Test version of the IProxyArpService which is used to simulate delays in
 * receiving ARP replies, as you would see in a real system due to the time
 * it takes to proxy ARP packets to/from the host. Requests are asynchronous,
 * and replies may come back to the requestor in a different order than the
 * requests were sent, which again you would expect to see in a real system.
 */
public class TestProxyArpService implements IProxyArpService {

    /**
     * The maximum possible delay before an ARP reply is received.
     */
    private static final int MAX_ARP_REPLY_DELAY = 30; // milliseconds

    /**
     * The probability that we already have the MAC address cached when the
     * caller calls {@link #getMacAddress(InetAddress)}.
     */
    private static final float MAC_ALREADY_KNOWN_PROBABILITY = 0.3f;

    private final ScheduledExecutorService replyTaskExecutor;

    private final Random random;

    /**
     * Class constructor.
     */
    public TestProxyArpService() {
        replyTaskExecutor = Executors.newSingleThreadScheduledExecutor();
        random = new Random();
    }

    /**
     * Task used to reply to ARP requests from a different thread. Replies
     * usually come on a different thread in the real system, so we need to
     * ensure we test this behaviour.
     */
    private static class ReplyTask implements Runnable {
        private IArpRequester requestor;
        private InetAddress ipAddress;
        private MACAddress macAddress;

        /**
         * Class constructor.
         *
         * @param requestor the client who requested the MAC address
         * @param ipAddress the target IP address of the request
         * @param macAddress the MAC address in the ARP reply
         */
        public ReplyTask(IArpRequester requestor, InetAddress ipAddress,
                MACAddress macAddress) {
            this.requestor = requestor;
            this.ipAddress = ipAddress;
            this.macAddress = macAddress;
        }

        @Override
        public void run() {
            requestor.arpResponse(ipAddress, macAddress);
        }
    }

    @Override
    public MACAddress getMacAddress(InetAddress ipAddress) {
        float replyChance = random.nextFloat();
        if (replyChance < MAC_ALREADY_KNOWN_PROBABILITY) {
            // Some percentage of the time we already know the MAC address, so
            // we reply directly when the requestor asks for the MAC address
            return SdnIpTest.generateMacAddress(ipAddress);
        }
        return null;
    }

    @Override
    public void sendArpRequest(InetAddress ipAddress, IArpRequester requester,
            boolean retry) {
        // Randomly select an amount of time to delay the reply coming back to
        // the requestor (simulating time taken to proxy the request to a
        // network host).
        int delay = random.nextInt(MAX_ARP_REPLY_DELAY);

        MACAddress macAddress = SdnIpTest.generateMacAddress(ipAddress);

        ReplyTask replyTask = new ReplyTask(requester, ipAddress, macAddress);

        replyTaskExecutor.schedule(replyTask , delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public List<String> getMappings() {
        // We don't care about this method for the current test use cases
        return null;
    }

}
